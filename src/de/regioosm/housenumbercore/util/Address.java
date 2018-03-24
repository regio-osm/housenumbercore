package de.regioosm.housenumbercore.util;

import java.sql.Connection;
import java.util.Map;
import java.util.TreeMap;


/*

	V1.0, 08.08.2013, Dietmar Seifert
		* works as a cache between table auswertung_hausnummern during the workflow phase auswertung_offline.java,
		* meaning 
		* - at program start (of a job) the existing content of table auswertung_hausnummern will be read into this cache
		* - in the table auswertung_hausnummern the rows will NOT be deleted furthermore
		* - the changes or unchanged calculation will be stored in this cache
		* - at the end, this cache will be written back to the table auswertung_hausnummern in this way
		* 	- untouched cache entries must be deleted in table, because they doesn't exists anymore
		* 	- unchanged cache entries doesn't cause any work
		* 	- changed cache entries will be written to the table back 
		* This cache has two goals
		* - time for program auswertung_offline.java should be dramatically lower, because most of time is insert of rows
		* - the auswertung for the actual job is not available during calculation, thats bad for end users
*/

	

public class Address {
	protected static Connection housenumberConn = null;

	/**
	 * fixed length of a sortable housenumber, for correct sorting of a housenumber. Should be set to at least 4.
	 */
	protected static final int HAUSNUMMERSORTIERBARLENGTH = 4;
	
	public static final double lonUnset = 999.0D;
	public static final double latUnset = 999.0D;

	
	protected String	countrycode = null;
	protected String	country = null;
	protected String	municipality = null;
	protected String	subArea = null;
	protected String	subareaId = null;
	protected String	street = null;
	protected String	place = null;					// in cases, the housenumber belongs not to a streets, but instead to a place, like in hamlets and very small villages
	protected String	housenumber = null;
	protected Double	lon = lonUnset;
	protected Double	lat = latUnset;
	protected String	sourcesrid = null;
	protected String	coordinatesourcetext = null;
	protected String	postcode = null;
	protected TreeMap<String,String> keyvalues = new TreeMap<String,String>();

	static Integer nodeid = 0;
    
    public Address()  {
	}
    
    public Address(String country, String postcode, String municipality, 
    	String subarea, String street, String housenumber) throws Exception {
    	setCountry(country);
    	this.postcode = postcode;
    	this.municipality = municipality;
    	this.subArea = subarea;
    	this.street = street;
    	this.housenumber = housenumber;
    }

	public static void connectDB(Connection housenumberConn) {
		Address.housenumberConn = housenumberConn;
	}
    
    public String printosm() {
    	String output = "";
    	nodeid--;
    	output += "<node id = '" + nodeid + "' lat='" + lat + "' lon='" + getLon() + "'>\n";
    	if(! getStreet().equals(""))
    		output += "<tag k='addr:street' v='" + getStreet().replace("'", "&quot;") + "' />\n";
    	if(! getPlace().equals(""))
    		output += "<tag k='addr:place' v='" + getPlace().replace("'", "&quot;") + "' />\n";
    	if(! getHousenumber().equals(""))
    		output += "<tag k='addr:housenumber' v='" + getHousenumber().replace("'", "&quot;") + "' />\n";
    	if(! getPostcode().equals(""))
    		output += "<tag k='addr:postcode' v='" + getPostcode().replace("'", "&quot;") + "' />\n";
    	if(! getMunicipality().equals(""))
    		output += "<tag k='addr:city' v='" + getMunicipality().replace("'", "&quot;") + "' />\n";
    	if(! getSubArea().equals(""))
    		output += "<tag k='temp_subarea' v='" + getSubArea().replace("'", "&quot;") + "' />\n";
    	if(! getSubArea().equals("") && ! getSubareaId().equals(""))
    		output += "<tag k='temp_subareaid' v='" + getSubareaId().replace("'", "&quot;") + "' />\n";
    	
    	for (Map.Entry<String,String> entry : keyvalues.entrySet()) {
			String key = entry.getKey();
			String value = entry.getValue();
			if((! value.equals("")) && ! (value.equals("N/A")))
				output += "<tag k='" + key.replace("'", "&quot;") + "' v='" + value.replace("'", "&quot;") + "' />\n";
		}
    	output += "</node>\n";
    	
        return output;
    }

    public String printtxt() {
    	String output = "";

    	output += "-1" + "\t";
    	if(!getStreet().equals(""))
    		output += getStreet();
    	else if(!getPlace().equals(""))
    		output += getPlace();
    	output += "\t" + getHousenumber() + "\t" + getPostcode() + "\t" + getMunicipality() +
    		"\t" + getSubArea() + "\t" + getSubareaId() + "\t" + "EPSG:" + getSourceSrid() + 
    		"\t" + getLon() + "\t" + getLat() + "\n";
        return output;
    }

	/**
	 * @return the land
	 */
	public String getCountry() {
		return this.country;
	}

	public String getCountrycode() {
		return this.countrycode;
	}
	
	/**
	 * @return the municipality
	 */
	public String getMunicipality() {
		return municipality;
	}

	/**
	 * @return the municipality
	 */
	public String getSubArea() {
		return subArea;
	}

	/**
	 * @return the subarea ID
	 */
	public String getSubareaId() {
		return subareaId;
	}

	/**
	 * @return the street
	 */
	public String getStreet() {
		return street;
	}

