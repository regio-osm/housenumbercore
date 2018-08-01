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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.regioosm.housenumbercore.util.Applicationconfiguration;


public class HousenumberList {

	/**
	 * signals not set the id of a city.
	 */
	private static final long MUNICIPALITYIDUNSET = -1L;
	/**
	 * signal not set of the id of a country.
	 */
	private static final long COUNTRYIDUNSET = -1L;


	private String countrycode = "";
	private String hierarchy = "";
	/**
	 * DB-internal Id of the actual country, the housenumber list belongs to
	 */
	private long countryDBId = COUNTRYIDUNSET;	// should be deleted completely
	
	private String municipality = "";
	/**
	 * DB-internal Id of the actual city, the housenumber list belongs to
	 */
	private long municipalityDBId = MUNICIPALITYIDUNSET;
	/**
	 * unique, country-specific, unique Reference Id of municipality
	 */
	private String municipalityRef = "";
	/**
	 * Should the sub areas of a municipality are evaluated?
	 * 
	 */
	private boolean subareaActive = false;
	/**
	 * are the geocoordinates are official to import and for directly use in osm?
	 * If not official useable in osm, but geocoordinates are available, they will be stored and used for quality assurance, but not for osm import
	 */
//TODO these are two light different aspects and must be configured separately
	private boolean officialgeocoordinates = false;
	/**
	 * defines, if housenumber additions must be exactly identical between officlal list and osm.
	 * set true for exactly writing of housenumber addition 'A' and 'a' are different.
	 * set false for housenumber addition case irrelevant, 'A' and 'a' are the same and hit.
	 */
	private boolean housenumberadditionsexactly = false;


	private String sourcelistUrl = null;
	private String sourcelistCopyright = null;
	private String sourcelistUseage = null;
	private Date sourcelistContentdate = null;
	private Date sourcelistFiledate = null;
	
	/**
	 * internal Structure to hold all housenumber-Objects;
	 */
	private Map<String, ImportAddress> housenumbers = new HashMap<>(1000);

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
	private static Connection housenumberConn = null;

	public HousenumberList() {
		
	}

	public HousenumberList(Municipality municipality) {
		this.setMunicipality(municipality);
	}
	
	public String getHousenumberKey(String street, String subarea, String postcode, 
		String housenumber) {

		String key = street + subarea;
		if(postcode != null)
			key += postcode;
		key += housenumber;
		return key;
	}
	
	public String getHousenumberKey(String street, String subarea, String postcode, 
		String housenumber, Double lon, Double lat) {

		String key = getHousenumberKey(street, subarea, postcode, housenumber);
//TODO before 2018-03, it was possible to import an identical address more than one time in DB, if it comes with two different geolocations
// in importfile. But it can't be distingished later in DB against OSM corresponding address, so this is not longer supported
/*		if(	ImportAddress.isDistingishHousenumberByCoordinates() &&
			(lon != ImportAddress.lonUnset) &&
			(lat != ImportAddress.latUnset)) {
			key += String.valueOf(lon) + String.valueOf(lat);
		}
*/
		return key;
	}

	public String getHousenumberKey(ImportAddress address) {
		return getHousenumberKey(address.getStreet(), address.getSubArea(), address.getPostcode(),
			address.getHousenumber(), address.getLon(), address.getLat());
	}

	/** 
	 * is the address already in housenumberlist
	 * @param address
	 * @return
	 */
	public boolean contains(ImportAddress address) {
		String key = getHousenumberKey(address.getStreet(), address.getSubArea(), address.getPostcode(),
			address.getHousenumber(), address.getLon(), address.getLat());
		if(housenumbers == null)
			return false;
		
		return housenumbers.containsKey(key);
	}

	public boolean addHousenumber(ImportAddress address) {

		String key = getHousenumberKey(address);
		if(!contains(address)) {
			housenumbers.put(key, address);
			return true;
		} else {
			System.out.println("address already defined as ===" + 
				housenumbers.get(key).toString() + "===,   new " + 
				address.toString() +" -- key was ===" + key + "===");
			return false;
		}
	}

