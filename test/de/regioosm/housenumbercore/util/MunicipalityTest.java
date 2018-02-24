/**
 * 
 */
package de.regioosm.housenumbercore.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;







import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author openstreetmap
 *
 */
public class MunicipalityTest {
	private static Connection housenumberConn = null;
	private static Connection osmdbConn = null;
	private static Municipality testmuni = null;
	

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Applicationconfiguration configuration = new Applicationconfiguration();

		java.util.Date program_starttime = new java.util.Date();

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

			String url_mapnik = configuration.db_osm2pgsql_url;
			osmdbConn = DriverManager.getConnection(url_mapnik, configuration.db_osm2pgsql_username, configuration.db_osm2pgsql_password);
			
			Municipality.connectDB(housenumberConn);
			Municipality.search("Bundesrepublik Deutschland", "Augsburg", "097*", "*");
			testmuni = Municipality.next();

		}
		catch( SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		housenumberConn.close();
		osmdbConn.close();
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#Municipality(java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testMunicipalityStringStringString() {
		String ccode = Country.getCountryShortname("Belgium");
		String searchmuni = "Kno*";
		String ref = "*";
		assertEquals(new Municipality(ccode, searchmuni, ref), new Municipality(ccode, searchmuni, ref)); 
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#search(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void testSearch() {
		String ccode = Country.getCountryShortname("Belgium");
		String searchmuni = "Kno*";
		String ref = "*";
		String adminhierarchy = "";
		try {
			assertEquals(Municipality.search(ccode, searchmuni, ref, adminhierarchy), 1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#next()}.
	 */
	@Test
	public void testNext() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#getCountrycode()}.
	 */
	@Test
	public void testGetCountrycode() {
		assertEquals(testmuni.getCountrycode(), "DE"); 
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#getName()}.
	 */
	@Test
	public void testGetName() {
		fail("Not yet implemented");
	}

}
