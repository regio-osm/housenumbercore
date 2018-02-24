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
}