	public boolean addHousenumber(String streetName, long streetid, String subid, 
			String postcode, String housenumber,
			String bemerkung, double longitude, double latitude, String sourcesrid, String sourcetext) {

		ImportAddress.connectDB(housenumberConn);
		ImportAddress address = null;
		try {
			address = new ImportAddress(streetName, streetid, subid, postcode, housenumber, bemerkung, longitude, latitude, sourcesrid, sourcetext);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.addHousenumber(address);
	}

	
	public ImportAddress getHousenumber(String key) {
		return housenumbers.get(key);
	}

	/**
	 * get all housenumbers
	 * @return list of all housenumbers as Map<String, ImportAddress>
	 */
	public Map<String, ImportAddress> getHousenumbers() {
		return this.housenumbers;
	}

	public int countHousenumbers() {
		return housenumbers.size();
	}

	public final boolean storeToDB() {
		Statement stmt = null;

		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");

			if(housenumberConn == null) {
				String urlHausnummern = configuration.db_application_url;
				housenumberConn = DriverManager.getConnection(urlHausnummern, 
					configuration.db_application_username, configuration.db_application_password);
			}

//TODO if sourcesrid is set, it must checked first, before any import action starts

			

			
				// Hol stadtId. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die stadtId
			String selectMunicipalitySql = "SELECT s.id AS muniid, l.id AS countryid " +
				"FROM stadt AS s JOIN land AS l ON s.land_id = l.id " +
				"WHERE stadt like ? " +
				"AND countrycode = ? ";
			if (!getMunicipalityRef().equals(""))
				selectMunicipalitySql += " AND officialkeys_id = ?";
			if (getMunicipalityDBId() != MUNICIPALITYIDUNSET)
				selectMunicipalitySql += " AND s.id = ?";
			selectMunicipalitySql += ";";
			PreparedStatement selectMunicipalityStmt = housenumberConn.prepareStatement(selectMunicipalitySql);
			System.out.println("Select-Anfrage ===" + selectMunicipalityStmt.toString() + "=== ...");

//TODO add info, if postcode is available
//TODO add finer configuration, if officialgeocoordinates allow directly osm upload
			String insertMunicipalitySql = "INSERT INTO stadt (land_id, stadt, " +
				"officialkeys_id, osm_hierarchy, " +
				"subareasidentifyable, housenumberaddition_exactly, officialgeocoordinates, " +
				"sourcelist_url, sourcelist_copyrighttext, sourcelist_useagetext, " +
				"sourcelist_contentdate, sourcelist_filedate) " +
				"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id;";
			PreparedStatement insertMunicipalityStmt = housenumberConn.prepareStatement(insertMunicipalitySql);

//TODO add finer configuration, if officialgeocoordinates allow directly osm upload
			String updateMunicipalitySql = "UPDATE stadt SET " +
				"sourcelist_url = ?, sourcelist_copyrighttext = ?, sourcelist_useagetext = ?, " +
				"officialgeocoordinates = ?, " +
				"sourcelist_contentdate = ?, sourcelist_filedate = ? " +
				"WHERE id = ?;";
			PreparedStatement updateMunicipalityStmt = housenumberConn.prepareStatement(updateMunicipalitySql);
			
			String insertHousenumberWithGeometrySql = "INSERT into stadt_hausnummern(land_id, stadt_id, strasse_id, " +
				"postcode, hausnummer, hausnummer_sortierbar, sub_id, hausnummer_bemerkung, extraosmtags, " +
				"point, pointsource) " +
				"VALUES(?, ?, ?, " +
				"?, ?, ?, ?, ?, ?::hstore, " +
				"ST_Transform(ST_Setsrid(ST_Makepoint(?, ?), ?), 4326), ?);";
			PreparedStatement insertHnoWithGeomStmt = housenumberConn.prepareStatement(insertHousenumberWithGeometrySql);

			String insertHousenumberWithoutGeometrySql = "INSERT into stadt_hausnummern(land_id, stadt_id, strasse_id, " +
				"postcode, hausnummer, hausnummer_sortierbar, sub_id, hausnummer_bemerkung, extraosmtags) " +
				"VALUES(?, ?, ?, " +
				"?, ?, ?, ?, ?, ?::hstore);";
			PreparedStatement insertHnoWoGeomStmt = housenumberConn.prepareStatement(insertHousenumberWithoutGeometrySql);


			housenumberConn.setAutoCommit(false);

			boolean municipalityAlreadyExists = false;
			stmt = housenumberConn.createStatement();
				// Hol stadtId. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die stadtId
			int stmtindex = 1;
			selectMunicipalityStmt.setString(stmtindex++, getMunicipalityName());
			selectMunicipalityStmt.setString(stmtindex++, getCountrycode());
			if(! getMunicipalityRef().equals(""))
				selectMunicipalityStmt.setString(stmtindex++, getMunicipalityRef());
			if (getMunicipalityDBId() != MUNICIPALITYIDUNSET)
				selectMunicipalityStmt.setLong(stmtindex++, getMunicipalityDBId());
			System.out.println("Select Municipality, if existing, Statement " + 
				selectMunicipalityStmt.toString());

			ResultSet selectMunicipalityRs = selectMunicipalityStmt.executeQuery();
			if (selectMunicipalityRs.next()) {
				setMunicipalityDBId(selectMunicipalityRs.getLong("muniid"));
				countryDBId = selectMunicipalityRs.getLong("countryid");
				municipalityAlreadyExists = true;

				stmtindex = 1;

//if((getMunicipalityRef() != null) && !getMunicipalityRef().equals(""))
				updateMunicipalityStmt.setString(stmtindex++, getSourcelistUrl());
				updateMunicipalityStmt.setString(stmtindex++, getsourcelistCopyright());
				updateMunicipalityStmt.setString(stmtindex++, getsourcelistUseage());
				updateMunicipalityStmt.setString(stmtindex++, isOfficialgeocoordinates() ? "y" : "n");
				updateMunicipalityStmt.setDate(stmtindex++, new java.sql.Date(getContentDate().getTime()));
				updateMunicipalityStmt.setDate(stmtindex++, new java.sql.Date(getFileDate().getTime()));
				updateMunicipalityStmt.setLong(stmtindex++, selectMunicipalityRs.getLong("muniid"));
				System.out.println("Update Municipality Statement ===" + updateMunicipalityStmt.toString() + "===");
				updateMunicipalityStmt.executeUpdate();
			} else {
				if(countryDBId == COUNTRYIDUNSET) {
					String selectCountrySql = "SELECT l.id AS countryid " +
							"FROM land AS l WHERE countrycode = ?;";
					PreparedStatement selectCountryStmt = 
						housenumberConn.prepareStatement(selectCountrySql);
					selectCountryStmt.setString(1, this.countrycode);
					System.out.println("Select country Statement ===" + 
						selectCountryStmt.toString() + "=== ...");
					ResultSet selectCountryRs = selectCountryStmt.executeQuery();
					if (selectCountryRs.next()) {
						countryDBId = selectCountryRs.getLong("countryid");
					}
				}

				stmtindex = 1;
				insertMunicipalityStmt.setLong(stmtindex++, countryDBId);
				insertMunicipalityStmt.setString(stmtindex++, getMunicipalityName());
				if((getMunicipalityRef() != null) && !getMunicipalityRef().equals(""))
					insertMunicipalityStmt.setString(stmtindex++, getMunicipalityRef());
				else
					insertMunicipalityStmt.setString(stmtindex++, "dummy");
				if((hierarchy != null) && !hierarchy.equals(""))
					insertMunicipalityStmt.setString(stmtindex++, hierarchy);
				else
					insertMunicipalityStmt.setString(stmtindex++, "");
				insertMunicipalityStmt.setString(stmtindex++, isSubareaActive() ? "y" : "n");
				insertMunicipalityStmt.setString(stmtindex++, housenumberadditionsexactly ? "y" : "n");
				insertMunicipalityStmt.setString(stmtindex++, isOfficialgeocoordinates() ? "y" : "n");
				insertMunicipalityStmt.setString(stmtindex++, getSourcelistUrl());
				insertMunicipalityStmt.setString(stmtindex++, getsourcelistCopyright());
				insertMunicipalityStmt.setString(stmtindex++, getsourcelistUseage());
				insertMunicipalityStmt.setDate(stmtindex++, new java.sql.Date(getContentDate().getTime()));
				insertMunicipalityStmt.setDate(stmtindex++, new java.sql.Date(getFileDate().getTime()));
				System.out.println("Insert Municipality Statement ===" + insertMunicipalityStmt.toString() + "===");

				selectMunicipalityRs = insertMunicipalityStmt.executeQuery();
				if (selectMunicipalityRs.next()) {
					setMunicipalityDBId(selectMunicipalityRs.getLong("id"));
				}
			}
			selectMunicipalityRs.close();
			selectMunicipalityStmt.close();
			
			if(municipalityAlreadyExists) {
				String deleteActualHousenumberListSql = "DELETE FROM stadt_hausnummern " +
					"WHERE stadt_id = ?;";
				PreparedStatement deleteActualHousenumberListStmt = 
					housenumberConn.prepareStatement(deleteActualHousenumberListSql);
				deleteActualHousenumberListStmt.setLong(1, getMunicipalityDBId());
				System.out.println("delete actual housenumberlist for municipality, if existent. " +
					"Statement is " + deleteActualHousenumberListStmt.toString());
				deleteActualHousenumberListStmt.executeUpdate();

				housenumberConn.commit();
				housenumberConn.setAutoCommit(false);
			}

			String selectStreetidSql = "SELECT id FROM strasse where strasse = ?;";
			PreparedStatement selectStreetidStmt = housenumberConn.prepareStatement(selectStreetidSql);

			String insertstreetsql = "INSERT INTO strasse (strasse) VALUES (?) returning id;";
			PreparedStatement insertstreetstmt = housenumberConn.prepareStatement(insertstreetsql);

				// check for every street in new housenumber list, that street name is already available
				// in table street (independend from municipality)
			for(Map.Entry<String, ImportAddress> housenumberentry: housenumbers.entrySet()) {
				ImportAddress housenumber = housenumberentry.getValue();

				if(housenumber.getStreetDBId() == 0L) {
					if( street_idlist.containsKey(housenumber.getStreet())) {
						housenumber.setStreetDBId(street_idlist.get(housenumber.getStreet()));
					} else {
						selectStreetidStmt.setString(1, housenumber.getStreet());
						System.out.println("query for street ===" + selectStreetidStmt.toString() + "=== ...");
						ResultSet selstreetRS = selectStreetidStmt.executeQuery();
						if (selstreetRS.next()) {
							street_idlist.put(housenumber.getStreet(), selstreetRS.getInt("id"));
							housenumber.setStreetDBId(selstreetRS.getInt("id"));
						} else {
								// wow, street isn't already in table street, so store now
							insertstreetstmt.setString(1, housenumber.getStreet());
							System.out.println("insert_sql statement ===" + insertstreetstmt.toString() + "===");
							try {
								ResultSet insertstreetRs = insertstreetstmt.executeQuery();
							    if (insertstreetRs.next()) {
									street_idlist.put(housenumber.getStreet(), insertstreetRs.getInt("id"));
									housenumber.setStreetDBId(insertstreetRs.getInt("id"));
							    } 
							    insertstreetRs.close();
							}
							catch( SQLException e) {
								System.out.println("ERROR: during insert in table evaluation_overview, insert code was ===" + insertstreetsql + "===");
								System.out.println(e.toString());
							}
						}
					}
				}
				
				String osmtagsString = "";
				if (housenumber.osmtags.size() > 0) {
					List<OSMTag> tags = housenumber.osmtags.getTags();
					for(int tagindex = 0; tagindex < tags.size(); tagindex++) {
						if (!osmtagsString.equals(""))
							osmtagsString += ",";
						osmtagsString += tags.get(tagindex).getKey() + "=>" +
								tags.get(tagindex).getValue();
					}
				}

				if(		(housenumber.getLon() != ImportAddress.lonUnset) && 
						(housenumber.getLat() != ImportAddress.latUnset)) {
					stmtindex = 1;
					insertHnoWithGeomStmt.setLong(stmtindex++, this.countryDBId);
					insertHnoWithGeomStmt.setLong(stmtindex++, this.getMunicipalityDBId());
					insertHnoWithGeomStmt.setLong(stmtindex++, housenumber.getStreetDBId());
					insertHnoWithGeomStmt.setString(stmtindex++, housenumber.getPostcode());
					insertHnoWithGeomStmt.setString(stmtindex++, housenumber.getHousenumber());
					insertHnoWithGeomStmt.setString(stmtindex++, housenumber.getHousenumberSortable());
					insertHnoWithGeomStmt.setString(stmtindex++, housenumber.getSubArea());
					insertHnoWithGeomStmt.setString(stmtindex++, housenumber.getNote());
					insertHnoWithGeomStmt.setString(stmtindex++, osmtagsString);
					insertHnoWithGeomStmt.setDouble(stmtindex++, housenumber.getLon());
					insertHnoWithGeomStmt.setDouble(stmtindex++, housenumber.getLat());
					insertHnoWithGeomStmt.setInt(stmtindex++, Integer.parseInt(housenumber.getSourceSrid()));
					insertHnoWithGeomStmt.setString(stmtindex++, housenumber.getCoordinatesSourceText());
					System.out.println("insert hno with geom " + insertHnoWithGeomStmt.toString());
					insertHnoWithGeomStmt.executeUpdate();
				} else {
					stmtindex = 1;
					insertHnoWoGeomStmt.setLong(stmtindex++, this.countryDBId);
					insertHnoWoGeomStmt.setLong(stmtindex++, this.getMunicipalityDBId());
					insertHnoWoGeomStmt.setLong(stmtindex++, housenumber.getStreetDBId());
					insertHnoWoGeomStmt.setString(stmtindex++, housenumber.getPostcode());
					insertHnoWoGeomStmt.setString(stmtindex++, housenumber.getHousenumber());
					insertHnoWoGeomStmt.setString(stmtindex++, housenumber.getHousenumberSortable());
					insertHnoWoGeomStmt.setString(stmtindex++, housenumber.getSubArea());
					insertHnoWoGeomStmt.setString(stmtindex++, housenumber.getNote());
					insertHnoWoGeomStmt.setString(stmtindex++, osmtagsString);
					System.out.println("insert hno without geom " + insertHnoWoGeomStmt.toString());
					insertHnoWoGeomStmt.executeUpdate();
				}
			}

			System.out.println("vor store_to_db commit ...");
			housenumberConn.commit();
			System.out.println("nach store_to_db commit");

			selectStreetidStmt.close();
			insertHnoWithGeomStmt.close();
			insertHnoWoGeomStmt.close();
			stmt.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
			try {
				housenumberConn.rollback();
				stmt.close();
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

	/**
	 * @return the countrycode
	 */
	public String getCountrycode() {
		return countrycode;
	}

	public String getHierarchy() {
		return this.hierarchy;
	}

	/**
	 * @return the municipalityDBId
	 */
	public long getMunicipalityDBId() {
		return municipalityDBId;
	}

	/**
	 * @return the municipality
	 */
	public String getMunicipalityName() {
		return municipality;
	}

	/**
	 * @return the municipalityRef
	 */
	public String getMunicipalityRef() {
		return this.municipalityRef;
	}

	public String getSourcelistUrl() {
		return this.sourcelistUrl;
	}
	
	public String getsourcelistCopyright() {
		return this.sourcelistCopyright;
	}

	public String getsourcelistUseage() {
		return this.sourcelistUseage;
	}

	public Date getContentDate() {
		return this.sourcelistContentdate;
	}

	public Date getFileDate() {
		return this.sourcelistFiledate;
	}
	

	/**
	 * @return the officialgeocoordinates
	 */
	public boolean isOfficialgeocoordinates() {
		return officialgeocoordinates;
	}

	/**
	 * @return the subareaActive
	 */
	public boolean isSubareaActive() {
		return subareaActive;
	}


	/**
	 * @param countrycode the countrycode to set
	 */
	public void setCountrycode(String countrycode) {
		this.countrycode = countrycode;
	}

	public void setHierarchy(String hierarchy) {
		this.hierarchy = hierarchy;
	}

	/**
	 * @param municipality the municipality to set
	 */
	public void setMunicipalityName(String municipality) {
		this.municipality = municipality;
	}
	
	public void setMunicipality(Municipality m) {
		this.setCountrycode(m.getCountrycode());
		this.countryDBId = m.countryDBId;
		this.municipality = m.getName();
		this.setMunicipalityDBId(m.getMunicipalityDBId());
		this.setMunicipalityRef(m.getOfficialRef());
	}

	/**
	 * @param municipalityDBId the municipalityDBId to set
	 */
	public void setMunicipalityDBId(long municipalityDBId) {
		this.municipalityDBId = municipalityDBId;
	}

	/**
	 * @param municipalityRef the municipalityRef to set
	 */
	public void setMunicipalityRef(String municipalityRef) {
		this.municipalityRef = municipalityRef;
	}

	/**
	 * @param subareaActive the subareaActive to set
	 */
	public void setSubareaActive(boolean subareaActive) {
		this.subareaActive = subareaActive;
	}

	public void setSourcelistUrl(String downloadurl) {
		this.sourcelistUrl = downloadurl;
	}

	public void setSourcelistCopyright(String copyrighttext) {
		this.sourcelistCopyright = copyrighttext;
	}

	public void setSourcelistUseage(String useagetext) {
		this.sourcelistUseage = useagetext;
	}

	public void setContentDate(Date contentdate) {
		this.sourcelistContentdate = contentdate;
	}

	public void setFileDate(Date filedate) {
		this.sourcelistFiledate = filedate;
	}

	/**
	 * @param officialgeocoordinates the officialgeocoordinates to set
	 */
	public void setOfficialgeocoordinates(boolean officialgeocoordinates) {
		this.officialgeocoordinates = officialgeocoordinates;
	}

	public static void connectDB(Connection housenumberdbconn) {
		housenumberConn = housenumberdbconn;
	}
}
