package de.regioosm.housenumbercore.imports;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import de.regioosm.housenumbercore.MunicipalityArea;
import de.regioosm.housenumbercore.MunicipalityJobs;
import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.Country;
import de.regioosm.housenumbercore.util.CsvImportparameter;
import de.regioosm.housenumbercore.util.CsvImportparameter.HEADERFIELD;
import de.regioosm.housenumbercore.util.CsvReader;
import de.regioosm.housenumbercore.util.HousenumberList;
import de.regioosm.housenumbercore.util.ImportAddress;
import de.regioosm.housenumbercore.util.Municipality;
import de.regioosm.housenumbercore.util.OSMStreet;
import de.regioosm.housenumbercore.util.Street;


/**
 * Import or update of a housenumber list from a municipality.
 * @author Dietmar Seifert
 *
 */
public class UpdateMunicipalityArea {

	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	/**
	 * DB connection to Hausnummern
	 */
	private static Connection housenumberConn = null;

	/**
	 * Main Program to import a new housenumber list from a municipality.
	 *
	 * @param args
	 * @author Dietmar Seifert
	 */
	public static void main(final String[] args) {

		DateFormat dateformatUS = new SimpleDateFormat("yyyy-MM-dd");

		if ((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-country name: local nation use of country name. if missing, default is 'Bundesrepublik Deutschland'");
			System.out.println("-municipality name: municipality name, if single file for one municipality and its information is not in file");
			System.out.println("-municipalityref xy: reference id for municipality. You must know, which reference within a country will be used. In Germany, it's amtlicher Gemeindeschluessel");
			System.out.println("-useoverpass  yes|no: should Osm Overpass used for getting data or local DB (default)");
			//System.out.println("Liste enthält keine Stadtteilzuordnungen zur jeweiligen Hausnummer");
			System.out.println("");
			System.out.println("Importfile must have commentline in line 1, starting with #");
			System.out.println("columns need values in any order:   Stadt	Straße   Hausnummer    Hausnummerzusatz   Hausnummerzusatz2   Bemerkung   Subid   Laengengrad   Breitengrad");
			return;
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			System.out.println("args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}



		String fieldSeparator = "\t";

		String parameterLand = "Bundesrepublik Deutschland";
		String parameterStadt = "";
		String parameterAgs = "";
		boolean parameterUseOsmOverpass = false;

		
		if (args.length >= 1) {
			int argsOkCount = 0;
			for (int argsi = 0; argsi < args.length; argsi += 2) {
				System.out.print(" args pair analysing #: " + argsi + "  ===" + args[argsi] + "===");
				if (args.length > (argsi + 1)) {
					System.out.print("  args #+1: " + (argsi + 1) + "   ===" + args[argsi + 1] + "===");
				}
				System.out.println("");

				if (args[argsi].equals("-country")) {
					parameterLand = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-municipality")) {
					parameterStadt = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-municipalityref")) {
					parameterAgs = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-useoverpass")) {
					if (!args[argsi + 1].equals("")) {
						String yesno = args[argsi + 1].toLowerCase().substring(0,1);
						if (yesno.equals("y") || yesno.equals("j")) {
							parameterUseOsmOverpass = true;
						} else {
							parameterUseOsmOverpass = false;
						}
					}
					argsOkCount += 2;
				}

			}
			if (argsOkCount != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}
		
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		try {
			String urlHausnummern = configuration.db_application_url;
			housenumberConn = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);

			try {
				Country.connectDB(housenumberConn);
				if(Country.getCountryShortname(parameterLand).equals("")) {
					System.out.println("Error: unknown country, please check if correct.");
					return;
				}
			} catch (Exception e2) {
				System.out.println("Error occured, when tried to connect to DB, program stopped");
				e2.printStackTrace();
				return;
			}


			Municipality.connectDB(housenumberConn);
			Municipality municipality = null;
			try {
				municipality = new Municipality(parameterLand, parameterStadt, parameterAgs);
			} catch (Exception e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			int numbermunicipalities = 0;
			try {
				numbermunicipalities = Municipality.search(municipality.getCountrycode(), 
					municipality.getName(), municipality.getOfficialRef(), "");
			} catch (Exception e1) {
				System.out.println("Error ocurred, when tried to get the municipality");
				System.out.println("Error details: " + e1.toString());
				return;
			}
			if(numbermunicipalities != 1) {
				System.out.println("municipality was not exactly one-time found (instead: " + 
					numbermunicipalities + ", there stopping program");
				return;
			}
				// ok, get the one hit with all details from db result
			municipality = Municipality.next();

			MunicipalityArea newarea = null;
			MunicipalityJobs jobs = new MunicipalityJobs();
			jobs.setOverpassForEvaluation(parameterUseOsmOverpass);
			try {
				newarea = new MunicipalityArea(municipality);
				System.out.println("Processing municipality " + municipality.toString() + "===");
				if(newarea.generateMunicipalityPolygon(municipality, 0, true)) {
					if(newarea.generateSuburbPolygons(municipality, true)) {
						while( newarea != null ) {
							System.out.println("Processing newarea: " + newarea.toString() + "===");
							jobs.generateJob(newarea);
							Map<Street, OSMStreet> osmstreets = jobs.getOSMStreets(newarea);
							jobs.storeStreets(newarea, osmstreets);
							newarea = MunicipalityArea.next();
						}   // loop over all found municipality areas
					}
				} else {
					System.out.println("Administrative polygon couldn't be created for " +
						"municipality " + municipality.toString() + ".");
					System.out.println("Import was successfully, but processing for evaluation has stopped");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}


			housenumberConn.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}

	public String getCountryCode() {
		return this.getCountryCode();
	}
}
