package de.regioosm.housenumbercore.util;

/*
	V1.1, 16.02.2011, Dietmar Seifert
		*	Anpassung der Tabellen für allgemeine Nutzung; Ergänzung Tabellen land und stadt
		*	Datensätze in land und stadt werden ergänzt, wenn noch nicht vorhanden

	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Augsburg" "09761" "Stadtvermessungsamt-Hausnummern.txt"
	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Kaufbeuren" "09762" "Stadt-Kaufbeuren-Hausnummern.txt"
*/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.TreeMap;

import de.regioosm.housenumbercore.util.Applicationconfiguration;


public class ImportHausnummerliste {
	private static boolean latlonforUniqueness = false;
	public static final double lon_unset = 999.0D;
	public static final double lat_unset = 999.0D;

	/**
	 * signals not set the id of a city.
	 */
	private static final long STADTIDUNSET = -1L;
	/**
	 * signal not set of the id of a country.
	 */
	private static final long LANDIDUNSET = -1L;
	/**
	 * fixed length of a sortable housenumber, for correct sorting of a housenumber. Should be set to at least 4.
	 */
	private static final int HAUSNUMMERSORTIERBARLENGTH = 4;


	/**
	 * DB-internal Id of the actual country, the housenumber list belongs to
	 */
	public String land = "";
	public String hierarchy = "";
	public long land_dbid = LANDIDUNSET;	// should be deleted completely
	
	public String stadt = "";
	public String stadt_id = "";
	public String subbereichescharf = "n";
	public String officialgeocoordinates = "";
	public String housenumberadditionseparator = "";
	/**
	 * y for exactly writing of housenumber addition 'A' and 'a' are different.
	 * n for housenumber addition case irrelevant, 'A' and 'a' are the same and hit.
	 */
	public String hausnummerzusatzgrosskleinrelevant = "y";
	/**
	 * DB-internal Id of the actual city, the housenumber list belongs to
	 */
	public long stadt_dbid = STADTIDUNSET;
	/**
	 * internal Structure to hold all housenumber-Objects;
	 */
	private TreeMap<String, Hausnummer> store;

		// storage of streetnames and their internal DB id. If a street is missing in DB, it will be inserted,
		// before the insert of the housenumbers at the streets will be inserted
	static HashMap<String, Integer> street_idlist = new HashMap<String, Integer>();
	
	
	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	/**
	 * DB connection to Hausnummern
	 */
	private static Connection conHausnummern = null;
	private static Connection conListofstreets = null;

	public void expandlatlonforUniqueness() {
		latlonforUniqueness = true;
	}

	public void clear() {
		store.clear();
		stadt = "";
		stadt_id = "";
		hierarchy = "";
		stadt_dbid = STADTIDUNSET;
	}


	/**
	 * Internal class for all information about a housenumber, as read from a housenumber input file for a municipality
	 * @author Dietmar Seifert
	 *
	 */
	public class Hausnummer {
	
		public String strasse = "";
		public long strasse_id = -1L;
		public String subid = "";
		public String postcode = "";
		public String hausnummer = "";
		public String hausnummersortierbar = "";
		public String bemerkung = "";
		public double longitude = 0d;
		public double latitude = 0d;
		public String sourcesrid = "";
		public String sourcetext = "";
		
		
			/**
			 * 
			 * @param strasse
			 * @param strasse_id
			 * @param subid
			 * @param postcode
			 * @param hausnummer
			 * @param bemerkung
			 * @param longitude
			 * @param latitude
			 * @param sourcesrid
			 * @param sourcetext
			 * 
			 * parameter postcode is new at 2015-03-06
			 */
		public Hausnummer (final String strasse, final long strasse_id, final String subid, 
				final String postcode, final String hausnummer,
				final String bemerkung, final double longitude, final double latitude, final String sourcesrid, final String sourcetext) {
			this.strasse = strasse;
			this.strasse_id = strasse_id;
			this.subid = subid;
			this.postcode = postcode;
			this.hausnummer = hausnummer;
			this.bemerkung = bemerkung;
			this.longitude = longitude;
			this.latitude = latitude;
			this.sourcesrid = sourcesrid;
			if((longitude != 0.0D) && (latitude != 0.0D) && sourcesrid.equals(""))
				this.sourcesrid = "4326";
			this.sourcetext = sourcetext;
			
				// Hack: hier werden die Hausnummern normiert und damit über
				// normale Select order by sortierbar from Hausnummern
				// 4stellig mit führenden Nullen 1=0001  47 1/2=0047 1/2  11 1/128b = 0011 1/128b
			this.hausnummersortierbar = "";
			if (!hausnummer.equals("")) {
				int numstellen = 0;
				for (int posi = 0; posi < hausnummer.length(); posi++) {
					int charwert = hausnummer.charAt(posi);
					if ((charwert >= '0') && (charwert <= '9')) {
						numstellen++;
					} else {
						break;
					}
				}
				for (int anzi = 0; anzi < (HAUSNUMMERSORTIERBARLENGTH - numstellen); anzi++) {
					this.hausnummersortierbar += "0";
				}
				this.hausnummersortierbar += hausnummer;
			}
		}
	}

