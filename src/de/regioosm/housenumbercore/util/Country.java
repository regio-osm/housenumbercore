package de.regioosm.housenumbercore.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Country {
	private static Connection housenumberConn = null;

	public static void connectDB(Connection housenumberConn) {
		Country.housenumberConn = housenumberConn;
	}

	public static String getCountryShortname(String country) {
		if(housenumberConn == null) {
			return "";
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
			String result = "";
				// get only first hit
			if( getCountryRs.next() ) {
				result = getCountryRs.getString("countrycode");
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
