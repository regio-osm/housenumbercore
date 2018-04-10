package de.regioosm.housenumbercore.util;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * represents a municipality, identified by name, country and, if possible, a unique reference id.
 * It doesn't include the geometry of the municipality. Therefore see class MunicipalityArea 
 * 
 * @author Dietmar Seifert
 * @version 0.9, 2018-02-20
 * 
 */
public class Municipality implements Comparable<Municipality> {
	private static Connection housenumberConn = null;

	/**
	 * Two letter ISO Code for Countries, see
	 * {@link https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2}
	 */
	protected String countrycode = "";
	/**
	 * Name of municipality. Should be the name like in OSM key name in municipality boundary relation
	 */
	protected String name = "";
	/**
	 * country specific, free usable <b>unique</b> identification for municiapality
	 * <br>In Germany, it is Amtlicher Gemeindeschlüssel, which is stored in OSM as key de:amtlicher_gemeindeschluessel
	 * <br>This ref uses the value of the key
	 * @see officialKey
	 */
	protected String officialRef = "";
	/**
	 * internal use: DB-Id for Country
	 */
	protected Long countryDBId = -1L;
	/**
	 * internnal use: DB-Id for municipality
	 */
	protected Long municipalityDBId = -1L;
	/**
	 * country specific, free usable identification for municiapality
	 * <br>In Germany, it is Amtlicher Gemeindeschlüssel, which is stored in OSM as key de:amtlicher_gemeindeschluessel
	 * <br>This ref uses the key
	 * 
	 * @see officialRef
	 */
	protected String officialKey = "";
	/**
	 * Define, which boundary administrative polygons in OSM should be used to make evaluations.
	 * <br>List of OSM key admin_level values.
	 * <br>If List is empty, all available boundary polygons on each admin level will be used.
	 * <br>This list is useful, when boundary polygons in a admin level is only partly used or
	 * in two different levels are the same boundary polygons.
	 */
	protected List<Integer> adminLevels = new ArrayList<>();
	/**
	 * Depending on the official housenumber list for a municipality, this is set to true, 
	 * if the list contains a sub area id field.
	 * <br>In this case, the evaluation is more precise, as it nows, in which sub area a 
	 * missing housenumber must be.
	 * <br>If set, adminLevels must be set to two values. One value is municipality level (mostly level 8),
	 * and the second value is sub area level, which corresponds to sub area field of housenumber list
	 * 
	 * @see adminLevels
	 */
	protected boolean subareasidentifyable;
	/**
	 * Optionally field in cases, where street names in official housenumber list differs 
	 * from mainly used language in OSM key name.
	 * <br>Example: in Belgium, two languages are mainly used. With this field, you define
	 * a two letter ISO language code (for example NL and then OSM key name:NL will be used,
	 * near main OSM key name, to find named streets
	 */
	protected String languagecode = "";	// force search/evaluate for names with country prefixes on osm key name:xx

	/**
	 * internal store to hold search results from method search.
	 * <br>Get a result with method next()
	 */
	private static List<Municipality> searchResults = new ArrayList<>();
	/**
	 * internal pointer to hold the actual result entry in list searchResults
	 * <br>It will be incremented every time next() will be called
	 */
	private static int searchResultindex = 0;
	/**
	 * Class-wide field for languagecode, which will can be set, if useful.
	 */
	private static String defaultLanguagecode = "";

	/**
	 * support an empty constructor.
	 * <br>for use of the instance, use at least setter setCountrycode and setName
	 */
	public Municipality() {
		this.countrycode = "";
		this.name = "";
		this.officialRef = "";
	}

	/**
	 * constructor for a municipality instance
	 * @param countrycode		Two letter ISO Code for Country, see {@link https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2}
	 * <br>You can get the code from Country.getCountryShortname
	 * @see Country.getCountryShortname
	 *
	 * @param municipalityName	Name of municipality, as stored in OSM in boundary polygon in key name
	 * 
	 * @param officialRef		country specific, free usable <b>unique</b> identification for municiapality
	 * @throws Exception 
	 * @see officialKey
	 */
	public Municipality(String country, String municipalityName, String officialRef) throws Exception {
		setCountrycode(country);
		this.name = municipalityName;
		this.officialRef = officialRef;
	}
	
	public Municipality(Municipality muni) throws Exception {
		this(muni.countrycode, muni.name, muni.officialRef);
		setCountryDBId(muni.countryDBId);
		setMunicipalityDBId(muni.municipalityDBId);
		this.officialKey = muni.officialKey;
		this.adminLevels = muni.adminLevels;
		this.subareasidentifyable = muni.subareasidentifyable;
		this.languagecode = muni.languagecode;
	}

