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
	

	/**
	 * @throws java.lang.Exception
	 */
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
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#Municipality(java.lang.String, java.lang.String, java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void MunicipalityStringStringStringTest() throws Exception {
		Country.connectDB(housenumberConn);
		String ccode = Country.getCountryShortname("Belgium");
		String searchmuni = "Kno*";
		String ref = "*";
		assertEquals(new Municipality(ccode, searchmuni, ref), new Municipality(ccode, searchmuni, ref));
	}

	@Test( expected = Exception.class )
	public void existsExceptionTest() throws Exception {
		Municipality testm = new Municipality("DE", "%", "%");
		try {
			testm.exists();
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#search(java.lang.String, java.lang.String, java.lang.String, java.lang.String)}.
	 * @throws Exception 
	 */
	@Test
	public void SearchTest() throws Exception {
		Country.connectDB(housenumberConn);
		String ccode = Country.getCountryShortname("Bundesrepublik Deutschland");
		String searchmuni = "Augsbur*";
		String ref = "*";
		String adminhierarchy = "";
		try {
			assertEquals(1, Municipality.search(ccode, searchmuni, ref, adminhierarchy));
		} catch (Exception e) {
			System.out.println("Error in SearchTest(). Details follow ...");
			e.printStackTrace();
		} 
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#next()}.
	 * @throws Exception 
	 */
	@Test
	public void nextTest() throws Exception {
		try {
			Municipality.search("DE", "%", "%", "%");
		} catch (Exception e) {
			throw e;
		}
		Municipality testm = Municipality.next();
		assertNotNull(testm);

		testm = Municipality.next();
		assertNotNull(testm);
		
		testm = Municipality.next();
		assertNotNull(testm);
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#getCountrycode()}.
	 * @throws Exception 
	 */
	@Test
	public void getCountrycodeTest() throws Exception {
		Municipality testmuni = new Municipality("BE", "belgiumtesttest", "");
		assertEquals(testmuni.getCountrycode(), "BE"); 
	}

	/**
	 * Test method for {@link de.regioosm.housenumbercore.util.Municipality#getName()}.
	 * @throws Exception 
	 */
	@Test
	public void getNameTest() throws Exception {
		Municipality testm = new Municipality("DE", "testmuni", "");
		assertEquals(testm.getName(), "testmuni");
	}

}
