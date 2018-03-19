package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.regioosm.housenumbercore.util.CsvReader;
import de.regioosm.housenumbercore.util.HousenumberList;
import de.regioosm.housenumbercore.util.ImportAddress;
import de.regioosm.housenumbercore.util.CsvReader.HEADERFIELD;

public class CsvReaderTest {

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
	public void CsvReaderTest() {
		HousenumberList housenumberlist = new HousenumberList();
		housenumberlist.setImportfile("dummyfile");
		housenumberlist.setCountrycode("DE");

		CsvReader reader = new CsvReader(housenumberlist, "UTF-8");
	}

	@Test( expected = IllegalArgumentException.class )
	public void CsvReaderExceptionTest() {
		HousenumberList housenumberlist = new HousenumberList();

		CsvReader reader = new CsvReader(housenumberlist, "UTF-8");
	}

	@Test
	public void getFieldseparatorTest() {
		HousenumberList housenumberlist = new HousenumberList();
		housenumberlist.setImportfile("dummyfile");
		housenumberlist.setCountrycode("DE");

		CsvReader reader = new CsvReader(housenumberlist, "UTF-8");
		reader.setFieldSeparator(";");
		assertEquals(";", reader.getFieldSeparator());
	}

	@Test
	public void setHousenumberFieldseparatorsTest() {
		HousenumberList housenumberlist = new HousenumberList();
		housenumberlist.setImportfile("dummyfile");
		housenumberlist.setCountrycode("DE");

		CsvReader reader = new CsvReader(housenumberlist, "UTF-8");
		reader.setHousenumberFieldseparators(" ",  "/");
	}

	@Test
	public void setFieldSeparatorTest() {
		HousenumberList housenumberlist = new HousenumberList();
		housenumberlist.setImportfile("dummyfile");
		housenumberlist.setCountrycode("DE");

		CsvReader reader = new CsvReader(housenumberlist, "UTF-8");
		reader.setFieldSeparator(";");
		assertEquals(";", reader.getFieldSeparator());
	}

	@Test
	public void nextTest() {
		HousenumberList housenumberlist = new HousenumberList();
		housenumberlist.setImportfile("test/resources/Aachen_Housenumberlist.csv");
		housenumberlist.setCountrycode("DE");
		housenumberlist.setSourceCoordinateSystem("4326");

		CsvReader csvreader = new CsvReader(housenumberlist, "UTF-8");
		csvreader.setFieldSeparator(",");
		csvreader.setHeaderfield(HEADERFIELD.housenumber, 4);
		csvreader.setHeaderfield(HEADERFIELD.housenumberaddition, 5);
		csvreader.setHeaderfield(HEADERFIELD.lat, 13);
		csvreader.setHeaderfield(HEADERFIELD.lon, 14);

		ImportAddress address = null;
		try {
			address = csvreader.next();
			if(address != null) {
				assertEquals("Aachener Straße", address.getStreet());
				assertEquals("52076", address.getPostcode());
				assertEquals("12", address.getHousenumber());
				assertEquals(6.139022, address.getLon(), 0.01);
			}
			address = csvreader.next();
			if(address != null) {
				assertEquals("Aachener Straße", address.getStreet());
				assertEquals("52076", address.getPostcode());
				assertEquals("13", address.getHousenumber());
				assertEquals(6.139457, address.getLon(), 0.01);
			}
		} catch (Exception e) {
			e.printStackTrace();
			fail("exception occured");
		}
	}

	@Test
	public void initialiseLuxembourgTest() {
		HousenumberList housenumberlist = new HousenumberList();
		housenumberlist.setImportfile("dummy");
		housenumberlist.setCountrycode("LU");

		CsvReader csvreader = new CsvReader(housenumberlist, "UTF-8");
		assertEquals("Betzdorf", csvreader.getLuxembourgMunicipalityforSubarea("Berg (Betzdorf)"));
	}

}
