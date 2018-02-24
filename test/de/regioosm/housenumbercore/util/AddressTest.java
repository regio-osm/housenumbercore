package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AddressTest {
	String testcountry = "Bundesrepublik Deutschland";
	String testmunicipality = "testmuni";
	String testpostcode = "98765";
	String testsubarea = "testsub";
	String teststreet = "alphastreet";
	String testhousenumber = "123";

	Address testaddress = null; 
	static Connection housenumberConn = null;

	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Applicationconfiguration configuration = new Applicationconfiguration();
		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");

			String urlHausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);
		}
		catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		housenumberConn.close();
	}

	@Before
	public void setUp() throws Exception {
		Address.connectDB(housenumberConn);
		testaddress = new Address(testcountry, testpostcode, testmunicipality, 
			testsubarea, teststreet, testhousenumber);
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testAddressStringStringStringStringStringString() {
		try {
			Address localaddress = new Address("Nederland", "4711ab", "Blobberwaard", "Subidu", "Turkeije", "11");
			assertEquals("Nederland", localaddress.getCountry());
			assertEquals("4711ab", localaddress.getPostcode());
			assertEquals("Blobberwaard", localaddress.getMunicipality());
			assertEquals("Subidu", localaddress.getSubArea());
			assertEquals("Turkeije", localaddress.getStreet());
			assertEquals("11", localaddress.getHousenumber());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test
	public void connectDBTest() throws SQLException, ClassNotFoundException {
		Applicationconfiguration configuration = new Applicationconfiguration();
		Connection housenumberConn = null;
		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");

			String urlHausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);
			String simpleQuery = "SELECT count(*) FROM land;";
			Statement stmt = housenumberConn.createStatement();
			ResultSet rs = stmt.executeQuery(simpleQuery);
			if(rs.next())
				assertTrue(true);

			rs.close();
			stmt.close();
			housenumberConn.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
			throw e;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw e;
		}

	}

	@Test
	public void testPrintosm() {
		Address localaddress;
		try {
			localaddress = new Address("Nederland", "4711ab", "Blobberwaard", "Subidü", "Turkeije", "1112");
			String output = localaddress.printosm();
			boolean found = (output.indexOf("k='addr:city'") != -1) &&
							(output.indexOf("v='Blobberwaard'") != -1) &&
							(output.indexOf("k='addr:housenumber'") != -1) &&
							(output.indexOf("v='1112'") != -1);
			assertTrue(found);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void printtxtTest() {
		Address localaddress;
		try {
			localaddress = new Address("Nederland", "4711ab", "Blobberwaard", "Subidü", "Turkeije", "1112");
			String output = localaddress.printtxt();
			boolean found = (output.indexOf("4711ab") != -1) &&
							(output.indexOf("Blobberwaard") != -1) &&
							(output.indexOf("Subidü") != -1) &&
							(output.indexOf("Turkeije") != -1);
			assertTrue(found);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getCountryTest() {
		assertEquals(testcountry, testaddress.getCountry());
	}

	@Test
	public void getCountrycodeTest() {
		Address localaddress = new Address();
		try {
			localaddress.setCountry("Bundesrepublik Deutschland");
			assertEquals("DE", localaddress.getCountrycode());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void getMunicipalityTest() {
		assertEquals(testmunicipality, testaddress.getMunicipality());
	}

	@Test
	public void getSubAreaTest() {
		assertEquals(testsubarea,  testaddress.getSubArea());
	}

	@Test
	public void getSubIdTest() {
		assertNull(testaddress.getSubareaId());
	}

	@Test
	public void getStreetTest() {
		assertEquals(teststreet, testaddress.getStreet());
	}

	@Test
	public void getPlaceTest() {
		String testplace = "placetest";
		testaddress.setPlace(testplace);
		assertEquals(testplace,  testaddress.getPlace());
	}

	@Test
	public void getHousenumberTest() {
		assertEquals(testhousenumber,  testaddress.getHousenumber());
	}

	@Test
	public void getLonTest() {
		assertEquals(Address.lonUnset, testaddress.getLon(), 0.1);
		testaddress.setLon(10.1);
		assertEquals(10.1, testaddress.getLon(), 0.1);
	}

	@Test
	public void getLatTest() {
		assertEquals(Address.latUnset, testaddress.getLat(), 0.1);
		testaddress.setLat(48.8);
		assertEquals(48.8, testaddress.getLat(), 0.1);
	}

	@Test
	public void getCoordinatesSourceTextTest() {
		assertNull(testaddress.getCoordinatesSourceText());
		testaddress.setCoordinatesSourceText("Administration Office XY");
		assertEquals("Administration Office XY", testaddress.getCoordinatesSourceText());
	}

	@Test
	public void getSourceSridTest() {
		assertNull(testaddress.getSourceSrid());
		testaddress.setSourceSrid("4326");
		assertEquals("4326", testaddress.getSourceSrid());
//TODO it should be also checked, if invalid SRID Entries will be rejected
	}

	@Test
	public void getPostcodeTest() {
		assertEquals(testpostcode, testaddress.getPostcode());
	}

	@Test
	public void getKeyvalueTest() {
		assertNull(testaddress.getKeyvalue("dummy"));
		testaddress.addKeyvalue("highway", "footway");
		assertNotEquals("trunk", testaddress.getKeyvalue("highway"));
		assertEquals("footway", testaddress.getKeyvalue("highway"));
	}

	@Test
	public void setCountryTest() {
		Address localaddress = new Address();
		assertNull(localaddress.getCountry());
		try {
			localaddress.setCountry("Bundesrepublik Deutschland");
			assertEquals("Bundesrepublik Deutschland", localaddress.getCountry());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Address secondlocal = new Address();
		assertNull(secondlocal.getCountry());
		try {
			secondlocal.setCountry("DE");
			assertEquals("Bundesrepublik Deutschland", secondlocal.getCountry());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void setCountrycodeTest() {
		Address localaddress = new Address();
		try {
			localaddress.setCountrycode("DE");
			assertEquals("DE", localaddress.getCountrycode());
			assertEquals("Bundesrepublik Deutschland", localaddress.getCountry());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void setLatTest() {
		testaddress.setLat(-33.0);
		assertEquals(-33.0, testaddress.getLat(), 0.1);
	}

	@Test
	public void setLonTest() {
		testaddress.setLon(178.0);
		assertEquals(178.0, testaddress.getLon(), 0.1);
	}

	@Test
	public void setLonLatTest() {
		testaddress.setLonLat(-23.65, -55.8);
		assertEquals(-23.65, testaddress.getLon(), 0.1);
		assertEquals(-55.8, testaddress.getLat(), 0.1);
	}

	@Test
	public void setMunicipalityTest() {
		Address localaddress = new Address();
		localaddress.setMunicipality("helloMuniTest");
		assertEquals("helloMuniTest", localaddress.getMunicipality());
	}

	@Test
	public void setSubAreaTest() {
		Address localaddress = new Address();
		localaddress.setSubArea("hello Testsubarea Name");
		assertEquals("hello Testsubarea Name", localaddress.getSubArea());
	}

	@Test
	public void setSubareaIdTest() {
		Address localaddress = new Address();
		localaddress.setSubareaId("471147124713");
		assertEquals("471147124713", localaddress.getSubareaId());
	}

	@Test
	public void setStreetTest() {
		assertEquals(teststreet, testaddress.getStreet());
		testaddress.setStreet("Dr. Werner-Schiebulski-Straße");
		assertEquals("Dr. Werner-Schiebulski-Straße", testaddress.getStreet());

		Address localaddress = new Address();
		localaddress.setStreet("Fast Runners Rd");
		assertEquals("Fast Runners Rd", localaddress.getStreet());
	}

	@Test
	public void setHousenumberTest() {
		Address localaddress = new Address();
		localaddress.setHousenumber("47a");
		assertEquals("47a", localaddress.getHousenumber());
	}

	@Test
	public void testSetPlace() {
		assertNull(testaddress.getPlace());

		Address localaddress = new Address();
		localaddress.setPlace("Siebenbrunn");
		assertEquals("Siebenbrunn", localaddress.getPlace());
		localaddress.setPlace("Wellenburg");
		assertEquals("Wellenburg", localaddress.getPlace());
		assertNotEquals("wellenreiter", localaddress.getPlace());
	}

	@Test
	public void setLocationTest() {
		Double lon = -128.5;
		Double lat = -48.3;
		Address localaddress = new Address();
		localaddress.setLocation(lon, lat);
		assertEquals(lon, localaddress.getLon(), 0.1);
		assertEquals(lat, localaddress.getLat(), 0.1);
	}

	@Test
	public void setCoordinateSourceTest() {
		Address localaddress = new Address();
		localaddress.setCoordinatesSourceText("FBI, CIA, or something else");
		assertEquals("FBI, CIA, or something else", localaddress.getCoordinatesSourceText());
	}

	@Test
	public void setSourceSridTest() {
		Address localaddress = new Address();
		localaddress.setSourceSrid("4326");
		assertEquals("4326", localaddress.getSourceSrid());
	}

	@Test
	public void setPostcodeTest() {
		assertEquals(testpostcode, testaddress.getPostcode());
		testaddress.setPostcode("007");
		assertEquals("007", testaddress.getPostcode());
		
		Address localaddress = new Address();
		localaddress.setPostcode("123987");
		assertEquals("123987", localaddress.getPostcode());
	}

	@Test
	public void getHousenumberSortableTest() {
			// first, check, if sortable length has change from fixed value 4
		assertEquals(4, Address.HAUSNUMMERSORTIERBARLENGTH);

		Address localaddress = new Address();

		localaddress.setHousenumber("7");
		assertEquals("0007", localaddress.getHousenumberSortable());

		localaddress.setHousenumber("123a");
		assertEquals("0123a", localaddress.getHousenumberSortable());
	}

	@Test
	public void addKeyvalueTest() {
		assertNull(testaddress.getKeyvalue("highway"));
		testaddress.addKeyvalue("highway", "footway");
		assertEquals("footway", testaddress.getKeyvalue("highway"));
	}

}
