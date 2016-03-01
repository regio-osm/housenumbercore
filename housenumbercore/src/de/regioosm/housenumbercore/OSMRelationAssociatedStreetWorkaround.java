package de.regioosm.housenumbercore;
/*
* 			OFFEN: In der associatedStreet Relation gibt es echt Subrelationen, wie z.B. 1547341
* 					Diese Subrelationen sind ggfs. auch einfache Gebäude, die als Relation gezeichnet wurden
*/

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.*;

import de.regioosm.housenumbercore.util.Applicationconfiguration;

/**
 * 
 * @author openstreetmap
 *
 */
public class OSMRelationAssociatedStreetWorkaround {
	static Applicationconfiguration configuration = new Applicationconfiguration();
	static Connection con_mapnik;

	static String replace(String sourcestring, String searchstring, String replacestring)
	{
		String outputstring = sourcestring;

		if(sourcestring.contains(searchstring)) {
			Pattern pattern = Pattern.compile(searchstring);
			//System.out.println("searchstring==="+searchstring+"=== pattern ==="+pattern.toString()+"===");
			Matcher match = pattern.matcher(sourcestring);
			StringBuffer sb = new StringBuffer();
			boolean match_find = match.find();
			while(match_find) {
				match.appendReplacement(sb,replacestring);
				match_find = match.find();
			}
			match.appendTail(sb);
			outputstring = sb.toString();
		}
		return outputstring;
	}

	private static void Protokoll(String typ, String outputtext) {
		PrintWriter debug_output = null;
		String workcache_debugfile = "protokoll_" + typ + ".txt";
		
		try {
			debug_output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(workcache_debugfile, true),"UTF-8")));
			debug_output.println(outputtext);
			System.out.println(outputtext);
			debug_output.close();
		} catch (IOException ioerror) {
			System.out.println("ERROR: couldn't open file to write, filename was ==="+workcache_debugfile+"===");
			ioerror.printStackTrace();
		}
	}
	
	
	public static void main(String args[]) {

		final String TAG_ADDRESSASSOCSTREETADDED = "___temp___addressstreetfromassociatedrel";	// Tag, that will be added to addresses, where addr:street was set in osm via associatedStreet Relation 
		final String TAG_RELATIONPROCESSED = "___temp___relationprocessed";	// Tag, that will be added to associatedStreet relation, when all members are processed 

		java.util.Date time_program_starttime = new java.util.Date();
		java.util.Date time_program_endtime = new java.util.Date();
		java.util.Date time_debug_starttime = new java.util.Date();
		java.util.Date time_debug_endtime = new java.util.Date();

Integer paramMaxminutes = 600;			// 600 = 10 hours
Integer paramMaxrelations = 20;
		
		for(int lfdnr=0;lfdnr<args.length;lfdnr++) {
			System.out.println("args["+lfdnr+"] ==="+args[lfdnr]+"===");
		}
		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-maxminutes xy");
			System.out.println("-maxrelations 4711");
			return;
		}

		
		String act_strassenname = "";
		int anzahl_aenderungen = 0;
		int anzahl_kandidaten = 0;
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
			if(args.length >= 1) {
				int args_ok_count = 0;
				for(int argsi=0;argsi<args.length;argsi+=2) {
					System.out.println(" args pair analysing #: "+argsi+"  ==="+args[argsi]+"===   #+1: "+(argsi+1)+"   ==="+args[argsi+1]+"===");
					if(args[argsi].equals("-maxminutes")) {
						paramMaxminutes = Integer.parseInt(args[argsi+1]);
						System.out.println("set parameter maxminutes to " + paramMaxminutes);
						args_ok_count += 2;
					}
					if(args[argsi].equals("-maxrelations")) {
						paramMaxrelations = Integer.parseInt(args[argsi+1]);
						System.out.println("set parameter maxrelations to " + paramMaxrelations);
						args_ok_count += 2;
					}
				}
				if(args_ok_count != args.length) {
					System.out.println("ERROR: not all programm parameters were valid, STOP");
					return;
				}
			}
				
			
			String url_mapnik = configuration.db_osm2pgsql_url;
			String act_username = configuration.db_osm2pgsql_username;
