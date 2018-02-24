package de.regioosm.theoreticalhousenumbercore;
/*
 *
 * 				OFFEN OFFEN theo. Berechnung 08/2014:
 * 					während in anzahl_osmadressen alle Adressobjekte aufgelöst wurden, wurden in ...treffer... nur die objektanzahl ermittlet, das ergibt etliche Unterschiede
 * 					select stadt,gemeinde_id,anzahl_osmadressen,(anzahl_osmadressennodes + anzahl_osmadressenways + anzahl_osmadressenrels - anzahl_nodes_addrstreet_treffer - anzahl_ways_addrstreet_treffer - anzahl_polygons_addrstreet_treffer - anzahl_nodes_associatedstreet_treffer - anzahl_ways_associatedstreet_treffer - anzahl_polygons_associatedstreet_treffer) as osmadressendiff  from theoevaluations where tstamp > '2014-08-01' and (anzahl_osmadressennodes + anzahl_osmadressenways + anzahl_osmadressenrels - anzahl_nodes_addrstreet_treffer - anzahl_ways_addrstreet_treffer - anzahl_polygons_addrstreet_treffer - anzahl_nodes_associatedstreet_treffer - anzahl_ways_associatedstreet_treffer - anzahl_polygons_associatedstreet_treffer) != 0  order by osmadressendiff desc;
 * 				OFFEN OFFEN OFFEN 06.09.2013
 * 						1. Schritt: potentiell infrage kommenden Multipolygone suchen
 * 							select tags->'name' as name,tags->'building' as building,tags->'addr:housenumber' as hnr, osm_id,tags from planet_polygon where way @ (select st_buffer(st_transform(st_geomfromtext('POINT(11.565079 48.1451228)',4326),900913),5000)) and ( tags ? 'building') and osm_id < 0 order by tags->'name';
 * 							Hinweis: negative osm_id, weil sonst jedes building gefunden wird.
 * 							a) Treffer mit addr:housenumber direkt verwenden. Evtl. stoppen (oder auch weitermachen, weil evtl. Sub-Objekte noch andere Hausnummer haben?
 *							b)  								
 * 
 *            OFFEN OFFEN OFFEN  30.08.2013
 *            			am Ende derzeit auskommentiert fehlende Ergänzungen in jobs_strassen von den Straßen
 *            			die vorher nciht über jobs_erstellen gefunden wurden (hamlet a la Siebenbrunn in Augsburg)
 *            			diese fehlen derzeit in der Auswertung !!!
 * 
 * 
 * 					OFFEN  14.08.2013: 		jetzt werden die associated-Street relations nicht mehr pur durchlaufen
 * 											dadurch werden jetzt keine Fehler gefunden, wenn ein Objekt ein addr:street=x hat und 
 * 												in einer anderen (oder der selben) Street Relation ist
 *          prüfen, ob Relations-Member mit Adressen oder unvollständigen Adressen berücksichtigt werden 
 *          (zumindest ohne addr:street also über associatedStreet gehts nicht)
 *			osm_mapnik=> select osm_id,tags->'name' as name, tags->'addr:street' as street, tags->'addr:housenumber' as hnr,tags from planet_polygon where osm_id < 0 and tags ? 'building' and st_within(way, (select way from planet_polygon where osm_id = -62407)) order by tags->'addr:street', tags->'addr:housenumber',tags->'name';
 * 
 *                  OFFEN                    bei wechsel stadt_id vorab delete from osm_hausnummern where stadt_id = xxx
 *
 *
 *
 *
 *
 *
 *
 *
 * Update-SQL, um an einem Member-Objekt einer associatedStreet-Relation die addr:street dranzuhängen
 * es läuft noch ein Test, was bei einem Update des Member-Objekts passiert (diese addr:street Anhängerei sollte wieder weg sein, bitte, bitte)
 * update planet_point set tags = tags || ('addr:street' => 'Elisenstraße') where osm_id = 748443965;


osm_mapnik=> SELECT * FROM planet_rels WHERE 'w88739014' = ANY(members) AND 'associatedStreet' = ANY(tags);
   id   | way_off | rel_off |                                        parts                                        |                                                                       members                                                                       |                   tags                    | pending 
--------+---------+---------+-------------------------------------------------------------------------------------+-----------------------------------------------------------------------------------------------------------------------------------------------------+-------------------------------------------+---------
 936907 |       2 |       9 | {748443965,748444095,4233712,70220253,88844081,88739011,88739007,88844077,88739014} | {w88739014,house,w88844077,house,w88739007,house,w88739011,house,w88844081,house,w70220253,house,w4233712,street,n748444095,house,n748443965,house} | {type,associatedStreet,name,Elisenstraße} | f
(1 row)

(END)




osm_mapnik=> select osm_id,tags from planet_point where osm_id = 748443965;
  osm_id   |                                                                                                        tags                                                                                                        
-----------+--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 748443965 | "name"=>"Meistro Imbiss", "amenity"=>"fast_food", "cuisine"=>"kebab", "addr:city"=>"Augsburg", "addr:state"=>"Bayern", "wheelchair"=>"no", "addr:country"=>"DE", "addr:postcode"=>"86159", "addr:housenumber"=>"2"
(1 row)

osm_mapnik=> update planet_point set tags = tags || ('addr:street' => 'Elisenstraße') where osm_id = 748443965;
UPDATE 1
osm_mapnik=> select osm_id,tags from planet_point where osm_id = 748443965;
  osm_id   |                                                                                                                       tags                                                                                                                        
-----------+---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 748443965 | "name"=>"Meistro Imbiss", "amenity"=>"fast_food", "cuisine"=>"kebab", "addr:city"=>"Augsburg", "addr:state"=>"Bayern", 
 "wheelchair"=>"no", 
 "addr:street"=>"Elisenstraße", "addr:country"=>"DE", "addr:postcode"=>"86159", "addr:housenumber"=>"2"
(1 row)



osm_mapnik=> select osm_id,tags from planet_polygon where osm_id = 88844081;
  osm_id  |                                                                                  tags                                                                                  
----------+------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 88844081 | "building"=>"yes", "way_area"=>"235.896700", "addr:city"=>"Augsburg", "addr:state"=>"Bayern", "addr:country"=>"DE", "addr:postcode"=>"86159", "addr:housenumber"=>"4a"
(1 row)

osm_mapnik=> update planet_polygon set tags = tags || ('addr:street' => 'Elisenstraße') where osm_id = 88844081;
UPDATE 1
osm_mapnik=> select osm_id,tags from planet_polygon where osm_id = 88844081;
  osm_id  |                                                                                                 tags                                                                                                  
----------+-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
 88844081 | "building"=>"yes", "way_area"=>"235.896700", "addr:city"=>"Augsburg", "addr:state"=>"Bayern", "addr:street"=>"Elisenstraße", "addr:country"=>"DE", "addr:postcode"=>"86159", "addr:housenumber"=>"4a"
(1 row)

osm_mapnik=> 



 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * OFFEN 06.05.2013: die associatedStreet Relations kommen nicht in der planet_polygon an 
 * (OSM-Wiki: OSM object type: can be node, way or both separated by a comma. way will also apply to relations with type=multipolygon, type=boundary, or type=route; 
 * all other relations are ignored by osm2pgsql. 
 * Quelle: http://wiki.openstreetmap.org/wiki/Osm2pgsql#Import_style)
 *
 * 	Hack: Anfrage je gefundenes Straßenstück: select id,parts from planet_rels where tags @> '{associatedStreet}' and members @> '{w51045195}';
 * 
 * 
	V4.0, 03.05.2013, Dietmar Seifert
		* Portierung auf regio-osm.de und Berücksichtigung große DB, nicht mehr Snapshot mit den genau passenden Gebieten und sonst nichts

	V3.0, 28.12.2011
		* Anwendung osm2pgsql kompatibel machen.
		OFFEN: einige Spezialanfragen sind ohne ST_Covers Zusatz. Prüfen, ob das richtig ist

	V2.0, 22.02.2011
		* neue Version kurz vor Produktionsreife, die andere DB-Strukturen nimmt als die Version 1 und über Augsburg hinaus funktioniert
		* bei den Abfragen wird ab jetzt die geometrische Zugehörigkeit geprüft über ST_Covers, ob innerhalb Gebiet des Job-Polygons
		* direkt danach wurde bei Wegen noch für die bbox der geometrische Mittelpunkt genommen mit ST_Centroid, weil sonst in einigen Fällen ein Gebäude
		  komplett in einem Polygon als nicht komplett enthalten vorkam, wenn nur eine Koordinate der bbox außerhalb des Polygons war


OFFEN: relation, type=multipolygon, addr:housenumber=*
		kommt nur einmal in ganz Augsburg vor, ist aber gültig

	V1.1, 30.01.2011
		*	bei Relationen: jetzt über alle Relationen zu einer Straße, weil es mehrere geben kann
			außerdem das zweistufig Verfahren auf eine Stufe reduziert: bisher relationsmember holen und dann je Objekt nochmal sql-Anfrage, jetzt zusammengefasst

	V1.0 28.01.2011
		* Erstellung

*/

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import java.util.HashMap;
import java.util.TreeMap;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.zalando.typemapper.postgres.HStore;
import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.theoreticalhousenumbercore.util.Workcache;
import de.regioosm.theoreticalhousenumbercore.util.Workcache_Entry;


public class TheoreticalHousenumbers {
	static Applicationconfiguration configuration = new Applicationconfiguration();
	private static final Logger logger = Logger.getLogger(TheoreticalHousenumbers.class.getName());
	static Connection con_listofstreets;
	static Connection con_hausnummern;
	static Connection con_mapnik;


	static Integer anzahl_objects_associatedstreet_incache = 0;
	static TreeMap<String,String> associatedstreet_object_map = new TreeMap<String,String>();

