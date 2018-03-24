package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.nio.charset.Charset;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.regioosm.housenumbercore.util.CsvImportparameter.HEADERFIELD;
import de.regioosm.housenumbercore.util.CsvReader;
import de.regioosm.housenumbercore.util.HousenumberList;
import de.regioosm.housenumbercore.util.ImportAddress;

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
		CsvImportparameter importparameter = new CsvImportparameter();

		importparameter.setCountrycode("DE");
		importparameter.setImportfile("dummyfile", Charset.forName("UTF-8"));

		CsvReader reader = new CsvReader(importparameter);
		fail("incomplete");
	}

	@Test( expected = IllegalArgumentException.class )
	public void CsvReaderExceptionTest() {
		HousenumberList housenumberlist = new HousenumberList();

		CsvImportparameter importparameter = new CsvImportparameter();
		importparameter.setCountrycode("DE");
		importparameter.setImportfile(null, Charset.forName("UTF-8"));
		
		CsvReader reader = new CsvReader(importparameter);
		fail("incomplete");
	}

	@Test
	public void setHousenumberFieldseparatorsTest() {

		CsvImportparameter importparameter = new CsvImportparameter();
		importparameter.setCountrycode("DE");
		importparameter.setImportfile("dummyfile", Charset.forName("UTF-8"));
		importparameter.setHousenumberFieldseparators(" ",  "/");
		
		CsvReader reader = new CsvReader(importparameter);
		fail("incomplete");
	}

	@Test
	public void executeTest() {

		CsvImportparameter importparameter = new CsvImportparameter();
		importparameter.setCountrycode("DE");
		importparameter.setImportfile("test/resources/Aachen_Housenumberlist.csv", Charset.forName("UTF-8"));
		importparameter.setSourceCoordinateSystem("4326");
		importparameter.setFieldSeparator(",");
		importparameter.setHeaderfield(HEADERFIELD.housenumber, 4);
		importparameter.setHeaderfield(HEADERFIELD.housenumberaddition, 5);
		importparameter.setHeaderfield(HEADERFIELD.lat, 13);
		importparameter.setHeaderfield(HEADERFIELD.lon, 14);

		CsvReader csvreader = new CsvReader(importparameter);

		ImportAddress address = null;
		fail("incomplete");
/*
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
*/
	}

	@Test
	public void initialiseLuxembourgTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		importparameter.setCountrycode("LU");
		importparameter.setImportfile("dummy", Charset.forName("UTF-8"));
		
		CsvReader csvreader = new CsvReader(importparameter);
		assertEquals("Betzdorf", csvreader.getLuxembourgMunicipalityforSubarea("Berg (Betzdorf)"));
	}

}
