package de.regioosm.housenumbercore.util;

import de.regioosm.housenumbercore.util.Address;

public class ImportAddress extends Address {
	private final String EPSG_WGS84 = "4326";

	private String municipalityRef = null;
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
}
