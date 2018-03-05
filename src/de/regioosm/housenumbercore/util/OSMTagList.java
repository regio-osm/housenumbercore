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
}
