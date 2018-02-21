package de.regioosm.housenumbercore.util;

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
	private String	country = "";
	private String	municipality = "";
	private String	municipalityId = "";
	private String	subArea = "";
	private String	subId = "";
	private String	street = "";
	private String	place = "";					// in cases, the housenumber belongs not to a streets, but instead to a place, like in hamlets and very small villages
	private String	housenumber = "";
	private String	housenumberaddition = "";
	private Double	lon = 999D;
	private Double	lat = 999D;
	private String	lonlat_source = "";
	private String	lonlat_srid = "";
	private String	dataid = "";
	private String	postcode = "";
	private TreeMap<String,String> keyvalues = new TreeMap<String,String>();

	static Integer nodeid = 0;
    
    public Address()  {
		setCountry("");
		setMunicipality("");
		setMunicipalityId("");
		setSubArea("");
		setSubId("");
		setStreet("");
		setPlace("");
		setHousenumber("");
		setHousenumberaddition("");
		setLocation(999D, 999D);
		setLonlat_source("");
		setLonlat_srid("");
	    setDataid("");
	    setPostcode("");
	    keyvalues.clear();
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
    	if(! getHousenumberaddition().equals(""))
    		output += "<tag k='temp_addr:housenumberaddition' v='" + getHousenumberaddition().replace("'", "&quot;") + "' />\n";
    	if(! getPostcode().equals(""))
    		output += "<tag k='addr:postcode' v='" + getPostcode().replace("'", "&quot;") + "' />\n";
    	if(! getMunicipality().equals(""))
    		output += "<tag k='addr:city' v='" + getMunicipality().replace("'", "&quot;") + "' />\n";
    	if(! getMunicipality().equals("") && ! getMunicipalityId().equals(""))
    		output += "<tag k='temp_cityid' v='" + getMunicipalityId().replace("'", "&quot;") + "' />\n";
    	if(! getSubArea().equals(""))
    		output += "<tag k='temp_subarea' v='" + getSubArea().replace("'", "&quot;") + "' />\n";
    	if(! getSubArea().equals("") && ! getSubId().equals(""))
    		output += "<tag k='temp_subid' v='" + getSubId().replace("'", "&quot;") + "' />\n";
    	
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

    	output += getDataid() + "\t" + "-1" + "\t";
    	if(!getStreet().equals(""))
    		output += getStreet();
    	else if(!getPlace().equals(""))
    		output += getPlace();
    	output += "\t" + getHousenumber() + "\t" +	getHousenumberaddition() + "\t" + getPostcode() + "\t" + getMunicipalityId() + "\t" + getMunicipality()
    		+ "\t" + getSubArea() + "\t" + getSubId() + "\t" + "EPSG:" + getLonlat_srid() + "\t" + getLon() + "\t" + lat + "\n";
        return output;
    }

	/**
	 * @return the land
	 */
	public String getCountry() {
		return country;
	}

	/**
	 * @return the municipality
	 */
	public String getMunicipality() {
		return municipality;
	}

	/**
	 * @return the municipalityId
	 */
	public String getMunicipalityId() {
		return municipalityId;
	}

	/**
	 * @return the municipality
	 */
	public String getSubArea() {
		return subArea;
	}

	/**
	 * @return the municipalityId
	 */
	public String getSubId() {
		return subId;
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
	 * @return the housenumberaddition
	 */
	public String getHousenumberaddition() {
		return housenumberaddition;
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
	 * @return the lonlat_source
	 */
	public String getLonlat_source() {
		return lonlat_source;
	}

	/**
	 * @return the lonlat_srid
	 */
	public String getLonlat_srid() {
		return lonlat_srid;
	}

	/**
	 * @return the dataid
	 */
	public String getDataid() {
		return dataid;
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
	 */
	public void setCountry(String country) {
		this.country = country;
	}

	/**
	 * @param municipality the municipality to set
	 */
	public void setMunicipality(String municipality) {
		this.municipality = municipality;
	}

	/**
	 * @param municipalityId the municipalityId to set
	 */
	public void setMunicipalityId(String municipalityId) {
		this.municipalityId = municipalityId;
	}

	/**
	 * @return the municipality
	 */
	public void setSubArea(String subarea) {
		this.subArea = subarea;
	}

	/**
	 * @return the municipalityId
	 */
	public void setSubId(String subid) {
		this.subId = subid;
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
	 * @param housenumberaddtion the housenumberaddition to set
	 */
	public void setHousenumberaddition(String housenumberaddition) {
		this.housenumberaddition = housenumberaddition;
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
	 * @param lonlat_source the lonlat_source to set
	 */
	public void setLonlat_source(String lonlat_source) {
		this.lonlat_source = lonlat_source;
	}

	/**
	 * @param lonlat_srid the lonlat_srid to set
	 */
	public void setLonlat_srid(String lonlat_srid) {
		this.lonlat_srid = lonlat_srid;
	}

	/**
	 * @param dataid the dataid to set
	 */
	public void setDataid(String dataid) {
		this.dataid = dataid;
	}

	/**
	 * @param postcode the postcode to set
	 */
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	/**
	 * @param key the key part of key-value pair to set
	 * @param value the value part of key-value pair to set
	 */
	public void addKeyvalue(String key, String value) {
		this.keyvalues.put(key,value);
	}
}

