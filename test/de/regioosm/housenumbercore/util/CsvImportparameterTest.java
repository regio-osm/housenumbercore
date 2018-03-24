package de.regioosm.housenumbercore.util;

import static org.junit.Assert.*;

import java.nio.charset.Charset;
import java.util.HashMap;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class CsvImportparameterTest {

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
	public void getHeaderfieldColumnTest() {
		fail("Not yet implemented");
	}

	@Test
	public void getFieldseparatorTest() {
		CsvImportparameter importparameter = new CsvImportparameter();
		importparameter.setImportfile("dummyfile", Charset.forName("UTF-8"));
		importparameter.setFieldSeparator(";");
		assertEquals(";", importparameter.getFieldSeparator());
	}

	@Test
	public void getHousenumberFieldseparatorTest() {
		fail("Not yet implemented");
	}

	@Test
	public void getHousenumberFieldseparator2Test() {
		fail("Not yet implemented");
	}

	@Test
	public void getImportfileTest() {
		fail("Not yet implemented");
	}

	@Test
	public void getImportfileFormatTest() {
		fail("Not yet implemented");
	}

	@Test
	public void getMunicipalityIDListEntryTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		HashMap<String,String> idlist = new HashMap<String,String>();
		idlist.put("1", "muni1");
		idlist.put("2", "muni2");
		idlist.put("47", "muni3");
		idlist.put("99", "muni4");
		importparameter.setMunicipalityIDList(idlist);
		assertEquals("muni1", importparameter.getMunicipalityIDListEntry("1"));
		assertEquals("muni2", importparameter.getMunicipalityIDListEntry("2"));
		assertEquals("muni3", importparameter.getMunicipalityIDListEntry("47"));
		assertEquals("muni4", importparameter.getMunicipalityIDListEntry("99"));
	}

	@Test
	public void getSourceCoordinateSystemTest() {
		CsvImportparameter importparameter = new CsvImportparameter();
		
		importparameter.setSourceCoordinateSystem("4326");
		assertEquals("4326", importparameter.getSourceCoordinateSystem());
	}

	@Test
	public void getStreetIDListEntryTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		HashMap<String,String> streetlist = new HashMap<String,String>();
		streetlist.put("1", "A Street");
		streetlist.put("2", "B Straße");
		streetlist.put("94", "C Rd");
		streetlist.put("10101", "D Street");
		importparameter.setStreetIDList(streetlist);
		assertEquals("A Street", importparameter.getStreetIDListEntry("1"));
		assertEquals("B Straße", importparameter.getStreetIDListEntry("2"));
		assertEquals("C Rd", importparameter.getStreetIDListEntry("94"));
		assertEquals("D Street", importparameter.getStreetIDListEntry("10101"));
	}

	@Test
	public void getSubareaMunicipalityIDListEntryTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		HashMap<String,String> subarealist = new HashMap<String,String>();
		subarealist.put("A1", "Sub 1");
		subarealist.put("B2", "Sub 2");
		subarealist.put("C222", "Sub 3");
		subarealist.put("X4711", "Sub 4747");
		importparameter.setSubareaMunicipalityIDList(subarealist);
		assertEquals("Sub 1", importparameter.getSubareaMunicipalityIDListEntry("A1"));
		assertEquals("Sub 2", importparameter.getSubareaMunicipalityIDListEntry("B2"));
		assertEquals("Sub 3", importparameter.getSubareaMunicipalityIDListEntry("C222"));
		assertEquals("Sub 4747", importparameter.getSubareaMunicipalityIDListEntry("X4711"));
	}

	@Test
	public void isSubareaActiveTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		importparameter.setSubareaActive(true);
		assertTrue(importparameter.isSubareaActive());
		importparameter.setSubareaActive(false);
		assertTrue(!importparameter.isSubareaActive());
	}

	
	
	@Test
	public void setFieldSeparatorTest() {
		CsvImportparameter importparameter = new CsvImportparameter();
		
		importparameter.setFieldSeparator(";");
		assertEquals(";", importparameter.getFieldSeparator());
	}

	@Test
	public void setHeaderfieldTest() {
		fail("Not yet implemented");
	}

	@Test
	public void setMunicipalityIDListTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		HashMap<String,String> idlist = new HashMap<String,String>();
		idlist.put("1", "muni1");
		idlist.put("2", "muni2");
		idlist.put("47", "muni3");
		idlist.put("99", "muni4");
		importparameter.setMunicipalityIDList(idlist);
		assertEquals("muni4", importparameter.getMunicipalityIDListEntry("99"));
	}

	@Test
	public void setStreetIDListTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		HashMap<String,String> streetlist = new HashMap<String,String>();
		streetlist.put("1", "A Street");
		streetlist.put("2", "B Straße");
		streetlist.put("94", "C Rd");
		streetlist.put("10101", "D Street");
		importparameter.setStreetIDList(streetlist);
		assertEquals("C Rd", importparameter.getStreetIDListEntry("94"));
	}

	@Test
	public void setSubareaActiveTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		importparameter.setSubareaActive(true);
		assertTrue(importparameter.isSubareaActive());
		importparameter.setSubareaActive(false);
		assertTrue(!importparameter.isSubareaActive());
	}

	@Test
	public void setSubareaMunicipalityIDListTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		HashMap<String,String> subarealist = new HashMap<String,String>();
		subarealist.put("A1", "Sub 1");
		subarealist.put("B2", "Sub 2");
		subarealist.put("C222", "Sub 3");
		subarealist.put("X4711", "Sub 4747");
		importparameter.setSubareaMunicipalityIDList(subarealist);
		assertEquals("Sub 3", importparameter.getSubareaMunicipalityIDListEntry("C222"));
	}

	@Test
	public void setSourceCoordinateSystemTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		importparameter.setSourceCoordinateSystem("4326");
		assertEquals("4326", importparameter.getSourceCoordinateSystem());
	}

	@Test
	public void setImportfileTest() {
		CsvImportparameter importparameter = new CsvImportparameter();

		importparameter.setImportfile("/a/b/c/d/simpletest.csv", Charset.forName("UTF-8"));
		assertEquals("/a/b/c/d/simpletest.csv", importparameter.getImportfile());

		importparameter.setImportfile("../d/simpletest.csv", Charset.forName("UTF-8"));
		assertEquals("../d/simpletest.csv", importparameter.getImportfile());

		importparameter.setImportfile("..\\d\\simpletest.csv", Charset.forName("UTF-8"));
		assertEquals("..\\d\\simpletest.csv", importparameter.getImportfile());

		importparameter.setImportfile("simpletest.csv", Charset.forName("UTF-8"));
		assertEquals("simpletest.csv", importparameter.getImportfile());
	}

	@Test
	public void setFieldseparatorsTest() {
		CsvImportparameter importparameter = new CsvImportparameter();
		
		importparameter.setHousenumberFieldseparators("x",  "Y");
		assertEquals("x", importparameter.getHousenumberFieldseparator());
		assertEquals("Y", importparameter.getHousenumberFieldseparator2());
	}


}
