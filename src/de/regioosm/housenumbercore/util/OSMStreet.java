package de.regioosm.housenumbercore.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.regioosm.housenumbercore.util.OSMSegment.OSMType;

/**
 * OSM representation of a street within a municipality, including its geometry and OSM tags
 * @author Dietmar Seifert
 *
 */
public class OSMStreet extends Street implements Comparable {
	private static Connection housenumberConn = null;

	private static List<OSMTagList> validHighwayTypes = null;
	
	/**
	 * List of OSM ways, which belong to the named OSM Street
	 */
	private List<OSMSegment> segments = new ArrayList<>();
	private String geometryWKT = null;
	/**
	 * internal Flag: will be set to true, when a way segment has added
	 * <br>If the geometryWKT will be get, this flag will be evaluated first,
	 * and, if set, the geometry will be created new first
	 * @see addSegment, geometryWKT
	 */
	private boolean geometryIsDirty = false;

	


	public OSMStreet(Municipality municipality, String streetname) {
		super(municipality, streetname);
		if(validHighwayTypes == null)
			setDefaultValidHighwayTypes();

		Applicationconfiguration configuration = new Applicationconfiguration();

		if(housenumberConn == null) {
			try {
				System.out.println("ok, jetzt Class.forName Aufruf ...");
				Class.forName("org.postgresql.Driver");
				System.out.println("ok, nach Class.forName Aufruf!");
			}
			catch(ClassNotFoundException e) {
				e.printStackTrace();
				System.exit(1);
			}
			try {
				String url_hausnummern = configuration.db_application_url;
				housenumberConn = DriverManager.getConnection(url_hausnummern, configuration.db_application_username, configuration.db_application_password);
			}
			catch( SQLException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	/**
	 * represent an osm way for a named street within a municipality, including osm-id and geometry (in well known text format)
	 * @param municipality
	 * @param streetname
	 * @param type
	 * @param osmid
	 * @param geomwkt
	 */
	public OSMStreet(Municipality municipality, String streetname, OSMType type, long osmid, String geomwkt) {
		this(municipality, streetname);
		addSegment(new OSMSegment(type, osmid, geomwkt));
	}

	public static void connectDB(Connection housenumberConn) {
		OSMStreet.housenumberConn = housenumberConn;
	}

	/**
	 * in 2018-02 this method was new created.
	 * Up to this time, a list of highway types was used, which are NOT useful.
	 * From now on, here is the list of positive Types, which are only valid 
	 */
	private static void setDefaultValidHighwayTypes() {
		OSMStreet.validHighwayTypes = new ArrayList<>();
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "primary")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "secondary")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "tertiary")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "unclassified")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "residential")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "living_street")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "service")));
		OSMStreet.validHighwayTypes.add(new OSMTagList(new OSMTag("highway", "track")));
//TODO add custom method to override this default setting of valid highway types
	}

	/**
	 * Be sure, that the way has geometry in WGS84 (EPSG 4326) Coordinate system
	 * @param way
	 */
	public void addSegment(OSMSegment way) {
		segments.add(way);
		this.geometryIsDirty = true;
		setStreetGeometryFromSegments();
	}
	
	public void addSegment(OSMType type, long osmid, String geomWKT) {
		OSMSegment way = new OSMSegment(type, osmid, geomWKT);
		segments.add(way);
		this.geometryIsDirty = true;
		setStreetGeometryFromSegments();
	}

	public String normalizeName() {
//TODO add some country specific normalizations
		String resultstreetname = this.getName();
		if(this.getMunicipality().getCountrycode().equals("RO")) {	//RO = Romania
			if(resultstreetname.startsWith("Strada ")) {
				resultstreetname = resultstreetname.substring("Strada ".length());
			}
		}
		return resultstreetname;
	}

	public String getGeometryWKT() {
		if(geometryIsDirty) {
			setStreetGeometryFromSegments();
		}
		return this.geometryWKT;
	}

	private boolean setStreetGeometryFromSegments() {
		if(this.segments.size() == 0) {
			this.geometryWKT = null;
			this.geometryIsDirty = false;
			return true;
		} else if(this.segments.size() == 1) {
			this.geometryWKT = this.segments.get(0).geometryWKT;
			this.geometryIsDirty = false;
			return true;
		}

		String mergedways = this.segments.get(0).geometryWKT;
		for(int segmentindex = 1; segmentindex < this.segments.size(); segmentindex++) {
			String actualway = this.segments.get(segmentindex).geometryWKT;
			
			if(mergedways.equals("")) {
				mergedways = actualway.substring(actualway.indexOf("("),actualway.indexOf(")")+1);
			} else {
				if(mergedways.indexOf("MULTILINESTRING(") != -1) {
					System.out.println("multlinestring-gefunden");
					mergedways = mergedways.substring(mergedways.indexOf("MULTILINESTRING(")+16,mergedways.lastIndexOf(")"));
				} else if(mergedways.indexOf("LINESTRING(") != -1) {
					System.out.println("linestring-gefunden");
					mergedways = mergedways.substring(mergedways.indexOf("LINESTRING(")+10);
				}
				System.out.println("vorhandener linestring im datensatz  netto ==="+mergedways+"===");
				mergedways += "," + actualway.substring(actualway.indexOf("("),actualway.indexOf(")")+1);

				String mergeGeometriesSql = "SELECT ST_AsText(ST_LineMerge(ST_GeomFromText('MULTILINESTRING("+mergedways+")'))) as multistring;";
				Statement mergeGeometriesStmt;
				try {
					mergeGeometriesStmt = housenumberConn.createStatement();
					ResultSet rs_strassengeometriemerge = mergeGeometriesStmt.executeQuery(mergeGeometriesSql);
					if( rs_strassengeometriemerge.next() ) {
						mergedways = rs_strassengeometriemerge.getString("multistring");
						System.out.println("Ausgabe Multilinestring ==="+mergedways+"===");
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					this.geometryWKT = null;
					this.geometryIsDirty = true;
					return false;
				}
			}
		}
		this.geometryWKT = mergedways;
		this.geometryIsDirty = false;
		return true;
	}

	public String getOSMIdsAsString() {
		String idList = "";
		
		for(int segmentindex = 0; segmentindex < this.segments.size(); segmentindex++) {
			if(!idList.equals(""))
				idList += ",";
			idList += this.segments.get(segmentindex).osmId;
		}
		return idList;
	}

	public static boolean isValidNamedHighwaytype(String osmhighwaykey, String osmhighwayvalue) {
		if(validHighwayTypes == null)
			setDefaultValidHighwayTypes();

		for(int typesindex = 0; typesindex < validHighwayTypes.size(); typesindex++) {
			OSMTagList actualtaglist = validHighwayTypes.get(typesindex);
			for(int tagsindex = 0; tagsindex < actualtaglist.tags.size(); tagsindex++) {
				OSMTag actualtag = actualtaglist.tags.get(tagsindex);
				if(actualtag.getKey().equals(osmhighwaykey) && actualtag.getValue().equals(osmhighwayvalue)) {
					return true;
				}
			}
		}
		return false;
	}
		
	public String toString() {
		String output = "";

		output += "street: " + this.getName();
		output += ", in " + this.getMunicipality().toString();

		return output;
	}
}