act_username = "gis";
			con_mapnik = DriverManager.getConnection(url_mapnik, act_username, configuration.db_osm2pgsql_password);

			String sqlquery_indirect_associatedstreetobjects = "";
			sqlquery_indirect_associatedstreetobjects = "SELECT id, tags AS tagsalltogether, members"
				+ " FROM planet_rels"
				+ " WHERE 'associatedStreet' = ANY(tags)"
				+ " AND NOT ? = ANY(tags);";
			PreparedStatement stmt_indirect_associatedstreetobjects = con_mapnik.prepareStatement(sqlquery_indirect_associatedstreetobjects);
			stmt_indirect_associatedstreetobjects.setString(1, TAG_RELATIONPROCESSED);
			System.out.println("Case polygon_object: sqlquery_indirect_associatedstreetobjects ==="+sqlquery_indirect_associatedstreetobjects+"===");

			time_program_starttime = new java.util.Date();

			time_debug_starttime = new java.util.Date();
			ResultSet rs_indirect_associatedstreetobjects = stmt_indirect_associatedstreetobjects.executeQuery();
			time_debug_endtime = new java.util.Date();
			System.out.println("Time: sqlquery_indirect_associatedstreetobjects in ms: "+(time_debug_endtime.getTime()-time_debug_starttime.getTime()));
			time_debug_starttime = new java.util.Date();

			
			String getOSMPointObjectSql = "SELECT osm_id, tags->'addr:housenumber' AS housenumber,"
				+ " tags->'addr:street' AS street, tags->'addr:place' AS place,"
				+ " ST_AsText(ST_Transform(ST_Centroid(way),4326)) as mittelpunkt "
				+ " FROM planet_point WHERE osm_id = ?;";
			PreparedStatement getOSMPointObjectStmt = con_mapnik.prepareStatement(getOSMPointObjectSql);

			String getOSMLineObjectSql = "SELECT osm_id, tags->'addr:housenumber' AS housenumber,"
				+ " tags->'addr:street' AS street, tags->'addr:place' AS place"
				+ " FROM planet_line WHERE osm_id = ?;";
			PreparedStatement getOSMLineObjectStmt = con_mapnik.prepareStatement(getOSMLineObjectSql);

			String getOSMPolygonObjectSql = "SELECT osm_id, tags->'addr:housenumber' AS housenumber,"
				+ " tags->'addr:street' AS street, tags->'addr:place' AS place"
				+ " FROM planet_polygon WHERE osm_id = ?;";
			PreparedStatement getOSMPolygonObjectStmt = con_mapnik.prepareStatement(getOSMPolygonObjectSql);
				
			String updateOSMRelsObjectSql = "UPDATE planet_rels"
				+ " SET tags = array_cat(tags, ARRAY[?::text,'yes'::text])"
				+ " WHERE id = ?;";
			PreparedStatement updateOSMRelsObjectStmt = con_mapnik.prepareStatement(updateOSMRelsObjectSql);

			String updateOSMPointObjectSql = "UPDATE planet_point"
				+ " SET tags = tags || hstore('addr:street',?) || hstore(?,'yes')"
				+ " WHERE osm_id = ?;";
			PreparedStatement updateOSMPointObjectStmt = con_mapnik.prepareStatement(updateOSMPointObjectSql);

			String updateOSMLineObjectSql = "UPDATE planet_line"
				+ " SET tags = tags || hstore('addr:street',?) || hstore(?,'yes')"
				+ " WHERE osm_id = ?;";
			PreparedStatement updateOSMLineObjectStmt = con_mapnik.prepareStatement(updateOSMLineObjectSql);

			String updateOSMPolygonObjectSql = "UPDATE planet_polygon"
				+ " SET tags = tags || hstore('addr:street',?) || hstore(?,'yes')"
				+ " WHERE osm_id = ?;";
			PreparedStatement updateOSMPolygonObjectStmt = con_mapnik.prepareStatement(updateOSMPolygonObjectSql);
			

			// change to transaction mode
			con_mapnik.setAutoCommit(false);
			
			
			int anzahl_relationen = 0;
			int anzahl_strassevorhanden = 0;
			long actAssocatedrelationId = -1L;
			while( rs_indirect_associatedstreetobjects.next() ) {
				if(anzahl_relationen > paramMaxrelations) {
					System.out.println("Limit paramMaxrelations reached, STOP working");
					break;
				}
				anzahl_relationen++;

				int program_duration_min = (int) ((new java.util.Date().getTime() - time_program_starttime.getTime()) / 1000 / 60);
				if(program_duration_min > paramMaxminutes) {
					System.out.println("Limit paramMaxminutes reached, STOP working");
					break;
				}

				act_strassenname = "";

				actAssocatedrelationId = rs_indirect_associatedstreetobjects.getLong("id");
				
				Array tags_array_raw =  rs_indirect_associatedstreetobjects.getArray("tagsalltogether");
				String[] tags_array = (String[]) tags_array_raw.getArray();
				System.out.println("Anzahl Teile tagsalltogether: "+(tags_array.length/2));
				if((tags_array.length % 2) == 1) {
					System.out.println("FEHLER FEHLER: Relations Tag-Parts können nicht ausgelesen werden, value-key Anzahl ist ungerade ==="+rs_indirect_associatedstreetobjects.getString("tagsalltogether")+"===");
					String meldetext = actAssocatedrelationId + "\t" + rs_indirect_associatedstreetobjects.getString("tagsalltogether"); 
					Protokoll("RelationsMemberfehler", meldetext);
					continue;
				}
				for(Integer tupelindex = 0; tupelindex < tags_array.length; tupelindex += 2) {
					String act_key = tags_array[tupelindex];
					String act_value = tags_array[tupelindex+1];
					System.out.println("Tupel  # "+tupelindex+":  [" + tags_array[tupelindex] + "] ===" + tags_array[tupelindex+1] + "===      wurde zu ["+act_key+"] ==="+act_value+"===");
					if(act_key.equals("name")) {
						act_strassenname = act_value;
						System.out.println("in relation id #" + actAssocatedrelationId + " found name=* with value ==="+act_strassenname+"===");
						break;
					}
				}

					// lets analyse members and store non-street members in appropriate memory arrays for fast access

				if(rs_indirect_associatedstreetobjects.getArray("members") == null) {
					System.out.println("Warnung: Relation Id " + actAssocatedrelationId + " hat keine Member und wird ignoriert");
					String meldetext = actAssocatedrelationId + "\t" + act_strassenname;
					Protokoll("emtpyRelation", meldetext);
					continue;
				}
				Array member_parts_raw =  rs_indirect_associatedstreetobjects.getArray("members");
				String[] member_parts = (String[]) member_parts_raw.getArray();
				String objecttype = "";
				String member_role = "";
				String member_id_typed = "";
				Long member_id = 0L;
				System.out.println("Anzahl member: "+(member_parts.length/2));
				for(Integer tupelindex = 0; tupelindex < member_parts.length; tupelindex += 2) {
					member_id_typed = member_parts[tupelindex];
					member_role = member_parts[tupelindex+1];

					objecttype = member_id_typed.substring(0,1);
					member_id = Long.parseLong(member_id_typed.substring(1));

					if(act_strassenname.equals(""))
						continue;
					if(member_role.equals("street"))
						continue;
					String mapniktable = "";
					long osm_id = -1L;
					boolean add_street = false;
					if(objecttype.equals("n")) {
						mapniktable = "planet_point";

						getOSMPointObjectStmt.setLong(1, member_id);
						ResultSet getOSMPointObjectRS = getOSMPointObjectStmt.executeQuery();
						while(getOSMPointObjectRS.next()) {	// es reicht 1 Datensatz (sollte auch nur einer kommen)
							System.out.println("Datensatz   id: ==="+getOSMPointObjectRS.getLong("osm_id")+"===      street ==="+getOSMPointObjectRS.getString("street") + "===    housenumber ==="+getOSMPointObjectRS.getString("housenumber") + "===");
							if(getOSMPointObjectRS.getString("street") == null) { 
								if(		(getOSMPointObjectRS.getString("housenumber") == null)
									&&	(getOSMPointObjectRS.getString("place") == null)) {
									// object has no housenumber. just ignore, but could be logged as error
									System.out.println("WARNING: in table " + mapniktable + " didn't find housenumber in record  id: ==="+getOSMPointObjectRS.getLong("osm_id")+"===      street ==="+getOSMPointObjectRS.getString("street") + "===    housenumber ==="+getOSMPointObjectRS.getString("housenumber") + "===");										
								} else {
									osm_id = getOSMPointObjectRS.getLong("osm_id");
									add_street = true;
									anzahl_kandidaten++;
								}
							} else {
								if( ! getOSMPointObjectRS.getString("street").equals(act_strassenname)) {
									String meldetext = actAssocatedrelationId + "\t" + objecttype + "\t" + member_id + "\t" + act_strassenname + "\t" + getOSMPointObjectRS.getString("street");
									Protokoll("falschestrasse", meldetext);
									String objekt_centroid = getOSMPointObjectRS.getString("mittelpunkt");
									if((objekt_centroid != null) && (! objekt_centroid.equals(""))) {
										Pattern wildcard_pattern = Pattern.compile("POINT\\(([\\d\\.]+) ([\\d\\.]+)");
										Matcher match = wildcard_pattern.matcher(objekt_centroid);
										StringBuffer sb = new StringBuffer();
										boolean match_find = match.find();
										if(match_find) {
											match.appendReplacement(sb,"lon=\"$1\" lat=\"$2\"");
											//match_find = match.find();		no longer while loop, otherwise 13 1/2 would be translated to 13 12, but want only 13
										}
										objekt_centroid = sb.toString();
										meldetext = "<wpt " + objekt_centroid + ">" + "\n";
										meldetext += "<name>" + getOSMPointObjectRS.getString("street") + " vs " + act_strassenname + "</name>" + "\n";
										meldetext += "</wpt>" + "\n";
										Protokoll("falschestrasse_gpx", meldetext);
									}
								} else {
									anzahl_strassevorhanden++;
								}
							}
						}
					} else if(objecttype.equals("w")) {
						mapniktable = "planet_polygon";

						getOSMPolygonObjectStmt.setLong(1, member_id);
						ResultSet getOSMPolygonObjectRS = getOSMPolygonObjectStmt.executeQuery();
						int anzahl_datensatztreffer = 0;
						while(getOSMPolygonObjectRS.next()) {	// es reicht 1 Datensatz (sollte auch nur einer kommen)
							anzahl_datensatztreffer++;
							System.out.println("Datensatz   id: ==="+getOSMPolygonObjectRS.getLong("osm_id")+"===      street ==="+getOSMPolygonObjectRS.getString("street") + "===    housenumber ==="+getOSMPolygonObjectRS.getString("housenumber") + "===");
							if(getOSMPolygonObjectRS.getString("street") == null) { 
								if(		(getOSMPolygonObjectRS.getString("housenumber") == null)
									&&	(getOSMPolygonObjectRS.getString("place") == null)) {
									// object has no housenumber. just ignore, but could be logged as error
									System.out.println("WARNING: in table " + mapniktable + " didn't find housenumber in record  id: ==="+getOSMPolygonObjectRS.getLong("osm_id")+"===      street ==="+getOSMPolygonObjectRS.getString("street") + "===    housenumber ==="+getOSMPolygonObjectRS.getString("housenumber") + "===");										
								} else {
									osm_id = getOSMPolygonObjectRS.getLong("osm_id");
									add_street = true;
									anzahl_kandidaten++;
								}
							} else {
								if( ! getOSMPolygonObjectRS.getString("street").equals(act_strassenname)) {
									String meldetext = actAssocatedrelationId + "\t" + objecttype + "\t" + member_id + "\t" + act_strassenname + "\t" + getOSMPolygonObjectRS.getString("street");
									Protokoll("falschestrasse", meldetext);
								} else {
									anzahl_strassevorhanden++;
								}
							}
						}
						if(anzahl_datensatztreffer == 0) {
							mapniktable = "planet_line";
							getOSMLineObjectStmt.setLong(1, member_id);
							ResultSet getOSMLineObjectRS = getOSMLineObjectStmt.executeQuery();
							while(getOSMLineObjectRS.next()) {	// es reicht 1 Datensatz (sollte auch nur einer kommen)
								System.out.println("Datensatz   id: ==="+getOSMLineObjectRS.getLong("osm_id")+"===      street ==="+getOSMLineObjectRS.getString("street") + "===    housenumber ==="+getOSMLineObjectRS.getString("housenumber") + "===");
								if(getOSMLineObjectRS.getString("street") == null) { 
									if(		(getOSMLineObjectRS.getString("housenumber") == null)
										&&	(getOSMLineObjectRS.getString("place") == null)) {
										// object has no housenumber. just ignore, but could be logged as error
										System.out.println("WARNING: in table " + mapniktable + " didn't find housenumber in record  id: ==="+getOSMLineObjectRS.getLong("osm_id")+"===      street ==="+getOSMLineObjectRS.getString("street") + "===    housenumber ==="+getOSMLineObjectRS.getString("housenumber") + "===");										
									} else {
										osm_id = getOSMLineObjectRS.getLong("osm_id");
										add_street = true;
										anzahl_kandidaten++;
									}
								} else {
									if( ! getOSMLineObjectRS.getString("street").equals(act_strassenname)) {
										String meldetext = actAssocatedrelationId + "\t" + objecttype + "\t" + member_id + "\t" + act_strassenname + "\t" + getOSMLineObjectRS.getString("street");
										Protokoll("falschestrasse", meldetext);
									} else {
										anzahl_strassevorhanden++;
									}
								}
							}
						}
					} else {
						System.out.println("FEHLER FEHLER: OSM Objektart ist weder node noch way, sondern ==="+objecttype+"===, diese wird noch nicht unterstützt, Relation-Member wird ignoriert");
						String meldetext = objecttype + "\t" + member_id + "\t" + act_strassenname + "\t" + "   ====" + rs_indirect_associatedstreetobjects.getString("members") + "====";
						Protokoll("falschertyp", meldetext);
						continue;
					}
if((anzahl_relationen % 100) == 0)
	System.out.println("Zwischenstand: Relationen: "+anzahl_relationen+"  anzahl_strassevorhanden: "+anzahl_strassevorhanden+"   anzahl_kandidaten: "+anzahl_kandidaten+"   anzahl_aenderungen: "+anzahl_aenderungen);

					if(add_street) {
						System.out.println("Relationsinfos: # " + anzahl_relationen + "    Name ==="+act_strassenname +"===    Role: ===" + member_parts[tupelindex+1] + "===     OSM-Typ: ===" + objecttype +"===   Id: ==="+member_id+"===");

						try {
							if(mapniktable.equals("planet_point")) {
								updateOSMPointObjectStmt.setString(1, act_strassenname);
								updateOSMPointObjectStmt.setString(2, TAG_ADDRESSASSOCSTREETADDED);
								updateOSMPointObjectStmt.setLong(3, osm_id);
								System.out.println("Update row in table " + mapniktable + "  osm_id ===" + osm_id + "=== with streetname ===" + act_strassenname + "===");
								updateOSMPointObjectStmt.executeUpdate();
							}
							else if(mapniktable.equals("planet_line")) {
								updateOSMLineObjectStmt.setString(1, act_strassenname);
								updateOSMLineObjectStmt.setString(2, TAG_ADDRESSASSOCSTREETADDED);
								updateOSMLineObjectStmt.setLong(3, osm_id);
								System.out.println("Update row in table " + mapniktable + "  osm_id ===" + osm_id + "=== with streetname ===" + act_strassenname + "===");
								updateOSMLineObjectStmt.executeUpdate();
							}
							else if(mapniktable.equals("planet_polygon")) {
								updateOSMPolygonObjectStmt.setString(1, act_strassenname);
								updateOSMPolygonObjectStmt.setString(2, TAG_ADDRESSASSOCSTREETADDED);
								updateOSMPolygonObjectStmt.setLong(3, osm_id);
								System.out.println("Update row in table " + mapniktable + "  osm_id ===" + osm_id + "=== with streetname ===" + act_strassenname + "===");
								updateOSMPolygonObjectStmt.executeUpdate();
							}
							String meldetext = actAssocatedrelationId + "\t" + objecttype + "\t" + member_id + "\t" + act_strassenname;
							Protokoll("update", meldetext);
							anzahl_aenderungen++;
						}
						catch( SQLException e) {
							System.out.println("ERROR: during update row===");
							e.printStackTrace();
						}
					}
				}
				
				try {
					updateOSMRelsObjectStmt.setString(1, TAG_RELATIONPROCESSED);
					updateOSMRelsObjectStmt.setLong(2, actAssocatedrelationId);
					System.out.println("Update flag to planet_rels associatedStreet in table planet_rels   osm_id ===" 
						+ actAssocatedrelationId + "=== with streetname ===" + act_strassenname + "===");
					updateOSMRelsObjectStmt.executeUpdate();
				}
				catch( SQLException updateOSMRelsObjectError) {
					System.out.println("SQL-Error, when Update flag to planet_rels associatedStreet in table planet_rels   osm_id ===" 
							+ actAssocatedrelationId + "=== with streetname ===" + act_strassenname + "===");
					updateOSMRelsObjectError.printStackTrace();
				}
					
				System.out.println("ok, commit starts ...");
				con_mapnik.commit();							// commit after each associatedStreet
				System.out.println("ok, commit ended");
			}	// end of loop over all associatedStreet Relations in planet_rels
			System.out.println("Anzahl anzahl_relationen: "+anzahl_relationen);
			System.out.println("Anzahl anzahl_strassevorhanden: "+anzahl_strassevorhanden);
			System.out.println("Anzahl anzahl_kandidaten: "+anzahl_kandidaten);
			System.out.println("Anzahl anzahl_aenderungen: "+anzahl_aenderungen);
			
			time_program_endtime = new java.util.Date();
			System.out.println("Program-Time in ms: "+(time_program_endtime.getTime()-time_program_starttime.getTime()));

			getOSMPointObjectStmt.close();
			getOSMLineObjectStmt.close();
			getOSMPolygonObjectStmt.close();
			updateOSMPointObjectStmt.close();
			updateOSMLineObjectStmt.close();
			updateOSMPolygonObjectStmt.close();
			updateOSMRelsObjectStmt.close();
			con_mapnik.close();
		}
		catch( SQLException e) {
			e.printStackTrace();
		}
	}
}