	public ImportHausnummerliste() {
		store = new TreeMap<String, Hausnummer>();
	}

	public final Hausnummer getHousenumber(final String key) {
		return store.get(key);
	}

	public final Hausnummer getHousenumberbyIndex(final Integer index) {
		Hausnummer ergebnishausnummer = null;
		NavigableSet<String> s = store.descendingKeySet();
		Iterator<String> it = s.iterator();
		int lfdnr = 0;
		while (it.hasNext()) {
			lfdnr++;
			String key = it.next();
			if(lfdnr == index) {
				ergebnishausnummer = store.get(key);
				break;
			}
		}
		return ergebnishausnummer;
	}


	public final boolean addHousenumber(final String streetName, final long streetid, final String subid, 
			final String postcode, final String housenumber,
			final double longitude, final double latitude, final String sourcesrid, final String sourcetext) {
		boolean value_added = false;
		String key = streetName + subid + housenumber;
		if(	(latlonforUniqueness) &&
			(longitude != lon_unset) &&
			(latitude != lat_unset))
		{
			key += String.valueOf(longitude) + String.valueOf(latitude);
		}
		Hausnummer street = store.get(key);
		if (street == null) {
			street = new Hausnummer(streetName, streetid, subid, postcode, housenumber, "", longitude, latitude, sourcesrid, sourcetext);
			store.put(key, street);
			value_added = true;
		}
		return value_added;
	}

	public final boolean addHousenumber(final String streetName, final long streetid, final String subid, 
			final String postcode, final String housenumber,
			final String bemerkung, final double longitude, final double latitude, final String sourcesrid, final String sourcetext) {
		boolean value_added = false;
		String key = streetName + subid + housenumber;
		if(	(latlonforUniqueness) &&
				(longitude != lon_unset) &&
				(latitude != lat_unset))
			{
				key += String.valueOf(longitude) + String.valueOf(latitude);
			}
		Hausnummer street = store.get(key);
		if (street == null) {
			street = new Hausnummer(streetName, streetid, subid, postcode, housenumber, bemerkung, longitude, latitude, sourcesrid, sourcetext);
			store.put(key, street);
			value_added = true;
		} else {
			System.out.println("address already defined as ===" + street.toString() + "===,   new " + streetName + " " 
				+ subid + " " + postcode + " " + housenumber + " " + bemerkung + " "
				+ longitude + " " + latitude + " -- key was ===" + key + "===");
			value_added = false;
		}
		return value_added;
	}

	public final void addHousenumber(final String streetName, final long streetid, final String subid, 
										final String postcode, final String housenumber) {
		addHousenumber(streetName, streetid, subid, postcode, housenumber, 0d, 0d, "", "");
	}

	
	public final int count() {
		NavigableSet<String> s = store.descendingKeySet();
		Iterator<String> it = s.iterator();
		int lfdnr = 0;
		while (it.hasNext()) {
			lfdnr++;
			String key = it.next();
		}
		return lfdnr;
	}

	public final boolean storeToDB() {
		Statement stmt = null;
boolean debug = false;
		String key = null;
		Hausnummer housenumber = null;

		if(officialgeocoordinates.equals("")) {
			System.out.println("ERROR: ImportHausnumerliste public entity officialgeocoordinates is not set, therefor the list will not be stored into DB");
			return false;
		}

		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");

			String urlHausnummern = configuration.db_application_url;
			conHausnummern = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);

			String urlListofstreets = configuration.db_application_listofstreets_url;
			conListofstreets = DriverManager.getConnection(urlListofstreets, 
					configuration.db_application_listofstreets_username, configuration.db_application_listofstreets_password);

			String sqlOfficialMunicipality = "SELECT name_unique, ags, hierarchy"
				+ " FROM officialkeys AS o, country AS c"
				+ " WHERE name like ?"
				+ " AND ags like ?"		// like is necessary, because ags can be empty and the query will be then ags like '%'
				+ " AND c.country = ?"
				+ " AND c.id = o.country_id"
				+ " AND o.level = 6"	// level = 6 means municipalities
				+ ";";
			System.out.println("sqlOfficialMunicipality-Anfrage ===" + sqlOfficialMunicipality + "=== ...");
			PreparedStatement stmtListofstreets = conListofstreets.prepareStatement(sqlOfficialMunicipality);
			if(! stadt.equals(""))
				stmtListofstreets.setString(1, stadt);
			else
				stmtListofstreets.setString(1, "%");