	/**
	 * give this class access to housenumber DB
	 * @param housenumberConn
	 */
	public static void connectDB(Connection housenumberConn) {
		Municipality.housenumberConn = housenumberConn;
	}

	/**
	 * search for one or many municipalities in DB and returns the number of found municipalities.
	 * <br>The result will be stored internally and can get one municipality per time via calls to next()
	 * 
	 * @param country			In which Country the municipalities will be searched. Wildcard * allowed 
	 * @param municipalityName	Name of municipality. Wildcard * allowed, to get 
	 * 							more than one or even all municipality in country
	 * @param officialRef		Official reference of the municipality. Examine OSM key for 
	 * 							official ref in country via Country.getOfficialReferenceOsmKey()
	 * @param adminHierachyTextform		filter municipalities via admin hierarchy in textform. 
	 * 									For example "Bundesrepublik Deutschland,Bayern".
	 * 									Wildcard * allowed
	 * @return 					number of found municipalities in DB
	 * @throws Exception 
	 */
	public static int search(String municipalityCountry, String municipalityName, String officialRef, String adminHierachyTextform) throws Exception {
		if(housenumberConn == null) {
			throw new IllegalStateException("please first use .connectDB");
		}

		if((municipalityCountry == null) || municipalityCountry.equals("")) {
			throw new IllegalStateException("missing Country");
		}
		
		String countrycode = municipalityCountry;
		if(municipalityCountry.length() > 2)
			countrycode = Country.getCountryShortname(municipalityCountry);
		if(countrycode.equals(""))
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
		if((adminHierachyTextform == null) || adminHierachyTextform.equals("")) {
			adminHierachyTextform = "%";
		} else  if(adminHierachyTextform.indexOf("*") != -1) {
			adminHierachyTextform = adminHierachyTextform.replace("*",  "%");
		}

			// initialize array for results (could be already set by previous search
		searchResults.clear();
		searchResultindex = 0;

		String searchMunicipalitiesSql = "";
		searchMunicipalitiesSql = "SELECT m.id AS muniid, stadt AS name, " +
			"officialkeys_id AS officialref, gemeindeschluessel_key AS officialkey, land AS country, " +
			"countrycode, c.id AS countryid, subareasidentifyable, active_adminlevels " +
			"FROM stadt AS m JOIN land AS c ON m.land_id = c.id " +
			"WHERE c.countrycode = ? " +
			"AND m.stadt like ? " +
			"AND officialkeys_id like ? " +
			"AND osm_hierarchy like ?;";

		try {
			PreparedStatement searchMunicipalitiesStmt = housenumberConn.prepareStatement(searchMunicipalitiesSql);
			int stmtindex = 1;
			searchMunicipalitiesStmt.setString(stmtindex++, countrycode);
			searchMunicipalitiesStmt.setString(stmtindex++, municipalityName);
			searchMunicipalitiesStmt.setString(stmtindex++, officialRef);
			searchMunicipalitiesStmt.setString(stmtindex++, adminHierachyTextform);
			System.out.println("sql query for municipalities ===" + searchMunicipalitiesStmt.toString() + "===");
			ResultSet searchMunicipalitiesRs = searchMunicipalitiesStmt.executeQuery();
			int resultcount = 0;
				// loop over all found municipalities. store results in internal searchResults Array
			while( searchMunicipalitiesRs.next() ) {
				resultcount++;
				Municipality muni = new Municipality(searchMunicipalitiesRs.getString("countrycode"), 
					searchMunicipalitiesRs.getString("name"),
					searchMunicipalitiesRs.getString("officialref"));
				muni.municipalityDBId = searchMunicipalitiesRs.getLong("muniid");
				muni.officialKey = searchMunicipalitiesRs.getString("officialkey");
				if(searchMunicipalitiesRs.getArray("active_adminlevels") != null) {
					Array templevels = searchMunicipalitiesRs.getArray("active_adminlevels");
					muni.adminLevels = Arrays.asList((Integer[]) templevels.getArray());
				}
				if(	searchMunicipalitiesRs.getString("subareasidentifyable") != null && 
					searchMunicipalitiesRs.getString("subareasidentifyable").equals("y"))
					muni.subareasidentifyable = true;
				else
					muni.subareasidentifyable = false;
				if((Municipality.defaultLanguagecode != null) && !Municipality.defaultLanguagecode.equals(""))
					muni.languagecode = Municipality.defaultLanguagecode;
				else
					muni.languagecode = searchMunicipalitiesRs.getString("countrycode");
				searchResults.add(muni);
			}
			searchMunicipalitiesRs.close();
			searchMunicipalitiesStmt.close();
			return resultcount;
		}
		catch( SQLException e) {
			e.printStackTrace();
			throw e;
		}
	}