	/**
	 * @return the place
	 */
	public String getPlace() {
		return place;
	}

	/**
	 * @return the housenumber
	 */
	public String getHousenumber() {
		return housenumber;
	}


	/**
	 * @return the lon
	 */
	public Double getLon() {
		return lon;
	}

	/**
	 * @return the lat
	 */
	public Double getLat() {
		return lat;
	}

	/**
	 * @return the source SRID (postgresql coordinate system)
	 */
	public String getSourceSrid() {
		return sourcesrid;
	}

	/**
	 * @return the lonlat_source
	 */
	public String getCoordinatesSourceText() {
		return coordinatesourcetext;
	}

	/**
	 * @return the postcode
	 */
	public String getPostcode() {
		return postcode;
	}
	
	/**
	 * @return the value with given key or null, if key is not available
	 */
	public String getKeyvalue(String key) {
		return this.keyvalues.get(key);
	}



	/**
	 * @param country the country to set
	 * @throws Exception 
	 */
	public void setCountry(String country) throws Exception {
		if((country == null) || country.equals("")) {
			this.countrycode = null;
			this.country = null;
			return;
		}
		
		Country.connectDB(housenumberConn);
		if((country != null) && (country.length() == 2)) {
			this.countrycode = country;
			this.country = Country.getCountryLongname(country);
		} else {
			this.country = country;
			this.countrycode = Country.getCountryShortname(country);
		}
	}

	public void setCountrycode(String countrycode) throws Exception {
		if((countrycode == null) || countrycode.equals("")) {
			this.countrycode = null;
			this.country = null;
			return;
		}

		Country.connectDB(housenumberConn);
		if(countrycode.length() == 2) {
			this.countrycode = countrycode;
			this.country = Country.getCountryLongname(countrycode);
		} else {
			this.country = countrycode;
			this.countrycode = Country.getCountryShortname(countrycode);
		}
	}
	
	public void setLat(Double latitude) {
		this.lat = latitude;
	}

	public void setLon(Double longitude) {
		this.lon = longitude;
	}

	public void setLonLat(Double longitude, Double latitude) {
		setLon(longitude);
		setLat(latitude);
	}
	/**
	 * @param municipality the municipality to set
	 */
	public void setMunicipality(String municipality) {
		this.municipality = municipality;
	}

	/**
	 * set sub area name
	 */
	public void setSubArea(String subarea) {
		this.subArea = subarea;
	}

	/**
	 * set sub area ID
	 */
	public void setSubareaId(String subareaid) {
		this.subareaId = subareaid;
	}

	/**
	 * @param street the street to set
	 */
	public void setStreet(String street) {
		this.street = street;
	}

	/**
	 * @param housenumber the housenumber to set
	 */
	public void setHousenumber(String housenumber) {
		this.housenumber = housenumber;
	}

	/**
	 * @param place the place to set
	 */
	public void setPlace(String place) {
		this.place = place;
	}
	/**
	 * @param lon the lon to set
	 * @param lat the lat to set
	 */
	public void setLocation(Double lon, Double lat) {
		this.lon = lon;
		this.lat = lat;
	}

	/**
	 * @param sourcesrid the lonlat_source to set
	 */
	public void setSourceSrid(String sourcesrid) {
		this.sourcesrid = sourcesrid;
	}
//TODO should it be tested here to DB for valid value?

	/**
	 * @param 
	 */
	public void setCoordinatesSourceText(String coordinatessourcetext) {
		this.coordinatesourcetext = coordinatessourcetext;
	}
	
	/**
	 * @param postcode the postcode to set
	 */
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	public String getHousenumberSortable() {
		String result = "";
			// Hack: hier werden die Hausnummern normiert und damit über
			// normale Select order by sortierbar from Hausnummern
			// 4stellig mit führenden Nullen 1=0001  47 1/2=0047 1/2  11 1/128b = 0011 1/128b
		int numstellen = 0;
		if((housenumber != null) && !housenumber.equals("")) {
			for (int posi = 0; posi < housenumber.length(); posi++) {
				int charwert = housenumber.charAt(posi);
				if ((charwert >= '0') && (charwert <= '9')) {
					numstellen++;
				} else {
					break;
				}
			}
		}
		for (int anzi = 0; anzi < (HAUSNUMMERSORTIERBARLENGTH - numstellen); anzi++) {
			result += "0";
		}
		result += housenumber;
		
		return result;
	}
		
	/**
	 * @param key the key part of key-value pair to set
	 * @param value the value part of key-value pair to set
	 */
	public void addKeyvalue(String key, String value) {
		this.keyvalues.put(key,value);
	}

	@Override
	public String toString() {
		String output = "";

		output += countrycode + " " + postcode + " " + municipality;
		if((subArea != null) && !subArea.equals("")) {
			output += " - " + subArea;
			if((subareaId != null) && !subareaId.equals(""))
				output += " (Id: " + subareaId + ")";
		}
		if((street != null) && !street.equals(""))
			output += " " + street + " " + housenumber;
		else
			output += " " + place + " " + housenumber;
		output += " lon= " + lon + " lat=" + lat;
		if((sourcesrid != null) && !sourcesrid.equals(""))
			output += " (EPSG: " + sourcesrid + ")";
		return output;

		// possible further elements to output
		//coordinatesourcetext
		//keyvalues
	}
}

