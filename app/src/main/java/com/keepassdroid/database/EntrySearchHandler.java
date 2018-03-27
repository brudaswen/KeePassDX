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

import java.util.Date;
import java.util.List;

public abstract class EntrySearchHandler extends EntryHandler<PwEntry> {
	private List<PwEntry> listStorage;
	private SearchParameters sp;
	private Date now;
	
	public static EntrySearchHandler getInstance(PwGroup group, SearchParameters sp, List<PwEntry> listStorage) {
		if (group instanceof PwGroupV3) { // TODO WTF ?
            return new EntrySearchHandlerV4(sp, listStorage);
		} else if (group instanceof PwGroupV4) {
			return new EntrySearchHandlerV4(sp, listStorage);
		} else {
			throw new RuntimeException("Not implemented.");
		}
	}

	protected EntrySearchHandler(SearchParameters sp, List<PwEntry> listStorage) {
		this.sp = sp;
		this.listStorage = listStorage;
		now = new Date();
	}

	@Override
	public boolean operate(PwEntry entry) {
		if (sp.respectEntrySearchingDisabled && !entry.isSearchingEnabled()) {
			return true;
		}
		
		if (sp.excludeExpired && entry.expires() && now.after(entry.getExpiryTime().getDate())) {
			return true;
		}
		
		String term = sp.searchString;
		if (sp.ignoreCase) {
			term = term.toLowerCase();
		}
		
		if (searchStrings(entry, term)) { 
            listStorage.add(entry);
			return true; 
        }
		
		if (sp.searchInGroupNames) {
			PwGroup parent = entry.getParent();
			if (parent != null) {
                String groupName = parent.getName();
                if (groupName != null) {
                	if (sp.ignoreCase) {
                		groupName = groupName.toLowerCase();
                	}

                	if (groupName.contains(term)) {
                        listStorage.add(entry);
                        return true;
                	}
                }
			}
		}
		
		if (searchID(entry)) {
            listStorage.add(entry);
            return true;
		}
		
		return true;
	}
	
	protected boolean searchID(PwEntry entry) {
		return false;
	}
	
	private boolean searchStrings(PwEntry entry, String term) {
		EntrySearchStringIterator iter = EntrySearchStringIterator.getInstance(entry, sp);
		while (iter.hasNext()) {
			String str = iter.next();
			if (str != null && str.length() > 0) {
				if (sp.ignoreCase) {
					str = str.toLowerCase();
				}
				
				if (str.contains(term)) {
					return true;
				}
			}
		}
		
		return false;
	}
}
