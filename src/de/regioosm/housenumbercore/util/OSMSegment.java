package de.regioosm.housenumbercore.util;

import java.util.HashMap;
import java.util.Map;

public class OSMSegment {
	/**
	 * OSM Type of the segment
	 * CAUTION: As of 2018-02, only way Type is supported! 
	 */
	public enum OSMType {node, way, relation};

	/**
	 * OSM Id of the segment. Must be used in conjunction with osmType to really identify the object
	 */
	protected Long osmId = 0L;
	protected OSMType osmType = null;
//	protected Map<String, String> metatags = new HashMap<>();
	/**
	 * List of OSM Tags for the OSM segment
	 * CAUTION: only a very small subset of tags are stored in the list,
	 * which are necessary in this model
	 */
	protected OSMTagList tags = null;
	/**
	 * Way of the street segment from OSM, in Postgis Well-Known-Text Format
	 */
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
	
