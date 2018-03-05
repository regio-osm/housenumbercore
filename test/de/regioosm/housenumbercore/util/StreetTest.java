package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class StreetTest {

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
	public void streetMunicipalityStringTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street = new Street(m, "Hauptstraße");
			
			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
			assertEquals("Hauptstraße", street.getName());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void streetMunicipalityStringLongTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street = new Street(m, "Hauptstraße", 987612345L);
			
			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
			assertEquals("Hauptstraße", street.getName());
			assertEquals(987612345L, street.getStreetDBId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void getMunicipalityTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street = new Street(m, "Hauptstraße", 987612345L);
			
			assertEquals("DE", street.getMunicipality().getCountrycode());
			assertEquals("testCity", street.getMunicipality().getName());
			assertEquals("1234567890011", street.getMunicipality().getOfficialRef());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void getNameTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street = new Street(m, "Hauptstraße", 987612345L);
			
			assertEquals("Hauptstraße", street.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void getStreetDBIdTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street = new Street(m, "Hauptstraße", 987612345L);
			
			assertEquals(987612345L, street.getStreetDBId());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void setNameTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street = new Street(m, "Hauptstraße", 987612345L);

			assertEquals("Hauptstraße", street.getName());

			street.setName("Friedrich-Ebert-Straße");
			assertEquals("Friedrich-Ebert-Straße", street.getName());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void hashCodeTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street1 = new Street(m, "Hauptstraße");

			assertNotEquals(street1.hashCode(), street1.hashCode());
			
			Street street2 = new Street(m, "Haupstraße");

			assertNotEquals(street1.hashCode(), street2.hashCode());

			Municipality m2 = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street3 = new Street(m2, "Hauptstraße");

			assertEquals(street1.hashCode(), street3.hashCode());

			Municipality m3 = new Municipality("Bundesrepublik Deutschland", "", "");
			Street street4 = new Street(m3, "Haupstraße");

			assertNotEquals(street1.hashCode(), street4.hashCode());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void equalsObjectTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street1 = new Street(m, "Hauptstraße");

			assertTrue(street1.equals(street1));

			Street street2 = new Street(m, "Haupstraße");

			assertFalse(street1.equals(street2));

			Municipality m2 = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street3 = new Street(m2, "Hauptstraße");

			assertTrue(street1.equals(street3));

			Municipality m3 = new Municipality("Bundesrepublik Deutschland", "", "");
			Street street4 = new Street(m3, "Haupstraße");

			assertFalse(street1.equals(street4));

				// test, if equal, because municipality ref will not be used for comparision
			Municipality m5 = new Municipality("Bundesrepublik Deutschland", "Testmuni", "123");
			Street street5 = new Street(m5, "Haupstraße");
			Municipality m6 = new Municipality("Bundesrepublik Deutschland", "Testmuni", "999999999");
			Street street6 = new Street(m5, "Haupstraße");

			assertTrue(street5.equals(street6));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void compareToTest() {
		Municipality m;
		try {
			m = new Municipality("Bundesrepublik Deutschland", "testCity", "1234567890011");
			Street street1 = new Street(m, "Hauptstraße");
			Street street2 = new Street(m, "Haupt1straße");
			assertTrue(street1.equals(street1));

			assertTrue(street1.compareTo(street2) < 0);

			Municipality m3 = new Municipality("Bundesrepublik Deutschland", "testCitxx", "1234567890011");
			Street street3 = new Street(m3, "Hauptstraße");

			assertTrue(street1.compareTo(street3) > 0);

			assertTrue(street1.compareTo(street1) == 0);

			Municipality m4 = new Municipality("Bundesrepublik Deutschland", "testCity", "007");
			Street street4 = new Street(m4, "Hauptstraße");

			assertTrue(street1.compareTo(street4) == 0);

			assertTrue(street1.compareTo(null) > 0);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
