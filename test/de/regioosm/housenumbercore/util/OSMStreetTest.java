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

import de.regioosm.housenumbercore.util.OSMSegment.OSMType;

public class OSMStreetTest {
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	private static Connection housenumberConn = null;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try {
			String urlHausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);

			try {
				Municipality.connectDB(housenumberConn);
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
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
	public void OSMStreetMunicipalityStringTest() {
		Municipality m;
		try {
				// should return null, because yet no instance created
			//assertNull(OSMStreet.isValidNamedHighwaytype("highway", "residential"));

			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße");
			
			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
			assertEquals("Hauptstraße", street.getName());

				// after a instance has been made, highway type should be found
			assertTrue(OSMStreet.isValidNamedHighwaytype("highway", "residential"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void OSMStreetMunicipalityStringOSMTypeLongStringTest() {
		Municipality m;
		try {
				// should return null, because yet no instance created
			//assertNull(OSMStreet.isValidNamedHighwaytype("highway", "residential"));

			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
			assertEquals("Hauptstraße", street.getName());
			assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", street.getGeometryWKT());

				// after a instance has been made, highway type should be found
			assertTrue(OSMStreet.isValidNamedHighwaytype("highway", "residential"));
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void addSegmentOSMSegmentTest() {
		Municipality m;
		OSMStreet.connectDB(housenumberConn);
		try {
				// should return null, because yet no instance created
			//assertNull(OSMStreet.isValidNamedHighwaytype("highway", "residential"));

			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
			assertEquals("Hauptstraße", street.getName());
			assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", street.getGeometryWKT());

			OSMSegment osmsegment = new OSMSegment(OSMType.way, 4712L, 
			"LINESTRING(10.1 40.1,10.0 40.2)");
			street.addSegment(osmsegment);
			System.out.println("danach geom ===" + street.getGeometryWKT() + "===");
				// caution: postgis skips .0 values, so 10.0 will be represented as 10
			assertEquals("LINESTRING(10 40,10.1 40.1,10 40.2)", street.getGeometryWKT());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void addSegmentOSMTypeLongStringTest() {
		Municipality m;
		OSMStreet.connectDB(housenumberConn);
		try {
				// should return null, because yet no instance created
			//assertNull(OSMStreet.isValidNamedHighwaytype("highway", "residential"));

			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
			assertEquals("Hauptstraße", street.getName());
			assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", street.getGeometryWKT());

			street.addSegment(OSMType.way, 4712L, 
				"LINESTRING(10.1 40.1,10.0 40.2)");
			System.out.println("danach geom ===" + street.getGeometryWKT() + "===");
				// caution: postgis skips .0 values, so 10.0 will be represented as 10
			assertEquals("LINESTRING(10 40,10.1 40.1,10 40.2)", street.getGeometryWKT());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void normalizeNameTest() {
		try {
			Municipality m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Strada Test", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			assertEquals("Strada Test", street.normalizeName());

			Municipality m2 = new Municipality("România", "testCity", "1234567890011");
			OSMStreet street2 = new OSMStreet(m2, "Strada Test", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			assertEquals("Test", street2.normalizeName());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getGeometryWKTTest() {
		Municipality m;
		OSMStreet.connectDB(housenumberConn);
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			assertEquals("LINESTRING(10.0 40.0,10.1 40.1)", street.getGeometryWKT());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getOSMIdsAsStringTest() {
		Municipality m;
		OSMStreet.connectDB(housenumberConn);
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße", OSMType.way, 4711L, 
				"LINESTRING(10.0 40.0,10.1 40.1)");

			street.addSegment(OSMType.way, 4712L, 
				"LINESTRING(10.1 40.1,10.0 40.2)");
System.out.println("osm-ids ===" + street.getOSMIdsAsString() + "===");
			assertEquals("4711,4712", street.getOSMIdsAsString());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void isValidNamedHighwaytypeTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße");

			assertTrue(OSMStreet.isValidNamedHighwaytype("highway", "residential"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void toStringTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			OSMStreet street = new OSMStreet(m, "Hauptstraße");

			assertTrue(street.toString().indexOf("Hauptstraße") != -1);
			assertTrue(street.toString().indexOf("testCity") != -1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
