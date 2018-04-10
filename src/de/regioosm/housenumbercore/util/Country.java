package de.regioosm.housenumbercore.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Country {
	private static Connection housenumberConn = null;
	private static Map<String, String> countrylist = new HashMap<>();

	public static void connectDB() {
		
		if (housenumberConn != null) 
			return;

		Applicationconfiguration configuration = new Applicationconfiguration();

		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");
		}
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		try {
			String url_hausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(url_hausnummern, configuration.db_application_username, configuration.db_application_password);
		}
		catch( SQLException e) {
			e.printStackTrace();
			return;
		}
	}
	
	public static void connectDB(Connection housenumberConn) {
		Country.housenumberConn = housenumberConn;
	}

	public static String getCountryShortname(String country) {
		String result = "";

		if((countrylist != null) && (countrylist.size() > 0)) {
			if(countrylist.containsKey(country)) {
				return countrylist.get(country);
			}
		}
		
		if(housenumberConn == null) {
			connectDB();
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

	public static String getCountryLongname(String countrycode) {

		if((countrylist != null) && (countrylist.size() > 0)) {
			if(countrylist.containsValue(countrycode)) {
				for(Map.Entry<String, String> countryitem: countrylist.entrySet()) {
					if(countryitem.getValue().equals(countrycode))
						return countryitem.getKey();
				}
			}
		}

		if(housenumberConn == null) {
			connectDB();
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
