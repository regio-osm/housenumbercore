package de.regioosm.housenumbercore.util;

import java.nio.charset.Charset;
import java.text.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/* origin class was StreetObject.javva from package de.diesei.listofstreets, as of 2018-02-16 */

public class Street implements Comparable {


	private Municipality municipality = null;
	private String name = null;
	private long streetDBId = 0L;

	public Street(Municipality municipality, String name) {
		this.municipality = municipality;
		this.name = name;
	}

	public Street(Municipality municipality, String name, long streetdbid) {
		this(municipality, name);
		this.streetDBId = streetdbid;
	}
	
	public Municipality getMunicipality() {
		return municipality;
	}
	
	public String getName() {
		return name;
	}
	
	public long getStreetDBId() {
		return streetDBId;
	}

	public void setName(String streetName) {
		this.name = streetName;
	}
	
	@Override
	public boolean equals(Object obj) {
		boolean result = false;
		if(! (obj instanceof Street))
			return result;
		Street other = (Street) obj;
		if(	this.getMunicipality().equals(other.getMunicipality()) &&
			(this.getName().equals(other.getName()))) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		int hashCode = 13;
		int multi = 31;
		hashCode += municipality.hashCode();
		hashCode *= multi + name.hashCode();

		return hashCode;
	}

    public int compareTo(Object obj) {
		final int BEFORE = -1;
		final int EQUAL = 0;
		final int AFTER = 1;

		if(this == null) return AFTER;
		if(obj == null) return AFTER;

		if(! (obj instanceof Street))
			return BEFORE;
		Street other = (Street) obj;
		if(this == other) return EQUAL;

		int comparison = this.getMunicipality().compareTo(other.getMunicipality());
		if (comparison != EQUAL) return comparison;

		comparison = this.name.compareTo(other.name);
		if (comparison != EQUAL)
			return comparison;

		return EQUAL;
	}	
}