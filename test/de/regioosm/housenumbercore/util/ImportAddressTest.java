package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class ImportAddressTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void ImportAddressTest() {
			// check all ImportAddress fields for correct initialisation values
		ImportAddress adr = new ImportAddress();
		assertNull(adr.getMunicipalityRef());
		assertNull(adr.getSourceSrid());
		assertNull(adr.getCoordinatesSourceText());
		assertNull(adr.getNote());
		assertEquals((long) 0L, (long) adr.getStreetDBId());
			// check some Superclass Address fields
		assertNull(adr.getCountrycode());
		assertNull(adr.getMunicipality());
		assertNull(adr.getStreet());
		assertEquals(ImportAddress.lonUnset, adr.getLon(), 0.1D);
		assertEquals(ImportAddress.latUnset, adr.getLat(), 0.1D);
		assertNull(adr.getPostcode());
	}

	@Test
	public void ImportAddressStringLongStringStringStringString() {
		try {
			ImportAddress adr = new ImportAddress("Teststraße", 4711L, "subARea", "471112", 
				"12 1/3", "just a NoTE");

			assertEquals("Teststraße", adr.getStreet());
			assertEquals((long) 4711L, (long) adr.getStreetDBId());
			assertEquals("subARea", adr.getSubArea());
			assertEquals("471112", adr.getPostcode());
			assertEquals("12 1/3", adr.getHousenumber());
			assertEquals("just a NoTE", adr.getNote());
			assertEquals(ImportAddress.lonUnset, adr.getLon(), 0.1D);
			assertEquals(ImportAddress.latUnset, adr.getLat(), 0.1D);
			assertNull(adr.getSourceSrid());
			assertNull(adr.getCoordinatesSourceText());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void ImportAddressStringLongStringStringStringStringDoubleDoubleStringStringTest() {
		try {
			ImportAddress adr = new ImportAddress("Teststraße", 4711L, "subARea", "471112", 
				"12 1/3", "just a NoTE", -34.3, 3.9, "4326", "Geocoordinate Supporter");

			assertEquals("Teststraße", adr.getStreet());
			assertEquals((long) 4711L, (long) adr.getStreetDBId());
			assertEquals("subARea", adr.getSubArea());
			assertEquals("471112", adr.getPostcode());
			assertEquals("12 1/3", adr.getHousenumber());
			assertEquals("just a NoTE", adr.getNote());
			assertEquals(-34.3, adr.getLon(), 0.1D);
			assertEquals(3.9, adr.getLat(), 0.1D);
			assertEquals("4326", adr.getSourceSrid());
			assertEquals("Geocoordinate Supporter", adr.getCoordinatesSourceText());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getMunicipalityRefTest() {
		ImportAddress adr = new ImportAddress();
		adr.setMunicipalityRef("47114712test4713");
		assertEquals("47114712test4713",adr.getMunicipalityRef());
	}

	@Test
	public void getNoteTest() {
		ImportAddress adr = new ImportAddress();
		adr.setNote("just a TeST noTE");
		assertEquals("just a TeST noTE", adr.getNote());
	}

	@Test
	public void getStreetDBIdTest() {
		ImportAddress adr = new ImportAddress();
		adr.setStreetDBId(12345679012345L);
		assertEquals((long) 12345679012345L, (long) adr.getStreetDBId());
	}

	@Test
	public void setNoteTest() {
		ImportAddress adr = new ImportAddress();
		adr.setNote("just a TeST noTE");
		assertEquals("just a TeST noTE", adr.getNote());
	}

	@Test
	public void setStreetDBIdTest() {
		ImportAddress adr = new ImportAddress();
		adr.setStreetDBId(12345679012345L);
		assertEquals((long) 12345679012345L, (long) adr.getStreetDBId());
	}

	@Test
	public void setMunicipalityRefTest() {
		ImportAddress adr = new ImportAddress();
		adr.setMunicipalityRef("47114712test4713");
		assertEquals("47114712test4713",adr.getMunicipalityRef());
	}

}
