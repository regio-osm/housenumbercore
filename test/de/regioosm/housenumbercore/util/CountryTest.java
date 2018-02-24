package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CountryTest {
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	private static Connection housenumberConn = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		try {
			String urlHausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);
		}
		catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		housenumberConn.close();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void getCountryShortnameTest() {
		Country.connectDB(housenumberConn);
		try {
			assertEquals("DE", Country.getCountryShortname("Bundesrepublik Deutschland"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getCountryLongnameTest() {
		Country.connectDB(housenumberConn);
		try {
			assertEquals("Bundesrepublik Deutschland", Country.getCountryLongname("DE"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
