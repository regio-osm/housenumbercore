package de.regioosm.housenumbercore.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Country {
	private static Connection housenumberConn = null;
	private static Map<String, String> countrylist = new HashMap<>();

	public static void connectDB(Connection housenumberConn) {
		Country.housenumberConn = housenumberConn;
	}

	public static String getCountryShortname(String country) throws Exception {
		String result = "";

		if((countrylist != null) && (countrylist.size() > 0)) {
			if(countrylist.containsKey(country)) {
				return countrylist.get(country);
			}
		}
		
		if(housenumberConn == null) {
			throw new Exception("Class Country can only be used, when first call method connectDB");
		}
		try {

			String getCountrySql = "";
			getCountrySql = "SELECT countrycode " +
				"FROM land " +
				"WHERE land.land = ?;";

			PreparedStatement getCountryStmt = housenumberConn.prepareStatement(getCountrySql);
			getCountryStmt.setString(1, country);
			System.out.println("sql query for country ===" + getCountryStmt.toString() + "===");
			ResultSet getCountryRs = getCountryStmt.executeQuery();
				// get only first hit
			if( getCountryRs.next() ) {
				result = getCountryRs.getString("countrycode");
				countrylist.put(country, getCountryRs.getString("countrycode"));
			}
			getCountryRs.close();
			getCountryStmt.close();

			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static String getCountryLongname(String countrycode) throws Exception {

		if((countrylist != null) && (countrylist.size() > 0)) {
			if(countrylist.containsValue(countrycode)) {
				for(Map.Entry<String, String> countryitem: countrylist.entrySet()) {
					if(countryitem.getValue().equals(countrycode))
						return countryitem.getKey();
				}
			}
		}

		if(housenumberConn == null) {
			throw new Exception("please first call .connectDB to further use Country Class");
		}
		try {

			String getCountrySql = "";
			getCountrySql = "SELECT land " +
				"FROM land " +
				"WHERE countrycode = ?;";

			PreparedStatement getCountryStmt = housenumberConn.prepareStatement(getCountrySql);
			getCountryStmt.setString(1, countrycode);
			System.out.println("sql query for country ===" + getCountryStmt.toString() + "===");
			ResultSet getCountryRs = getCountryStmt.executeQuery();
			String result = "";
				// get only first hit
			if( getCountryRs.next() ) {
				result = getCountryRs.getString("land");
				countrylist.put(getCountryRs.getString("land"), countrycode);
			}
			getCountryRs.close();
			getCountryStmt.close();

			return result;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
}
