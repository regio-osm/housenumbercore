package de.regioosm.housenumbercore.util;

import java.util.HashMap;
import java.util.Map;

public class OSMSegment {
	public enum OSMType {node, way, relation};

	protected Long osmId = 0L;
	protected OSMType osmType = null;
//	protected Map<String, String> metatags = new HashMap<>();
	protected OSMTagList tags = null;
	protected String geometryWKT = null;
	
	public OSMSegment() {
		this.osmId = 0L;
		this.osmType = null;
		this.geometryWKT = null;
		this.tags = null;
	}

	public OSMSegment(OSMType type, long osmid, String wayAsWKT) {
		this.osmType = type;
		this.osmId = osmid;
		this.geometryWKT = wayAsWKT;
		this.tags = null;
	}
	
	public OSMSegment(OSMType type, long osmid, String wayAsWKT, OSMTagList tags) {
		this(type, osmid, wayAsWKT);
		setTags(tags);
	}

	public void setTags(OSMTagList tags) {
		this.tags = tags;
	}
}
	