	/**
	 * get next municipality from result of the search() call
	 */
	public static Municipality next() {
		Municipality municipality = null;
		
		if(searchResults.size() > searchResultindex) {
			municipality = (Municipality) searchResults.get(searchResultindex);
			searchResultindex++;
		}
		return municipality;
	}
	
	public String getCountrycode() {
		return this.countrycode;
	}

	public String getLanguagecode() {
		return this.languagecode;
	}

	public List<Integer> getAdminLevels() {
		return adminLevels;
	}

	public long getMunicipalityDBId() {
		return municipalityDBId;
	}
		
	public String getName() {
		return this.name;
	}


	public String getOfficialRef() {
		return this.officialRef;
	}

	public String getOfficialKey() {
		return this.officialKey;
	}
	
	public boolean isSubareasidentifyable() {
		return subareasidentifyable;
	}

	/**
	 * set the 2letter ISO code for Country, if you want to force searches and evaluations of objects with key name:xx
	 * for example in Countries, where 2 languages are spoken, like Belgium or in Italy Area of South-Tirol
	 * @param languagecode
	 */
	public static void setLanguageCode(String languagecode) {
		Municipality.defaultLanguagecode = languagecode;
	}
	
	public void setCountrycode(String countrycode) throws Exception {
		if((countrycode != null) && (countrycode.length() == 2)) {
			this.countrycode = countrycode;
		} else {
			Country.connectDB(housenumberConn);
			this.countrycode = Country.getCountryShortname(countrycode);
		}
	}

	/**
	 * parameter country can be either long form of country name
	 * or two letter ISO Code 3166-1_alpha_2 for Countries
	 * @param country
	 * @throws Exception 
	 */
	public void setCountry(String country) throws Exception {
		if((country != null) && (country.length() == 2)) {
			this.countrycode = country;
		} else {
			Country.connectDB(housenumberConn);
			this.countrycode = Country.getCountryShortname(country);
		}
	}

	public void setMunicipalityDBId(long municipalityid) {
		this.municipalityDBId = municipalityid;
	}
		
	public void setName(String name) {
		this.name = name;
	}
	
	public void setOfficialRef(String officialref) {
		this.officialRef = officialref;
	}

	public void setCountryDBId(long countryid) {
		this.countryDBId = countryid;
	}

	/**
	 * set flag for municipality, that subarea of municipality is identifyable (if parameter is set to true)
	 * @param subareaidentifyable
	 */
	public void setSubareasidentifyable(boolean subareaidentifyable) {
		this.subareasidentifyable = subareaidentifyable;
	}
	
