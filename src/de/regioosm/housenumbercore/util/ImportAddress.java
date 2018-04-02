package de.regioosm.housenumbercore.util;

import de.regioosm.housenumbercore.util.Address;

public class ImportAddress extends Address {
	private final String EPSG_WGS84 = "4326";

	private String municipalityRef = null;
	private String municipalityId= null;
	private String adminHierarchy = null;
	protected String note = null;
	private Long streetDBId = 0L;

	
	public ImportAddress() {
		super();
	}

	public ImportAddress(String street, long streetdbid, String subarea, 
		String postcode, String housenumber, String note) throws Exception {
		super(null, postcode, null, subarea, street, housenumber);
		this.streetDBId = streetdbid;
		this.note = note;
	}

	public ImportAddress(String street, long streetdbid, String subarea, 
		String postcode, String housenumber, String note, 
		double longitude, double latitude, String sourcesrid, String sourcetext) throws Exception {
		this(street, streetdbid, subarea, postcode, housenumber, note);
		this.setLonLat(longitude, latitude);
		this.setSourceSrid(sourcesrid);
		if(	(longitude != lonUnset) && (latitude != latUnset) && 
			((sourcesrid == null) || sourcesrid.equals("")))
			this.setSourceSrid(EPSG_WGS84);
		this.setCoordinatesSourceText(sourcetext);
	}

	public String getAdminHierarchy() {
		return this.adminHierarchy;
	}

	/**
	 * @return the coordinatesourcetext
	 */
	@Override
	public String getCoordinatesSourceText() {
		return coordinatesourcetext;
	}

	/**
	 * @return the municipalityRef
	 */
	public String getMunicipalityRef() {
		return this.municipalityRef;
	}

	/**
	 * get a technical Id for municipality, when it's name is only indirectly avaiable via extra ref muni Id -> name 
	 * In this case, set extra ref in object importparameter
	 * @param municipalityRef the municipalityRef to set
	 */
	public String getMunicipalityId() {
		return this.municipalityId;
	}

	/**
	 * get the name for municipality, given only the Id of the municipality. 
	 * It uses extra ref in object importparameter, if set
	 * @param municipalityRef the municipalityRef to set
	 */
	public String getMunicipalityById(CsvImportparameter importparameter) {
		return importparameter.getMunicipalityIDListEntry(this.municipalityId);
	}

	public String getNote() {
		return this.note;
	}

	/**
	 * @return the sourcesrid
	 */
	@Override
	public String getSourceSrid() {
		return this.sourcesrid;
	}

	public Long getStreetDBId() {
		return this.streetDBId;
	}
	
	public void setAdminHierarchy(String adminhierarchy) {
		this.adminHierarchy = adminhierarchy;
	}
	
	public void setNote(String note) {
		this.note = note;
	}

	public void setStreetDBId(long streetdbid) {
		this.streetDBId = streetdbid;
	}

	/**
	 * @param municipalityRef the municipalityRef to set
	 */
	public void setMunicipalityRef(String municipalityRef) {
		this.municipalityRef = municipalityRef;
	}

	/**
	 * set a technical Id for municipality, when it's name is only indirectly avaiable via extra ref muni Id -> name 
	 * In this case, set extra ref in object importparameter
	 * @param municipalityRef the municipalityRef to set
	 */
	public void setMunicipalityId(String municipalityid) {
		this.municipalityId = municipalityid;
	}

	/**
	 * @param sourcesrid the sourcesrid to set
	 */
	@Override
	public void setSourceSrid(String sourcesrid) {
		this.sourcesrid = sourcesrid;
	}

	/**
	 * @param coordinatesourcetext the coordinatesourcetext to set
	 */
	@Override
	public void setCoordinatesSourceText(String coordinatesourcetext) {
		this.coordinatesourcetext = coordinatesourcetext;
	}
	
	@Override
	public String toString() {
		String output = super.toString();
		
		if ((municipalityRef != null) && !municipalityRef.equals(""))
			output += " muni-ref: " + municipalityRef;
		if ((adminHierarchy != null) && !adminHierarchy.equals(""))
			output += " AdminHierarchy: " + adminHierarchy;
		if ((note != null) && !note.equals(""))
			output += " Note: " + note;
		if (streetDBId != 0L)
			output += " Street DB-Id: " + streetDBId;
		
		return output;
	}
}
