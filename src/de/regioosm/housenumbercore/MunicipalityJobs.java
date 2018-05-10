package de.regioosm.housenumbercore;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;


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

	private static final String OVERPASSURL = "http://overpass-api.de/api/";
	//private static final String OVERPASSURL = "http://overpass.osm.rambler.ru/cgi/";
	//private static final String OVERPASSURL = "http://dev.overpass-api.de/api_mmd/";

	private static Connection housenumberConn = null;
	private static Connection osmdbConn = null;


	private static boolean useOverpassForQueries = false;
	
	private long jobDBId = -1L;

	static Applicationconfiguration configuration = new Applicationconfiguration();

	public MunicipalityJobs() {
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
	
				String url_mapnik = configuration.db_osm2pgsql_url;
				osmdbConn = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);
			}
			catch( SQLException e) {
				e.printStackTrace();
				return;
			}
		}
	}

	private static class logger {
//TODO replace with real logger
		private static void log(Level level, String text) {
			System.out.println("Level: " + level.toString() + " ===" + text + "===");
		}
	}

	public static void setOverpassForEvaluation(boolean useoverpass) {
		useOverpassForQueries = useoverpass;
	}
	

	
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

			this.jobDBId = 0L;
			ResultSet selectJobRS = selectJobStmt.executeQuery();
				// if job already exists ..
			if( selectJobRS.next() ) {
				this.jobDBId = selectJobRS.getLong("jobid");
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
				deleteJobStreetsStmt.setLong(1, this.jobDBId);
				System.out.println("delete existing job streets ===" + 
					deleteJobStreetsStmt.toString() + "===");
				deleteJobStreetsStmt.executeUpdate();
			} else {
				String insertJobSql = "INSERT INTO jobs (jobname, land_id, stadt_id, gebiete_id, checkedtime) " +
					" VALUES (?, (SELECT id FROM land WHERE countrycode = ?), ?, ?, now()) returning id;";
				PreparedStatement insertJobStmt = housenumberConn.prepareStatement(insertJobSql);
				insertJobStmt.setString(1, muniarea.getName());
				insertJobStmt.setString(2, muniarea.getCountrycode());
				insertJobStmt.setLong(3, muniarea.getMunicipalityDBId());
				insertJobStmt.setLong(4, muniarea.getMuniAreaDBId());
				System.out.println("insert job statement ===" + 
					insertJobStmt.toString() + "===");

				ResultSet insertedJobRS = insertJobStmt.executeQuery();
				if( insertedJobRS.next() ) {
					this.jobDBId = insertedJobRS.getLong("id");
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
	 * get OSM streets for municipality area - either from local osm2pgsql DB or from OSM Overpass live - depends on user setting via setOverpassForEvaluation
	 * @param muniarea
	 */
	public Map<Street, OSMStreet> getOSMStreets(MunicipalityArea muniarea) {
		if(useOverpassForQueries)
			return getOSMStreetsFromOverpass(muniarea);
		else
			return getOSMStreetsFromDB(muniarea);
	}

		
	private Map<Street, OSMStreet> getOSMStreetsFromDB(MunicipalityArea muniarea) {
		Map<Street, OSMStreet> streets = new TreeMap<>();

		String gebietsgeometrie = muniarea.getAreaPolygonAsWKB();
		System.out.println("in getOSMStreetsFromDB gebietsgeometrie ===" + gebietsgeometrie + "===");

//TODO better let is use externally from Code User and bring it as second parameter 
// to this method
		List<Long> ignoreStreetsBlacklist = getBlacklistStreetOSMIds();

//TODO evtl. nicht nur pur Key name suchen, sondern auch einige Varianten a la loc_name, alt_name, official_name, name:de etc.
//TODO Auswertungsproblem: hier werden nur highway=* gefunden. pure Plätze ohne highway=* oder ähnliche Konstrukte (z.B. place=*) werden hier nicht gefunden

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
				"ST_AsText(ST_Transform(way,4326)) AS way_astext, way, %%tags AS taglist " +
				"FROM planet_line WHERE " +
				"(ST_Contains(?::geometry, way) OR ST_Crosses(?::geometry, way)) AND " +
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
			int countstreetsegments = 0;
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
				if (selectStreetGeometriesRS.getString("streetname").equals("Alzaia Naviglio Pavese"))
					System.out.println("debug strasse gefunden in subarea " + muniarea.getName());
				countstreetsegments++;
				System.out.println("#" + countstreetsegments + " ** "+selectStreetGeometriesRS.getString("osmid") + 
					"===  Orig-DB-Straße ===" + selectStreetGeometriesRS.getString("streetname") +
					"===, normalisiert ===" + actualOSMStreet.getName() + "===");

				List<OSMTag> osmtags = new ArrayList<>();
				String[] taglist = (String[]) selectStreetGeometriesRS.getArray("taglist").getArray();
				for(int taglistindex = 0; taglistindex < taglist.length; taglistindex += 2) {
					osmtags.add(new OSMTag(taglist[taglistindex], taglist[taglistindex + 1]));
				}

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

	/**
	 * get OSM streets live from OSM Overpass Server and get filtered streets back, with geometry
	 * CAUTION: up to now (2018-02), only OSM ways will be search for, not nodes or relations
	 * @param muniarea
	 * @return
	 */
	private Map<Street, OSMStreet> getOSMStreetsFromOverpass(final MunicipalityArea muniarea) {
		URL                url; 
		URLConnection      urlConn; 
		BufferedReader     dis;

		final Integer MAXOVERPASSTRIES = 3;

		final Map<Street, OSMStreet> streets = new TreeMap<>();
		
		final IdTracker availableNodes = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
		final IdTracker availableWays = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
		final IdTracker availableRelations = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
		
		final Map<Long, Node> allNodes = new HashMap<>();
		final Map<Long, Way> allWays = new HashMap<>();
		final Map<Long, Relation> allRelations = new HashMap<>();
		
			
		
		String overpass_queryurl = "interpreter?data=";
		String overpass_query = "[timeout:3600][maxsize:1073741824]\n" +
			"[out:xml];\n" +
			"area(" + (3600000000L + -1* muniarea.getAdminPolygonOsmId()) + ")->.boundaryarea;\n" +
			"(\n" +
			"way(area.boundaryarea)[\"highway\"][\"name\"];>;\n" +
			");\n" +
			"out meta;";
		logger.log(Level.FINE, "OSM Overpass Query ===" + overpass_query + "===");

		String url_string = "";
		File osmFile = null;

		try {
			String overpass_query_encoded = URLEncoder.encode(overpass_query, "UTF-8");
			overpass_query_encoded = overpass_query_encoded.replace("%28","(");
			overpass_query_encoded = overpass_query_encoded.replace("%29",")");
			overpass_query_encoded = overpass_query_encoded.replace("+","%20");
			url_string = OVERPASSURL + overpass_queryurl + overpass_query_encoded;
			logger.log(Level.INFO, "Request for Overpass-API to get housenumbers ...");
			logger.log(Level.FINE, "Overpass Request URL to get housenumbers ===" + url_string + "===");

			StringBuffer osmresultcontent = new StringBuffer();

			InputStream overpassResponse = null; 
			String responseContentEncoding = "";
			Integer numberfailedtries = 0;
			boolean finishedoverpassquery = false;
			do {
				try {
					url = new URL(url_string);

					if(numberfailedtries > 0) {
						logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again, now: " 
							+ new Date().toString());
						TimeUnit.SECONDS.sleep(2 * numberfailedtries);
						logger.log(Level.WARNING, "ok, slept for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again, now: "
							+ new Date().toString());
					}
					
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
					urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
					urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
					
					overpassResponse = urlConn.getInputStream(); 
		
					Integer headeri = 1;
					logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
					while(urlConn.getHeaderFieldKey(headeri) != null) {
						logger.log(Level.FINE, "  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
						if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
							responseContentEncoding = urlConn.getHeaderField(headeri);
						headeri++;
					}
					finishedoverpassquery = true;
				} catch (MalformedURLException mue) {
					logger.log(Level.WARNING, "Overpass API request produced a malformed Exception (Request #" 
						+ (numberfailedtries + 1) + ", Request URL was ===" + url_string + "===, Details follows ...");					
					logger.log(Level.WARNING, mue.toString());
					numberfailedtries++;
					if(numberfailedtries > MAXOVERPASSTRIES) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						//setResponseState(numberfailedtries);
						return null;
					}
					//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
					//TimeUnit.SECONDS.sleep(2 * numberfailedtries);
				} catch( ConnectException conerror) {
					numberfailedtries++;
					if(numberfailedtries > MAXOVERPASSTRIES) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						//setResponseState(numberfailedtries);
						return null;
					}

					url_string = OVERPASSURL + "status";
					System.out.println("url to get overpass status for this server requests ===" + url_string + "===");
					url = new URL(url_string);
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
					urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
					urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
					
					overpassResponse = urlConn.getInputStream(); 
		
					Integer headeri = 1;
					logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
					while(urlConn.getHeaderFieldKey(headeri) != null) {
						logger.log(Level.FINE, "  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
						if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
							responseContentEncoding = urlConn.getHeaderField(headeri);
						headeri++;
					}
					if(responseContentEncoding.equals("gzip")) {
						dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
					} else {
						dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
					}
					String inputline = "";
					while ((inputline = dis.readLine()) != null)
					{ 
						System.out.println("Content ===" + inputline + "===\n");
					}
					dis.close();
				
				} catch (IOException ioe) {
					logger.log(Level.WARNING, "Overpass API request produced an Input/Output Exception  (Request #" 
						+ (numberfailedtries + 1) + ", Request URL was ===" + url_string + "===, Details follows ...");					
					logger.log(Level.WARNING, ioe.toString());
					numberfailedtries++;
					if(numberfailedtries > MAXOVERPASSTRIES) {
						logger.log(Level.SEVERE, "Overpass API didn't delivered data, gave up after 3 failed requests, Request URL was ===" + url_string + "===");
						//setResponseState(numberfailedtries);
						return null;
					}
					//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
					//TimeUnit.SECONDS.sleep(2 * numberfailedtries);

				
					url_string = OVERPASSURL + "status";
					System.out.println("url to get overpass status for this server requests ===" + url_string + "===");
					url = new URL(url_string);
					urlConn = url.openConnection(); 
					urlConn.setDoInput(true); 
					urlConn.setUseCaches(false);
					urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
					urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
					
					overpassResponse = urlConn.getInputStream(); 
		
					Integer headeri = 1;
					logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
					while(urlConn.getHeaderFieldKey(headeri) != null) {
						logger.log(Level.FINE, "  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
						if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
							responseContentEncoding = urlConn.getHeaderField(headeri);
						headeri++;
					}
					if(responseContentEncoding.equals("gzip")) {
						dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
					} else {
						dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
					}
					String inputline = "";
					while ((inputline = dis.readLine()) != null)
					{ 
						System.out.println("Content ===" + inputline + "===\n");
					}
					dis.close();
				
				
				}
			} while(! finishedoverpassquery);
	
			//setResponseState(numberfailedtries);
			String inputline = "";
			if(responseContentEncoding.equals("gzip")) {
				dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
			} else {
				dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
			}
			while ((inputline = dis.readLine()) != null)
			{ 
				osmresultcontent.append(inputline + "\n");
			}
			dis.close();
			
				// first, save upload data as local file, just for checking or for history
			DateFormat timeformatterUSlong = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
			timeformatterUSlong.setTimeZone(TimeZone.getTimeZone("UTC"));
			DateFormat time_formatter = new SimpleDateFormat("yyyyMMdd-HHmmss'Z'");
			DateFormat germanyformatter = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			String downloadtime = time_formatter.format(new Date());
			
			String filename = configuration.application_datadir + File.separator + "overpassdownload" 
				+ File.separator + downloadtime + ".osm";

			try {
				osmFile = new File(filename);
				PrintWriter osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(filename),StandardCharsets.UTF_8)));
				osmOutput.println(osmresultcontent.toString());
				osmOutput.close();
				logger.log(Level.INFO, "Saved Overpass OSM Data Content to file " + filename);
			} catch (IOException ioe) {
				logger.log(Level.SEVERE, "Error, when tried to save Overpass OSM Data in file " + filename);
				logger.log(Level.SEVERE, ioe.toString());
			}
				// ok, osm result is in osmresultcontent.toString() available
			logger.log(Level.FINE, "Dateilänge nach optionalem Entpacken in Bytes: " + osmresultcontent.toString().length());

			int firstnodepos = osmresultcontent.toString().indexOf("<node");
			if(firstnodepos != -1) {
				String osmheader = osmresultcontent.toString().substring(0,firstnodepos);
				int osm_base_pos = osmheader.indexOf("osm_base=");
				if(osm_base_pos != 1) {
					int osm_base_valuestartpos = osmheader.indexOf("\"",osm_base_pos);
					int osm_base_valueendpos = osmheader.indexOf("\"",osm_base_valuestartpos + 1);
					if((osm_base_valuestartpos != -1) && (osm_base_valueendpos != -1)) { 
						String osm_base_value = osmheader.substring(osm_base_valuestartpos + 1,osm_base_valueendpos);
						DateFormat utc_formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
						try {
							Date osmtime = utc_formatter.parse(osm_base_value);
							System.out.println("gefundener OSM-Datenstand: " + germanyformatter.format(osmtime));
						} catch (ParseException parseerror) {
							logger.log(Level.SEVERE, "Couldn't parse OSM DB timestamp from Overpass OSM Data, timestamp was ===" + osm_base_value + "===");					
							logger.log(Level.SEVERE, parseerror.toString());
						}
					}
				}
			}

			final List<Long> ignoreStreetsBlacklist = getBlacklistStreetOSMIds();
			
			
			Sink sinkImplementation = new Sink() {

				@Override
				public void release() {
					logger.log(Level.FINEST, "hallo Sink.release   aktiv !!!");
				}
				
				@Override
				public void complete() {
					logger.log(Level.FINEST, "hallo Sink.complete  aktiv ...");//    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);

					OSMStreet actualOSMStreet = null;
	    	    		// loop over all osm way objects
	    	    	for (Map.Entry<Long, Way> waymap: allWays.entrySet()) {
	    				Long objectid = waymap.getKey();
		        		Collection<Tag> tags = waymap.getValue().getTags();

							//TODO add all Tags from osm ways, not just highway=*
						List<OSMTag> osmtags = new ArrayList<>();
		        		
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			keyvalues.put(tag.getKey(), tag.getValue());
		        			osmtags.add(new OSMTag(tag.getKey(), tag.getValue()));
		        		}
		        		osmtags.add(new OSMTag("osm_timestamp", timeformatterUSlong.format(waymap.getValue().getTimestamp())));
		        		osmtags.add(new OSMTag("osm_user", waymap.getValue().getUser().getName()));
		        		osmtags.add(new OSMTag("osm_uid", "" + waymap.getValue().getUser().getId()));
		        		osmtags.add(new OSMTag("osm_version", "" + waymap.getValue().getVersion()));
		        		osmtags.add(new OSMTag("osm_changeset", "" + waymap.getValue().getChangesetId()));
		        		
							// if actual osm-way has been marked as to ignore (by a user in website), 
							// then ignore it really (doubled named ways, for example, in allotments) 
						if(ignoreStreetsBlacklist.contains(objectid)) {
							System.out.println("Warnung-Blacklist: OSM-Weg mit id ===" + 
								objectid + 
								"=== wird ignoriert, weil auf Blacklist");
							continue;
						}
			
						String osmhighwaytype = keyvalues.get("highway");
						if(!OSMStreet.isValidNamedHighwaytype("highway", osmhighwaytype)) {
							continue;
						}
			
						actualOSMStreet = new OSMStreet(muniarea, keyvalues.get("name"));
						actualOSMStreet.setName(actualOSMStreet.normalizeName());
			
						
						System.out.println("* "+objectid + 
							"===  Orig-DB-Straße ===" + keyvalues.get("name") +
							"===, normalisiert ===" + actualOSMStreet.getName() + "===");


						OSMSegment osmway = new OSMSegment(OSMType.way,
							objectid, "", osmtags);
						osmway.setWayFromOsmNodes(allNodes, waymap.getValue().getWayNodes());
			
						if((streets.size() > 0) && (streets.containsKey(actualOSMStreet))) {
							OSMStreet streetsobject = streets.get(actualOSMStreet);
							streetsobject.addSegment(osmway);
							streets.put(actualOSMStreet, streetsobject);
						} else {
							actualOSMStreet.addSegment(osmway);
							streets.put(actualOSMStreet, actualOSMStreet);
						}
	    			}
		
	    	    		// loop over all osm relation objects with addr:housenumber Tag
	    	    	//for (Map.Entry<Long, Relation> relationmap: allRelations.entrySet()) {
	    	    	//}
				}
				
				@Override
				public void initialize(Map<String, Object> metaData) {
	    	    	for (Map.Entry<String, Object> daten: metaData.entrySet()) {
	    				String key = daten.getKey();
	    				Object tags = daten.getValue();
	    	    	}
				}
				
				@Override
				public void process(EntityContainer entityContainer) {

			        Entity entity = entityContainer.getEntity();
			        if (entity instanceof Node) {
			            //do something with the node
			        	//nodes_count++;

		    			availableNodes.set(entity.getId());

						NodeContainer nodec = (NodeContainer) entityContainer;
						Node node = nodec.getEntity();
//						node.setChangesetId(entity.getChangesetId());
//						node.setUser(entity.getUser());
//						node.setTimestamp(entity.getTimestamp());
//						node.setVersion(entity.getVersion());
						//System.out.println("Node lon: "+node.getLongitude() + "  lat: "+node.getLatitude()+"===");

						allNodes.put(entity.getId(), node);
			        } else if (entity instanceof Way) {
			        	//ways_count++;
			        	
		    			availableWays.set(entity.getId());

						WayContainer wayc = (WayContainer) entityContainer;
						Way way = wayc.getEntity();
//						way.setChangesetId(entity.getChangesetId());
//						way.setUser(entity.getUser());
//						way.setTimestamp(entity.getTimestamp());
//						way.setVersion(entity.getVersion());

						allWays.put(entity.getId(), way);
			        
			        } else if (entity instanceof Relation) {
			        	// do nothing to collection relations
			        }
				}
			};
			
			RunnableSource osmfilereader;


			File tempfile = null;
			try {
			    // Create temp file.
			    tempfile = File.createTempFile("overpassresult", ".osm");
			    // Delete temp file when program exits.
			    tempfile.deleteOnExit();
			    // Write to temp file
			    BufferedWriter out = new BufferedWriter(new FileWriter(tempfile));
			    out.write(osmresultcontent.toString());
			    out.close();
			} catch (IOException e) {
			}	
//			osmfilereader = new XmlReader(osmFile, true, CompressionMethod.None);
			osmfilereader = new XmlReader(tempfile, true, CompressionMethod.None);

			osmfilereader.setSink(sinkImplementation);

			Thread readerThread = new Thread(osmfilereader);
			readerThread.start();

			while (readerThread.isAlive()) {
		        readerThread.join();
			}
		} catch (OsmosisRuntimeException osmosiserror) {
			logger.log(Level.SEVERE, "Osmosis runtime Error ...");
			logger.log(Level.SEVERE, osmosiserror.toString());
	    } catch (InterruptedException e) {
	    	logger.log(Level.WARNING, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.WARNING, e.toString());
	        /* do nothing */
	    } catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
	    	logger.log(Level.SEVERE, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.SEVERE, e.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
	    	logger.log(Level.SEVERE, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.SEVERE, e.toString());
		}
		return streets;
	}
		
				
		
	public void storeStreets(MunicipalityArea muniarea, Map<Street, OSMStreet> streets) {
		List<Long> streetDBIdList = new ArrayList<>();

		if ((streets == null) || (streets.size() == 0))
			return;
		
		try {
			String selectStreetSql = "SELECT id  FROM strasse WHERE strasse = ?";
			PreparedStatement selectStreetStmt = housenumberConn.prepareStatement(selectStreetSql);

			String insertStreetSql = "INSERT INTO strasse (strasse) VALUES (?) returning id;";
			PreparedStatement insertStreetStmt = housenumberConn.prepareStatement(insertStreetSql);

			String insertJobStreetSql = "INSERT INTO jobs_strassen " +
				"(job_id, land_id, stadt_id, strasse_id, osm_ids,linestring) " +
				"VALUES (?, (SELECT id FROM land WHERE countrycode = ?), ?, ?, ?, ST_Transform(ST_SetSrid(?::geometry,?),900913));";
			PreparedStatement insertJobStreetStmt = housenumberConn.prepareStatement(insertJobStreetSql);

			String selectOfficialJobStreetsSql = "SELECT DISTINCT ON (strasse, strasse_id) " +
				"strasse AS street, strasse_id AS streetid " +
				"FROM stadt_hausnummern JOIN strasse ON strasse_id = strasse.id " +
				"WHERE " +
				"sub_id like ? AND " +
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
				insertJobStreetStmt.setString(stmtindex++, muniarea.getCountrycode());
				insertJobStreetStmt.setLong(stmtindex++, muniarea.getMunicipalityDBId());
				insertJobStreetStmt.setLong(stmtindex++, streetDBid);
				insertJobStreetStmt.setString(stmtindex++, osmStreet.getOSMIdsAsString());
				insertJobStreetStmt.setString(stmtindex++, osmStreet.getGeometryWKT());
				insertJobStreetStmt.setInt(stmtindex++, 4326);
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
					insertJobStreetStmt.setString(stmtindex++, muniarea.getCountrycode());
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
			System.out.println("-useoverpass yes|no -- should be use overpass -> yes with uptodate osm data or a local osm DB in osm2pgsql format -> no (default)");
			return;
		}

		String parameter_land = "";
		String parameter_stadt = "";
		String parameter_jobname = "";
		String parameter_officialkeysid = "";
		String parameter_adminhierarchy = "";
		boolean parameterUseOverpass = false;
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
				if(args[argsi].equals("-useoverpass")) {
					if(args[argsi+1].toLowerCase().indexOf("y") == 0)
						parameterUseOverpass = true;
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

			MunicipalityJobs.setOverpassForEvaluation(parameterUseOverpass);
			MunicipalityArea.connectDB(housenumberConn, osmdbConn);

			OSMStreet.connectDB(housenumberConn);

			MunicipalityJobs jobs = new MunicipalityJobs();

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
