package de.regioosm.housenumbercore.util;

public class OSMTag {
	private String key;
	private String value;
	
	public OSMTag(String key, String value) {
		this.key = key;
		this.value = value;
	}
	
	public String getKey() {
		return key;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return "[" + this.key + "] = " + this.value;
	}
}
