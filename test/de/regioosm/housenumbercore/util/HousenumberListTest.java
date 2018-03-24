package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HousenumberListTest {
	private static Connection housenumberConn = null;
	private static Long testmunicipalityDBid = 0L;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
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

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
			// if test municipality has been stored in DB ... 
		if(testmunicipalityDBid != 0L) {
				// delete stored housenumber list of test municipality
			String deleteHousenumbersSql = "DELETE FROM stadt_hausnummern " +
				"WHERE stadt_id = ?;";
			PreparedStatement deleteHousenumbersStmt = 
				housenumberConn.prepareStatement(deleteHousenumbersSql);
			deleteHousenumbersStmt.setLong(1, testmunicipalityDBid);
			System.out.println("delete housenumberlist ===" + 
				deleteHousenumbersStmt.toString() + "===");
			int count = deleteHousenumbersStmt.executeUpdate();

				// delete test municipality
			String deleteMunicipalitySql = "DELETE FROM stadt " +
				"WHERE id = ?;";
			PreparedStatement deleteMunicipalityStmt = 
				housenumberConn.prepareStatement(deleteMunicipalitySql);
			deleteMunicipalityStmt.setLong(1, testmunicipalityDBid);
			System.out.println("delete housenumberlist ===" + 
				deleteMunicipalityStmt.toString() + "===");
			count = deleteMunicipalityStmt.executeUpdate();
			deleteMunicipalityStmt.close();
		}

		housenumberConn.close();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void addHousenumberStringLongStringStringStringStringDoubleDoubleStringStringTest() {
		HousenumberList hnol = new HousenumberList();
		try {
				// test standard exception: don't distingish between identical addresses
			hnol.addHousenumber("Teststreet", 123L, "Subarea", "12345", "98", 
				"noteXX", 23.9, 51.1, "4326", "Coordinate Supporters");
			String key = hnol.getHousenumberKey("Teststreet", "Subarea", "12345", "98");
			ImportAddress checkadr = hnol.getHousenumber(key);
			assertEquals("Teststreet", checkadr.getStreet());
			assertEquals((long) 123L, (long) checkadr.getStreetDBId());
			assertEquals("Subarea", checkadr.getSubArea());
			assertEquals("12345", checkadr.getPostcode());
			assertEquals("98", checkadr.getHousenumber());
			assertEquals("noteXX", checkadr.getNote());
			assertEquals(23.9, checkadr.getLon(), 0.1d);
			assertEquals(51.1, checkadr.getLat(), 0.1d);
			assertEquals("4326", checkadr.getSourceSrid());
			assertEquals("Coordinate Supporters", checkadr.getCoordinatesSourceText());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void addHousenumberImportAddressTest() {
		HousenumberList hnol = new HousenumberList();
		try {
				// test standard exception: don't distingish between identical addresses
			ImportAddress adr = new ImportAddress("Purple Road", 0L, "", "4711", "2459", "");

			hnol.addHousenumber(adr);
			String key = hnol.getHousenumberKey("Purple Road", "", "4711", "2459");
			ImportAddress checkadr = hnol.getHousenumber(key);
			assertEquals("Purple Road", checkadr.getStreet());
			assertEquals((long) 0L, (long) checkadr.getStreetDBId());
			assertEquals("", checkadr.getSubArea());
			assertEquals("4711", checkadr.getPostcode());
			assertEquals("2459", checkadr.getHousenumber());
			assertEquals("", checkadr.getNote());
			assertEquals(ImportAddress.lonUnset, checkadr.getLon(), 0.1d);
			assertEquals(ImportAddress.latUnset, checkadr.getLat(), 0.1d);
			assertEquals("", checkadr.getSourceSrid());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void storeToDBTest() {
		try {
			Municipality.connectDB(housenumberConn);
			Municipality m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			assertFalse(m.exists());

			System.out.println("before store: muni DBid: " + m.getMunicipalityDBId());
			m.store();
			assertTrue(m.exists());
			System.out.println("after store: muni DBid: " + m.getMunicipalityDBId());

			HousenumberList hnol = new HousenumberList();
			hnol.setMunicipality(m);
			
			ImportAddress adr1 = new ImportAddress("Hauptstraße", 0L, "", "12345", "1", "");
			hnol.addHousenumber(adr1);
			ImportAddress adr2 = new ImportAddress("Hauptstraße", 0L, "", "12345", "47", "");
			hnol.addHousenumber(adr2);
			ImportAddress adr3 = new ImportAddress("testStrAßee", 0L, "", "12345", "3 1/158", "");
			hnol.addHousenumber(adr3);

			hnol.storeToDB();

			assertTrue(m.exists());

			testmunicipalityDBid = m.getMunicipalityDBId();

			String selHousenumbersSql = "SELECT countrycode, m.stadt AS municipality, " +
				"sub_id AS subarea, s.strasse AS street, s.id AS streetid, postcode, " +
				"hausnummer AS housenumber, hausnummer_sortierbar AS housenumbersortable, " +
				"st_x(point) AS lon, st_y(point) AS lat FROM " +
				"land AS c JOIN stadt AS m ON c.id = m.land_id " +
				"JOIN stadt_hausnummern AS mh ON mh.stadt_id = m.id " +
				"JOIN strasse as s ON mh.strasse_id = s.id " +
				"WHERE countrycode = ? AND m.stadt = ? " +
				"ORDER BY countrycode, municipality, subarea, street, housenumbersortable;";
			PreparedStatement selHousenumbersStmt = housenumberConn.prepareStatement(selHousenumbersSql);
			selHousenumbersStmt.setString(1, m.getCountrycode());
			selHousenumbersStmt.setString(2, m.getName());
			System.out.println("select housenumberlist ===" + selHousenumbersStmt.toString() + "===");
			ResultSet selHousenumbersRs = selHousenumbersStmt.executeQuery();
			ImportAddress checkadr = null;
			int round = 0;
			while( selHousenumbersRs.next() ) {
				round++;
				if(round == 1)
					checkadr = adr1;
				else if(round == 2)
					checkadr = adr2;
				else 
					checkadr = adr3;
				assertEquals(hnol.getCountrycode(), selHousenumbersRs.getString("countrycode"));
				assertEquals(hnol.getMunicipalityName(), selHousenumbersRs.getString("municipality"));
				assertEquals(checkadr.getSubArea(), selHousenumbersRs.getString("subarea"));
				assertEquals(checkadr.getStreet(), selHousenumbersRs.getString("street"));
				//assertEquals((long) checkadr.getStreetDBId(), selHousenumbersRs.getLong("streetid"));
				assertEquals(checkadr.getPostcode(), selHousenumbersRs.getString("postcode"));
				assertEquals(checkadr.getHousenumber(), selHousenumbersRs.getString("housenumber"));
				assertEquals(checkadr.getHousenumberSortable(), selHousenumbersRs.getString("housenumbersortable"));
				Double lon = selHousenumbersRs.getDouble("lon");
				if(lon == 0.0D)
					lon = ImportAddress.lonUnset;
				assertEquals(checkadr.getLon(), lon, 0.1d);
				Double lat = selHousenumbersRs.getDouble("lat");
				if(lat == 0.0D)
					lat = ImportAddress.latUnset;
				assertEquals(checkadr.getLat(), lat, 0.1d);
			}
			selHousenumbersRs.close();
			selHousenumbersStmt.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test
	public void getCountrycodeTest() {
		HousenumberList hnol = new HousenumberList();
		hnol.setCountrycode("BE");
		assertEquals("BE", hnol.getCountrycode());
	}

	@Test
	public void getHousenumberKeyTest() {
		HousenumberList hnol = new HousenumberList();
		try {
			ImportAddress adr1 = new ImportAddress("Teststreet", 123L, "Subarea", 
				"12345", "98", "", 23.9, 51.1, "4326", "");
				// identical address, only coordinates differ
			ImportAddress adr2 = new ImportAddress("Teststreet", 123L, "Subarea", 
					"12345", "98", "", 23.777, 51.2222, "4326", "");
				// key for both housenumber should be equal
			assertEquals(hnol.getHousenumberKey(adr1), hnol.getHousenumberKey(adr2));
			
				// address without coordinates
			ImportAddress adr3 = new ImportAddress("Main Rd", 123L, "centre", 
					"12345", "98", "");
				// address without coordinates, identical postcode
			ImportAddress adr4 = new ImportAddress("Main Rd", 123L, "centre", 
					"12345", "98", "");
				// key for both housenumber should be equal
			assertEquals(hnol.getHousenumberKey(adr3), hnol.getHousenumberKey(adr4));
			
				// address without coordinates, other postcode
			ImportAddress adr5 = new ImportAddress("Main Rd", 123L, "centre", 
					"99999", "98", "");
				// key for both housenumber should be equal
			assertNotEquals(hnol.getHousenumberKey(adr3), hnol.getHousenumberKey(adr5));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void isOfficialgeocoordinatesTest() {
		HousenumberList hnol = new HousenumberList();
			// check default mode (not official geocoordinates)
		assertEquals(false, hnol.isOfficialgeocoordinates());
	
		hnol.setOfficialgeocoordinates(true);
		assertEquals(true, hnol.isOfficialgeocoordinates());
	
		hnol.setOfficialgeocoordinates(false);
		assertEquals(false, hnol.isOfficialgeocoordinates());
	}

	@Test
	public void isSubareaActiveTest() {
		HousenumberList hnol = new HousenumberList();
		hnol.setSubareaActive(true);
		assertTrue(hnol.isSubareaActive());
		hnol.setSubareaActive(false);
		assertTrue(!hnol.isSubareaActive());
	}

	@Test
	public void setCountrycodeTest() {
		HousenumberList hnol = new HousenumberList();
		hnol.setCountrycode("BE");
		assertEquals("BE", hnol.getCountrycode());
	}

	@Test
	public void setMunicipalityTest() {
		Municipality m;
		try {
			m = new Municipality("IT", "Milano", "0042a9");
			m.setMunicipalityDBId(1234569984L);
			m.setOfficialRef("ref5921");

			HousenumberList hnol = new HousenumberList();
			hnol.setMunicipality(m);

			assertEquals("IT", hnol.getCountrycode());
			assertEquals((long) 1234569984L, (long) hnol.getMunicipalityDBId());
			assertEquals("Milano", hnol.getMunicipalityName());
			assertEquals((long) 1234569984L, hnol.getMunicipalityDBId());
			assertEquals("ref5921", hnol.getMunicipalityRef());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void setSubareaActiveTest() {
		HousenumberList hnol = new HousenumberList();
		hnol.setSubareaActive(true);
		assertTrue(hnol.isSubareaActive());
		hnol.setSubareaActive(false);
		assertTrue(!hnol.isSubareaActive());
	}

	@Test
	public void setOfficialgeocoordinatesTest() {
		HousenumberList hnol = new HousenumberList();
			// check default mode (not official geocoordinates)
		assertEquals(false, hnol.isOfficialgeocoordinates());

		hnol.setOfficialgeocoordinates(true);
		assertEquals(true, hnol.isOfficialgeocoordinates());

		hnol.setOfficialgeocoordinates(false);
		assertEquals(false, hnol.isOfficialgeocoordinates());
	}
}