	/**
	 * find out, if OSM object belong to an associatedStreet relation.
	 * This need a lot of DB query time, because this relation type is only in import tables and without DB indexes.
	 * Therefor, this mode is normally not used here, but instead a one-time-a-month action 
	 * with specialised standalone program TheoreticalHousenumbers
	 * 
	 * @param osm_objecttype:	one character abbreviation of n(ode), w(ay) or r(elation)
	 * @param osmid:			osm object id, for which the missing street will be searched for
	 * @return:					name of street, if found. Otherwise null
	 */
	public static String hol_strassenname_zur_id( String osm_objecttype, Long osmid) {
		String out_strassenname = null;

		java.util.Date time_debug_starttime;
		java.util.Date time_debug_endtime;

		String search_objectid = osm_objecttype + osmid.toString();

		if( associatedstreet_object_map.get(search_objectid) != null) {
			out_strassenname = associatedstreet_object_map.get(search_objectid);
			System.out.println("foundentryinassociatedstreet-map ["+search_objectid+"] ==="+out_strassenname+"===");
			anzahl_objects_associatedstreet_incache++;
			return out_strassenname;
		}

		try {
			String sqlquery_indirect_associatedstreetobjects = "";
			sqlquery_indirect_associatedstreetobjects = "SELECT id, tags AS tagsalltogether, members";
			sqlquery_indirect_associatedstreetobjects += " FROM planet_rels";
			sqlquery_indirect_associatedstreetobjects += " WHERE ? = ANY(members)";
			sqlquery_indirect_associatedstreetobjects += " AND 'associatedStreet' = ANY(tags);";
			System.out.println("Case polygon_object: sqlquery_indirect_associatedstreetobjects ==="+sqlquery_indirect_associatedstreetobjects+"===");
			PreparedStatement stmt_indirect_associatedstreetobjects = con_mapnik.prepareStatement(sqlquery_indirect_associatedstreetobjects);
			stmt_indirect_associatedstreetobjects.setString(1, osm_objecttype + osmid.toString());
	
			time_debug_starttime = new java.util.Date();
			ResultSet rs_indirect_associatedstreetobjects = stmt_indirect_associatedstreetobjects.executeQuery();
			time_debug_endtime = new java.util.Date();
			System.out.println("Time: sqlquery_indirect_associatedstreetobjects in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
			time_debug_starttime = new java.util.Date();
	
			while( rs_indirect_associatedstreetobjects.next() ) {
				System.out.println("tagsalltogether ==="+rs_indirect_associatedstreetobjects.getString("tagsalltogether")+"===");
				String[] tags_parts = (String[])  rs_indirect_associatedstreetobjects.getArray("tagsalltogether").getArray();
				System.out.println("Anzahl parts: "+tags_parts.length);
				for(Integer tupelindex = 0; tupelindex < tags_parts.length; tupelindex += 2) {
					String act_key = tags_parts[tupelindex];
					if(act_key.charAt(0) == '{')
						act_key = act_key.substring(1);
					if(act_key.charAt(act_key.length()-1) == '}')
						act_key = act_key.substring(0, act_key.length()-1);
					if(act_key.charAt(0) == '"')
						act_key = act_key.substring(1);
					if(act_key.charAt(act_key.length()-1) == '"')
						act_key = act_key.substring(0, act_key.length()-1);
					String act_value = tags_parts[tupelindex+1];
					if(act_value.charAt(0) == '{')
						act_value = act_value.substring(1);
					if(act_value.charAt(act_value.length()-1) == '}')
						act_value = act_value.substring(0, act_value.length()-1);
					if(act_value.charAt(0) == '"')
						act_value = act_value.substring(1);
					if(act_value.charAt(act_value.length()-1) == '"')
						act_value = act_value.substring(0, act_value.length()-1);
					System.out.println("Tupel  # "+tupelindex+":  [" + tags_parts[tupelindex] + "] ===" + tags_parts[tupelindex+1] + "===      wurde zu ["+act_key+"] ==="+act_value+"===");
					if(act_key.equals("name")) {
						out_strassenname = act_value;
						System.out.println("in relation found name=* with value ==="+out_strassenname+"===");
						break;
					}
				}

					// lets analyse members and store non-street members in appropriate memory arrays for fast access
				System.out.println("associatedstreet-member ==="+rs_indirect_associatedstreetobjects.getString("members")+"===");
				String[] member_parts = rs_indirect_associatedstreetobjects.getString("members").split(",");
				System.out.println("Anzahl member: "+member_parts.length);
				for(Integer tupelindex = 0; tupelindex < member_parts.length; tupelindex += 2) {
					String member_id = member_parts[tupelindex];
					if(member_id.charAt(0) == '{')
						member_id = member_id.substring(1);
					if(member_id.charAt(member_id.length()-1) == '}')
						member_id = member_id.substring(0, member_id.length()-1);
					if(member_id.charAt(0) == '"')
						member_id = member_id.substring(1);
					if(member_id.charAt(member_id.length()-1) == '"')
						member_id = member_id.substring(0, member_id.length()-1);
					String member_role = "";
					if((member_parts[tupelindex+1] != null) && member_parts[tupelindex+1].equals("")) {
						member_role = member_parts[tupelindex+1];
						if(member_role.charAt(0) == '{')
							member_role = member_role.substring(1);
						if(member_role.charAt(member_role.length()-1) == '}')
							member_role = member_role.substring(0, member_role.length()-1);
						if(member_role.charAt(0) == '"')
							member_role = member_role.substring(1);
						if(member_role.charAt(member_role.length()-1) == '"')
							member_role = member_role.substring(0, member_role.length()-1);
					}
					String objecttype = member_id.substring(0,1);
					//member_id = member_id.substring(1);
					System.out.println("Tupel  # "+tupelindex+":  [" + member_parts[tupelindex] + "] ===" + member_parts[tupelindex+1] + "===      wurde zu type ("+objecttype+")   ["+member_id+"] ==="+member_role+"===");
						// i could ignore objects with role = streets, but who knows, if a housenumber-object is set as a street
					if( associatedstreet_object_map.get(member_id) == null) {
						associatedstreet_object_map.put(member_id, out_strassenname);
						System.out.println("add to associatedstreet-map ["+member_id+"] ==="+out_strassenname+"===");
					}
				}
			} // end of loop over all records which are possible associatedStreet
			rs_indirect_associatedstreetobjects.close();
			stmt_indirect_associatedstreetobjects.close();
		}
		catch( SQLException e) {
			e.printStackTrace();
		}
		return out_strassenname;
	}

	
	private static String[] Hausnummernbereich_aufloesen(String hausnummertext) {
		List<String>	hausnummern_array = new ArrayList<String>();

		if(hausnummertext.indexOf("-") == -1) {
			hausnummern_array.add(hausnummertext);
		} else {
			try {
				Integer bindestrich_pos = hausnummertext.indexOf("-");
//TODO parseInt fails for 28d for example
				Integer hausnummer_start_int = Integer.parseInt(hausnummertext.substring(0,bindestrich_pos).trim());
				Integer hausnummer_ende_int = Integer.parseInt(hausnummertext.substring(bindestrich_pos+1).trim());
				if(hausnummer_ende_int > hausnummer_start_int) {
					for(Integer hausnummerindex=hausnummer_start_int; hausnummerindex <= hausnummer_ende_int; hausnummerindex+=2)
						hausnummern_array.add(hausnummerindex.toString());
				}
			} catch( NumberFormatException e) {
				System.out.println("Error during parsing text to integer, text was ==="+hausnummertext+"===");
				e.printStackTrace();
			}

		}
		String[] return_string_array = new String[hausnummern_array.size()];
		return_string_array = hausnummern_array.toArray(return_string_array);
		return return_string_array;
	}



	static private String polygon_holen(String akt_gemeindename, String akt_gemeindeschluessel) {
		String actual_polygon_part = "";
		String complete_polygon = "";
		String complete_polygon_idlist = "";

		String gemeindeschluessel_key = "de:amtlicher_gemeindeschluessel";

		String akt_gemeindename_ohne_suffix = akt_gemeindename;
		if(akt_gemeindename_ohne_suffix.indexOf("(") != -1) {
			akt_gemeindename_ohne_suffix = akt_gemeindename_ohne_suffix.substring(0,akt_gemeindename_ohne_suffix.indexOf("(")).trim();
		}
		
		String selectbefehl_relation = "";
		selectbefehl_relation = "SELECT osm_id AS id,";
		selectbefehl_relation += " name, tags->'name:prefix' AS name_prefix, tags->'name:suffix' AS name_suffix,";
		selectbefehl_relation += " tags-> ? AS gemeindeschluessel,";
		selectbefehl_relation += " boundary, tags->'is_in' AS is_in, tags->'is_in:state' AS is_in_state, tags->'is_in:country' AS is_in_country,";
		selectbefehl_relation += " tags->'is_in:region' AS is_in_region, tags->'is_in:county' AS is_in_county,";
		selectbefehl_relation += " way AS polygon_geometry";
		selectbefehl_relation += " FROM planet_polygon";
		selectbefehl_relation += " WHERE";
		selectbefehl_relation += " (tags @> 'boundary=>administrative') AND";
		selectbefehl_relation += " (tags @> hstore(?, ?))";

		try {
			String local_adminid1 = "";
			String local_adminid2 = "";
				// only in Germany: variant without suffix 0
			if(gemeindeschluessel_key.equals("de:amtlicher_gemeindeschluessel")) {
				local_adminid1 = akt_gemeindeschluessel;
				while(( local_adminid1.length() >= 1) && (local_adminid1.substring(local_adminid1.length()-1,local_adminid1.length()).equals("0")))
					local_adminid1 = local_adminid1.substring(0,local_adminid1.length()-1);
				if( ! akt_gemeindeschluessel.equals(local_adminid1))
					selectbefehl_relation += " OR (tags @> hstore('de:amtlicher_gemeindeschluessel', ?))";
					// Variant with some spacens "03 1 52 012" (for example G�ttingen)
				local_adminid2 = akt_gemeindeschluessel;
				local_adminid2 = local_adminid2.substring(0,2) + " " + local_adminid2.substring(2,3) + " " + local_adminid2.substring(3,5) + " " + local_adminid2.substring(5);
				selectbefehl_relation += " OR (tags @> hstore('de:amtlicher_gemeindeschluessel', ?)) ";
			}
			selectbefehl_relation += " ORDER BY osm_id;";
	
			System.out.println("Ausgabe selectbefehl_relation ==="+selectbefehl_relation+"===");
			PreparedStatement stmt_relation = con_mapnik.prepareStatement(selectbefehl_relation);
			Integer statementIndex = 1;
			stmt_relation.setString(statementIndex++, gemeindeschluessel_key);
			stmt_relation.setString(statementIndex++, gemeindeschluessel_key);
			stmt_relation.setString(statementIndex++, akt_gemeindeschluessel);
			if(gemeindeschluessel_key.equals("de:amtlicher_gemeindeschluessel")) {
				if( ! akt_gemeindeschluessel.equals(local_adminid1)) {
					stmt_relation.setString(statementIndex++, local_adminid1);
				}
				stmt_relation.setString(statementIndex++, local_adminid2);
			}
			
			ResultSet rs_relation = stmt_relation.executeQuery();
			Integer count_relations = 0;
			Integer count_correct_relations = 0;
	//ToDo relation_wrong should be written to gebiete table with optionally info about bad polygon
			String relation_wrong = "";
			String municipality_officialkeysid = "";
			int municipality_officialkeysid_origin_length = 0;		// origin found length (without spaces) for check longest original key as best
			String found_osm_relation_id = "";
				// loop over all parts of polygon (in osm2pgsql scheme, every single closed polygon has its own row, but all share same osm relation id)
			while( rs_relation.next() ) {
				count_relations++;
				System.out.println("Relation #"+count_relations+":  id==="+rs_relation.getString("id")+"===  name ==="+rs_relation.getString("name")+"===   gemeindeschluessel ==="+rs_relation.getString("gemeindeschluessel")+"===  boundary ==="+rs_relation.getString("boundary")+"===");
				if((rs_relation.getString("name_prefix") != null) || (rs_relation.getString("name_suffix") != null)) {
	//ToDo Prefix und Suffix ergänzen, um Objekt eindeutig zu identifizieren
					System.out.println(" (Forts.) Name-Zusatz   name:prefix ==="+rs_relation.getString("name_prefix")+"===    name:suffix ==="+rs_relation.getString("name_suffix")+"===");
				}
				
				if((rs_relation.getString("boundary") == null) || ( ! rs_relation.getString("boundary").equals("administrative"))) {
					System.out.print("Warning: Relation-ID is invalid. It's not boundary=administrative, but ");
					if(rs_relation.getString("boundary") == null) {
						relation_wrong = "error-relation-no-boundary-tag";
						System.out.println("is missing");
					} else {
						relation_wrong = "error-relation-wrong-boundary-value";
						System.out.println("has wrong value ==="+rs_relation.getString("boundary")+"===");
					}
					continue;
				}
				
					// check actual administrationid and normalize it to length=8
				String local_actual_municipality_officialkeysid = "";
				int local_actual_municipality_officialkeysid_origin_length = 0;
				if(rs_relation.getString("gemeindeschluessel") != null) {
					local_actual_municipality_officialkeysid = rs_relation.getString("gemeindeschluessel");
					if(( (! local_actual_municipality_officialkeysid.equals("")) && local_actual_municipality_officialkeysid.indexOf(" ") != -1))
						local_actual_municipality_officialkeysid = local_actual_municipality_officialkeysid.replace(" ","");
					local_actual_municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid.length();
					if(( (! local_actual_municipality_officialkeysid.equals("")) && local_actual_municipality_officialkeysid.length() < 8)) {
						String local_string0 = "00000000";
						System.out.println("Warning: german gemeindeschluessel too short, will be appended by 0s  original ==="+local_actual_municipality_officialkeysid+"=== for municipality ==="+akt_gemeindename+"=== in relation");
						local_actual_municipality_officialkeysid += local_string0.substring(0,(8 - local_actual_municipality_officialkeysid.length()));
						System.out.println("  (cont.) german gemeindeschluessel after appended ==="+local_actual_municipality_officialkeysid+"=== for municipality ==="+akt_gemeindename+"=== in relation");
					}
					if(( (! local_actual_municipality_officialkeysid.equals("")) && (local_actual_municipality_officialkeysid.length() > 8))) {
						System.out.println("ERROR german gemeindeschluessel too long ==="+local_actual_municipality_officialkeysid+"=== for municipality ==="+akt_gemeindename+"=== in relation");
						local_actual_municipality_officialkeysid = "";
						local_actual_municipality_officialkeysid_origin_length = 0;
					}
				}
			
				if(found_osm_relation_id.equals("")) {
					// first usable relation-id
				found_osm_relation_id = rs_relation.getString("id");
				municipality_officialkeysid = local_actual_municipality_officialkeysid;
				municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid_origin_length;
				} else {
						// if not first found polygon-part and new relation (so really different boundary-polygon): check, if better than previous found one
					if(	( ! found_osm_relation_id.equals("")) && ( ! found_osm_relation_id.equals(rs_relation.getString("id")))) {
							// if actual administation-id is longer then before, use the actual, its more precise, hopefully
						if(local_actual_municipality_officialkeysid_origin_length > municipality_officialkeysid_origin_length) {
							System.out.println("ok, actual relation is origin-length longer ("+local_actual_municipality_officialkeysid_origin_length+") ");
							System.out.println("  (cont) than previous found ("+municipality_officialkeysid_origin_length+")");
							System.out.println("  (cont) previous found municipality was ==="+municipality_officialkeysid+"===");
							System.out.println("  (cont) actual municipality is ==="+local_actual_municipality_officialkeysid+"===");
							System.out.println("  (cont) previous found osm relation id was ==="+found_osm_relation_id+"===");
							System.out.println("  (cont) actual osm relation id is ==="+rs_relation.getString("id")+"===");
							System.out.println("  (cont) so get now the actual better ones!");
							municipality_officialkeysid = local_actual_municipality_officialkeysid;
							municipality_officialkeysid_origin_length = local_actual_municipality_officialkeysid_origin_length;
							found_osm_relation_id = rs_relation.getString("id");
						} else if(	(local_actual_municipality_officialkeysid_origin_length == municipality_officialkeysid_origin_length) &&
											 	(akt_gemeindename.equals(rs_relation.getString("name")))
										) {
							municipality_officialkeysid = local_actual_municipality_officialkeysid;
							found_osm_relation_id = rs_relation.getString("id");
							System.out.println("ok, actual relation is equal length in administrationid, but name is equals to municipality should-be-name, so i use now osm relation-id ==="+found_osm_relation_id+"===");
							count_correct_relations = 0;	// reset number correct relations to 0, so new start to get polygon(parts)
						} else {
							System.out.println("actual relation is other than before and orgin key length is not longer thant before, so actual one will be ignored");
							continue;
						}
					}
				}
			
				count_correct_relations++;
	
				actual_polygon_part = rs_relation.getString("polygon_geometry");
	
	
				if( rs_relation.getString("name") != null && (! rs_relation.getString("name").equals(akt_gemeindename))) {
					String local_name_test = rs_relation.getString("name");
					if(local_name_test.indexOf(akt_gemeindename) == 0) {
						String local_name_suffix = local_name_test.replace(akt_gemeindename,"");
						if( ! local_name_suffix.equals("")) {
							System.out.println("place objects hat municipality name ==="+akt_gemeindename+"=== but contains suffix ==="+local_name_suffix+"=== ...");
							boolean name_suffix_ok = false;
							if(local_name_suffix.indexOf(" im ") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf(" ob ") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf(" bei ") == 0)
								name_suffix_ok =true;
							if(local_name_suffix.indexOf(", Stadt") == 0)
								name_suffix_ok =true;
							if( ! name_suffix_ok) {
								System.out.println("WARNING: Difference in name of relation ==="+rs_relation.getString("name")+"=== to name of municipality ==="+akt_gemeindename+"===");
							}											
						}
					}
				}
	
	
				if( ! municipality_officialkeysid.equals(""))
					System.out.println("Info: got municipality administrationid (german gemeindeschluessel) ==="+municipality_officialkeysid+"===");
				if(count_correct_relations == 1) {
						// polygon first or only part, safe in extra variable
					complete_polygon = actual_polygon_part;
					complete_polygon_idlist = rs_relation.getString("id");
					System.out.println("ok, got first part of relation");
				} else if(count_correct_relations > 1) {
					System.out.println("ok, got another part of relation");
	
					// polygon another part, now union with previous part(s) as a multipolygon
					System.out.println("Warning: got more than one relation, Select statement was ==="+selectbefehl_relation+"===");
	
	
					String temp_create_multipolygon_sql = "SELECT ST_AsText(?) as completepoly, ST_AsText(?) as polypart;";
					System.out.println("temp_create_multipolygon_sql==="+temp_create_multipolygon_sql+"===");
					try {
						PreparedStatement temp_stmt_create_multipolygon = con_hausnummern.prepareStatement(temp_create_multipolygon_sql);
						temp_stmt_create_multipolygon.setString(1, complete_polygon);
						temp_stmt_create_multipolygon.setString(2, actual_polygon_part);
						ResultSet temp_rs_create_multipolygon = temp_stmt_create_multipolygon.executeQuery();
						if( temp_rs_create_multipolygon.next() ) {
							System.out.println("ok, completepoly ==="+temp_rs_create_multipolygon.getString("completepoly")+"===");
							System.out.println("ok, polypart ==="+temp_rs_create_multipolygon.getString("polypart")+"===");
						}
						temp_rs_create_multipolygon.close();
						temp_stmt_create_multipolygon.close();
					}	// ende try DB-connect
					catch( SQLException e) {
						System.out.println("ERROR occured when tried to get both polygon-parts as text. SQL-Statement was ==="+temp_create_multipolygon_sql+"===");
						e.printStackTrace();
						relation_wrong = "invalid-multipolygon";
						String local_messagetext = "actual polygon-part with osm-id " + rs_relation.getString("id") + " could not be get as ST_AsText from database";
						//new LogMessage(LogMessage.CLASS_ERROR, "create_municipality_polygons", rs_municipalities.getString("name"), rs_municipalities.getLong("id"), local_messagetext);
					}
	
	
					//production String create_multipolygon_sql = "SELECT ST_Union('"+complete_polygon+"','"+actual_polygon_part+"') as unionpolygon, ";
					String create_multipolygon_sql = "SELECT ST_Union(?::geometry, ?::geometry) as unionpolygon, ";
					create_multipolygon_sql += "ST_AsText(ST_Union(?::geometry, ?::geometry)) as justfordebug_unionpolygon_astext;";
					System.out.println("create_multipolygon_sql==="+create_multipolygon_sql+"===");
					try {
						PreparedStatement stmt_create_multipolygon = con_hausnummern.prepareStatement(create_multipolygon_sql);
						stmt_create_multipolygon.setString(1, actual_polygon_part);
						stmt_create_multipolygon.setString(2, complete_polygon);
						stmt_create_multipolygon.setString(3, complete_polygon);
						stmt_create_multipolygon.setString(4, actual_polygon_part);
						ResultSet rs_create_multipolygon = stmt_create_multipolygon.executeQuery();
						if( rs_create_multipolygon.next() ) {
							complete_polygon_idlist += "," + rs_relation.getString("id");	// add actual polygon osm-id to list of all unioned-polygons
							System.out.println("ok, union polygon ==="+rs_create_multipolygon.getString("justfordebug_unionpolygon_astext")+"===");
							complete_polygon = rs_create_multipolygon.getString("unionpolygon");
						}
						rs_create_multipolygon.close();
						stmt_create_multipolygon.close();
					}	// ende try DB-connect
					catch( SQLException e) {
						System.out.println("ERROR occured when tried to create ST_Union Polygon");
						System.out.println(" (cont.) here original stack ==="+e.toString()+"===");
						e.printStackTrace();
						relation_wrong = "invalid-multipolygon";
					}
				}
			} // end over loop of all found relation-records (ends now earlier from 17.11.2011) - while( rs_relation.next() ) {
			rs_relation.close();
			stmt_relation.close();
		}
		catch( SQLException e) {
			e.printStackTrace();
			return "";
		}
		
		return complete_polygon;
	}


	private static void Protokoll(String filename, String outputtext) {
		String completefilename = "Protokoll-" + filename + ".log";
		try {
			PrintWriter fileoutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(completefilename, true),StandardCharsets.UTF_8)));
			fileoutput.println(outputtext);
			fileoutput.close();
		} catch (IOException ioerror) {
			System.out.println("ERROR: couldn't open file to write, filename was ===" + filename + "===");
			ioerror.printStackTrace();
		}
	}
	

	public static void main(String args[]) {

		java.util.Date time_debug_starttime;
		java.util.Date time_debug_endtime;

		PrintWriter osmOutput = null;
		String osmOutputFilename = "";
		PrintWriter sqlOutput = null;
		String sqlOutputFilename = "";
		String protokolloutput = "";

		final String TAG_ADDRESSINCOMPLETE = "___temp___addressincomplete";
		final String TAG_ADDRESSASSOCSTREETADDED = "___temp___addressstreetfromassociatedrel";	// Tag, that will be added to addresses, where addr:street was set in osm via associatedStreet Relation 

		DateFormat date_formatter = new SimpleDateFormat("yyyy-MM-dd");		// in iso8601 format, with timezone
		DateFormat time_formatter_iso8601 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");		// in iso8601 format, with timezone

		java.util.Date time_programm_starttime = new java.util.Date();
		java.util.Date time_programm_endtime;
		long insert_osm_hausnummern_ms = 0L;
		System.out.println("Programm-Startzeitpunkt OSM_Hausnummernermittlung: "+time_programm_starttime.toString());

		HStore hstore = null;
		HashMap<String,String> tags = new HashMap<String,String>();

		Integer anzahl_nodes_addrstreet_treffer = 0;
		Integer anzahl_ways_addrstreet_treffer = 0;
		Integer anzahl_polygons_addrstreet_treffer = 0;
		Integer anzahl_nodes_associatedstreet_versuche = 0;
		Integer anzahl_nodes_associatedstreet_treffer = 0;
		Integer anzahl_ways_associatedstreet_treffer = 0;
		Integer anzahl_polygons_associatedstreet_versuche = 0;
		Integer anzahl_polygons_associatedstreet_treffer = 0;
		Integer anzahl_neu_durch_assiociatedstreets_query = 0;
		Integer anzahl_changed_durch_assiociatedstreets_query = 0;

		String osmosis_laststatefile = configuration.osmosis_laststatefile;

		String parameterAgsarea = "%";
		String parameterStartTimestamp = "";
		boolean parameterSearchForAssociatedStreetRelations = false;

		if ((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-agsarea german Gemeindeschlüssel, can include Wildcard %, for example 081%");
			System.out.println("-startdate: must have form 2016-02-01");
			System.out.println("-lookforassociatedstreetrelations yes/no: should incomplete addresses looked for associateStreet Relation members (default: no)");
			return;
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			System.out.println("args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}

		if (args.length >= 1) {
			int argsOkCount = 0;
			for (int argsi = 0; argsi < args.length; argsi += 2) {
				System.out.print(" args pair analysing #: " + argsi + "  ===" + args[argsi] + "===");
				if (args.length > (argsi + 1)) {
					System.out.print("  args #+1: " + (argsi + 1) + "   ===" + args[argsi + 1] + "===");
				}
				System.out.println("");

				if (args[argsi].equals("-agsarea")) {
					parameterAgsarea = args[argsi + 1];
					argsOkCount += 2;
				} else  if (args[argsi].equals("-startdate")) {
					parameterStartTimestamp = args[argsi + 1];
					argsOkCount += 2;
				} else  if (args[argsi].equals("-lookforassociatedstreetrelations")) {
					if(		(args[argsi + 1].toLowerCase().equals("ja"))
						||	(args[argsi + 1].toLowerCase().equals("yes"))) {
						parameterSearchForAssociatedStreetRelations = true;
					} else {
						parameterSearchForAssociatedStreetRelations = false;
					}
					argsOkCount  += 2;
				} else {
					System.out.println("unknown program input parameter ===" + args[argsi] + "===");
				}
			}
			if (argsOkCount != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}

		if(parameterStartTimestamp.equals("")) {
			System.out.println("Error: program input parameter -startdate missing, must be set. Program ended.");
			return;
		}
		
		java.util.Date parameterStartTimestampDate = null;
		try {
			parameterStartTimestampDate = date_formatter.parse(parameterStartTimestamp);
		} catch (ParseException e1) {
			System.out.println("Error: program input parameter -startdate contains invalid date, please correct and start program again. Program ended.");
			System.out.println(e1.toString());
			return;
		}
		
		try {
			Handler handler = new ConsoleHandler();
			handler.setLevel(configuration.logging_console_level);
			logger.addHandler(handler);
			FileHandler fhandler = new FileHandler(configuration.logging_filename);
			fhandler.setLevel(configuration.logging_file_level);
			logger.addHandler(fhandler);
			logger.setLevel(configuration.logging_console_level);
		} catch (IOException e) {
			System.out.println("Fehler beim Logging-Handler erstellen, ...");
			System.out.println(e.toString());
		}
		
		try {
			logger.log(Level.INFO, "ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			logger.log(Level.INFO, "ok, nach Class.forName Aufruf!");
		}
		catch(ClassNotFoundException e) {
			logger.log(Level.SEVERE,"Exception ClassNotFoundException, Details ...");
			logger.log(Level.SEVERE,e.toString());
			System.exit(1);
		}
		try {
			String url_listofstreets = configuration.db_application_listofstreets_url;
			con_listofstreets = DriverManager.getConnection(url_listofstreets, 
				configuration.db_application_listofstreets_username, configuration.db_application_listofstreets_password);
//TODO fix DB connection
			String url_hausnummern = configuration.db_application_url;
			con_hausnummern = DriverManager.getConnection(url_hausnummern, 
				configuration.db_application_username, configuration.db_application_password);

			String url_mapnik = configuration.db_osm2pgsql_url;
con_mapnik = DriverManager.getConnection(url_mapnik, "gis", configuration.db_osm2pgsql_password);
//orig con_mapnik = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);


			osmOutputFilename = "testallmunis.osm";
			sqlOutputFilename = "testallmunis.sql";

			osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(osmOutputFilename),StandardCharsets.UTF_8)));
			osmOutput.println("<?xml version='1.0' encoding='UTF-8'?>");
			osmOutput.println("<osm version='0.6' upload='false' generator='JOSM'>");
			osmOutput.close();
					
			Integer anzahl_datensaetze_total_kpl = 0;

			java.util.Date time_actjob_evaluation = null;
			java.util.Date time_osmdb = null;


			String sqlbefehl_jobs = "SELECT name_unique AS stadt, ags, country AS land, country.id AS land_id,"
				+ " bevoelkerungszahl, gliederungstadtland, flaechekm2"
				+ " FROM officialkeys, country"
				+ " WHERE"
				+ " level = 6"
				+ " AND officialkeys.country_id = country.id"
//TODO can be expanded to at least Austria in 09/2014: but be careful, other countries can have less columns filled in in table				
				+ " AND country.country = 'Bundesrepublik Deutschland'"
				+ " AND bevoelkerungszahl > 0"		// ignore municipalities, which have no people
				+ " AND ags LIKE ?"
				//			Sortierung mindestens nach stadt lassen, weil bei Stadtwechsel alle entsprechenden osm_hausnummern vorab gelöscht werden
				+ " ORDER BY hierarchy, name_unique;";

			logger.log(Level.FINE, "jobs Select-Anfrage ==="+sqlbefehl_jobs+"=== ...");
			PreparedStatement stmt_jobs = con_listofstreets.prepareStatement(sqlbefehl_jobs);
			stmt_jobs.setString(1, parameterAgsarea);
			int jobs_count = 0;
			String land = "";
			long land_id = -4711L;																	// WIRD HIER DAUERHAFT -1 BLEIBEN
			long stadt_id = -4711L;																// WIRD HIER DAUERHAFT -1 BLEIBEN
			long job_id = -4711L;																	// WIRD HIER DAUERHAFT -1 BLEIBEN
			String jobname = "";																	// WIRD HIER DAUERHAFT leer bleiben
			String ags = "";
			String gemeindename = "";
			int anzahl_adressen_nodes = 0;
			int anzahl_adressen_ways = 0;
			int anzahl_adressen_rels = 0;
			int anzahl_adressen_places = 0;
			int anzahl_adressen_nodes_unvollstaendig = 0;
			int anzahl_adressen_ways_unvollstaendig = 0;
			int anzahl_adressen_rels_unvollstaendig = 0;

			String ausgabetext = "";

			ResultSet rs_jobs = stmt_jobs.executeQuery();
				// Schleife über alle Jobs
			while( rs_jobs.next() ) {
				jobs_count++;

				time_actjob_evaluation = new java.util.Date();

				anzahl_adressen_nodes = 0;
				anzahl_adressen_ways = 0;
				anzahl_adressen_rels = 0;

				anzahl_nodes_addrstreet_treffer = 0;
				anzahl_ways_addrstreet_treffer = 0;
				anzahl_polygons_addrstreet_treffer = 0;
				anzahl_nodes_associatedstreet_versuche = 0;
				anzahl_nodes_associatedstreet_treffer = 0;
				anzahl_ways_associatedstreet_treffer = 0;
				anzahl_polygons_associatedstreet_versuche = 0;
				anzahl_polygons_associatedstreet_treffer = 0;
				anzahl_neu_durch_assiociatedstreets_query = 0;
				anzahl_changed_durch_assiociatedstreets_query = 0;
				anzahl_adressen_places = 0;
				anzahl_adressen_nodes_unvollstaendig = 0;
				anzahl_adressen_ways_unvollstaendig = 0;
				anzahl_adressen_rels_unvollstaendig = 0;
				StringBuffer adressenunvollstaendig_liste = new StringBuffer();

				ags = rs_jobs.getString("ags");
				gemeindename = rs_jobs.getString("stadt");
				land = rs_jobs.getString("land");
				land_id = rs_jobs.getInt("land_id");

				logger.log(Level.INFO, "BEGINN Job # "+jobs_count+"  Gemeinde: "+rs_jobs.getString("stadt")+"    AGS: " + rs_jobs.getString("ags") + "   Land: "+rs_jobs.getString("land")+" ...");

				Workcache osmhausnummerncache = new Workcache();	// define and initiate here, so start new at every job start
				logger.log(Level.INFO, "Anzahl im Cache bei Job-Start und init: "+osmhausnummerncache.count()+"   "+osmhausnummerncache.count_unchanged());
				osmhausnummerncache.clear();


				String result_sqlbefehl = "SELECT id, tstamp FROM theoevaluations"
					+ " WHERE gemeinde_id = ? AND"
					+ " tstamp > ?;";
				PreparedStatement stmt_result = con_hausnummern.prepareStatement(result_sqlbefehl);
				stmt_result.setString(1, rs_jobs.getString("ags"));
				stmt_result.setDate(2, new java.sql.Date(parameterStartTimestampDate.getTime()));
				ResultSet rs_result = stmt_result.executeQuery();
				if(rs_result.next()) {
					logger.log(Level.INFO, "Auswertung wird ignoriert für Gemeinde: "+rs_jobs.getString("stadt")+"    AGS: " + rs_jobs.getString("ags") 
						+ "   Land: "+rs_jobs.getString("land") + "  theoeval DB-id: " + rs_result.getLong("id") + "   tstamp: " + rs_result.getString("tstamp"));
					continue;
				}
				

				
					// get the actual OSM DB timestamp of last diff-importfile for the local OSM DB Timestamp
				try {
				    BufferedReader filereader = new BufferedReader(new InputStreamReader(new FileInputStream(osmosis_laststatefile), StandardCharsets.UTF_8));
							//#Fri Sep 21 07:39:59 CEST 2012
							//sequenceNumber=121
							//timestamp=2012-09-17T08\:00\:00Z
				    String dateizeile = "";
				    while ((dateizeile = filereader.readLine()) != null) {
						if (dateizeile.indexOf("timestamp=") == 0) {
							String local_time = dateizeile.substring(dateizeile.indexOf("=")+1);
								// remove special charater \: to :
							local_time = local_time.replace("\\","");
								// change abbreviation Z to +00:00, otherwise parsing fails
							local_time = local_time.replace("Z","+0000");
							logger.log(Level.FINEST, "local_time ===" + local_time + "===");
							java.util.Date temp_time = new java.util.Date();
							logger.log(Level.FINEST, "test zeit iso8601 ===" + time_formatter_iso8601.format(temp_time) + "===");

							time_osmdb = time_formatter_iso8601.parse(local_time);
						}
					}
					filereader.close();
				}
				catch (Exception e) {
					logger.log(Level.SEVERE, "ERROR: failed to read osmosis last.state.txt file ==="+osmosis_laststatefile+"===");
					logger.log(Level.SEVERE, e.toString());
					return;
				}


					// Hol das Geometrie-Polygon zum aktuellen Job
				String gebietsgeometrie = "";

				gebietsgeometrie = polygon_holen(gemeindename, ags);
				if(gebietsgeometrie.equals("")) {
					System.out.println("Warnung: kein Polygon verfügbar für Gemeinde " + gemeindename + ", wird ignoriert");
					protokolloutput = "Gemeinde ===" + gemeindename + "===   ags ===" + ags + "===";
					Protokoll("PolygonNichtGefunden", protokolloutput);
					continue;
				}
				String adminboundary_centroid_lat = "";
				String adminboundary_centroid_lon = "";
				String sqlbefehl_admincenter = "SELECT ST_X(ST_Transform(ST_Centroid(?::geometry),4326)) AS lon,"
					+ " ST_Y(ST_Transform(ST_Centroid(?::geometry),4326)) AS lat;";
				PreparedStatement stmt_admincenter = con_mapnik.prepareStatement(sqlbefehl_admincenter);
				stmt_admincenter.setString(1, gebietsgeometrie);
				stmt_admincenter.setString(2, gebietsgeometrie);
				ResultSet rs_admincenter = stmt_admincenter.executeQuery();
				if( rs_admincenter.next() ) {
					adminboundary_centroid_lat = rs_admincenter.getString("lat");
					adminboundary_centroid_lon = rs_admincenter.getString("lon");
				}
				rs_admincenter.close();
				stmt_admincenter.close();

				Integer anzahl_datensaetze_kpl = 0;


					// Hol alle Hausnummern an Polygon-Objekten (in osm ways)
				String sqlbefehl_objekte = "";
				sqlbefehl_objekte = "SELECT osm_id AS id, tags->'addr:housenumber' AS hausnummer, tags->'addr:street' AS strasse, tags->'addr:place' AS place,";
				sqlbefehl_objekte += " tags->'building' AS building, tags AS tags_string, ST_AsText(ST_Transform(ST_Centroid(way),4326)) as lonlat,";
				sqlbefehl_objekte += " tags -> ? AS addressincomplete, tags -> ? AS streetfromassociatedrel";
				sqlbefehl_objekte += " FROM planet_polygon WHERE";
				sqlbefehl_objekte += " (ST_Covers(?::geometry, way)";
				sqlbefehl_objekte += ") AND";
				sqlbefehl_objekte += " exist(tags, 'addr:housenumber')";
				sqlbefehl_objekte += " ORDER BY tags->'addr:street',tags->'addr:housenumber';";
				System.out.println("sqlbefehl_objekte (polygons) ==="+sqlbefehl_objekte+"===");
				PreparedStatement stmt_objekte = con_mapnik.prepareStatement(sqlbefehl_objekte);
				stmt_objekte.setString(1, TAG_ADDRESSINCOMPLETE);
				stmt_objekte.setString(2, TAG_ADDRESSASSOCSTREETADDED);
				stmt_objekte.setString(3, gebietsgeometrie);

				ResultSet rs_objekte;
				time_debug_starttime = new java.util.Date();
				try {
					rs_objekte = stmt_objekte.executeQuery();
				}
				catch( SQLException e) {
					System.out.println("SQL-Fehler beim suche hausnummer-Objekte in planet_polygon mit sql-statement ===" + sqlbefehl_objekte + "=== Details ...");
					System.out.println(e.toString());
					continue;
				}
				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: sqlbefehl_objekte in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) Gesamtzeit in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();

				Integer anzahl_datensaetze = 0;
				while( rs_objekte.next() ) {
					String temp_aktstrasse = rs_objekte.getString("strasse");
					String temp_aktplace = rs_objekte.getString("place");
					String temp_akthausnummer = rs_objekte.getString("hausnummer");
					
					if(rs_objekte.getString("addressincomplete") != null) {
						if(rs_objekte.getLong("id") < 0) {
							anzahl_adressen_rels_unvollstaendig++;
							adressenunvollstaendig_liste.append("r" + (-1 * rs_objekte.getLong("id")) + ",");
						} else {
							anzahl_adressen_ways_unvollstaendig++;
							adressenunvollstaendig_liste.append("w" + rs_objekte.getLong("id") + ",");
						}
						logger.log(Level.FINEST, "INFO planet_polygon theoretischer Kandidat ist bereits als unvollständige Address markiert und wird ignoriert");
						continue;
					}

					if(temp_akthausnummer == null)
						continue;

					if(temp_aktstrasse != null) {
						if(rs_objekte.getLong("id") < 0) {
							anzahl_polygons_addrstreet_treffer++;
						} else {
							anzahl_ways_addrstreet_treffer++;
						}
					}

					if(rs_objekte.getString("streetfromassociatedrel") != null) {
						if(rs_objekte.getLong("id") < 0) {
							anzahl_polygons_associatedstreet_treffer++;
						} else {
							anzahl_ways_associatedstreet_treffer++;
						}
					}

						// wenn addr:street fehlt, dann prüfen, ob das Objekt zu einer relation associatedStreet gehört
					if((temp_aktstrasse == null) && (temp_aktplace == null)) {
						if(!parameterSearchForAssociatedStreetRelations) {
							if(rs_objekte.getLong("id") < 0) {
								adressenunvollstaendig_liste.append("r" + (-1 * rs_objekte.getLong("id")) + ",");
								anzahl_adressen_rels_unvollstaendig++;
							} else {
								adressenunvollstaendig_liste.append("w" + rs_objekte.getLong("id") + ",");
								anzahl_adressen_ways_unvollstaendig++;
							}
							continue;
						} else {
							logger.log(Level.FINE, "INFO planet_polygon Kandidat fuer addr-Polygon mit associatedStreet, way-id: "+rs_objekte.getLong("id")+"===");
							anzahl_polygons_associatedstreet_versuche++;
							temp_aktstrasse = hol_strassenname_zur_id("w",rs_objekte.getLong("id"));
							if(temp_aktstrasse != null) {
								if(temp_aktstrasse != null) {
									if(rs_objekte.getLong("id") < 0) {
										anzahl_polygons_associatedstreet_treffer++;
									} else {
										anzahl_ways_associatedstreet_treffer++;
									}
								}
							} else {
// TODO rausschreiben in eine log-tabelle, damit User über diese potentiellen Kandidaten informiert werden
								logger.log(Level.FINE, "Keine-Strasse-ermittelbar an relations-Objekte osm_id ==="+rs_objekte.getString("id"));
	
								String updateobject_sqlbefehl = "UPDATE planet_polygon SET tags ="
									+ " tags || ? => 'yes'::hstore"
									+ " WHERE osm_id = ?;";
								try {
									logger.log(Level.FINEST, "SQL-Update Befehl für update planet_polygon   object ===" + updateobject_sqlbefehl + "===");
									PreparedStatement stmt_updateobject = con_mapnik.prepareStatement(updateobject_sqlbefehl);
									stmt_updateobject.setString(1, TAG_ADDRESSINCOMPLETE);
									stmt_updateobject.setLong(2, rs_objekte.getLong("id"));
									//noch nicht in jdbc 9.1xx verfuegbar 
									//stmt_updateobject.setQueryTimeout(4);
									stmt_updateobject.executeUpdate();
									logger.log(Level.FINEST, "jobs_strassen UPDATE war erfolgreich für planet_polygon   object ===" + rs_objekte.getString("id") + "===");
									if(rs_objekte.getLong("id") < 0) {
										adressenunvollstaendig_liste.append("r" + (-1 * rs_objekte.getLong("id")) + ",");
										anzahl_adressen_rels_unvollstaendig++;
									} else {
										adressenunvollstaendig_liste.append("w" + rs_objekte.getLong("id") + ",");
										anzahl_adressen_ways_unvollstaendig++;
									}
									stmt_updateobject.close();
								}
								/* noch nicht in jdbc 9.1xx verfuegbar  catch ( SQLTimeoutException timeout) {
									System.out.println("SQL-Timeout bei update aufgetreten, wurde als nicht gesetzt   object ===" + rs_objekte.getString("id") + "===");
									System.out.println(timeout.toString());
								*/
								catch( SQLException e) {
									logger.log(Level.SEVERE, "Fehler beim update planet_polygon   object ===" + rs_objekte.getString("id") + "===");
									logger.log(Level.SEVERE, e.toString());
								}
							}
						}
					}	// end of if addr:street key is missing in actual addr:housenumber object

					if((temp_aktplace != null) && (temp_aktstrasse == null)) {
						anzahl_adressen_places++;
						temp_aktstrasse = temp_aktplace;
					}
					
					if(temp_aktstrasse != null) {
						hstore = new HStore(rs_objekte.getString("tags_string"));
						tags.clear();
						tags = (HashMap<String,String>) hstore.asMap();

						if(temp_akthausnummer.indexOf(",") != -1)
							temp_akthausnummer = temp_akthausnummer.replace(",",";");
						String[] temp_akthausnummern = temp_akthausnummer.split(";");
						for(int tempi=0;tempi<temp_akthausnummern.length;tempi++) {
							anzahl_datensaetze++;

							String temp_akthausnummer_single = temp_akthausnummern[tempi].trim();
							logger.log(Level.FINEST, "way # "+anzahl_datensaetze+"  ID: "+rs_objekte.getLong("id")+"  Strasse: "+temp_aktstrasse+"  Hnr. ==="+temp_akthausnummer_single+"===    tags_string "+rs_objekte.getString("tags_string")+"===");

							String[] temp_akthausnummer_single_array = Hausnummernbereich_aufloesen(temp_akthausnummer_single);
							for(String tempsinglei: temp_akthausnummer_single_array) {
								Workcache_Entry new_real_housenumber_entry = new Workcache_Entry(false);
								new_real_housenumber_entry.setLand(land, land_id);
								new_real_housenumber_entry.setStadt(gemeindename, stadt_id);
								//new_real_housenumber_entry.strasse_id = temp_aktstrasse;
								new_real_housenumber_entry.setStrasse(temp_aktstrasse);			// set strasse instead of strasse_id, because id is unknown here
								new_real_housenumber_entry.setJob(jobname, job_id);
								new_real_housenumber_entry.set_osm_tag(tags);
								new_real_housenumber_entry.setOSMObjekt("way", rs_objekte.getLong("id"));
								new_real_housenumber_entry.setHausnummer(tempsinglei);
								new_real_housenumber_entry.setLonlat(rs_objekte.getString("lonlat"));
								new_real_housenumber_entry.setLonlat_source("OSM");
								//new_real_housenumber_entry.hausnummer_sortierbar = null;
								new_real_housenumber_entry.setTreffertyp(Workcache_Entry.Treffertyp.OSM_ONLY);
								
								osmhausnummerncache.update(new_real_housenumber_entry);
							}
						}
					}
				}
				rs_objekte.close();
				stmt_objekte.close();
				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: recordset-handling sqlbefehl_objekte planet_polygon in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) Gesamtzeit in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();

				logger.log(Level.FINE, "bei Wege vorab Anzahl Hausnummern gefunden: "+anzahl_datensaetze);
				logger.log(Level.FINE, "(forts.) und Anzahl im Cache nach holen planet_polygon: "+osmhausnummerncache.count()+"   "+osmhausnummerncache.count_unchanged());
				anzahl_adressen_ways = anzahl_datensaetze;
				anzahl_datensaetze_kpl += anzahl_datensaetze;
				anzahl_datensaetze = 0;


					// Hol alle Hausnummern an Flächen-Objekten (nicht-Relationen)
				sqlbefehl_objekte = "";
				sqlbefehl_objekte = "SELECT osm_id as id, tags->'addr:housenumber' AS hausnummer, tags->'addr:street' AS strasse, tags->'addr:place' AS place,";
				sqlbefehl_objekte += " tags->'building' AS building, tags AS tags_string, ST_AsText(ST_Transform(ST_Centroid(way),4326)) as lonlat,";
				sqlbefehl_objekte += " tags -> ? AS addressincomplete, tags -> ? AS streetfromassociatedrel";
				sqlbefehl_objekte += " FROM planet_line WHERE";
				sqlbefehl_objekte += " exist(tags, 'addr:housenumber') AND";
				sqlbefehl_objekte += " (ST_Covers(?::geometry, way) OR";			// ST_Covers = no point outside
				sqlbefehl_objekte += " ST_Crosses(?::geometry, way))";			// ST_Crosses = some, but not all, common
				sqlbefehl_objekte += " ORDER BY tags->'addr:street',tags->'addr:housenumber';";

				stmt_objekte = con_mapnik.prepareStatement(sqlbefehl_objekte);
				stmt_objekte.setString(1, TAG_ADDRESSINCOMPLETE);
				stmt_objekte.setString(2, TAG_ADDRESSASSOCSTREETADDED);
				stmt_objekte.setString(3, gebietsgeometrie);
				stmt_objekte.setString(4, gebietsgeometrie);
				logger.log(Level.FINE, "sqlbefehl_objekte (line) ==="+sqlbefehl_objekte+"===");

				time_debug_starttime = new java.util.Date();
				rs_objekte = stmt_objekte.executeQuery();
				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: sqlbefehl_objekte planet_line in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) Gesamtzeit in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();

				anzahl_datensaetze = 0;
				while( rs_objekte.next() ) {
					String temp_aktstrasse = rs_objekte.getString("strasse");
					String temp_aktplace = rs_objekte.getString("place");
					String temp_akthausnummer = rs_objekte.getString("hausnummer");

					if(rs_objekte.getString("addressincomplete") != null) {
						adressenunvollstaendig_liste.append("w" + rs_objekte.getLong("id") + ",");
						anzahl_adressen_ways_unvollstaendig++;
						logger.log(Level.FINE, "INFO planet_polygon theoretischer Kandidat ist bereits als unvollständige Address markiert und wird ignoriert");
						continue;
					}

					if(temp_akthausnummer == null)
						continue;

					if(temp_aktstrasse != null) {
							anzahl_ways_addrstreet_treffer++;
					}

					if(rs_objekte.getString("streetfromassociatedrel") != null) {
						anzahl_ways_associatedstreet_treffer++;
					}
					
					if((temp_aktstrasse == null) && (temp_aktplace == null)) {
						if(!parameterSearchForAssociatedStreetRelations) {
							adressenunvollstaendig_liste.append("w" + rs_objekte.getLong("id") + ",");
							anzahl_adressen_ways_unvollstaendig++;
							continue;
						} else {
							logger.log(Level.FINE, "INFO planet_line Kandidat fuer addr-Polygon mit associatedStreet, way-id: "+rs_objekte.getLong("id")+"===");
							anzahl_polygons_associatedstreet_versuche++;
							temp_aktstrasse = hol_strassenname_zur_id("w",rs_objekte.getLong("id"));
							if(temp_aktstrasse != null) {
								anzahl_ways_associatedstreet_treffer++;
							} else {
	// TODO rausschreiben in eine log-tabelle, damit User über diese potentiellen Kandidaten informiert werden
								logger.log(Level.FINE, "Keine-Strasse-ermittelbar an relations-Objekte osm_id ==="+rs_objekte.getString("id"));

								String updateobject_sqlbefehl = "UPDATE planet_line SET tags ="
										+ " tags || ? => 'yes'::hstore"
										+ " WHERE osm_id = ?;";
								try {
									logger.log(Level.FINE, "SQL-Update Befehl für update planet_polygon   object ===" + updateobject_sqlbefehl + "===");
									PreparedStatement stmt_updateobject = con_mapnik.prepareStatement(updateobject_sqlbefehl);
									stmt_updateobject.setString(1, TAG_ADDRESSINCOMPLETE);
									stmt_updateobject.setLong(2, rs_objekte.getLong("id"));
									//noch nicht in jdbc 9.1xx verfuegbar 
									//stmt_updateobject.setQueryTimeout(4);
									stmt_updateobject.executeUpdate();
									logger.log(Level.FINE, "jobs_strassen UPDATE war erfolgreich für planet_line   object ===" + rs_objekte.getString("id") + "===");
									adressenunvollstaendig_liste.append("w" + rs_objekte.getLong("id") + ",");
									anzahl_adressen_ways_unvollstaendig++;
								}
								/* noch nicht in jdbc 9.1xx verfuegbar  catch ( SQLTimeoutException timeout) {
									System.out.println("SQL-Timeout bei update aufgetreten, wurde als nicht gesetzt   object ===" + rs_objekte.getString("id") + "===");
									System.out.println(timeout.toString());
								}*/
								catch( SQLException e) {
									logger.log(Level.SEVERE, "Fehler beim update planet_line   object ===" + rs_objekte.getString("id") + "===");
									logger.log(Level.SEVERE, e.toString());
								}
							}
						}
					}	// end of if addr:street key is missing in actual addr:housenumber object

					if((temp_aktplace != null) && (temp_aktstrasse == null)) {
						anzahl_adressen_places++;
						temp_aktstrasse = temp_aktplace;
					}

					if(temp_aktstrasse != null) {
						hstore = new HStore(rs_objekte.getString("tags_string"));
						tags.clear();
						tags = (HashMap<String,String>) hstore.asMap();

						if(temp_akthausnummer.indexOf(",") != -1)
							temp_akthausnummer = temp_akthausnummer.replace(",",";");
						String[] temp_akthausnummern = temp_akthausnummer.split(";");
						for(int tempi=0;tempi<temp_akthausnummern.length;tempi++) {
							anzahl_datensaetze++;

							logger.log(Level.FINEST, "way # "+anzahl_datensaetze+"  ID: "+rs_objekte.getLong("id")+"  Strasse: "+rs_objekte.getString("strasse")+"  Hnr. ==="+temp_akthausnummern[tempi]+"===    tags_string "+rs_objekte.getString("tags_string")+"===");
							
							Workcache_Entry new_real_housenumber_entry = new Workcache_Entry(false);
							new_real_housenumber_entry.setLand(land, land_id);
							new_real_housenumber_entry.setStadt(gemeindename, stadt_id);
							new_real_housenumber_entry.setStrasse(temp_aktstrasse);			// set strasse instead of strasse_id, because id is unknown here
							new_real_housenumber_entry.setJob(jobname, job_id);
							new_real_housenumber_entry.set_osm_tag(tags);
							new_real_housenumber_entry.setOSMObjekt("way", rs_objekte.getLong("id"));
							new_real_housenumber_entry.setHausnummer(temp_akthausnummern[tempi]);
							new_real_housenumber_entry.setLonlat(rs_objekte.getString("lonlat"));
							new_real_housenumber_entry.setLonlat_source("OSM");
							new_real_housenumber_entry.setTreffertyp(Workcache_Entry.Treffertyp.OSM_ONLY);
							osmhausnummerncache.update(new_real_housenumber_entry);
						}
					}
				}
				rs_objekte.close();
				stmt_objekte.close();
					
				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: recordset-handling sqlbefehl_objekte planet_line in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) Gesamtzeit in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
	
				logger.log(Level.FINE, "bei Wege vorab Anzahl Hausnummern gefunden: "+anzahl_datensaetze);
				logger.log(Level.FINE, "Anzahl im Cache nach planet_line : "+osmhausnummerncache.count()+"   "+osmhausnummerncache.count_unchanged());
				anzahl_adressen_ways += anzahl_datensaetze;
				anzahl_datensaetze_kpl += anzahl_datensaetze;
				anzahl_datensaetze = 0;
	

					// Hol alle Hausnummern an Knoten (in osm nodes)
				sqlbefehl_objekte = "SELECT osm_id AS id, tags->'addr:housenumber' AS hausnummer, tags->'addr:street' AS strasse, tags->'addr:place' AS place,";
				sqlbefehl_objekte += " tags->'building' as building, tags AS tags_string, ST_AsText(ST_Transform(way,4326)) as lonlat,";
				sqlbefehl_objekte += " tags -> ? AS addressincomplete, tags -> ? AS streetfromassociatedrel";
				sqlbefehl_objekte += " FROM planet_point";
				sqlbefehl_objekte += " WHERE exist(tags, 'addr:housenumber') AND ST_Covers(?::geometry, way);";

				stmt_objekte = con_mapnik.prepareStatement(sqlbefehl_objekte);
				stmt_objekte.setString(1, TAG_ADDRESSINCOMPLETE);
				stmt_objekte.setString(2, TAG_ADDRESSASSOCSTREETADDED);
				stmt_objekte.setString(3, gebietsgeometrie);
				logger.log(Level.FINE, "sqlbefehl_objekte (nodes) ==="+sqlbefehl_objekte+"===");
				
				time_debug_starttime = new java.util.Date();
				rs_objekte = stmt_objekte.executeQuery();
				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: sqlbefehl_objekte planet_point in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) Gesamtzeit in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();
				anzahl_datensaetze = 0;
				while( rs_objekte.next() ) {
					String temp_aktstrasse = rs_objekte.getString("strasse");
					String temp_aktplace = rs_objekte.getString("place");
					String temp_akthausnummer = rs_objekte.getString("hausnummer");

					if(rs_objekte.getString("addressincomplete") != null) {
						adressenunvollstaendig_liste.append("n" + rs_objekte.getLong("id") + ",");
						anzahl_adressen_nodes_unvollstaendig++;
						logger.log(Level.FINE, "INFO planet_polygon theoretischer Kandidat ist bereits als unvollständige Address markiert und wird ignoriert");
						continue;
					}

					if(temp_akthausnummer == null)
						continue;

					if(temp_aktstrasse != null) {
						anzahl_nodes_addrstreet_treffer++;
					}

					if(rs_objekte.getString("streetfromassociatedrel") != null) {
						anzahl_nodes_associatedstreet_treffer++;
					}

					if((temp_aktstrasse == null) && (temp_aktplace == null)) {
						if(!parameterSearchForAssociatedStreetRelations) {
							adressenunvollstaendig_liste.append("n" + rs_objekte.getLong("id") + ",");
							anzahl_adressen_nodes_unvollstaendig++;
							continue;
						} else {
							logger.log(Level.FINE, "INFO planet_point Kandidat fuer addr-Polygon mit associatedStreet, knoten-id: "+rs_objekte.getLong("id")+"===");
							anzahl_nodes_associatedstreet_versuche++;
							temp_aktstrasse = hol_strassenname_zur_id("n",rs_objekte.getLong("id"));
							if(temp_aktstrasse != null) {
								anzahl_nodes_associatedstreet_treffer++;
							} else {
								String updateobject_sqlbefehl = "UPDATE planet_point SET tags = tags || hstore(?, 'yes')"
										+ " WHERE osm_id = ?;";
								try {
									logger.log(Level.FINE, "SQL-Update Befehl für update planet_polygon   object ===" + updateobject_sqlbefehl + "===");
									PreparedStatement stmt_updateobject = con_mapnik.prepareStatement(updateobject_sqlbefehl);
									stmt_updateobject.setString(1, TAG_ADDRESSINCOMPLETE);
									stmt_updateobject.setLong(2, rs_objekte.getLong("id"));
									// noch nicht in jdbc 9.1xx verfuegbar
									//stmt_updateobject.setQueryTimeout(4);
									stmt_updateobject.executeUpdate();
									logger.log(Level.FINE, "jobs_strassen UPDATE war erfolgreich für planet_point   object ===" + rs_objekte.getString("id") + "===");
									adressenunvollstaendig_liste.append("n" + rs_objekte.getLong("id") + ",");
									anzahl_adressen_nodes_unvollstaendig++;
									stmt_updateobject.close();
								}
								/* noch nicht in jdbc 9.1xx verfuegbar  catch ( SQLTimeoutException timeout) {
									System.out.println("SQL-Timeout bei update aufgetreten, wurde als nicht gesetzt   object ===" + rs_objekte.getString("id") + "===");
									System.out.println(timeout.toString());
								}*/
								catch( SQLException e) {
									logger.log(Level.SEVERE, "Fehler beim update planet_point   object ===" + rs_objekte.getString("id") + "===");
									logger.log(Level.SEVERE, e.toString());
								}
							}
						}
					}	// end of if addr:street key is missing in actual addr:housenumber object

					if((temp_aktplace != null) && (temp_aktstrasse == null)) {
						anzahl_adressen_places++;
						temp_aktstrasse = temp_aktplace;
					}
					
					if(temp_aktstrasse != null) {
						hstore = new HStore(rs_objekte.getString("tags_string"));
						tags.clear();
						tags = (HashMap<String,String>) hstore.asMap();

						if(temp_akthausnummer.indexOf(",") != -1)
							temp_akthausnummer = temp_akthausnummer.replace(",",";");
						String[] temp_akthausnummern = temp_akthausnummer.split(";");
						for(int tempi=0;tempi<temp_akthausnummern.length;tempi++) {
							anzahl_datensaetze++;

							logger.log(Level.FINEST, "node # "+anzahl_datensaetze+"  ID: "+rs_objekte.getLong("id")+"  Strasse: "+temp_aktstrasse+"  Hnr. ==="+temp_akthausnummern[tempi]+"===    tags_string"+rs_objekte.getString("tags_string")+"===");

							Workcache_Entry new_real_housenumber_entry = new Workcache_Entry(false);
							new_real_housenumber_entry.setLand(land, land_id);
							new_real_housenumber_entry.setStadt(gemeindename, stadt_id);
							new_real_housenumber_entry.setStrasse(temp_aktstrasse);			// set strasse instead of strasse_id, because id is unknown here
							new_real_housenumber_entry.setJob(jobname, job_id);
							new_real_housenumber_entry.set_osm_tag(tags);
							new_real_housenumber_entry.setOSMObjekt("node", rs_objekte.getLong("id"));
							new_real_housenumber_entry.setHausnummer(temp_akthausnummern[tempi]);
							new_real_housenumber_entry.setLonlat(rs_objekte.getString("lonlat"));
							new_real_housenumber_entry.setLonlat_source("OSM");
							new_real_housenumber_entry.setTreffertyp(Workcache_Entry.Treffertyp.OSM_ONLY);
							osmhausnummerncache.update(new_real_housenumber_entry);
						}
					} else {
						logger.log(Level.FINE, "INFO Kandidat fuer addr-Knoten mit associatedStreet, knoten-id: "+rs_objekte.getLong("id")+"===");
					}
				}
				rs_objekte.close();
				stmt_objekte.close();
				logger.log(Level.FINE, "bei Knoten vorab Anzahl Hausnummern gefunden: "+anzahl_datensaetze);
				logger.log(Level.FINE, "Anzahl im Cache nach planet_point : "+osmhausnummerncache.count()+"   "+osmhausnummerncache.count_unchanged());

				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: loop planet_point in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) Gesamtzeit in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();

				anzahl_adressen_nodes = anzahl_datensaetze;
				anzahl_datensaetze_kpl += anzahl_datensaetze;
				anzahl_datensaetze = 0;

				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time gesamt bisher in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				logger.log(Level.FINE, "Anzahl im Cache nach große Schleife über alle Straßen und associatedstreet : "+osmhausnummerncache.count()+"   "+osmhausnummerncache.count_unchanged());
				time_debug_starttime = new java.util.Date();

				logger.log(Level.FINEST, "Anzahl im Cache vor store_to_table_osm_hausnummern : "+osmhausnummerncache.count()+"   "+osmhausnummerncache.count_unchanged());


				time_debug_endtime = new java.util.Date();
				logger.log(Level.FINE, "Time: store_to_table_osm_hausnummern in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
				logger.log(Level.FINE, " (forts.) gesamt bisher in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();
				
				anzahl_datensaetze_total_kpl += anzahl_datensaetze_kpl;
				

				logger.log(Level.INFO, "ENDE Job # "+jobs_count+"  id: "+job_id+"     Stadt: "+rs_jobs.getString("stadt")+"    Land: "+rs_jobs.getString("land")+" ...");
				java.util.Date local_now = new java.util.Date();
				logger.log(Level.INFO, "Job duration in sec: "+(local_now.getTime()-time_actjob_evaluation.getTime())/1000);

				time_debug_endtime = new java.util.Date();
				logger.log(Level.INFO, "Time: total bis Ende aktueller Job in ms: "+(time_debug_endtime.getTime()-time_programm_starttime.getTime()));
				time_debug_starttime = new java.util.Date();


				ausgabetext = "";
				ausgabetext +=  "<node id='-" + jobs_count + "' lat='" + adminboundary_centroid_lat + "' lon='" + adminboundary_centroid_lon + "'>\n";
				ausgabetext +=  "  <tag k= 'AGS' v='" + rs_jobs.getString("ags") + "' />\n";
				ausgabetext +=  "  <tag k= 'Gemeinde' v='" + rs_jobs.getString("stadt") + "' />\n";
				ausgabetext +=  "  <tag k= 'Bevoelkerungszahl' v='" + rs_jobs.getString("bevoelkerungszahl") + "' />\n";
				ausgabetext +=  "  <tag k= 'flaechekm2' v='" + rs_jobs.getString("flaechekm2") + "' />\n";
				ausgabetext +=  "  <tag k= 'gliederungstadtland' v='" + rs_jobs.getString("gliederungstadtland") + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Nodes' v='" + anzahl_adressen_nodes + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Ways' v='" + anzahl_adressen_ways + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Rels' v='" + anzahl_adressen_rels + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Addrstreet' v='" + (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)+ "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_AssociatedStreet' v='" + (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Places' v='" + anzahl_adressen_places + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Nodes_unvollstaendig' v='" + anzahl_adressen_nodes_unvollstaendig + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Ways_unvollstaendig' v='" + anzahl_adressen_ways_unvollstaendig + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_Rels_unvollstaendig' v='" + anzahl_adressen_rels_unvollstaendig + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen_unvollstaendig' v='" + (anzahl_adressen_nodes_unvollstaendig + anzahl_adressen_ways_unvollstaendig + anzahl_adressen_rels_unvollstaendig) + "' />\n";
				ausgabetext +=  "  <tag k= 'Anzahl_Adressen' v='" + (anzahl_adressen_nodes + anzahl_adressen_ways + anzahl_adressen_rels) + "' />\n";
				ausgabetext +=  "</node>\n";
				osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(osmOutputFilename, true),StandardCharsets.UTF_8)));
				osmOutput.println(ausgabetext);
				osmOutput.close();
				
				Protokoll("Adressenunvollstaendig", rs_jobs.getString("ags") + "\t" + adressenunvollstaendig_liste.toString());

				String insertresult_sqlbefehl = "INSERT INTO theoevaluations (land, stadt,"
					+ " gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland,"
					+ " anzahl_osmadressen, anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels,"
					+ " anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,"
					+ " anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,"
					+ " anzahl_osmadressenplaces,"
					+ " anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig,"
					+ " anzahl_osmadressenrelsunvollstaendig,"
					+ " tstamp, osmdb_tstamp, polygon"
					+ ")";
				insertresult_sqlbefehl += " VALUES (?, ?, ?, ?, ?, ?, "
					+ "?, ?, ?, ?, ?, ?, "
					+ "?, ?, ?, ?, ?, ?, "
					+ "?, ?, ?, ?, ?::geometry);";

				PreparedStatement stmt_insertresult = con_hausnummern.prepareStatement(insertresult_sqlbefehl);
				stmt_insertresult.setString(1, rs_jobs.getString("land"));
				stmt_insertresult.setString(2, rs_jobs.getString("stadt"));
				stmt_insertresult.setString(3, rs_jobs.getString("ags"));
				stmt_insertresult.setFloat(4, rs_jobs.getFloat("flaechekm2"));
				stmt_insertresult.setInt(5, rs_jobs.getInt("bevoelkerungszahl"));
				stmt_insertresult.setInt(6, rs_jobs.getInt("gliederungstadtland"));
				stmt_insertresult.setInt(7, (anzahl_adressen_nodes + anzahl_adressen_ways + anzahl_adressen_rels));
				stmt_insertresult.setInt(8, anzahl_adressen_nodes);
				stmt_insertresult.setInt(9, anzahl_adressen_ways);
				stmt_insertresult.setInt(10, anzahl_adressen_rels);
				stmt_insertresult.setInt(11, anzahl_nodes_addrstreet_treffer);
				stmt_insertresult.setInt(12, anzahl_ways_addrstreet_treffer);
				stmt_insertresult.setInt(13, anzahl_polygons_addrstreet_treffer);
				stmt_insertresult.setInt(14, anzahl_nodes_associatedstreet_treffer);
				stmt_insertresult.setInt(15, anzahl_ways_associatedstreet_treffer);
				stmt_insertresult.setInt(16, anzahl_polygons_associatedstreet_treffer);
				stmt_insertresult.setInt(17, anzahl_adressen_places);
				stmt_insertresult.setInt(18, anzahl_adressen_nodes_unvollstaendig);
				stmt_insertresult.setInt(19, anzahl_adressen_ways_unvollstaendig);
				stmt_insertresult.setInt(20, anzahl_adressen_rels_unvollstaendig);
				stmt_insertresult.setTimestamp(21, new java.sql.Timestamp(time_actjob_evaluation.getTime()));
				stmt_insertresult.setTimestamp(22, new java.sql.Timestamp(time_osmdb.getTime()));
				stmt_insertresult.setString(23, gebietsgeometrie);

				try {
					logger.log(Level.FINE, "JOB-Insertbefehl ==="+insertresult_sqlbefehl+"===");
					stmt_insertresult.executeUpdate();
					sqlOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(sqlOutputFilename, true),StandardCharsets.UTF_8)));
					sqlOutput.println(insertresult_sqlbefehl);
					sqlOutput.close();
				}
				catch( SQLException e) {
					logger.log(Level.SEVERE, "Fehler beim insert result object ===" + insertresult_sqlbefehl + "===");
					logger.log(Level.SEVERE, e.toString());
				}

			} // Ende Schleife über alle Jobs
			rs_jobs.close();
			stmt_jobs.close();

			logger.log(Level.FINE, "Anzahl addr:housenumber über alle Straßen: "+anzahl_datensaetze_total_kpl);

			osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(osmOutputFilename, true),StandardCharsets.UTF_8)));
			osmOutput.println("</osm>\n");
			osmOutput.close();

			time_programm_endtime = new java.util.Date();
			logger.log(Level.INFO, "Programm-Endzeitpunkt: "+time_programm_endtime.toString());
			logger.log(Level.INFO, "Programm-Laufzeit in sek: "+(time_programm_endtime.getTime()-time_programm_starttime.getTime())/1000);
			logger.log(Level.INFO, "davon Gesamt-Laufzeit insert into osm_hausnummern in sek: "+(insert_osm_hausnummern_ms/1000));

			logger.log(Level.INFO, "anzahl_neu_durch_assiociatedstreets_query: "+anzahl_neu_durch_assiociatedstreets_query);
			logger.log(Level.INFO, "anzahl_changed_durch_assiociatedstreets_query: "+anzahl_changed_durch_assiociatedstreets_query);
			
			con_listofstreets.close();
			con_hausnummern.close();
			con_mapnik.close();

			logger.log(Level.INFO, "Time: total Program time in ms: "+(time_programm_endtime.getTime()-time_programm_starttime.getTime()));
		}
		catch( SQLException e) {
			logger.log(Level.SEVERE, "SQLException occured, details ...");
			logger.log(Level.SEVERE, e.toString());
			return;
		} catch (IOException ioerror) {
			logger.log(Level.SEVERE, "ERROR: couldn't open file to write, filename was ===" + osmOutputFilename + "===");
			logger.log(Level.SEVERE, ioerror.toString());
		}
	}

}
