package de.regioosm.housenumbercore.util;
/*

	V2.0, 03.05.2013, Dietmar Seifert
		* Portierung auf regio-osm.de und Berücksichtigung große DB, nicht mehr Snapshot mit den genau passenden Gebieten und sonst nichts

	V1.0 02.02.2011
		* Erstellung

*/

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//import de.diesei.listofstreets.LogMessage;


/*
 * 04.05.2015 die noch fehlenden städte in Niederlande bei den Auswertungen
 * SELECT land.id AS countryid, land, stadt.id AS municipalityid, stadt, officialkeys_id, osm_hierarchy FROM  stadt, land WHERE stadt.land_id = land.id AND land = 'Netherland' AND stadt.id NOT IN (SELECT distinct(s.id) FROM evaluations AS e, jobs AS j, gebiete AS g, stadt AS s, land as l  WHERE e.job_id = j.id AND j.gebiete_id = g.id AND g.stadt_id = s.id AND s.land_id = l.id and land = 'Netherland') ORDER BY osm_hierarchy,stadt, officialkeys_id;
 */


public class Municipality implements Comparable {
	private static Connection housenumberConn = null;

	protected String countrycode = "";
	protected String name = "";
	protected String officialRef = "";
	protected Long countryDBId = -1L;
	protected Long municipalityDBId = -1L;
	protected String officialKey = "";
	protected String adminPolygonWKB = null;
	protected String adminPolygonOsmIdlist = null;
	protected List<Integer> adminLevels = new ArrayList<>();
	protected boolean subareasidentifyable;
	protected String languagecode = "";	// force search/evaluate for names with country prefixes on osm key name:xx

	private static List<Municipality> searchResults = new ArrayList<>();
	private static int searchResultindex = 0;
	private static String defaultLanguagecode = "";
	
	public Municipality() {
		
	}
	
	public Municipality(String countrycode, String municipalityName, String officialRef) {
		this.countrycode = countrycode;
		this.name = municipalityName;
		this.officialRef = officialRef;
	}
	
	public Municipality(Municipality muni) {
		this(muni.countrycode, muni.name, muni.officialRef);
		setCountryDBId(muni.countryDBId);
		setMunicipalityDBId(muni.municipalityDBId);
		this.officialKey = muni.officialKey;
		this.adminPolygonWKB = muni.adminPolygonWKB;
		this.adminPolygonOsmIdlist = muni.adminPolygonOsmIdlist;
		this.adminLevels = muni.adminLevels;
		this.subareasidentifyable = muni.subareasidentifyable;
		this.languagecode = muni.languagecode;
	}

	public static void connectDB(Connection housenumberConn) {
		Municipality.housenumberConn = housenumberConn;
	}

	/**
	 * search municipalities in DB and store it internaly. After that, you can get one item per time with calls of next()
	 * @param country 
	 * @param municipalityName Name of municipality. Wildcard * allowed, to get more than one or even all municipality in country
	 * @param officialRef official reference of the municipality. Examine OSM key for official ref in country via Country.getOfficialReferenceOsmKey()
	 * @param adminHierachyTextform filter municipalities via admin hierarchy in textform. For example "Bundesrepublik Deutschland,Bayern".
	 * Wildcard * allowed
	 * @return number of found municipalities in DB
	 * @throws Exception 
	 */
	public static int search(String municipalityCountry, String municipalityName, String officialRef, String adminHierachyTextform) throws Exception {
		Country country = new Country();
		country.connectDB(housenumberConn);
		if(country.getCountryShortname(municipalityCountry).equals(""))
			throw new Exception("invalid or missing Country");
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
			adminHierachyTextform = adminHierachyTextform.replace("*",  "%*");
		}

			// initialize array for results (could be already set by previous search
		searchResults.clear();
		searchResultindex = 0;
		
		String searchMunicipalitiesSql = "";
		searchMunicipalitiesSql = "SELECT m.id AS muniid, stadt AS name, " +
			"officialkeys_id AS officialref, gemeindeschluessel_key AS officialkey, land AS country, " +
			"countrycode, c.id AS countryid, subareasidentifyable, active_adminlevels " +
			"FROM stadt AS m JOIN land AS c ON m.land_id = c.id " +
			"WHERE c.land = ? " +
			"AND m.stadt like ? " +
			"AND officialkeys_id like ? " +
			"AND osm_hierarchy like ?;";

		PreparedStatement searchMunicipalitiesStmt = housenumberConn.prepareStatement(searchMunicipalitiesSql);
		int stmtindex = 1;
		searchMunicipalitiesStmt.setString(stmtindex++, municipalityCountry);
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

	/**
	 * get next municipality (previously get via readfromDB(..)
	 */
	public static Municipality next() {
		Municipality municipality = null;
		
		if(searchResults.size() > searchResultindex) {
			municipality = (Municipality) searchResults.get(searchResultindex);
			searchResultindex++;
		}
		return municipality;
	}
	
	/**
	 * examine available administrative Levels from osm DB for the municipality.
	 * @return
	 */
//	private int[] getAdministrationLevels() {
//		
//	}

	public String getAdminPolygonWKB() {
		return this.adminPolygonWKB;
	}

	public String getAdminPolygonOsmIdlist() {
		return this.adminPolygonOsmIdlist;
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

	public void setCountryDBId(long countryid) {
		this.countryDBId = countryid;
	}

	public void setMunicipalityDBId(long municipalityid) {
		this.municipalityDBId = municipalityid;
	}
		
	
	/**
	 * set the 2letter ISO code for Country, if you want to force searches and evaluations of objects with key name:xx
	 * for example in Countries, where 2 languages are spoken, like Belgium or in Italy Area of South-Tirol
	 * @param languagecode
	 */
	public void setLanguageCode(String languagecode) {
		Municipality.defaultLanguagecode = languagecode;
	}
	
	/**
	 * define OSM administration boundary level(s), which should be used go generate housenumber evaluations for this municipality
	 * 	 (OSM Key 'admin_level')
	 * @param levelArray
	 */
//	private void setactiveAdministrativeLevel(int[] levelArray) {
//		
//	}

	@Override
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

	@Override
	public int hashCode() {
		int hashCode = 13;
		int multi = 31;
		hashCode += countrycode.hashCode();
		hashCode *= multi + name.hashCode();
		hashCode *= multi + officialRef.hashCode();

		return hashCode;
	}

	@Override
    public int compareTo(Object obj) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if(this == null) return AFTER;
		if(obj == null) return AFTER;

		if(! (obj instanceof Municipality))
			return BEFORE;
		Municipality other = (Municipality) obj;
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
