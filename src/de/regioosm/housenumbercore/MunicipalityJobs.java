package de.regioosm.housenumbercore;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.OSMSegment;
import de.regioosm.housenumbercore.util.OSMStreet;
import de.regioosm.housenumbercore.util.OSMTag;
import de.regioosm.housenumbercore.util.OSMTagList;
import de.regioosm.housenumbercore.util.Street;
import de.regioosm.housenumbercore.util.OSMSegment.OSMType;


/**
 * 
 * @author Dietmar Seifert
 *
 */
public class MunicipalityJobs {

	private static Connection housenumberConn = null;
	private static Connection osmdbConn = null;
	
	private long jobDBId = -1L;

	static Applicationconfiguration configuration = new Applicationconfiguration();
	static String parameter_land = "";
	static String parameter_stadt = "";


	public void generateJob(MunicipalityArea muniarea) {

		try {

			java.util.Date localDebugEndtime;
			java.util.Date localDebugStarttime;

				// to enable transaction mode, disable auto-transation mode for every db-action
			housenumberConn.setAutoCommit(false);

			localDebugStarttime = new java.util.Date();

			System.out.println("START processing municipality area " + muniarea.toString() + "...");

			String selectJobSql = "SELECT id AS jobid, jobname FROM jobs WHERE gebiete_id = ?;";
			PreparedStatement selectJobStmt = housenumberConn.prepareStatement(selectJobSql);
			selectJobStmt.setLong(1, muniarea.getMuniAreaDBId());
			System.out.println("select statement, to check, if job already exists ===" +
				selectJobStmt.toString() + "===");

			jobDBId = 0L;
			ResultSet selectJobRS = selectJobStmt.executeQuery();
				// if job already exists ..
			if( selectJobRS.next() ) {
				jobDBId = selectJobRS.getLong("jobid");
				System.out.println("ok, Job already existing, just update it. Job name is ===" +
					selectJobRS.getString("jobname"));

				String updateJobSql = "UPDATE jobs set jobname = ?, checkedtime = now() WHERE id = ?;";
				System.out.println("JOB Updatebefehl ===" + updateJobSql + "===");
				PreparedStatement updateJobStmt = housenumberConn.prepareStatement(updateJobSql);
				updateJobStmt.setString(1, muniarea.getName());
				updateJobStmt.setLong(2, jobDBId);
				System.out.println("update job record " + updateJobStmt.toString() + "===");
				updateJobStmt.executeUpdate();

				String deleteJobStreetsSql = "DELETE FROM jobs_strassen where job_id = ?;";
				PreparedStatement deleteJobStreetsStmt = housenumberConn.prepareStatement(deleteJobStreetsSql);
				deleteJobStreetsStmt.setLong(1, jobDBId);
				System.out.println("delete existing job streets ===" + 
					deleteJobStreetsStmt.toString() + "===");
				deleteJobStreetsStmt.executeUpdate();
			} else {
				String insertJobSql = "INSERT INTO jobs (jobname, land_id, stadt_id, gebiete_id, checkedtime) " +
					" VALUES (?, ?, ?, ?, now()) returning id;";
				PreparedStatement insertJobStmt = housenumberConn.prepareStatement(insertJobSql);
				insertJobStmt.setString(1, muniarea.getName());
				insertJobStmt.setLong(2, muniarea.getCountryDBId());
				insertJobStmt.setLong(3, muniarea.getMunicipalityDBId());
				insertJobStmt.setLong(4, muniarea.getMuniAreaDBId());
				System.out.println("insert job statement ===" + 
					insertJobStmt.toString() + "===");

				ResultSet insertedJobRS = insertJobStmt.executeQuery();
				if( insertedJobRS.next() ) {
					jobDBId = insertedJobRS.getLong("id");
					System.out.println(" return id from new job ===" + jobDBId + "===");
				} else {
					System.out.println("FEHLER FEHLER: nach Job-insert konnte Job-id nicht geholt werden");
					return;
				}
			}
			localDebugEndtime = new java.util.Date();
			System.out.println("time for select and potentionally insert job in msec ==="+(localDebugEndtime.getTime() - localDebugStarttime.getTime()));
			localDebugStarttime = new java.util.Date();
			housenumberConn.commit();
		}	// ende try DB-connect
		catch( SQLException e) {
			e.printStackTrace();
			try {
				housenumberConn.rollback();
			} catch( SQLException innere) {
				System.out.println("inner sql-exception (tried to rollback transaction or to close connection ...");
				innere.printStackTrace();
			}
			return;
		}
	}
	
