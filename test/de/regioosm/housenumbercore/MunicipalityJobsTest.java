/**
 * 
 */
package de.regioosm.housenumbercore;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;












import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.Country;
import de.regioosm.housenumbercore.util.Municipality;

/**
 * @author openstreetmap
 *
 */
public class MunicipalityJobsTest {
	private static Connection housenumberConn = null;
	private static Connection osmdbConn = null;

	private Municipality testmuni = null;
	
	private MunicipalityArea testarea = null;
	

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
		testmuni = new Municipality("Bundesrepublik Deutschland", "Bobingen", "09772125");
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
	public void MunicipalityStringStringStringTest() {
		String ccode;
		try {
			ccode = Country.getCountryShortname("Belgium");
			String searchmuni = "Kno*";
			String ref = "*";
			assertEquals(new Municipality(ccode, searchmuni, ref), new Municipality(ccode, searchmuni, ref)); 
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#search(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 */
	@Test
	public void searchTest() {
		String ccode;
		try {
			ccode = Country.getCountryShortname("Belgium");

			String searchmuni = "Kno*";
			String ref = "*";
			String adminhierarchy = "";
			assertEquals(Municipality.search(ccode, searchmuni, ref, adminhierarchy), 1);
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#next()}.
	 */
	@Test
	public void nextTest() {
		fail("Not yet implemented");
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#getCountrycode()}.
	 */
	@Test
	public void getCountrycodeTest() {
		assertEquals(testmuni.getCountrycode(), "DE"); 
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#getName()}.
	 */
	@Test
	public void getNameTest() {
		fail("Not yet implemented");
	}

}
