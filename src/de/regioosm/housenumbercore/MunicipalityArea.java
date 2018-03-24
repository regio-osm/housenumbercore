package de.regioosm.housenumbercore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.Country;
import de.regioosm.housenumbercore.util.Municipality;



/**
 * represents a municipality, identified by name, country and, if possible, a unique reference id.
 *  
 * @author Dietmar Seifert
 * @version 0.9, 2018-02-20
 * 
 */
public class MunicipalityArea extends Municipality {

	private static Connection housenumberConn = null;
	private static Connection osmdbConn = null;

	private String adminPolygonWKB = null;
	@Deprecated
	private String adminPolygonOsmIdlist = "";
	private Long adminPolygonOsmId = 0L;

	/**
	 * Name of municipality area
	 */
	private String name = "";
	/**
	 * Identification of municipality area in officialhousenumer list
	 * Can be a name or a reference id 
	 */
	private String subid = "-1";
	private int adminlevel = 0;
	private Long muniareaDBId = -1L;
	private static List<MunicipalityArea> searchResults = new ArrayList<>();
	private static int searchResultindex = 0;
	

	public MunicipalityArea(Municipality muni) throws Exception {
		super(muni);

		Applicationconfiguration configuration = new Applicationconfiguration();

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

			String url_mapnik = configuration.db_osm2pgsql_url;
			osmdbConn = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);
		}
		catch( SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	public String getName() {
		return this.name;
	}
	
	public long getMuniAreaDBId() {
		return this.muniareaDBId;
	}
	
	public long getMunicipalityDBId() {
		return municipalityDBId;
	}

	public Municipality getMunicipality() {
		return (Municipality) this;
	}
	
	public long getCountryDBId() {
		return countryDBId;
	}
	
	public String getAreaPolygonAsWKB() {
		return adminPolygonWKB;
	}

	public Long getAdminPolygonOsmId() {
		return adminPolygonOsmId;
	}
	
	public String getAreaId() {
		return subid;
	}

	public int getAreaAdminlevel() {
		return adminlevel;
	}
	
	public void setArea(String name, int adminlevel, String subid) {
		this.name = name;
		this.adminlevel = adminlevel;
		this.subid = subid;
	}
	
	/**
	 * give this class access to housenumber DB
	 * @param housenumberConn
	 */
	public static void connectDB(Connection housenumberConnection, Connection osmConnection) {
//TODO verify alreadyy set in main() and bring code together
		MunicipalityArea.housenumberConn = housenumberConnection;
		MunicipalityArea.osmdbConn = osmConnection;
	}

	/**
	 * search for one or many areas of municipalities in DB and returns the number of found areas.
	 * <br>The result will be stored internally and can get one municipality per time via calls to next()
	 * 
	 * @param country			In which Country the areas for municipalities will be searched. Wildcard * allowed 
	 * @param municipalityName	Name of municipality. Wildcard * allowed, to get 
	 * 							more than one or even all municipality in country
	 * @param officialRef		Official reference of the municipality. Examine OSM key for 
	 * 							official ref in country via Country.getOfficialReferenceOsmKey()
	 * @param areaName			Name of area of a municipality. Wildcard * allowed, to get 
	 * 							more than one or even all areas in municipality
	 * @param adminHierachyTextform		filter municipalities via admin hierarchy in textform. 
	 * 									For example "Bundesrepublik Deutschland,Bayern".
	 * 									Wildcard * allowed
	 * @return 					number of found municipalities in DB
	 * @throws Exception
	 */
	public static int search(String country, String municipalityName, String officialRef, String areaName, String adminHierachyTextform) throws Exception {
		if(housenumberConn == null) {
			throw new IllegalStateException("please first use .connectDB");
		}

		Country.connectDB(housenumberConn);
		if(Country.getCountryShortname(country).equals(""))
			throw new IllegalStateException("invalid or missing Country");

		if((municipalityName == null) || municipalityName.equals("")) {
			municipalityName = "%";
		} else if(municipalityName.indexOf("*") != -1) {
			municipalityName = municipalityName.replace("*",  "%");
		}
		if((officialRef == null) || officialRef.equals("")) {
			officialRef = "%";
		} else  if(officialRef.indexOf("*") != -1) {
			officialRef = officialRef.replace("*",  "%");
		}
		if((areaName == null) || areaName.equals("")) {
			areaName = "%";
		} else  if(areaName.indexOf("*") != -1) {
			areaName = areaName.replace("*",  "%");
		}
		if((adminHierachyTextform == null) || adminHierachyTextform.equals("")) {
			adminHierachyTextform = "%";
		} else  if(adminHierachyTextform.indexOf("*") != -1) {
			adminHierachyTextform = adminHierachyTextform.replace("*",  "%");
		}

			// initialize array for results (could be already set by previous search
		searchResults.clear();
		searchResultindex = 0;

		String searchMunicipalityAreasSql = "";
		searchMunicipalityAreasSql = "SELECT a.id AS areaid, name, " +
			"subareasidentifyable, m.id as muniid, " +
			"admin_level, osm_id, sub_id, " +
			"stadt AS municipalityname, officialkeys_id, " +
			"c.id AS countryid, c.countrycode, " +
			"polygon " +
			"FROM gebiete AS a " +
			"JOIN stadt AS m ON a.stadt_id = m.id " +
			"JOIN land AS c ON m.land_id = c.id " +
			"WHERE c.land = ? "+
			"AND m.stadt like ? " +
			"AND officialkeys_id like ? " +
			"AND osm_hierarchy like ? " +
			"AND a.name like ?;";

		PreparedStatement searchMunicipalityAreasStmt = housenumberConn.prepareStatement(searchMunicipalityAreasSql);
		int stmtindex = 1;
		searchMunicipalityAreasStmt.setString(stmtindex++, country);
		searchMunicipalityAreasStmt.setString(stmtindex++, municipalityName);
		searchMunicipalityAreasStmt.setString(stmtindex++, officialRef);
		searchMunicipalityAreasStmt.setString(stmtindex++, adminHierachyTextform);
		searchMunicipalityAreasStmt.setString(stmtindex++, areaName);
		System.out.println("sql query for municipalities ===" + searchMunicipalityAreasStmt.toString() + "===");
		ResultSet searchMunicipalityAreasRs = searchMunicipalityAreasStmt.executeQuery();
		int resultcount = 0;
			// loop over all found areas for municipalities. store results in internal searchResults Array
		while( searchMunicipalityAreasRs.next() ) {
			resultcount++;
			Municipality muni = new Municipality(searchMunicipalityAreasRs.getString("countrycode"),
				searchMunicipalityAreasRs.getString("municipalityname"),
				searchMunicipalityAreasRs.getString("officialkeys_id"));
			muni.setCountryDBId(searchMunicipalityAreasRs.getLong("countryid"));
			muni.setMunicipalityDBId(searchMunicipalityAreasRs.getLong("muniid"));
			MunicipalityArea muniarea = new MunicipalityArea(muni);
			muniarea.name = searchMunicipalityAreasRs.getString("name");
			muniarea.subid = searchMunicipalityAreasRs.getString("sub_id");
			muniarea.adminlevel = searchMunicipalityAreasRs.getInt("admin_level");
			if(	searchMunicipalityAreasRs.getString("subareasidentifyable") != null && 
				searchMunicipalityAreasRs.getString("subareasidentifyable").equals("y"))
				muniarea.subareasidentifyable = true;
			else
				muniarea.subareasidentifyable = false;
			muniarea.muniareaDBId = searchMunicipalityAreasRs.getLong("areaid");
			muniarea.adminPolygonWKB = searchMunicipalityAreasRs.getString("polygon");
			muniarea.adminPolygonOsmId = searchMunicipalityAreasRs.getLong("osm_id");
			searchResults.add(muniarea);
		}
		searchMunicipalityAreasRs.close();
		searchMunicipalityAreasStmt.close();
		return resultcount;
	}

	/**
	 * get next municipality (previously get via readfromDB(..)
	 */
	public static MunicipalityArea next() {
		MunicipalityArea municipalityarea = null;
		
		if(searchResults.size() > searchResultindex) {
			municipalityarea = (MunicipalityArea) searchResults.get(searchResultindex);
			searchResultindex++;
		}
		return municipalityarea;
	}
	
	

	/**
	 * generate administrative boundary polygon(s) for adminLevel (OSM key 'admin_level')
	 * @param adminLevel: which adminLevel should be used. Select 0 for all available adminLevel. Examine available adminLevels via getAdministrationLevels 
	 * @param storeInDB
	 * @return
	 */
	public boolean generateMunicipalityPolygon(Municipality municipality, int adminLevel, boolean storeInDB) {
		boolean generatedpolygonstate = false;

		String actual_polygon_part = "";
		String complete_polygon = "";
		String complete_polygon_idlist = "";
		Long polygon_id = 0L;

		String osmadminrelationSql = "";
		osmadminrelationSql = "SELECT osm_id AS id, " +
			" name, tags->'name:prefix' AS name_prefix, tags->'name:suffix' AS name_suffix, ";
		if(!municipality.getOfficialKey().equals(""))
			osmadminrelationSql += "tags->'" + municipality.getOfficialKey() + "' AS gemeindeschluessel, ";
		else
			osmadminrelationSql += "null AS gemeindeschluessel, ";
		osmadminrelationSql += "boundary, tags->'is_in' AS is_in, tags->'is_in:state' AS is_in_state, " +
			"tags->'is_in:country' AS is_in_country, tags->'is_in:region' AS is_in_region, " +
			"tags->'is_in:county' AS is_in_county, tags->'admin_level' AS admin_level, " +
			"way AS polygon_geometry " +
			"FROM planet_polygon " +
			"WHERE " +
			"(tags @> 'boundary=>\"administrative\"') AND " +
			"((name like '%" + municipality.getName().replace("'", "''") + "%') OR " +
			"(tags @> 'official_name=>\"" + municipality.getName().replace("'", "''") + "\"') OR " +
			"(tags @> '\"name:" + municipality.getLanguagecode().toLowerCase() + "\"=>\"" + municipality.getName().replace("'","''") + "\"') OR " +
			"(tags @> 'alt_name=>\"" + municipality.getName().replace("'","''") + "\"')) ";
		if(	(municipality.getOfficialRef() != null) && !municipality.getOfficialRef().equals("") && 
			(municipality.getOfficialKey() != null) && !municipality.getOfficialKey().equals("")) {
			osmadminrelationSql += " AND (tags @> '" + municipality.getOfficialKey() + "=>\"" + municipality.getOfficialRef() + "\"')";
		}

		try {
//TODO change to prepared statement
			// only in Germany: variant without suffix 0
			if(municipality.getCountrycode().equals("DE") && municipality.getOfficialKey().equals("de:amtlicher_gemeindeschluessel")) {
				String local_adminid = municipality.getOfficialRef();
				while(	(local_adminid.length() >= 1) && 
						(local_adminid.substring(local_adminid.length()-1,local_adminid.length()).equals("0")))
					local_adminid = local_adminid.substring(0,local_adminid.length()-1);
				if(!municipality.getOfficialRef().equals(local_adminid))
					osmadminrelationSql += " OR (tags @> 'de:amtlicher_gemeindeschluessel=>\""+local_adminid+"\"')";
					// Variant with some spacens "03 1 52 012" (for example Göttingen)
				local_adminid = municipality.getOfficialRef();
				local_adminid = local_adminid.substring(0,2) + " " + local_adminid.substring(2,3) + " " + 
					local_adminid.substring(3,5) + " " + local_adminid.substring(5);
				osmadminrelationSql += " OR (tags @> 'de:amtlicher_gemeindeschluessel=>\""+local_adminid+"\"') ";
			}
			/* only for Luxembourg relevant on 2018-02: usage of country-wide polygon (stored in DB table land) helps to
			 * get correct municipalities - perhaps not needed furthermore
			 * if(activeCountryPolygon != null)
				osmadminrelationSql += " AND ST_Within(way, '" + activeCountryPolygon + "')";
			*/
			osmadminrelationSql += " ORDER BY osm_id;";
	
			Statement osmadminrelationStmt = osmdbConn.createStatement();
			System.out.println("Ausgabe osmadminrelationSql ===" + osmadminrelationSql + "===");
			ResultSet osmadminrelationRs = osmadminrelationStmt.executeQuery( osmadminrelationSql );
			Integer count_relations = 0;
			Integer count_correct_relations = 0;
	//ToDo relation_wrong should be written to gebiete table with optionally info about bad polygon
			String relation_wrong = "";
			String municipality_officialkeysid = "";
			Integer municipality_adminlevel = 0;
	
			int municipality_officialkeysid_origin_length = 0;		// origin found length (without spaces) for check longest original key as best
			String found_osm_relation_id = "";
				// loop over all parts of polygon (in osm2pgsql scheme, 
				// every single closed polygon has its own row, but all share same osm relation id)
			while( osmadminrelationRs.next() ) {
				count_relations++;
				System.out.println("Relation #"+count_relations + 
					":  id===" + osmadminrelationRs.getString("id") + 
					"===  name ===" + osmadminrelationRs.getString("name") + 
					"===   gemeindeschluessel ===" + osmadminrelationRs.getString("gemeindeschluessel") +
					"===  boundary ===" + osmadminrelationRs.getString("boundary") + "===");
				if(	(osmadminrelationRs.getString("name_prefix") != null) || 
					(osmadminrelationRs.getString("name_suffix") != null)) {
	//ToDo Prefix und Suffix ergänzen, um Objekt eindeutig zu identifizieren
					System.out.println(" (Forts.) Name-Zusatz   " +
						"name:prefix ===" + osmadminrelationRs.getString("name_prefix") + "===   " + 
						"name:suffix ==="+osmadminrelationRs.getString("name_suffix") + "===");
				}
				
				if(	(osmadminrelationRs.getString("boundary") == null) || 
					(!osmadminrelationRs.getString("boundary").equals("administrative"))) {
					System.out.print("Warning: Relation-ID is invalid. It's not boundary=administrative, but ");
					if(osmadminrelationRs.getString("boundary") == null) {
						relation_wrong = "error-relation-no-boundary-tag";
						System.out.println("is missing");
					} else {
						relation_wrong = "error-relation-wrong-boundary-value";
						System.out.println("has wrong value ==="+osmadminrelationRs.getString("boundary")+"===");
					}
					continue;
				}
					// get admin_level from this boundary relation for later check, 
					// if sub-boundaries have higher admin_level, if available
				if(osmadminrelationRs.getInt("admin_level") != 0) {
					municipality_adminlevel = osmadminrelationRs.getInt("admin_level");
				}
	
					// check actual administrationid and normalize it
				String local_actual_municipality_officialkeysid = "";
				int local_actual_municipality_officialkeysid_origin_length = 0;
				if(osmadminrelationRs.getString("gemeindeschluessel") != null) { 
					local_actual_municipality_officialkeysid = 
						osmadminrelationRs.getString("gemeindeschluessel");
					if(( 	(!local_actual_municipality_officialkeysid.equals("")) && 
							local_actual_municipality_officialkeysid.indexOf(" ") != -1))
						local_actual_municipality_officialkeysid = local_actual_municipality_officialkeysid.replace(" ","");
					local_actual_municipality_officialkeysid_origin_length = 
						local_actual_municipality_officialkeysid.length();
						// explicit for Germany: enhance or reduce Gemeindeschluessel
					if(municipality.getCountrycode().equals("DE") && municipality.getOfficialKey().equals("de:amtlicher_gemeindeschluessel")) {
						if(	!local_actual_municipality_officialkeysid.equals("") && 
							(local_actual_municipality_officialkeysid.length() < 8)) {
							String local_string0 = "00000000";
							System.out.println("Warning: german gemeindeschluessel too short, " +
								"will be appended by 0s  original ===" + 
								local_actual_municipality_officialkeysid + "=== for municipality ===" +
								municipality.getName() + "=== in relation");
							local_actual_municipality_officialkeysid += local_string0.substring(0,
								(8 - local_actual_municipality_officialkeysid.length()));
							System.out.println("  (cont.) german gemeindeschluessel after appended ===" + 
								local_actual_municipality_officialkeysid+"=== for municipality ===" + 
									municipality.getName() + "=== in relation");
						}
						if((!local_actual_municipality_officialkeysid.equals("") && 
							(local_actual_municipality_officialkeysid.length() > 8))) {
							System.out.println("ERROR german gemeindeschluessel too long ===" + 
								local_actual_municipality_officialkeysid+"=== for municipality ===" + 
									municipality.getName() + "=== in relation");
							local_actual_municipality_officialkeysid = "";
							local_actual_municipality_officialkeysid_origin_length = 0;
						}
					}
				}
			
				if(found_osm_relation_id.equals("")) {
					// first usable relation-id
				found_osm_relation_id = osmadminrelationRs.getString("id");
				municipality_officialkeysid = local_actual_municipality_officialkeysid;
				municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid_origin_length;
				} else {
						// if not first found polygon-part and new relation 
						// (so really different boundary-polygon): 
						// check, if better than previous found one
					if(	(!found_osm_relation_id.equals("")) && 
						(!found_osm_relation_id.equals(osmadminrelationRs.getString("id")))) {
							// if actual administation-id is longer then before, use the actual, its more precise, hopefully
						if(local_actual_municipality_officialkeysid_origin_length > 
							municipality_officialkeysid_origin_length) {
							System.out.println("ok, actual relation is origin-length longer (" + 
								local_actual_municipality_officialkeysid_origin_length+") ");
							System.out.println("  (cont) than previous found (" +
								municipality_officialkeysid_origin_length+")");
							System.out.println("  (cont) previous found municipality was ===" +
								municipality_officialkeysid+"===");
							System.out.println("  (cont) actual municipality is ===" +
								local_actual_municipality_officialkeysid+"===");
							System.out.println("  (cont) previous found osm relation id was ===" +
								found_osm_relation_id+"===");
							System.out.println("  (cont) actual osm relation id is ===" +
								osmadminrelationRs.getString("id")+"===");
							System.out.println("  (cont) so get now the actual better ones!");
							municipality_officialkeysid = local_actual_municipality_officialkeysid;
							municipality_officialkeysid_origin_length = 
								local_actual_municipality_officialkeysid_origin_length;
							found_osm_relation_id = osmadminrelationRs.getString("id");
						} else if(	(local_actual_municipality_officialkeysid_origin_length == municipality_officialkeysid_origin_length) &&
									municipality.getName().equals(osmadminrelationRs.getString("name"))) {
							municipality_officialkeysid = local_actual_municipality_officialkeysid;
							found_osm_relation_id = osmadminrelationRs.getString("id");
							System.out.println("ok, actual relation is equal length in administrationid, " +
								"but name is equals to municipality should-be-name, " +
								"so i use now osm relation-id ===" + found_osm_relation_id+"===");
							count_correct_relations = 0;	// reset number correct relations to 0, so new start to get polygon(parts)
						} else {
							System.out.println("actual relation is other than before and orgin key length " +
								"is not longer thant before, so actual one will be ignored");
							continue;
						}
					}
				}
			
				count_correct_relations++;
	
				actual_polygon_part = osmadminrelationRs.getString("polygon_geometry");
	
	
				if( (osmadminrelationRs.getString("name") != null) && 
					!osmadminrelationRs.getString("name").equals(municipality.getName())) {
					String local_name_test = osmadminrelationRs.getString("name");
						// check possible suffixes
					if(local_name_test.indexOf(municipality.getName()) == 0) {
						String local_name_suffix = local_name_test.replace(municipality.getName(),"");
						if( ! local_name_suffix.equals("")) {
							System.out.println("place objects hat municipality name ===" + municipality.getName() + 
								"=== but contains suffix ==="+local_name_suffix+"=== ...");
							boolean name_suffix_ok = false;
							if(local_name_suffix.indexOf(" im ") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf(" ob ") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf(" bei ") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf(", Stadt") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf("-Stadt") == 0)
								name_suffix_ok =true;
							if( ! name_suffix_ok) {
								System.out.println("WARNING: Difference in name of relation ===" + 
									osmadminrelationRs.getString("name")+"=== to name of municipality ===" + 
									municipality.getName() + "===");
							}											
						}
					} else if(local_name_test.indexOf(municipality.getName()) != -1) {
						String local_name_prefix = local_name_test.replace(municipality.getName(),"");
						if( ! local_name_prefix.equals("")) {
							System.out.println("place objects hat municipality name ===" + municipality.getName() +
								"=== but contains suffix ==="+local_name_prefix+"=== ...");
							boolean name_prefix_ok = false;
							if(local_name_prefix.indexOf("gmina ") == 0)
								name_prefix_ok =true;
							if(local_name_prefix.indexOf("Gemeinde ") == 0)
								name_prefix_ok =true;
							if( ! name_prefix_ok) {
								System.out.println("WARNING: Difference in name of relation ===" + 
									osmadminrelationRs.getString("name") + "=== to name of municipality ===" +
									municipality.getName() + "===");
							}											
						}
					}
				}
	
	
				if( ! municipality_officialkeysid.equals(""))
					System.out.println("Info: got municipality administrationid ==="+municipality_officialkeysid+"===");
				if(count_correct_relations == 1) {
						// polygon first or only part, safe in extra variable
					complete_polygon = actual_polygon_part;
					complete_polygon_idlist = osmadminrelationRs.getString("id");
					polygon_id = osmadminrelationRs.getLong("id");
					System.out.println("ok, got first part of relation");
				} else if(count_correct_relations > 1) {
					System.out.println("ok, got another part of relation");
	
					// polygon another part, now union with previous part(s) as a multipolygon
					System.out.println("Warning: got more than one relation, Select statement was ===" + 
							osmadminrelationSql + "===");
	
	
					String temp_create_multipolygon_sql = "SELECT ST_AsText('" + 
						complete_polygon + "') AS completepoly, " +
						"ST_AsText('"+actual_polygon_part+"') as polypart;";
					System.out.println("temp_create_multipolygon_sql===" + 
						temp_create_multipolygon_sql+"===");
					try {
						Statement temp_stmt_create_multipolygon = housenumberConn.createStatement();
						ResultSet temp_rs_create_multipolygon = temp_stmt_create_multipolygon.executeQuery( temp_create_multipolygon_sql );
						if( temp_rs_create_multipolygon.next() ) {
							System.out.println("ok, completepoly ===" + 
								temp_rs_create_multipolygon.getString("completepoly")+"===");
							System.out.println("ok, polypart ===" + 
								temp_rs_create_multipolygon.getString("polypart")+"===");
						}
					}	// ende try DB-connect
					catch( SQLException e) {
						System.out.println("ERROR occured when tried to get " +
							"both polygon-parts as text. SQL-Statement was ===" + 
							temp_create_multipolygon_sql+"===");
						e.printStackTrace();
						relation_wrong = "invalid-multipolygon";
					}
	
					String mergepolygonpartsSql = "SELECT ST_Union('" + actual_polygon_part + "', " +
						"'" + complete_polygon + "') " + "AS unionpolygon;";
					System.out.println("mergepolygonpartsSql==="+mergepolygonpartsSql+"===");
					try {
						Statement mergepolygonpartsStmt = housenumberConn.createStatement();
						ResultSet rs_create_multipolygon = mergepolygonpartsStmt.executeQuery( mergepolygonpartsSql );
						if( rs_create_multipolygon.next() ) {
							complete_polygon_idlist += "," + osmadminrelationRs.getString("id");	// add actual polygon osm-id to list of all unioned-polygons
//TODO handle, that polygon_id can't be set second time
							complete_polygon = rs_create_multipolygon.getString("unionpolygon");
						}
					}	// ende try DB-connect
					catch( SQLException e) {
						System.out.println("ERROR occured when tried to create ST_Union Polygon");
						System.out.println(" (cont.) here original stack ==="+e.toString()+"===");
						e.printStackTrace();
						relation_wrong = "invalid-multipolygon";
					}
				}
			} // end over loop of all found relation-records (ends now earlier from 17.11.2011) - while( osmadminrelationRs.next() ) {

			
			if(complete_polygon.equals("")) {
				System.out.println("Error with relation, ignored this municipality");
				System.out.println("no boundary-geometry found for actual municipality, " +
					"skip rest of work for this municipality");
				this.adminPolygonWKB = null;
				this.adminPolygonOsmIdlist = null;
				this.adminPolygonOsmId = 0L;
					// ok, abort actual municipality
				generatedpolygonstate = false;
			} else {
				this.adminPolygonWKB = complete_polygon;
//TODO store main area polygon in subarea-table and set a flag to identify its main state
				this.adminPolygonOsmIdlist = complete_polygon_idlist;
				this.adminPolygonOsmId = polygon_id;
				generatedpolygonstate = true;
				if(storeInDB) {
					String selectSubareaSql = "SELECT id, name FROM gebiete WHERE"
						+ " name = ? AND stadt_id = ? AND admin_level = ?;";
					PreparedStatement selectSubareaStmt = housenumberConn.prepareStatement(selectSubareaSql);
					int stmtindex = 1;
					selectSubareaStmt.setString(stmtindex++, municipality.getName());
					selectSubareaStmt.setLong(stmtindex++, municipality.getMunicipalityDBId());
					selectSubareaStmt.setInt(stmtindex++, municipality_adminlevel);
					System.out.println("query to get subarea ===" + selectSubareaStmt.toString() + "===");
		
					ResultSet selectSuburbRs = selectSubareaStmt.executeQuery();
					if( selectSuburbRs.next() ) {
						this.muniareaDBId = selectSuburbRs.getLong("id");
						this.name = selectSuburbRs.getString("name");
						System.out.println("ok, main area already found in DB, Name ===" + 
							selectSuburbRs.getString("name") + "===, will be updated");
		
						String updateSuburbSql = "UPDATE gebiete SET " +
								"osm_id = ?, polygon = ?::geometry, sub_id = ?, checkedtime = now() " +
								"WHERE id = ?;";
						PreparedStatement updateSuburbStmt = housenumberConn.prepareStatement(updateSuburbSql);
						stmtindex = 1;
						updateSuburbStmt.setLong(stmtindex++, this.adminPolygonOsmId);
						updateSuburbStmt.setString(stmtindex++, this.adminPolygonWKB);
						updateSuburbStmt.setString(stmtindex++, "-1");
						updateSuburbStmt.setLong(stmtindex++, this.muniareaDBId);
		
						System.out.println("update suburb DB statement ===" + updateSuburbStmt.toString() + "===");
						try {
							updateSuburbStmt.executeUpdate();
							System.out.println("Info: successfully updated area in database");
						}
						catch( SQLException errorupdate) {
							System.out.println("Error occured during try of update gebiete record with stadt.id ===" +
								municipality.getMunicipalityDBId());
							errorupdate.printStackTrace();
						}
					} else {
						try {
							String insertSuburbSql = "INSERT INTO gebiete ";
							insertSuburbSql += "(name, land_id, stadt_id, admin_level, " +
								"sub_id, osm_id, checkedtime, polygon) VALUES ";
							insertSuburbSql += " (?, (SELECT id FROM land WHERE countrycode = ?), " +
								"?, ?, ?, ?,  now(), ?::geometry) returning id;";
							PreparedStatement insertSuburbStmt = housenumberConn.prepareStatement(insertSuburbSql);
							stmtindex = 1;
							insertSuburbStmt.setString(stmtindex++, municipality.getName());
							insertSuburbStmt.setString(stmtindex++, municipality.getCountrycode());
							insertSuburbStmt.setLong(stmtindex++, municipality.getMunicipalityDBId());
							insertSuburbStmt.setInt(stmtindex++, municipality_adminlevel);
							insertSuburbStmt.setString(stmtindex++, "-1");
							insertSuburbStmt.setLong(stmtindex++, this.adminPolygonOsmId);
							insertSuburbStmt.setString(stmtindex++, this.adminPolygonWKB);

							ResultSet insertSuburbRS = insertSuburbStmt.executeQuery();
							if( insertSuburbRS.next() ) {
								this.muniareaDBId = insertSuburbRS.getLong("id");
								this.name = municipality.getName();
								System.out.println(" return id from new job ===" + muniareaDBId + "===");
							} else {
								System.out.println("FEHLER FEHLER: nach Area-insert konnte Area-id nicht geholt werden");
							}
						}
						catch( SQLException errorinsert) {
							System.out.println("Error occured during try of insert gebiete record with stadt.id ===" +
								municipality.getMunicipalityDBId());
							errorinsert.printStackTrace();
						}
					}
				}
			}
		}
		catch (SQLException sqle) {
			//TODO fill out
			sqle.printStackTrace();
			return false;
		}

		return generatedpolygonstate;
	}


	/**
	 * get suburb Polygons with the polygon of municipality, check them and optionally, store them in DB
	 * @param storeInDB	defines, that the found and valid suburb polygon should be stored in DB (should be default true)
	 * @return
	 */
	public boolean generateSuburbPolygons(Municipality municipality, boolean storeInDB) {	
			// TODO inactive variable: should be stored in a helper DB table to identify reason for not-usable admin polygon
		String relation_wrong = "";
		Integer municipality_adminlevel = 0;

		String subadminrelationSql = "SELECT osm_id AS id, tags->'name' AS name, " + 
			"tags->'admin_level' AS admin_level, ";
		if(municipality.getOfficialKey() != null)
			subadminrelationSql += "tags->? AS gemeindeschluessel, ";
		else
			subadminrelationSql += "null AS gemeindeschluessel, ";
		subadminrelationSql += "tags->'ref' AS ref, tags->'de:Stadtteilnummer' AS stadtbezirksnummer " +
			"FROM planet_polygon " +
			"WHERE " +
			 // use a small buffer around municipality admin polygon to fit sub relations
			"(ST_Within(way,ST_Buffer(?, 0.03))) AND " +
			"(tags @> 'boundary=>\"administrative\"' AND exist(tags, 'admin_level'));";
		PreparedStatement subadminrelationStmt = null;
		try {
			subadminrelationStmt = osmdbConn.prepareStatement(subadminrelationSql);
			int stmtindex = 1;
			subadminrelationStmt.setString(stmtindex++, municipality.getOfficialKey());
			subadminrelationStmt.setString(stmtindex++, adminPolygonWKB);
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("subadminrelationSql ===" + subadminrelationStmt.toString() + "===");
		ResultSet subadminrelationRs = null;
		try {
			subadminrelationRs = subadminrelationStmt.executeQuery();
		}
		catch( SQLException e) {
			System.out.println("ERROR occured when tried to take Polygon");
			System.out.println(" (cont.) here original stack ==="+e.toString()+"===");
			e.printStackTrace();
			relation_wrong = "invalid-geometry";
			return false;
		}
		
		try {
				// Loop over found OSM relations with tags boundary=administrative 
				// and admin_level=* within municipality admin polygon
			while( subadminrelationRs.next() ) {
	
				System.out.println("START processing Relations-ID # " + subadminrelationRs.getString("id") + 
					"   Name ===" + subadminrelationRs.getString("name") +
					"===    admin_level: " + subadminrelationRs.getString("admin_level") + 
					"   official key ===" + subadminrelationRs.getString("gemeindeschluessel") + "===");
	
				if(subadminrelationRs.getString("name") == null) {
					//TODO print error to logfile
					System.out.println("ERROR ERROR relation will be ignored, " +
						"because name is not set  at  Relations-ID # " + 
						subadminrelationRs.getString("id")+"   Name ===" + subadminrelationRs.getString("name") + 
						"===    admin_level: " + subadminrelationRs.getString("admin_level") +
						"   gemeindeschlüssel ===" + subadminrelationRs.getString("gemeindeschluessel") + "===");
					continue;
				}
					
				if(subadminrelationRs.getInt("admin_level") != 0) {
					if(subadminrelationRs.getInt("admin_level") < 4) {
						System.out.println("Relation will be ignored, because admin_level of " +
							"actual polygon is less than 4, concrete: " + 
							subadminrelationRs.getInt("admin_level"));
						continue;
					}
					if((municipality_adminlevel != 0) && (municipality_adminlevel > 
						subadminrelationRs.getInt("admin_level"))) {
						System.out.println("Relation will be ignored, because admin_level of " +
							"actual polygon is less than parent relation (" + 
							municipality_adminlevel+ "), concrete: "+subadminrelationRs.getInt("admin_level"));
						continue;
					}
				}
	
	
				int activeRelationAdminlevel = subadminrelationRs.getInt("admin_level");
				if(activeRelationAdminlevel == 0) {
					System.out.println("Info: Relation will be ignored, because no OSM-Tag admin_level available");
					continue;
				}
	
				if((municipality.getAdminLevels() != null) && (municipality.getAdminLevels().size() >= 1)) {
					boolean activeRelationAdminlevel_valid = false;
					for(int actualcheckAdminlevel = 0; actualcheckAdminlevel < 
							municipality.getAdminLevels().size(); actualcheckAdminlevel++) {
						if(municipality.getAdminLevels().get(actualcheckAdminlevel) == activeRelationAdminlevel) {
							System.out.println("Info: Relation is a candidate to use, " + 
								"because admin_level has valid number ===" + 
								municipality.getAdminLevels().get(actualcheckAdminlevel) + "===");
							activeRelationAdminlevel_valid = true;
							break;
						} else {
							System.out.println("Info: Relation check of admin_level value ===" + 
								activeRelationAdminlevel + "=== against actual valid value ===" + 
								municipality.getAdminLevels().get(actualcheckAdminlevel) + "=== was different");
						}
					}
					if(!activeRelationAdminlevel_valid) {
						System.out.println("Info: Relation will be ignored, " +
							"because value of admin_level is not in list of valid values ===" + 
							municipality.getAdminLevels().toString() + "===");
						continue;
					}
				}
				
				String subpolygonString = "";
	
				String selectbefehl_relation_kpl = "SELECT way FROM planet_polygon WHERE " +
					"osm_id = ?;";
				PreparedStatement stmt_relation_kpl = osmdbConn.prepareStatement(selectbefehl_relation_kpl);
				int stmtindex = 1;
				stmt_relation_kpl.setLong(1, subadminrelationRs.getLong("id"));
				System.out.println("Ausgabe selectbefehl_relation_kpl bei osm2pgsql DB-Variante===" + 
						stmt_relation_kpl.toString() + "===");
				ResultSet rs_relation_kpl = stmt_relation_kpl.executeQuery();
				Integer count_correct_subrelations = 0;
				String actual_sub_polygon_part = "";
				System.out.println("BEGINN Verarbeitung Sub Relation-kpl ...");
				while( rs_relation_kpl.next() ) {
					count_correct_subrelations++;
	
					actual_sub_polygon_part = rs_relation_kpl.getString("way");
	
					if(count_correct_subrelations == 1) {
						System.out.println("ok, got first part of sub relation");
						// polygon first or only part, safe in extra variable
						subpolygonString = actual_sub_polygon_part;
					} else if(count_correct_subrelations > 1) {
						System.out.println("ok, got another part of sub relation");
	
							// polygon another part, now union with previous part(s) as a multipolygon
						System.out.println("Warning: got more than one sub relation, " +
							"Select statement was ==="+selectbefehl_relation_kpl+"===");
	
						String create_multipolygon_sql = "SELECT ST_Union('"+actual_sub_polygon_part+"', " +
							"'"+subpolygonString+"') as unionpolygon, ";
						create_multipolygon_sql += "ST_AsText(ST_Union('"+subpolygonString+"', " +
							"'"+actual_sub_polygon_part+"')) as justfordebug_unionpolygon_astext;";
						System.out.println("create_multipolygon_sql for sub relation union ===" +
							create_multipolygon_sql+"===");
						try {
							Statement stmt_create_multipolygon = housenumberConn.createStatement();
							ResultSet rs_create_multipolygon = stmt_create_multipolygon.executeQuery( create_multipolygon_sql );
							if( rs_create_multipolygon.next() ) {
								System.out.println("ok, union sub polygon ===" + 
									rs_create_multipolygon.getString("justfordebug_unionpolygon_astext")+"===");
								subpolygonString = rs_create_multipolygon.getString("unionpolygon");
							}
						}	// ende try DB-connect
						catch( SQLException e) {
							System.out.println("ERROR occured when tried to create ST_Union sub Polygon");
							System.out.println(" (cont.) here original stack ==="+e.toString()+"===");
							e.printStackTrace();
							relation_wrong = "invalid-multipolygon";
						}
					}
				}
	
				String sub_id = "-1";
					// if official housenumber list contains district name and it should be used
				if(municipality.isSubareasidentifyable()) {
						//TODO country specific code - please improve
					if(municipality.getCountrycode().equals("PL")) {
							// in Poland, the municipality is admin_level = 7 instead of standard 8
						if(subadminrelationRs.getInt("admin_level") > 7) {
							if(subadminrelationRs.getString("ref") != null)
								sub_id = subadminrelationRs.getString("ref");
							else if(subadminrelationRs.getString("stadtbezirksnummer") != null)
								sub_id = subadminrelationRs.getString("stadtbezirksnummer");
							else if(subadminrelationRs.getString("name") != null)
								sub_id = subadminrelationRs.getString("name");
							System.out.println("special countrycode " + municipality.getCountrycode() + 
								": set sub_id to ===" + sub_id + "===");
						}
					} else {
						if(subadminrelationRs.getInt("admin_level") > 8) {
							if(subadminrelationRs.getString("ref") != null)
								sub_id = subadminrelationRs.getString("ref");
							else if(subadminrelationRs.getString("stadtbezirksnummer") != null)
								sub_id = subadminrelationRs.getString("stadtbezirksnummer");
							else if(subadminrelationRs.getString("name") != null)
								sub_id = subadminrelationRs.getString("name");
						}
						System.out.println("countrycode " + municipality.getCountrycode() + 
							": set sub_id to ===" + sub_id + "===");
					}
				}
				
	
				if(subadminrelationRs.getInt("admin_level") <= municipality_adminlevel) { 
					if((subadminrelationRs.getString("gemeindeschluessel") != null) &&
						( ! subadminrelationRs.getString("gemeindeschluessel").equals(""))) {
					} else {
						//TODO country specific code - please improve
						if(municipality.getCountrycode().equals("LU")) {
							System.out.println("Info: Relation without municipality reference-id, " +
								"but valid in active country " + municipality.getCountrycode());
						} else {
							System.out.println("ERROR ERROR: at osm relation, muicipality ref is missing. " +
								"Therefor, municipality can't be identified, WILL BE IGNORED");
							System.out.println("    (Forts.) Relations-ID # " + subadminrelationRs.getString("id") + 
								"   Name ===" + subadminrelationRs.getString("name") + 
								"===    admin_level: " + subadminrelationRs.getString("admin_level") +
								"   gemeindeschlüssel ===" + subadminrelationRs.getString("gemeindeschluessel")+"===");
							continue;
						}
					}
				}
	
				if(municipality.getMunicipalityDBId() == 0L) {
					System.out.println("ERROR ERROR: municipality couldn't be identified, WILL BE IGNORED");
					System.out.println("    (cont.) Relations-ID # " + subadminrelationRs.getString("id") +
						"   Name ===" + subadminrelationRs.getString("name") + 
						"===    admin_level: " + subadminrelationRs.getString("admin_level") +
						"   gemeindeschlüssel ===" + subadminrelationRs.getString("gemeindeschluessel") + "===");
					continue;
				}
	
	
				
				
				String selectSubareaSql = "SELECT id, name FROM gebiete WHERE"
					+ " name = ? AND stadt_id = ? AND admin_level = ?;";
				PreparedStatement selectSubareaStmt = housenumberConn.prepareStatement(selectSubareaSql);
				stmtindex = 1;
				selectSubareaStmt.setString(stmtindex++, subadminrelationRs.getString("name"));
				selectSubareaStmt.setLong(stmtindex++, municipality.getMunicipalityDBId());
				selectSubareaStmt.setInt(stmtindex++, subadminrelationRs.getInt("admin_level"));
				System.out.println("query to get subarea ===" + selectSubareaStmt.toString() + "===");
	
	
				ResultSet selectSuburbRs = selectSubareaStmt.executeQuery();
				if( selectSuburbRs.next() ) {
					System.out.println("ok, suburb already found in DB, Name ===" + 
						subadminrelationRs.getString("name") + "===, will be updated");
	
					String updateSuburbSql = "UPDATE gebiete SET " +
							"osm_id = ?, polygon = ?::geometry, sub_id = ?, checkedtime = now() " +
							"WHERE id = ?;";
					PreparedStatement updateSuburbStmt = housenumberConn.prepareStatement(updateSuburbSql);
					stmtindex = 1;
					updateSuburbStmt.setLong(stmtindex++, subadminrelationRs.getLong("id"));
					updateSuburbStmt.setString(stmtindex++, subpolygonString);
					updateSuburbStmt.setString(stmtindex++, sub_id);
					updateSuburbStmt.setLong(stmtindex++, selectSuburbRs.getLong("id"));
	
					System.out.println("update suburb DB statement ===" + updateSuburbStmt.toString() + "===");
					try {
						updateSuburbStmt.executeUpdate();
						System.out.println("Info: successfully updated area in database");
					}
					catch( SQLException errorupdate) {
						System.out.println("Error occured during try of update gebiete record with stadt.id ===" +
							municipality.getMunicipalityDBId());
						errorupdate.printStackTrace();
					}
				} else {
					try {
						String insertSuburbSql = "INSERT INTO gebiete ";
						insertSuburbSql += "(name, land_id, stadt_id, admin_level, " +
							"sub_id, osm_id, checkedtime, polygon) VALUES ";
						insertSuburbSql += " (?, (SELECT id FROM land WHERE countrycode = ?), " +
							"?, ?, ?, ?,  now(), ?::geometry) returning id;";
						PreparedStatement insertSuburbStmt = housenumberConn.prepareStatement(insertSuburbSql);
						stmtindex = 1;
						insertSuburbStmt.setString(stmtindex++, subadminrelationRs.getString("name"));
						insertSuburbStmt.setString(stmtindex++, municipality.getCountrycode());
						insertSuburbStmt.setLong(stmtindex++, municipality.getMunicipalityDBId());
						insertSuburbStmt.setInt(stmtindex++, subadminrelationRs.getInt("admin_level"));
						insertSuburbStmt.setString(stmtindex++, sub_id);
						insertSuburbStmt.setLong(stmtindex++, subadminrelationRs.getLong("id"));
						insertSuburbStmt.setString(stmtindex++, subpolygonString);

						ResultSet insertSuburbRS = insertSuburbStmt.executeQuery();
						if( insertSuburbRS.next() ) {
							System.out.println(" return id from new job ===" + muniareaDBId + "===");
						} else {
							System.out.println("FEHLER FEHLER: nach Area-insert konnte Area-id nicht geholt werden");
						}
					}
					catch( SQLException errorinsert) {
						System.out.println("Error occured during try of insert gebiete record with stadt.id ===" +
							municipality.getMunicipalityDBId());
						errorinsert.printStackTrace();
					}
				}
				System.out.println("FINISH processing Relation Name ===" + subadminrelationRs.getString("name")+"===");
			}	// end loop over all osm relations
//TODO check municipality areas in table gebiete, which have no actual checkedtime
// and delete them
		}
		catch( SQLException e) {
			//TODO fill out
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		String result = "";

		result += name + " , area id " + subid + ",   technical Details: area DBId: " + muniareaDBId;
//				+ "   von Stadt ==="+rs_gebiete.getString("stadt") + "===   admin_level: "+rs_gebiete.getString("admin_level") + "===   sub_id ===" + rs_gebiete.getString("sub_id") + "===");
		
		return result;
	}

	
	public static void main(String args[]) {

		
		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-land land (ohne Angabe: Bundesrepublik Deutschland)");
			System.out.println("-stadt einzelnestadt");
			System.out.println("-agsarea xy");
			System.out.println("-adminhierarchy xy");
			System.out.println("-languagecode xy (2digits ISO-Code, like DE)");
			return;
		}

		String paramCountry = "Bundesrepublik Deutschland";
		String paramMunicipality = "";
		String paramOfficialRef = "";
		String paramAdminHierarchy = "";
		String paramLanguageCode = "";
		if(args.length >= 1) {
			int args_ok_count = 0;
			for(int argsi=0;argsi<args.length;argsi+=2) {
				System.out.print(" args pair analysing #: "+argsi+"  ==="+args[argsi]+"===");
				if(args.length > argsi+1)
					System.out.print("  args #+1: "+(argsi+1)+"   ==="+args[argsi+1]+"===");
				System.out.println("");
				if(args[argsi].equals("-land")) {
					paramCountry = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-stadt")) {
					paramMunicipality = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-agsarea")) {
					paramOfficialRef = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-adminhierarchy")) {
					paramAdminHierarchy = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-languagecode")) {
					paramLanguageCode = args[argsi+1];
					args_ok_count += 2;
				}
			}
			if(args_ok_count != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}
		
		Applicationconfiguration configuration = new Applicationconfiguration();

		java.util.Date program_starttime = new java.util.Date();

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

			String url_mapnik = configuration.db_osm2pgsql_url;
			osmdbConn = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);

			
//TODO verify already set in connectDB() and bring code together
			Municipality.connectDB(housenumberConn);

			if((paramLanguageCode != null) && !paramLanguageCode.equals(""))
				Municipality.setLanguageCode(paramLanguageCode);
			Municipality newmuni = null;
			try {
				newmuni = new Municipality(paramCountry, paramMunicipality, paramOfficialRef);
				newmuni.store();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Municipality municipality = null;
				// loop over all found municipalities
			while( (municipality = Municipality.next()) != null ) {
				MunicipalityArea newarea = null;
				try {
					newarea = new MunicipalityArea(municipality);
					System.out.println("Processing municipality " + municipality.toString() + "===");
					if(!newarea.generateMunicipalityPolygon(municipality, 0, true)) {
						//TODO display error, generating Admin polygon for municipality
						continue;
					}
					if(!newarea.generateSuburbPolygons(municipality, true)) {
						//TODO display error, generating suburb polygons for municipality
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}   // loop over all found municipalities

			java.util.Date program_endtime = new java.util.Date();

			System.out.println("END main program at " + program_endtime.toString() + ", duration in sec.: " + (program_endtime.getTime() - program_starttime.getTime())/1000);
		}
		catch( SQLException e) {
			e.printStackTrace();
			return;
		}
	}
}