	public Municipality loadFromDB() throws Exception {
		Municipality result = null;
		
		String selectMunicipalitySql = "";
		selectMunicipalitySql = "SELECT m.id AS muniid, officialkeys_id " +
			"FROM stadt AS m JOIN land AS c ON m.land_id = c.id " +
			"WHERE c.countrycode like ? " +
			"AND m.stadt like ? " +
			"AND officialkeys_id like ?;";

		try {
			String officialref = "%";
			if((this.officialRef != null) && !this.officialRef.equals(""))
				officialref = this.officialRef;
			PreparedStatement selectMunicipalityStmt = housenumberConn.prepareStatement(selectMunicipalitySql);
			int stmtindex = 1;
			selectMunicipalityStmt.setString(stmtindex++, this.countrycode);
			selectMunicipalityStmt.setString(stmtindex++, this.name);
			selectMunicipalityStmt.setString(stmtindex++, officialref);
			System.out.println("sql query for municipalities ===" + selectMunicipalityStmt.toString() + "===");
			ResultSet selectMunicipalityRs = selectMunicipalityStmt.executeQuery();
				// check, if exactly one hit.
			int resultcount = 0;
			while( selectMunicipalityRs.next() ) {
				resultcount++;
				result = new Municipality(this.countrycode, this.name, 
					selectMunicipalityRs.getString("officialkeys_id"));
				result.setMunicipalityDBId(selectMunicipalityRs.getLong("muniid"));
			}
			selectMunicipalityRs.close();
			selectMunicipalityStmt.close();
			
			if(resultcount == 0)
				return null;
			else if(resultcount == 1)
				return result;
			else
				throw new IllegalStateException("Municipality instance is not unique");
		}
		catch( SQLException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Do the municipality exists in housenumber DB?
	 * There must be exactly one hit, only in this case true returns.
	 * If more than one hit was found, an exception will be thrown
	 * @return
	 * @throws Exception 
	 */
	public boolean exists() throws Exception {

		if(loadFromDB() != null)
			return true;
		else
			return false;
	}

	public Municipality store() throws Exception {
		Municipality resultmunicipality = null;
		
		resultmunicipality = loadFromDB();
		if( resultmunicipality != null)
			return resultmunicipality;

		try {
			String insertMunicipalitySql = "INSERT INTO stadt (land_id, stadt, "+
				"officialkeys_id, osm_hierarchy, " +
				"subareasidentifyable, housenumberaddition_exactly, officialgeocoordinates) " +
				"VALUES ((SELECT id FROM land WHERE countrycode = ?), " +
				"?, ?, ?, ?, ?, ?) RETURNING id;";
			PreparedStatement insertMunicipalityStmt = housenumberConn.prepareStatement(insertMunicipalitySql);
			int stmtindex = 1;
//TODO very simple storage of municipality: must be enhanced with more parameters, which are declared fix below
			insertMunicipalityStmt.setString(stmtindex++, getCountrycode());
			insertMunicipalityStmt.setString(stmtindex++, getName());
			insertMunicipalityStmt.setString(stmtindex++, getOfficialRef());
			insertMunicipalityStmt.setString(stmtindex++, "");
			insertMunicipalityStmt.setString(stmtindex++, isSubareasidentifyable() ? "y": "n");
			insertMunicipalityStmt.setString(stmtindex++, "n");
			insertMunicipalityStmt.setString(stmtindex++, "n");
			System.out.println("insert statement for new municipality ===" + insertMunicipalityStmt.toString() + "===");

			ResultSet insertMunicipalityRs = insertMunicipalityStmt.executeQuery();
			if (insertMunicipalityRs.next()) {
				setMunicipalityDBId(insertMunicipalityRs.getLong("id"));
			}
			insertMunicipalityRs.close();
			insertMunicipalityStmt.close();

			resultmunicipality = this;
		}
		catch( SQLException e) {
			e.printStackTrace();
			throw e;
		}
		return resultmunicipality;
	}
	
	@Override
	/**
	 * return a simple textual representation of the municipality instance 
	 */
	public String toString() {
		String result = "";
		
		result += countrycode + " " + name;
		if(subareasidentifyable)
			result += ", Subareas active"; 
		if(!languagecode.equals(countrycode))
			result += ", Language explicit set to " + languagecode;
			result += ", Subareas active"; 
		result += " " + officialRef + " (" + officialKey + ")";
		result += " - Technical Details: muni DBid: " + municipalityDBId + ", Admin-Levels: " + adminLevels.toString();
		
		return result;
	}


	/**
	 * method define, if parameter obj is identical or not to instance, depending on important fields of the class
	 */
	@Override
	public boolean equals(Object obj) {
		if(! (obj instanceof Municipality))
			return false;
		Municipality other = (Municipality) obj;
		if(	this.countrycode.equals(other.countrycode) &&
			this.name.equals(other.name)) {
			if((this.officialRef == null) && (other.officialRef == null))
				return true;
			if((this.officialRef != null) && (other.officialRef != null) &&
				this.officialRef.equals(other.officialRef))
				return true;
			else
				return false;
			
		} else
			return false;
	}

	/**
	 * method to get a fast hashcode for the instance, to distinct from other instances of municipality
	 */
	@Override
	public int hashCode() {
		int hashCode = 13;
		int multi = 31;
		hashCode += countrycode.hashCode();
		hashCode *= multi + name.hashCode();
		hashCode *= multi + officialRef.hashCode();

		return hashCode;
	}

	/**
	 * method to allow sorting of municipality instances
	 */
	@Override
    public int compareTo(Municipality other) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if(other == null) return AFTER;

		if(!(other instanceof Municipality))
			return BEFORE;
		if(this == other) return EQUAL;
		
		int comparison = this.countrycode.compareTo(other.countrycode);
		if (comparison != EQUAL) return comparison;

		comparison = this.name.compareTo(other.name);
		if (comparison != EQUAL) return comparison;

		comparison = this.officialRef.compareTo(other.officialRef);
		if (comparison != EQUAL) return comparison;

		return EQUAL;
	}
}
