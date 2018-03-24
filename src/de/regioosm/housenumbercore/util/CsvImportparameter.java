package de.regioosm.housenumbercore.util;

import java.nio.charset.Charset;

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
import java.util.Map;

import de.regioosm.housenumbercore.util.Applicationconfiguration;


public class CsvImportparameter {
	public enum HEADERFIELD {municipality, municipalityid, municipalityref, postcode, 
		subarea, subareaid, street, streetid, housenumber, housenumberaddition, 
		housenumberaddition2, note, sourcesrid, lon, lat, ignore};

	/**
	 * signal not set of the id of a country.
	 */
	private static final long COUNTRYIDUNSET = -1L;

	private String countrycode = "";
	
	/**
	 * SRID Id of coordinates System
	 */
	protected String coordinatesSourceSrid = "";

	/**	 use two or more identical housenumbers, if they have different geocoordinates.
	 * <br>Works only, if official geocoordinates are available
	 */
	@Deprecated
	private boolean latlonforUniqueness = false;

	 /**
	 * filename of official housenumber list to import
	 */
	private String filename = "";

	/**
	 * File format of import filename, 
	 * @see https://docs.oracle.com/javase/7/docs/api/java/nio/charset/StandardCharsets.html
	 */
	private Charset filenameCharsetname = null;

	private String fieldseparator = "";

	private String housenumberadditionseparator = "";
	private String housenumberadditionseparator2 = "-";

	/**
	 * Should the sub areas of a municipality are evaluated?
	 * 
	 */
	private boolean subareaActive = false;
	
	/**
	 * collect all housenumbers, as found in input file. Later, the housenumbers will be stored from this structure to DB. 
	 */
	private Map<HEADERFIELD, Integer> headerfields = new HashMap<>();
	
	/**
	 * List of municipality ids, when import file contains only references to municipalities
	 * 
	 */
	private Map<String, String> municipalityIDList = new HashMap<>();
	/**
	 * List of subarea municipality ids, when import file contains only references to subareas
	 */
	private Map<String, String> subareaMunicipalityIDList = new HashMap<>();
	/**
	 * List of street ids, when import file contains only references to streets, not the names
	 */
	private Map<String, String> streetIDList = new HashMap<>();


		// storage of streetnames and their internal DB id. If a street is missing in DB, it will be inserted,
		// before the insert of the housenumbers at the streets will be inserted
	static HashMap<String, Integer> street_idlist = new HashMap<String, Integer>();
	
	/**
	 * DB connection to Hausnummern
	 */
	private static Connection housenumberConn = null;
	
	
	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();

	/**
	 * @return the countrycode
	 */
	public String getCountrycode() {
		return countrycode;
	}

	/**
	 * @return the column for field, in Range 0 until number of fields minus 1
	 * -1 will be returned, if field name wasn't found
	 */
	public int getHeaderfieldColumn(HEADERFIELD municipality) {
		if((municipality == null) || municipality.equals(""))
			return -1;

		if(headerfields.containsKey(municipality))
			return headerfields.get(municipality);

		return -1;
	}

	public String getFieldSeparator() {
		return fieldseparator;
	}

	public String getHousenumberFieldseparator() {
		return this.housenumberadditionseparator;
	}

	public String getHousenumberFieldseparator2() {
		return this.housenumberadditionseparator2;
	}

	public String getImportfile() {
		return this.filename;
	}

	public Charset getImportfileFormat() {
		return this.filenameCharsetname;
	}
	
	/**
	 * @return the municipalityIDList
	 */
	public String getMunicipalityIDListEntry(String key) {
		return this.municipalityIDList.get(key);
	}

	public String getSourceCoordinateSystem() {
		return this.coordinatesSourceSrid;
	}

	/**
	 * @return the streetIDList
	 */
	public String getStreetIDListEntry(String key) {
		return this.streetIDList.get(key);
	}

	/**
	 * @return the subareaMunicipalityIDList
	 */
	public String getSubareaMunicipalityIDListEntry(String key) {
		return this.subareaMunicipalityIDList.get(key);
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

	public void setFieldSeparator(String separator) {
		this.fieldseparator = separator;
	}

	/**
	 * store information, what field "column" contains
	 * @param headerfields the headerfields to set
	 */
	public void setHeaderfield(HEADERFIELD field, int column) {
		this.headerfields.put(field, column);
	}
	
	
	/**
	 * @param municipalityIDList the municipalityIDList to set
	 */
	public void setMunicipalityIDList(Map<String, String> municipalityIDList) {
		this.municipalityIDList = municipalityIDList;
	}

	/**
	 * @param streetIDList the streetIDList to set
	 */
	public void setStreetIDList(Map<String, String> streetIDList) {
		this.streetIDList = streetIDList;
	}

	/**
	 * @param subareaActive the subareaActive to set
	 */
	public void setSubareaActive(boolean subareaActive) {
		this.subareaActive = subareaActive;
	}
	
	/**
	 * @param subareaMunicipalityIDList the subareaMunicipalityIDList to set
	 */
	public void setSubareaMunicipalityIDList(
			Map<String, String> subareaMunicipalityIDList) {
		this.subareaMunicipalityIDList = subareaMunicipalityIDList;
	}


	public void setSourceCoordinateSystem(String sourcesrid) {
		this.coordinatesSourceSrid = sourcesrid;
	}

	public void setImportfile(String filename, Charset charset) {
		this.filename = filename;
		this.filenameCharsetname = charset;
	}

	public void setHousenumberFieldseparators(String separator1, String separator2) {
		this.housenumberadditionseparator = separator1;
		this.housenumberadditionseparator2 = separator2;
	}

	public static void connectDB(Connection housenumberdbconn) {
		housenumberConn = housenumberdbconn;
	}
	
}