			if(! stadt_id.equals(""))
				stmtListofstreets.setString(2, stadt_id);
			else
				stmtListofstreets.setString(2, "%");
			stmtListofstreets.setString(3, land);

			
			String selectStreetidSql = "SELECT id FROM strasse where strasse = ?;";
			PreparedStatement selectStreetidStmt = conHausnummern.prepareStatement(selectStreetidSql);

			String insertstreetsql = "INSERT INTO strasse (strasse) VALUES (?) returning id;";
			PreparedStatement insertstreetstmt = conHausnummern.prepareStatement(insertstreetsql);
//TODO should be only one hit, otherwise error or not unique identification
			ResultSet rslOfficialMunicipality = stmtListofstreets.executeQuery();
			if (rslOfficialMunicipality.next()) {
				if(stadt.equals("")) {
					stadt = rslOfficialMunicipality.getString("name_unique");
				}
				if(stadt_id.equals(""))
					stadt_id = rslOfficialMunicipality.getString("ags");
				if(hierarchy.equals(""))
					hierarchy = rslOfficialMunicipality.getString("hierarchy");
			}
			rslOfficialMunicipality.close();
			stmtListofstreets.close();
			conListofstreets.close();

			
				// Hol stadtId. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die stadtId
			String selectMunicipalitySql = "SELECT s.id AS id"
				+ " FROM stadt AS s, land AS l"
				+ " WHERE stadt like ?"
				+ " AND land = ?"
				+ " AND s.land_id = l.id";
			if(! stadt_id.equals(""))
				selectMunicipalitySql += " AND officialkeys_id = ?";
			selectMunicipalitySql += ";";
			System.out.println("Select-Anfrage ===" + selectMunicipalitySql + "=== ...");
			PreparedStatement selectMunicipalityStmt = conHausnummern.prepareStatement(selectMunicipalitySql);

//TODO add info, if postcode is available
			String insertMunicipalitySql = "INSERT INTO stadt (land_id, stadt,"
				+ " officialkeys_id, osm_hierarchy,"
				+ "subareasidentifyable, housenumberaddition_exactly, officialgeocoordinates)"
				+ "  VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id;";
			PreparedStatement insertMunicipalityStmt = conHausnummern.prepareStatement(insertMunicipalitySql);

			String insertHousenumberWithGeometrySql = "INSERT into stadt_hausnummern(land_id, stadt_id, strasse_id,"
				+ " postcode, hausnummer, hausnummer_sortierbar, sub_id, hausnummer_bemerkung,"
				+ " point, pointsource)"
				+ " VALUES(?, ?, ?,"
				+ " ?, ?, ?, ?, ?,"
				+ " ST_Transform(ST_Setsrid(ST_Makepoint(?, ?), ?), 4326), ?);";
			PreparedStatement insertHousenumerWithGeometryStmt = conHausnummern.prepareStatement(insertHousenumberWithGeometrySql);

			String insertHousenumberWithoutGeometrySql = "INSERT into stadt_hausnummern(land_id, stadt_id, strasse_id,"
					+ " postcode, hausnummer, hausnummer_sortierbar, sub_id, hausnummer_bemerkung)"
					+ " VALUES(?, ?, ?,"
					+ " ?, ?, ?, ?, ?);";
			PreparedStatement insertHousenumerWithoutGeometryStmt = conHausnummern.prepareStatement(insertHousenumberWithoutGeometrySql);

			stmt = conHausnummern.createStatement();

			if(! debug)
				conHausnummern.setAutoCommit(false);


				// Hol stadtId. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die stadtId
			stadt_dbid = STADTIDUNSET;
			selectMunicipalityStmt.setString(1,stadt);
			selectMunicipalityStmt.setString(2,land);
			if(! stadt_id.equals(""))
				selectMunicipalityStmt.setString(3,stadt_id);
			System.out.println("Select Municipality, parameters ===" + stadt + "===, land ===" + land + "===, stadt_id = ===" + stadt_id 
				+ "===,    Anfrage ===" + selectMunicipalitySql + "=== ...");

			ResultSet rsStadt = selectMunicipalityStmt.executeQuery();
			if (rsStadt.next()) {
				stadt_dbid = rsStadt.getLong("id");
			} else {
				insertMunicipalityStmt.setLong(1, land_dbid);
				insertMunicipalityStmt.setString(2, stadt);
				insertMunicipalityStmt.setString(3, stadt_id);
				if(! stadt_id.equals(""))
					insertMunicipalityStmt.setString(3, stadt_id);
				else
					insertMunicipalityStmt.setString(3, "dummy");
				if((hierarchy != null) && !hierarchy.equals(""))
					insertMunicipalityStmt.setString(4, hierarchy);
				else
					insertMunicipalityStmt.setString(4, "");
				insertMunicipalityStmt.setString(5, subbereichescharf);
				insertMunicipalityStmt.setString(6, hausnummerzusatzgrosskleinrelevant);
				insertMunicipalityStmt.setString(7, officialgeocoordinates);
				rsStadt = insertMunicipalityStmt.executeQuery();
				if (rsStadt.next()) {
					stadt_dbid = rsStadt.getLong("id");
				}
			}
			selectMunicipalityStmt.close();
			
