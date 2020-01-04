package de.regioosm.housenumbercore.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

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
	protected List<OSMTag> tags = new ArrayList<>();
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
	
	public OSMSegment(OSMType type, long osmid, String wayAsWKT, List<OSMTag> tags) {
		this(type, osmid, wayAsWKT);
		setTags(tags);
	}

	/**
	 * build OSM Way als well known text postgis representation
	 * @param allNodes list of all OSM nodes
	 * @param waynodes list of OSM nodes, which are used to construct the way here
	 */
	public void setWayFromOsmNodes(Map<Long, Node> allNodes, List<WayNode> waynodes) {
		String result = "";

		for (WayNode waynode: waynodes) {
			Node actnode = allNodes.get(waynode.getNodeId());
			if(!result.equals(""))
				result += ",";
			result += actnode.getLongitude() + " " + actnode.getLatitude();
		}
		if(!result.equals(""))
			result = "LINESTRING(" + result + ")";
		System.out.println(" generated linestring ===" + result + "===");
		this.geometryWKT = result;
	}
	
	public void setTags(List<OSMTag> tags) {
		this.tags = tags;
	}
	
	@Override
	public String toString() {
		String output = "";
		output += "Tags: ";
		for ( int i = 0; i < this.tags.size(); i++ ) {
			if ( i > 0 )
				output += ", ";
			output += tags.get(i).getKey() + "=" + tags.get(i).getValue();
		}
		output += "\n";
		output += "Geometry";
		int outputlength = geometryWKT.length();
		if ( outputlength > 255 ) {
			outputlength = 255;
			output += " (partly)";
		}
		output += ": " + geometryWKT.substring(0,outputlength);

		return output;
	}
}
	
