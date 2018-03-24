package de.regioosm.housenumbercore;

import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.Municipality;

public class MunitipalityAreaTest {
	private static Connection housenumberConn = null;
	private static Connection osmdbConn = null;

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
			MunicipalityArea.connectDB(housenumberConn, osmdbConn);
		}
		catch( SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		housenumberConn.close();
		osmdbConn.close();
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void MunicipalityAreaMunicipalityTest() {
		Municipality testmuni;
		try {
			testmuni = new Municipality("DE", "testmuni", "");
			MunicipalityArea testarea = new MunicipalityArea(testmuni);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getNameTest() {
		Municipality testmuni;
		try {
			testmuni = new Municipality("DE", "testmuni", "");
			MunicipalityArea testarea = new MunicipalityArea(testmuni);
			testarea.setArea("testarea", 10, "xyz");
			assertEquals(testarea.getName(), "testarea");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void getMunicipalityTest() {
		Municipality testmuni;
		try {
			testmuni = new Municipality("DE", "testmuni", "");
			MunicipalityArea testarea = new MunicipalityArea(testmuni);
			assertEquals(testmuni, testarea.getMunicipality());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void getAreaAdminlevelTest() {
		Municipality testmuni;
		try {
			testmuni = new Municipality("DE", "testmuni", "");
			MunicipalityArea testarea = new MunicipalityArea(testmuni);
			testarea.setArea("testarea", 11, "xyz");
			assertEquals(testarea.getAreaAdminlevel(), 11);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void searchTest() {
		try {
			assertEquals(MunicipalityArea.search("DE", "Augsburg", "09761000", "Bärenkeller", null), 1);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	@Test
	public void nextTest() {
		try {
			MunicipalityArea.search("DE", "Augsburg", "09761000", "Bärenkeller", null);
			MunicipalityArea firsthit = MunicipalityArea.next();
			assertNotNull(firsthit);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