	/**
	 * get a list of OSM way-ids, which shall be ignored, if they will be found in Job area
	 * @return
	 */
	public List<Long> getBlacklistStreetOSMIds() {
//TODO better use as List of OSMStreet Objects
		
		// initialize and get optionaly a list of  (highway segments), which have to been ignored 
		List<Long> resultlist = new ArrayList<>();

		try {
			String selectHighwayBlacklistSql = "SELECT osm_ids, strasse " +
				"FROM jobs_strassen_blacklist JOIN strasse " +
				"ON jobs_strassen_blacklist.strasse_id = strasse.id " + 
				"WHERE job_id = ?;";
			PreparedStatement selectHighwayBlacklistStmt = housenumberConn.prepareStatement(selectHighwayBlacklistSql);
			selectHighwayBlacklistStmt.setLong(1, jobDBId);
			System.out.println("select statement to get a blacklist of streets " +
				"to be ignored for evaluation ===" + selectHighwayBlacklistStmt.toString() + "===");
	
			ResultSet selectHighwayBlacklistRS = selectHighwayBlacklistStmt.executeQuery();
				// loop over all blacklist streets
			while( selectHighwayBlacklistRS.next() ) {
				String ignoreStreet = selectHighwayBlacklistRS.getString("strasse");
				String[] ignoreOSMIdList = selectHighwayBlacklistRS.getString("osm_ids").split(",");
				for(int listindex = 0; listindex < ignoreOSMIdList.length; listindex++) {
					resultlist.add(Long.parseLong(ignoreOSMIdList[listindex]));
					System.out.println("Info-Blacklist-Entry osm_id ===" + ignoreOSMIdList[listindex] + "=== von Straße ==="+ignoreStreet+"===");
				}
			}

			selectHighwayBlacklistStmt.close();
		}	// ende try DB-connect
		catch( SQLException e) {
			e.printStackTrace();
			try {
				housenumberConn.rollback();
				return null;
			} catch( SQLException innere) {
				System.out.println("inner sql-exception (tried to rollback transaction or to close connection ...");
				innere.printStackTrace();
			}
			return null;
		}
		
		return resultlist;
	}

	
	/**
	 * 
	 * @param muniarea
	 */
	public Map<Street, OSMStreet> getOSMStreets(MunicipalityArea muniarea) {
		Map<Street, OSMStreet> streets = new TreeMap<>();

		String gebietsgeometrie = muniarea.getAreaPolygonAsWKB();

//TODO better let is use externally from Code User and bring it as second parameter 
// to this method
		List<Long> ignoreStreetsBlacklist = getBlacklistStreetOSMIds();

//TODO evtl. nicht nur pur Key name suchen, sondern auch einige Varianten a la loc_name, alt_name, official_name, name:de etc.
//TODO Auswertungsproblem: hier werden nur highway=* gefunden. pure Plätze ohne highway=* oder ähnliche Konstrukte (z.B. place=*) werden hier nicht gefunden

if(muniarea.getName().equals("Antonsviertel"))
	System.out.println("debugging, please");
		ResultSet selectStreetGeometriesRS = null;
		java.util.Date local_query_start = new java.util.Date();
		try {
				// find named streets, which are completely (ST_Contains) or partly (ST_Crosses)
				// within area of municipality
			String selectStreetGeometriesSql = "SELECT osm_id AS osmid, " +
				"tags->'name' AS streetname, tags->'highway' AS highwaytype, " +
//TODO new in 2018-02: use postal_code of street, if explicity given 
// this helps to differentiate identical names, if they are really 
// more than one in a municipality (for example, in Cologne, Germany)
				"tags->'postal_code' AS streetpostcode, " +
				"ST_AsText(way) AS way_astext, way " +
				"FROM planet_line WHERE " +
				"(ST_Contains(?::geometry, way) OR ST_Crosses(?::geometry, way)) AND " +
				//"ST_Relate(?::geometry, way,'1********') AND " +
				"exist(tags, 'highway') AND " +
				"exist(tags, 'name') " +
				"ORDER BY tags->'name';";	// ORDER BY is imported to keep identical highway names near another
//TODO perhaps support other kinds of name-variations, like name:xx, official_name, loc_name etc
			PreparedStatement selectStreetGeometriesStmt = 
				osmdbConn.prepareStatement(selectStreetGeometriesSql);
			selectStreetGeometriesStmt.setString(1, gebietsgeometrie);
			selectStreetGeometriesStmt.setString(2, gebietsgeometrie);
			System.out.println("sql statement to get named streets within area of " +
				"municipality for area name " + muniarea.getName() + " municipality area DBid: " + 
				muniarea.getMuniAreaDBId());
			selectStreetGeometriesRS = selectStreetGeometriesStmt.executeQuery();

		}
		catch( SQLException e) {
//TODO insert entry in error log
System.out.println("ERROR: invalid Geometry at osm relation id # " + muniarea.getMuniAreaDBId());
			e.printStackTrace();
			return null;
		}

		java.util.Date local_query_end = new java.util.Date();
		System.out.println("Dauer Geometrie-Query to get streets in msec ==="+(local_query_end.getTime()-local_query_start.getTime()));
		if((local_query_end.getTime()-local_query_start.getTime()) > 60000) {
			System.out.println("Dauer mehr als 1 min, bitte prüfen, es geht um das Polygon osm-id: " + muniarea.getMuniAreaDBId());
		}

		try {
			OSMStreet actualOSMStreet = null;
				// loop over all osm ways in municipality area
			while( selectStreetGeometriesRS.next() ) {
	
					// if actual osm-way has been marked as to ignore (by a user in website), 
					// then ignore it really (doubled named ways, for example, in allotments) 
				if(ignoreStreetsBlacklist.contains(selectStreetGeometriesRS.getLong("osmid"))) {
					System.out.println("Warnung-Blacklist: OSM-Weg mit id ===" + 
						selectStreetGeometriesRS.getString("osmid") + 
						"=== wird ignoriert, weil auf Blacklist");
					continue;
				}
	
				String osmhighwaytype = selectStreetGeometriesRS.getString("highwaytype");
				if(!OSMStreet.isValidNamedHighwaytype("highway", osmhighwaytype)) {
					continue;
				}
	
				actualOSMStreet = new OSMStreet(muniarea, selectStreetGeometriesRS.getString("streetname"));
				actualOSMStreet.setName(actualOSMStreet.normalizeName());
	
				
				System.out.println("* "+selectStreetGeometriesRS.getString("osmid") + 
					"===  Orig-DB-Straße ===" + selectStreetGeometriesRS.getString("streetname") +
					"===, normalisiert ===" + actualOSMStreet.getName() + "===");

					//TODO add all Tags from osm ways, not just highway=*
				OSMTagList osmtags = new OSMTagList(new OSMTag("highway", osmhighwaytype));

				OSMSegment osmway = new OSMSegment(OSMType.way,
					selectStreetGeometriesRS.getLong("osmid"),
					selectStreetGeometriesRS.getString("way_astext"),
					osmtags);
	
				if((streets.size() > 0) && (streets.containsKey(actualOSMStreet))) {
					OSMStreet streetsobject = streets.get(actualOSMStreet);
					streetsobject.addSegment(osmway);
					streets.put(actualOSMStreet, streetsobject);
				} else {
					actualOSMStreet.addSegment(osmway);
					streets.put(actualOSMStreet, actualOSMStreet);
				}
			}
		} // end of loop over all osm ways in municipality area
//		housenumberConn.commit();
		catch( SQLException e) {
//			System.out.println("ERROR: further way part1 was not insertable (happened because of identical name after normalizing), failed insert statement was ==="+insertJobStreetSql+"===");
			System.out.println(e.toString());
			return null;
		}
		return streets;
	}

	
		
				
	public void storeStreets(MunicipalityArea muniarea, Map<Street, OSMStreet> streets) {
		List<Long> streetDBIdList = new ArrayList<>();

		try {
			String selectStreetSql = "SELECT id  FROM strasse WHERE strasse = ?";
			PreparedStatement selectStreetStmt = housenumberConn.prepareStatement(selectStreetSql);

			String insertStreetSql = "INSERT INTO strasse (strasse) VALUES (?) returning id;";
			PreparedStatement insertStreetStmt = housenumberConn.prepareStatement(insertStreetSql);

			String insertJobStreetSql = "INSERT INTO jobs_strassen " +
				"(job_id, land_id, stadt_id, strasse_id, osm_ids,linestring) " +
				"VALUES (?, ?, ?, ?, ?, ?::geometry);";
			PreparedStatement insertJobStreetStmt = housenumberConn.prepareStatement(insertJobStreetSql);

			String selectOfficialJobStreetsSql = "SELECT DISTINCT ON (strasse, strasse_id) " +
				"strasse AS street, strasse_id AS streetid " +
				"FROM stadt_hausnummern JOIN strasse ON strasse_id = strasse.id " +
				"WHERE " +
				"sub_id like ? AND " +
				"land_id = ? AND " +
				"stadt_id = ? " +
				"ORDER BY strasse;";
			PreparedStatement selectOfficialJobStreetsStmt = housenumberConn.prepareStatement(selectOfficialJobStreetsSql);

				
			// to enable transaction mode, disable auto-transation mode for every db-action
			housenumberConn.setAutoCommit(false);

			long streetDBid = 0L;
			for (Street streetkey : streets.keySet()) {
				OSMStreet osmStreet = streets.get(streetkey);
			    System.out.println("Key [" + streetkey + "] ===" + osmStreet.toString() + "===");

			    selectStreetStmt.setString(1,  osmStreet.getName());
				System.out.println("search for street name ===" + selectStreetStmt.toString() + "=== ...");
				ResultSet selectStreetRs = selectStreetStmt.executeQuery();
					// if street is unknown in central street table, add it
				if(!selectStreetRs.next() ) {
					System.out.println("Information: street is unknown in table strasse, will be stored " +
						", name ===" + osmStreet.getName() + " ...");

					insertStreetStmt.setString(1, osmStreet.getName());
					ResultSet insertStreetRS = insertStreetStmt.executeQuery();
					if( insertStreetRS.next() ) {
						streetDBid = insertStreetRS.getLong("id");
					}
				} else {
					streetDBid = selectStreetRs.getLong("id");
				}
					// store DBid from checked or inserted streets for below to don't store once again there
				streetDBIdList.add(streetDBid);

				int stmtindex = 1;
				insertJobStreetStmt.setLong(stmtindex++, jobDBId);
				insertJobStreetStmt.setLong(stmtindex++, muniarea.getCountryDBId());
				insertJobStreetStmt.setLong(stmtindex++, muniarea.getMunicipalityDBId());
				insertJobStreetStmt.setLong(stmtindex++, streetDBid);
				insertJobStreetStmt.setString(stmtindex++, osmStreet.getOSMIdsAsString());
				insertJobStreetStmt.setString(stmtindex++, osmStreet.getGeometryWKT());
				System.out.println("insert statement to store a street for a " +
					"municipality area ===" + insertJobStreetStmt.toString() + "===");
				try {
					insertJobStreetStmt.executeUpdate();
				}
				catch( SQLException e) {
					System.out.println("ERROR: further way part1 was not insertable (happened because of identical name after normalizing), failed insert statement was ==="+insertJobStreetSql+"===");
					System.out.println(e.toString());
				}
			
			}	// end of loop over all streets

			// ---------------------------------------------------------------------------------
			// now get all official streets from stored housenumber list for municipality area
			// and check for each street, it is is already inserted in jobs_strassen
			// If not, the street is in OSM DB missing.
			// In this case, add this ONLY-official streets in jobs_strassen below
			// This happens in incomplete areas or the streets are only propossed or for other reasons
			// ---------------------------------------------------------------------------------

				// Wenn die Stadt-Gesamtauswertung aktuell ist, oder
				// wenn in der aktuellen Stadt die städischen Hausnummern dem Stadtteil zugeordnet sind
				// dann werte jetzt noch die Straßen mit Hausnummern aus, die bisher noch nicht gefunden wurden
			String areaId = muniarea.getAreaId();
				// if municipality main area is active and municipality has subareas
				// then manipulate areaId to get all streets
			if(areaId.equals("-1") && muniarea.isSubareasidentifyable()) {
				areaId = "%";
			}
			int aktAdminlevel = muniarea.getAreaAdminlevel();		// = 0, is sql null
	
			int stmtindex = 1;
			selectOfficialJobStreetsStmt.setString(stmtindex++, areaId);
			selectOfficialJobStreetsStmt.setLong(stmtindex++, muniarea.getCountryDBId());
			selectOfficialJobStreetsStmt.setLong(stmtindex++, muniarea.getMunicipalityDBId());
			System.out.println("select all official streets for municipality area ===" +
				selectOfficialJobStreetsStmt.toString() + "===");
			ResultSet selectOfficialJobStreetsRs = selectOfficialJobStreetsStmt.executeQuery();

			while(selectOfficialJobStreetsRs.next()) {
				streetDBid = selectOfficialJobStreetsRs.getLong("streetid");

				if(!streetDBIdList.contains(streetDBid)) {
						// actual street is still missing in table jobs_strassen

					stmtindex = 1;
					insertJobStreetStmt.setLong(stmtindex++, jobDBId);
					insertJobStreetStmt.setLong(stmtindex++, muniarea.getCountryDBId());
					insertJobStreetStmt.setLong(stmtindex++, muniarea.getMunicipalityDBId());
					insertJobStreetStmt.setLong(stmtindex++, streetDBid);
					insertJobStreetStmt.setString(stmtindex++, null);
					insertJobStreetStmt.setString(stmtindex++, null);
					System.out.println("insert statement to store a official street for a " +
						"municipality area ===" + insertJobStreetStmt.toString() + "===");
					try {
						insertJobStreetStmt.executeUpdate();
					}
					catch( SQLException e) {
						System.out.println("ERROR: further way part2 was not insertable " +
							"(happened because of identical name after normalizing), " +
							"failed insert statement was ===" + insertJobStreetStmt.toString() + "===");
						System.out.println(e.toString());
						housenumberConn.rollback();
						return;
					}
				}
			} // Ende sql-Schleife über alle osmfehlendestrassen-Straßen
			
			housenumberConn.commit();

			selectStreetStmt.close();
			insertStreetStmt.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			try {
				housenumberConn.rollback();
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			return;
		}
	}

	public static void main(String args[]) {
		if((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-country countryname -- Name of the country. english version or german 'Bundesrepublik Deutschland'");
			System.out.println("-stadt cityname -- can contain wildcard *, for example A* for all municipality starting with Letter A");
			System.out.println("-agsarea xy -- can contain wilcard *, in Germany for example 08% vor all Baden-Wuerttemberg municipalities");
			System.out.println("-adminhierarchy xy -- admin hierarchy, start at country, separated by comma, like Netherland,Genderland");
			System.out.println("-jobname jobname -- the named job for an evaluation in citites the jobname for whole municipality is same as municipality name");
			return;
		}

		String parameter_jobname = "";
		String parameter_officialkeysid = "";
		String parameter_adminhierarchy = "";
		if(args.length >= 1) {
			int args_ok_count = 0;
			for(int argsi=0;argsi<args.length;argsi+=2) {
				System.out.print(" args pair analysing #: "+argsi+"  ==="+args[argsi]+"===");
				if(args.length > argsi+1)
					System.out.print("  args #+1: "+(argsi+1)+"   ==="+args[argsi+1]+"===");
				System.out.println("");
				if(		args[argsi].equals("-country")
					||	args[argsi].equals("-land")) {
					parameter_land = args[argsi+1];
					args_ok_count += 2;
				}
				if(		args[argsi].equals("-municipality")
					|| 	args[argsi].equals("-stadt")) {
					parameter_stadt = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-agsarea")) {
					parameter_officialkeysid = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-adminhierarchy")) {
					parameter_adminhierarchy = args[argsi+1];
					args_ok_count += 2;
				}
				if(args[argsi].equals("-jobname")) {
					parameter_jobname = args[argsi+1];
					args_ok_count += 2;
				}
			}
			if(args_ok_count != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}

		if(!parameter_stadt.equals("")) {
			parameter_stadt.replace("*", "%");
		}

		if(parameter_land.equals("")) {
			System.out.println("Country must be set, program stopped");
			return;
		}
		
		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");

			String url_hausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(url_hausnummern, configuration.db_application_username, configuration.db_application_password);

			String url_mapnik = configuration.db_osm2pgsql_url;
			osmdbConn = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);

			MunicipalityJobs jobs = new MunicipalityJobs();

			
			MunicipalityArea.connectDB(housenumberConn, osmdbConn);
			OSMStreet.connectDB(housenumberConn);
			int areacount;
			try {
				areacount = MunicipalityArea.search(parameter_land, parameter_stadt, parameter_officialkeysid, parameter_jobname, parameter_adminhierarchy);

				System.out.println("number of municipality areas found to process: " + areacount);
					// loop over all found municipality areas
				MunicipalityArea muniArea = MunicipalityArea.next();
				while( muniArea != null ) {
					System.out.println("Processing muniArea: " + muniArea.toString() + "===");
					jobs.generateJob(muniArea);
					Map<Street, OSMStreet> osmstreets = jobs.getOSMStreets(muniArea);
					jobs.storeStreets(muniArea, osmstreets);
					muniArea = MunicipalityArea.next();
				}   // loop over all found municipality areas

				java.util.Date gebiete_endtime = new java.util.Date();

/*				System.out.println("Methode komplette Schleifendurchläufe über alle Gebiete in msek: "+(gebiete_endtime.getTime()-gebiete_starttime.getTime()));		// in sek: /1000

				System.out.println("Summen der Strassen: brutto # Strassen: "+total_anzahl_strassen_geom+"    netto # Strassen: "+total_anzahl_strassen_geomcheck);
				selectJobStmt.close();
				deleteJobStreetsStmt.close();
				insertJobStmt.close();
				selectStreetStmt.close();
*/

				housenumberConn.close();
				osmdbConn.close();
			
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch(SQLException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
