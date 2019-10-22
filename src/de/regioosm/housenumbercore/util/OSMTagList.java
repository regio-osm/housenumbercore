package de.regioosm.housenumbercore.util;

import java.util.ArrayList;
import java.util.List;

public class OSMTagList {
	protected List<OSMTag> tags = new ArrayList<>();
	
	public OSMTagList() {
		
	}
	
	public OSMTagList(OSMTag tag) {
		this.tags.add(tag);
	}
	
	public void add(OSMTag tag) {
		this.tags.add(tag);
	}

	public void add(String key, String value) {
		OSMTag tag = new OSMTag(key, value);
		this.tags.add(tag);
	}
	
	public List<OSMTag> getTags() {
		return tags;
	}

	public OSMTag getTag( String searchkey ) {
		for( int listindex = 0; listindex < tags.size(); listindex++ ) {
			if ( tags.get(listindex).equals(searchkey) )
				return tags.get(listindex);
		}
		return null;
	}

	public String getKey( int index ) {
		if ( index < tags.size() )
			return tags.get(index).getKey();
		return null;
	}

	public String getValue( int index ) {
		if ( index < tags.size() )
			return tags.get(index).getValue();
		return null;
	}

	public String getValue( String searchkey ) {
		for( int listindex = 0; listindex < tags.size(); listindex++ ) {
			if ( tags.get(listindex).equals(searchkey) )
				return tags.get(listindex).getValue();
		}
		return null;
	}

	public boolean hasKey( String searchkey ) {
		for( int listindex = 0; listindex < tags.size(); listindex++ ) {
			if ( tags.get(listindex).equals(searchkey) )
				return true;
		}
		return false;
	}
	
	public int size() {
		return tags.size();
	}

}
