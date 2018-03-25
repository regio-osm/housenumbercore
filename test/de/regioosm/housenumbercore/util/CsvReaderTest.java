package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

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
	private static Connection housenumberConn = null;

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

		assertNotNull(new CsvReader(importparameter));
	}

	@Test( expected = IllegalArgumentException.class )
	public void CsvReaderExceptionTest() {

		CsvImportparameter importparameter = new CsvImportparameter();
		importparameter.setCountrycode("DE");
		importparameter.setImportfile(null, Charset.forName("UTF-8"));
		
		assertNotNull(new CsvReader(importparameter));
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
		Map<Municipality, HousenumberList> lists = csvreader.execute();
		assertEquals(1, lists.size());
		for(Map.Entry<Municipality, HousenumberList> listentry : lists.entrySet()) {
			Municipality municipality = listentry.getKey();
			HousenumberList housenumberlist = listentry.getValue();
			ImportAddress testaddress = new ImportAddress();
			testaddress.setPostcode("52076");
			testaddress.setSubArea("");
			testaddress.setStreet("Aachener Straße");
			testaddress.setHousenumber("13");
			assertTrue(housenumberlist.contains(testaddress));
		}
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