			NavigableSet<String> s = store.descendingKeySet();
			Iterator<String> it = s.iterator();
			while (it.hasNext()) {
				key = it.next();
				housenumber = store.get(key);

				if(housenumber.strasse_id == 0L) {
					if( street_idlist.containsKey(housenumber.strasse)) {
						housenumber.strasse_id  = street_idlist.get(housenumber.strasse);
					} else {
						selectStreetidStmt.setString(1, housenumber.strasse);
						System.out.println("query for street ===" + housenumber.strasse + "=== ...");
						ResultSet selstreetRS = selectStreetidStmt.executeQuery();
						if (selstreetRS.next()) {
							street_idlist.put(housenumber.strasse, selstreetRS.getInt("id"));
							housenumber.strasse_id = selstreetRS.getInt("id");
						} else {
							insertstreetstmt.setString(1, housenumber.strasse);
							//System.out.println("insert_sql statement ===" + insertstreetsql + "===");
							try {
								ResultSet rs_getautogenkeys = insertstreetstmt.executeQuery();
							    if (rs_getautogenkeys.next()) {
									street_idlist.put(housenumber.strasse, rs_getautogenkeys.getInt("id"));
									housenumber.strasse_id = rs_getautogenkeys.getInt("id");
							    } 
							    rs_getautogenkeys.close();
							}
							catch( SQLException e) {
								System.out.println("ERROR: during insert in table evaluation_overview, insert code was ===" + insertstreetsql + "===");
								System.out.println(e.toString());
							}
						}
					}
				}
				
				if(			(housenumber.longitude != 0f) 
						&&	(housenumber.latitude != 0f)) {
					insertHousenumerWithGeometryStmt.setLong(1, this.land_dbid);
					insertHousenumerWithGeometryStmt.setLong(2, this.stadt_dbid);
					insertHousenumerWithGeometryStmt.setLong(3, housenumber.strasse_id);
					insertHousenumerWithGeometryStmt.setString(4, housenumber.postcode);
					insertHousenumerWithGeometryStmt.setString(5, housenumber.hausnummer);
					insertHousenumerWithGeometryStmt.setString(6, housenumber.hausnummersortierbar);
					insertHousenumerWithGeometryStmt.setString(7, housenumber.subid);
					insertHousenumerWithGeometryStmt.setString(8, housenumber.bemerkung);
					insertHousenumerWithGeometryStmt.setDouble(9, housenumber.longitude);
					insertHousenumerWithGeometryStmt.setDouble(10, housenumber.latitude);
					insertHousenumerWithGeometryStmt.setInt(11, Integer.parseInt(housenumber.sourcesrid));
					insertHousenumerWithGeometryStmt.setString(12, housenumber.sourcetext);
					insertHousenumerWithGeometryStmt.executeUpdate();
				} else {
					insertHousenumerWithoutGeometryStmt.setLong(1, this.land_dbid);
					insertHousenumerWithoutGeometryStmt.setLong(2, this.stadt_dbid);
					insertHousenumerWithoutGeometryStmt.setLong(3, housenumber.strasse_id);
					insertHousenumerWithoutGeometryStmt.setString(4, housenumber.postcode);
					insertHousenumerWithoutGeometryStmt.setString(5, housenumber.hausnummer);
					insertHousenumerWithoutGeometryStmt.setString(6, housenumber.hausnummersortierbar);
					insertHousenumerWithoutGeometryStmt.setString(7, housenumber.subid);
					insertHousenumerWithoutGeometryStmt.setString(8, housenumber.bemerkung);
					insertHousenumerWithoutGeometryStmt.executeUpdate();
				}
			}
			if(!debug) {
				System.out.println("vor store_to_db commit ...");
				conHausnummern.commit();
				System.out.println("nach store_to_db commit");
			}

			selectStreetidStmt.close();
			insertHousenumerWithGeometryStmt.close();
			insertHousenumerWithoutGeometryStmt.close();
			stmt.close();
			conHausnummern.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
			try {
				if(!debug) {
					conHausnummern.rollback();
				}
				stmt.close();
				conHausnummern.close();
				return false;
			} catch (SQLException innere) {
				System.out.println("inner sql-exception (tried to rollback transaction or to close connection ...");
				innere.printStackTrace();
			}
		} 
		catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		return true;
	}
}
