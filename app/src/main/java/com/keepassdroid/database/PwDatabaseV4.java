/*
 * Copyright 2017 Brian Pellin, Jeremy Jamet / Kunzisoft.
 *     
 * This file is part of KeePass DX.
 *
 *  KeePass DX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  KeePass DX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with KeePass DX.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.keepassdroid.database;

import android.webkit.URLUtil;

import com.keepassdroid.collections.VariantDictionary;
import com.keepassdroid.crypto.CryptoUtil;
import com.keepassdroid.crypto.engine.AesEngine;
import com.keepassdroid.crypto.engine.CipherEngine;
import com.keepassdroid.crypto.keyDerivation.AesKdf;
import com.keepassdroid.crypto.keyDerivation.KdfEngine;
import com.keepassdroid.crypto.keyDerivation.KdfFactory;
import com.keepassdroid.crypto.keyDerivation.KdfParameters;
import com.keepassdroid.database.exception.InvalidKeyFileException;
import com.keepassdroid.utils.EmptyUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import biz.source_code.base64Coder.Base64Coder;


public class PwDatabaseV4 extends PwDatabase {

	public static final int DEFAULT_ROUNDS = 6000;
	private static final int DEFAULT_HISTORY_MAX_ITEMS = 10; // -1 unlimited
	private static final long DEFAULT_HISTORY_MAX_SIZE = 6 * 1024 * 1024; // -1 unlimited
	private static final String RECYCLEBIN_NAME = "RecycleBin";

	private byte[] hmacKey;
	private UUID dataCipher = AesEngine.CIPHER_UUID;
	private CipherEngine dataEngine = new AesEngine();
	private PwCompressionAlgorithm compressionAlgorithm = PwCompressionAlgorithm.Gzip;

	// TODO: Refactor me away to get directly from kdfParameters
    private long numKeyEncRounds = 6000;
    private PwDate nameChanged = new PwDate();
    private PwDate settingsChanged = new PwDate();
    private String description = "";
    private PwDate descriptionChanged = new PwDate();
    private String defaultUserName = "";
    private PwDate defaultUserNameChanged = new PwDate();
    
    private PwDate keyLastChanged = new PwDate();
    private long keyChangeRecDays = -1;
    private long keyChangeForceDays = 1;
	private boolean keyChangeForceOnce = false;
    
    private long maintenanceHistoryDays = 365;
    private String color = "";
    private boolean recycleBinEnabled = true;
    private UUID recycleBinUUID = UUID_ZERO;
    private Date recycleBinChanged = new Date();
    private UUID entryTemplatesGroup = UUID_ZERO;
    private PwDate entryTemplatesGroupChanged = new PwDate();
    private int historyMaxItems = DEFAULT_HISTORY_MAX_ITEMS;
    private long historyMaxSize = DEFAULT_HISTORY_MAX_SIZE;
    private UUID lastSelectedGroup = UUID_ZERO;
    private UUID lastTopVisibleGroup = UUID_ZERO;
    private MemoryProtectionConfig memoryProtection = new MemoryProtectionConfig();
    private List<PwDeletedObject> deletedObjects = new ArrayList<>();
    private List<PwIconCustom> customIcons = new ArrayList<>();
    private Map<String, String> customData = new HashMap<>();

	private KdfParameters kdfParameters = KdfFactory.getDefaultParameters();
	private VariantDictionary publicCustomData = new VariantDictionary();
	private BinaryPool binPool = new BinaryPool();

    public String localizedAppName = "KeePassDX"; // TODO resource

    public byte[] getHmacKey() {
        return hmacKey;
    }

    public UUID getDataCipher() {
        return dataCipher;
    }

    public void setDataCipher(UUID dataCipher) {
        this.dataCipher = dataCipher;
    }

    public void setDataEngine(CipherEngine dataEngine) {
        this.dataEngine = dataEngine;
    }

    public PwCompressionAlgorithm getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    public void setCompressionAlgorithm(PwCompressionAlgorithm compressionAlgorithm) {
        this.compressionAlgorithm = compressionAlgorithm;
    }

    public long getNumKeyEncRounds() {
        return numKeyEncRounds;
    }

    public void setNumKeyEncRounds(long numKeyEncRounds) {
        this.numKeyEncRounds = numKeyEncRounds;
    }

    public PwDate getNameChanged() {
        return nameChanged;
    }

    public void setNameChanged(PwDate nameChanged) {
        this.nameChanged = nameChanged;
    }

    public PwDate getSettingsChanged() {
        return settingsChanged; // TODO change setting date
    }

    public void setSettingsChanged(PwDate settingsChanged) {
        this.settingsChanged = settingsChanged;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PwDate getDescriptionChanged() {
        return descriptionChanged;
    }

    public void setDescriptionChanged(PwDate descriptionChanged) {
        this.descriptionChanged = descriptionChanged;
    }

    public String getDefaultUserName() {
        return defaultUserName;
    }

    public void setDefaultUserName(String defaultUserName) {
        this.defaultUserName = defaultUserName;
    }

    public PwDate getDefaultUserNameChanged() {
        return defaultUserNameChanged;
    }

    public void setDefaultUserNameChanged(PwDate defaultUserNameChanged) {
        this.defaultUserNameChanged = defaultUserNameChanged;
    }

    public PwDate getKeyLastChanged() {
        return keyLastChanged;
    }

    public void setKeyLastChanged(PwDate keyLastChanged) {
        this.keyLastChanged = keyLastChanged;
    }

    public long getKeyChangeRecDays() {
        return keyChangeRecDays;
    }

    public void setKeyChangeRecDays(long keyChangeRecDays) {
        this.keyChangeRecDays = keyChangeRecDays;
    }

    public long getKeyChangeForceDays() {
        return keyChangeForceDays;
    }

    public void setKeyChangeForceDays(long keyChangeForceDays) {
        this.keyChangeForceDays = keyChangeForceDays;
    }

    public boolean isKeyChangeForceOnce() {
        return keyChangeForceOnce;
    }

    public void setKeyChangeForceOnce(boolean keyChangeForceOnce) {
        this.keyChangeForceOnce = keyChangeForceOnce;
    }

    public long getMaintenanceHistoryDays() {
        return maintenanceHistoryDays;
    }

    public void setMaintenanceHistoryDays(long maintenanceHistoryDays) {
        this.maintenanceHistoryDays = maintenanceHistoryDays;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public UUID getEntryTemplatesGroup() {
        return entryTemplatesGroup;
    }

    public void setEntryTemplatesGroup(UUID entryTemplatesGroup) {
        this.entryTemplatesGroup = entryTemplatesGroup;
    }

    public PwDate getEntryTemplatesGroupChanged() {
        return entryTemplatesGroupChanged;
    }

    public void setEntryTemplatesGroupChanged(PwDate entryTemplatesGroupChanged) {
        this.entryTemplatesGroupChanged = entryTemplatesGroupChanged;
    }

    public int getHistoryMaxItems() {
        return historyMaxItems;
    }

    public void setHistoryMaxItems(int historyMaxItems) {
        this.historyMaxItems = historyMaxItems;
    }

    public long getHistoryMaxSize() {
        return historyMaxSize;
    }

    public void setHistoryMaxSize(long historyMaxSize) {
        this.historyMaxSize = historyMaxSize;
    }

    public UUID getLastSelectedGroup() {
        return lastSelectedGroup;
    }

    public void setLastSelectedGroup(UUID lastSelectedGroup) {
        this.lastSelectedGroup = lastSelectedGroup;
    }

    public UUID getLastTopVisibleGroup() {
        return lastTopVisibleGroup;
    }

    public void setLastTopVisibleGroup(UUID lastTopVisibleGroup) {
        this.lastTopVisibleGroup = lastTopVisibleGroup;
    }

    public MemoryProtectionConfig getMemoryProtection() {
        return memoryProtection;
    }

    public void setMemoryProtection(MemoryProtectionConfig memoryProtection) {
        this.memoryProtection = memoryProtection;
    }

    public List<PwIconCustom> getCustomIcons() {
        return customIcons;
    }

    public void addCustomIcon(PwIconCustom customIcon) {
        this.customIcons.add(customIcon);
    }

    public Map<String, String> getCustomData() {
        return customData;
    }

    public void putCustomData(String label, String value) {
        this.customData.put(label, value);
    }

    @Override
	public byte[] getMasterKey(String key, InputStream keyInputStream)
			throws InvalidKeyFileException, IOException {
		assert(key != null);
		
		byte[] fKey = new byte[]{};
		
		if ( key.length() > 0 && keyInputStream != null) {
			return getCompositeKey(key, keyInputStream);
		} else if ( key.length() > 0 ) {
			fKey =  getPasswordKey(key);
		} else if ( keyInputStream != null) {
			fKey = getFileKey(keyInputStream);
		}
		
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-256 implementation");
		}
		
		return md.digest(fKey);
	}

	@Override
	public void makeFinalKey(byte[] masterSeed, byte[] masterSeed2, int numRounds) throws IOException {

		byte[] transformedMasterKey = transformMasterKey(masterSeed2, masterKey, numRounds);


		byte[] cmpKey = new byte[65];
		System.arraycopy(masterSeed, 0, cmpKey, 0, 32);
		System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32);
		finalKey = CryptoUtil.resizeKey(cmpKey, 0, 64, dataEngine.keyLength());

		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			cmpKey[64] = 1;
			hmacKey = md.digest(cmpKey);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-512 implementation");
		} finally {
			Arrays.fill(cmpKey, (byte)0);
		}
	}
	public void makeFinalKey(byte[] masterSeed, KdfParameters kdfP) throws IOException {
    	makeFinalKey(masterSeed, kdfP, 0);
	}

	public void makeFinalKey(byte[] masterSeed, KdfParameters kdfP, long roundsFix)
			throws IOException {

		KdfEngine kdfEngine = KdfFactory.get(kdfP.kdfUUID);
		if (kdfEngine == null) {
			throw new IOException("Unknown key derivation function");
		}

		// Set to 6000 rounds to open corrupted database
		if (roundsFix > 0 && kdfP.kdfUUID.equals(AesKdf.CIPHER_UUID)) {
			kdfP.setUInt32(AesKdf.ParamRounds, roundsFix);
			numKeyEncRounds = roundsFix;
		}

		byte[] transformedMasterKey = kdfEngine.transform(masterKey, kdfP);
		if (transformedMasterKey.length != 32) {
			transformedMasterKey = CryptoUtil.hashSha256(transformedMasterKey);
		}

        byte[] cmpKey = new byte[65];
		System.arraycopy(masterSeed, 0, cmpKey, 0, 32);
        System.arraycopy(transformedMasterKey, 0, cmpKey, 32, 32);
		finalKey = CryptoUtil.resizeKey(cmpKey, 0, 64, dataEngine.keyLength());

        MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-512");
			cmpKey[64] = 1;
			hmacKey = md.digest(cmpKey);
		} catch (NoSuchAlgorithmException e) {
			throw new IOException("No SHA-512 implementation");
		} finally {
			Arrays.fill(cmpKey, (byte)0);
		}
	}

	@Override
	protected String getPasswordEncoding() {
		return "UTF-8";
	}
    
	private static final String RootElementName = "KeyFile";
	//private static final String MetaElementName = "Meta";
	//private static final String VersionElementName = "Version";
	private static final String KeyElementName = "Key";
	private static final String KeyDataElementName = "Data";
	
	@Override
	protected byte[] loadXmlKeyFile(InputStream keyInputStream) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(keyInputStream);
			
			Element el = doc.getDocumentElement();
			if (el == null || ! el.getNodeName().equalsIgnoreCase(RootElementName)) {
				return null;
			}
			
			NodeList children = el.getChildNodes();
			if (children.getLength() < 2) {
				return null;
			}
			
			for ( int i = 0; i < children.getLength(); i++ ) {
				Node child = children.item(i);
				
				if ( child.getNodeName().equalsIgnoreCase(KeyElementName) ) {
					NodeList keyChildren = child.getChildNodes();
					for ( int j = 0; j < keyChildren.getLength(); j++ ) {
						Node keyChild = keyChildren.item(j);
						if ( keyChild.getNodeName().equalsIgnoreCase(KeyDataElementName) ) {
							NodeList children2 = keyChild.getChildNodes();
							for ( int k = 0; k < children2.getLength(); k++) {
								Node text = children2.item(k);
								if (text.getNodeType() == Node.TEXT_NODE) {
									Text txt = (Text) text;
									return Base64Coder.decode(txt.getNodeValue());
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public List<PwGroup> getGroups() {
		List<PwGroup> list = new ArrayList<>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
        buildChildGroupsRecursive(root, list);
		
		return list;
	}

	private static void buildChildGroupsRecursive(PwGroupV4 root, List<PwGroup> list) {
		list.add(root);
		for ( int i = 0; i < root.numbersOfChildGroups(); i++) {
			PwGroupV4 child = (PwGroupV4) root.getChildGroupAt(i);
			buildChildGroupsRecursive(child, list);
		}
	}

	@Override
	public List<PwGroup> getGrpRoots() {
		return rootGroup.getChildGroups();
	}

	@Override
	public List<PwEntry> getEntries() {
		List<PwEntry> list = new ArrayList<>();
		PwGroupV4 root = (PwGroupV4) rootGroup;
		buildChildEntriesRecursive(root, list);
		return list;
	}

    private static void buildChildEntriesRecursive(PwGroupV4 root, List<PwEntry> list) {
        for ( int i = 0; i < root.numbersOfChildEntries(); i++ ) {
            list.add(root.getChildEntryAt(i));
        }
        for ( int i = 0; i < root.numbersOfChildGroups(); i++ ) {
            PwGroupV4 child = (PwGroupV4) root.getChildGroupAt(i);
            buildChildEntriesRecursive(child, list);
        }
    }

	@Override
	public long getNumRounds() {
		return numKeyEncRounds;
	}

	@Override
	public void setNumRounds(long rounds) throws NumberFormatException {
		numKeyEncRounds = rounds;
		
	}

	@Override
	public boolean algorithmSettingsEnabled() {
		return false;
	}

	@Override
	public PwEncryptionAlgorithm getEncryptionAlgorithm() {
		return PwEncryptionAlgorithm.Rjindal;
	}

	@Override
	public PwGroupIdV4 newGroupId() {
		PwGroupIdV4 id = new PwGroupIdV4(UUID_ZERO);
		
		while (true) {
			id = new PwGroupIdV4(UUID.randomUUID());
			
			if (!isGroupIdUsed(id)) break;
		}
		
		return id;
	}

	@Override
	public PwGroup createGroup() {
		return new PwGroupV4();
	}

	@Override
	public boolean isBackup(PwGroup group) {
		if (!recycleBinEnabled) {
			return false;
		}
		
		return group.isContainedIn(getRecycleBin());
	}

	@Override
	public void populateGlobals(PwGroup currentGroup) {
		groups.put(rootGroup.getId(), rootGroup);
		
		super.populateGlobals(currentGroup);
	}
	
	/** Ensure that the recycle bin tree exists, if enabled and create it
	 *  if it doesn't exist 
	 *  
	 */
	private void ensureRecycleBin() {
		if (getRecycleBin() == null) {
			// Create recycle bin
				
			PwGroupV4 recycleBin = new PwGroupV4(RECYCLEBIN_NAME, iconFactory.getIcon(PwIconStandard.TRASH_BIN));
			recycleBin.setEnableAutoType(false);
			recycleBin.setEnableSearching(false);
			recycleBin.setExpanded(false);
			addGroupTo(recycleBin, rootGroup);
			
			recycleBinUUID = recycleBin.getUUID();
		}
	}

    public UUID getRecycleBinUUID() {
        return recycleBinUUID;
    }

    public void setRecycleBinUUID(UUID recycleBinUUID) {
        this.recycleBinUUID = recycleBinUUID;
    }

    @Override
    public boolean isRecycleBinAvailable() {
        return true;
    }

    @Override
	public boolean isRecycleBinEnabled() {
		return recycleBinEnabled;
	}

	public void setRecycleBinEnabled(boolean recycleBinEnabled) {
	    this.recycleBinEnabled = recycleBinEnabled;
    }

    public Date getRecycleBinChanged() {
        return recycleBinChanged;
    }

    public void setRecycleBinChanged(Date recycleBinChanged) {
        this.recycleBinChanged = recycleBinChanged;
    }

    @Override
	public boolean canRecycle(PwGroup group) {
		if (!recycleBinEnabled) {
			return false;
		}
		PwGroup recycle = getRecycleBin();
		return (recycle == null) || (!group.isContainedIn(recycle));
	}

	@Override
	public boolean canRecycle(PwEntry entry) {
		if (!recycleBinEnabled) {
			return false;
		}
		PwGroup parent = entry.getParent();
		return (parent != null) && canRecycle(parent);
	}

	@Override
	public void recycle(PwGroup group) {
		ensureRecycleBin();

		PwGroup parent = group.getParent();
		removeGroupFrom(group, parent);
		parent.touch(false, true);

		PwGroup recycleBin = getRecycleBin();
		addGroupTo(group, recycleBin);

        group.touch(false, true);
        // TODO ? group.touchLocation();
	}

	@Override
	public void recycle(PwEntry entry) {
		ensureRecycleBin();
		
		PwGroup parent = entry.getParent();
		removeEntryFrom(entry, parent);
		parent.touch(false, true);
		
		PwGroup recycleBin = getRecycleBin();
		addEntryTo(entry, recycleBin);
		
		entry.touch(false, true);
		entry.touchLocation();
	}

    @Override
    public void undoRecycle(PwGroup group, PwGroup origParent) {

        PwGroup recycleBin = getRecycleBin();
        removeGroupFrom(group, recycleBin);

        addGroupTo(group, origParent);
    }

	@Override
	public void undoRecycle(PwEntry entry, PwGroup origParent) {
		
		PwGroup recycleBin = getRecycleBin();
		removeEntryFrom(entry, recycleBin);
		
		addEntryTo(entry, origParent);
	}

    public List<PwDeletedObject> getDeletedObjects() {
        return deletedObjects;
    }

    public void addDeletedObject(PwDeletedObject deletedObject) {
        this.deletedObjects.add(deletedObject);
    }

    @Override
	public void deleteEntry(PwEntry entry) {
		super.deleteEntry(entry);
		deletedObjects.add(new PwDeletedObject(entry.getUUID()));
	}

	@Override
	public void undoDeleteEntry(PwEntry entry, PwGroup origParent) {
		super.undoDeleteEntry(entry, origParent);
		// TODO undo delete entry
        deletedObjects.remove(entry);
	}

	@Override
	public PwGroupV4 getRecycleBin() { // TODO delete recycle bin preference
		if (recycleBinUUID == null) {
			return null;
		}
		
		PwGroupId recycleId = new PwGroupIdV4(recycleBinUUID);
		return (PwGroupV4) groups.get(recycleId);
	}

    public KdfParameters getKdfParameters() {
        return kdfParameters;
    }

    public void setKdfParameters(KdfParameters kdfParameters) {
        this.kdfParameters = kdfParameters;
    }

    public VariantDictionary getPublicCustomData() {
        return publicCustomData;
    }

    public boolean containsPublicCustomData() {
	    return publicCustomData.size() > 0;
    }

    public void setPublicCustomData(VariantDictionary publicCustomData) {
        this.publicCustomData = publicCustomData;
    }

    public BinaryPool getBinPool() {
        return binPool;
    }

    public void setBinPool(BinaryPool binPool) {
        this.binPool = binPool;
    }

    @Override
	public boolean isGroupSearchable(PwGroup group, boolean omitBackup) {
		if (!super.isGroupSearchable(group, omitBackup)) {
			return false;
		}
		
		PwGroupV4 g = (PwGroupV4) group;
		
		return g.isSearchingEnabled();
	}

	@Override
	public boolean validatePasswordEncoding(String key) {
		return true;
	}

	@Override
	public void initNew(String dbPath) {
		String filename = URLUtil.guessFileName(dbPath, null, null);
		
		rootGroup = new PwGroupV4(dbNameFromPath(dbPath), iconFactory.getIcon(PwIconStandard.FOLDER));
		groups.put(rootGroup.getId(), rootGroup);
	}
	
	private String dbNameFromPath(String dbPath) {
		String filename = URLUtil.guessFileName(dbPath, null, null);
		
		if (EmptyUtils.isNullOrEmpty(filename)) {
			return "KeePass Database";
		}
		int lastExtDot = filename.lastIndexOf(".");
		if (lastExtDot == -1) {
			return filename;
		}
		
		return filename.substring(0, lastExtDot);
	}

	private class GroupHasCustomData extends GroupHandler<PwGroup> {

		public boolean hasCustomData = false;

		@Override
		public boolean operate(PwGroup group) {
            if (group == null) {
				return true;
			}
			PwGroupV4 g4 = (PwGroupV4) group;
			if (g4.containsCustomData()) {
				hasCustomData = true;
				return false;
			}

			return true;
		}
	}

	private class EntryHasCustomData extends EntryHandler<PwEntry> {

        public boolean hasCustomData = false;

		@Override
		public boolean operate(PwEntry entry) {
            if (entry == null) {
				return true;
			}

			PwEntryV4 e4 = (PwEntryV4)entry;
			if (e4.containsCustomData()) {
				hasCustomData = true;
				return false;
			}

			return true;
		}
	}

	public int getMinKdbxVersion() {
		if (!AesKdf.CIPHER_UUID.equals(kdfParameters.kdfUUID)) {
			return PwDbHeaderV4.FILE_VERSION_32;
		}

		if (publicCustomData.size() > 0) {
			return PwDbHeaderV4.FILE_VERSION_32;
		}

		EntryHasCustomData entryHandler = new EntryHasCustomData();
		GroupHasCustomData groupHandler = new GroupHasCustomData();

		if (rootGroup == null ) {
			return PwDbHeaderV4.FILE_VERSION_32_3;
		}
        rootGroup.preOrderTraverseTree(groupHandler, entryHandler);
        if (groupHandler.hasCustomData || entryHandler.hasCustomData) {
			return PwDbHeaderV4.FILE_VERSION_32;
		}

		return PwDbHeaderV4.FILE_VERSION_32_3;
	}

}