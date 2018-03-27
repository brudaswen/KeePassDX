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

import com.keepassdroid.database.iterator.EntrySearchStringIterator;
import com.keepassdroid.database.security.ProtectedString;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class PwEntry extends PwNode implements Cloneable {

	private static final String PMS_TAN_ENTRY = "<TAN>";

	protected UUID uuid = PwDatabase.UUID_ZERO;

	@Override
	protected void construct() {
	    super.construct();
        uuid = UUID.randomUUID();
    }

	public static PwEntry getInstance(PwGroup parent) {
		if (parent instanceof PwGroupV3) {
			return new PwEntryV3((PwGroupV3)parent);
		}
		else if (parent instanceof PwGroupV4) {
			return new PwEntryV4((PwGroupV4)parent);
		}
		else {
			throw new RuntimeException("Unknown PwGroup instance.");
		}
	}

	@Override
	public PwEntry clone() {
		PwEntry newEntry;
		try {
			newEntry = (PwEntry) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException("Clone should be supported");
		}
		return newEntry;
	}

	@Override
    protected void addCloneAttributesToNewEntry(PwEntry newEntry) {
	    super.addCloneAttributesToNewEntry(newEntry);
	    // uuid is clone automatically (IMMUTABLE)
    }

	@Override
	public Type getType() {
		return Type.ENTRY;
	}

    public void assign(PwEntry source) {
	    super.assign(source);
        uuid = source.uuid;
	}

    public UUID getUUID() {
        return uuid;
    }

    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

	public void startToDecodeReference(PwDatabase db) {}
	public void endToDecodeReference(PwDatabase db) {}

	public abstract String getTitle();
    public abstract void setTitle(String title);

	public abstract String getUsername();
    public abstract void setUsername(String user);

	public abstract String getPassword();
    public abstract void setPassword(String pass);

	public abstract String getUrl();
    public abstract void setUrl(String url);

	public abstract String getNotes();
	public abstract void setNotes(String notes);
	
	private boolean isTan() {
		return getTitle().equals(PMS_TAN_ENTRY) && (getUsername().length() > 0);
	}

	@Override
	public String getDisplayTitle() {
		if ( isTan() ) {
			return PMS_TAN_ENTRY + " " + getUsername();
		} else {
			return getTitle();
		}
	}

	// TODO encapsulate extra fields

    /**
     * To redefine if version of entry allow extra field,
     * @return true if entry allows extra field
     */
	public boolean allowExtraFields() {
		return false;
	}

    /**
     * Retrieve extra fields to show, key is the label, value is the value of field
     * @return Map of label/value
     */
	public Map<String, String> getExtraFields() {
		return new HashMap<>();
	}

    /**
     * Retrieve extra protected fields to show, key is the label, value is the value protected of field
     * @return Map of label/value
     */
    public Map<String, ProtectedString> getExtraProtectedFields() {
        return new HashMap<>();
    }

	/**
	 * If entry contains extra fields
	 * @return true if there is extra fields
	 */
	public boolean containsExtraFields() {
		return !getExtraProtectedFields().keySet().isEmpty();
	}

    /**
     * Add an extra field to the list
     * @param label Label of field, must be unique
     * @param value Value of field
     */
    public void addField(String label, ProtectedString value) {}

    /**
     * Delete all extra fields
     */
    public void removeExtraFields() {}

	/**
	 * If it's a node with only meta information like Meta-info SYSTEM Database Color
	 * @return false by default, true if it's a meta stream
	 */
	public boolean isMetaStream() {
		return false;
	}

    /**
     * Create a backup of entry
     */
    public void createBackup(PwDatabase db) {}

	public EntrySearchStringIterator stringIterator() {
		return EntrySearchStringIterator.getInstance(this);
	}
	
	public void touch(boolean modified, boolean touchParents) {
		PwDate now = new PwDate();

		setLastAccessTime(now);
		
		if (modified) {
			setLastModificationTime(now);
		}
		
		PwGroup parent = getParent();
		if (touchParents && parent != null) {
			parent.touch(modified, true);
		}
	}
	
	public void touchLocation() { }

	public boolean isSearchingEnabled() {
		return false;
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PwEntry pwEntry = (PwEntry) o;
        return isSameType(pwEntry)
                && (getUUID() != null ? getUUID().equals(pwEntry.getUUID()) : pwEntry.getUUID() == null);
    }

    @Override
    public int hashCode() {
        return getUUID() != null ? getUUID().hashCode() : 0;
    }
}
