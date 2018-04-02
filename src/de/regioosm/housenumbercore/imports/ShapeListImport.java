package de.regioosm.housenumbercore.imports;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.regioosm.housenumbercore.MunicipalityArea;
import de.regioosm.housenumbercore.MunicipalityJobs;
import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.Country;
import de.regioosm.housenumbercore.util.HousenumberList;
import de.regioosm.housenumbercore.util.Municipality;
import de.regioosm.housenumbercore.util.OSMStreet;
import de.regioosm.housenumbercore.util.ShapeImportparameter;
import de.regioosm.housenumbercore.util.ShapeImportparameter.HEADERFIELD;
import de.regioosm.housenumbercore.util.ShapeReader;
import de.regioosm.housenumbercore.util.Street;


/**
 * Import or update of a housenumber list from a municipality.
 * @author Dietmar Seifert
 *
 */
public class ShapeListImport {

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
			System.out.println("-land 'Bundesrepublik Deutschland' wenn fehlend");
			System.out.println("-stadt Stadtname");
			System.out.println("-ags 8stelligeramtlicherGemeindeschlüssel z.B. 09761000");
			System.out.println("-koordsystem srid z.B. 31468 für GK4");
			System.out.println("-datei importdateiname");
			System.out.println("-municipalityidfile filename with municipalty-id => municipality name, if in main -datei file is only municipality-id, not the municipality name itself: Column 1 has fix id, Column 2 has fix municipality name, file is UTF-8, text-format and tab-separated");
			System.out.println("-submunicipalityidfile filename with SUBmunicipalty-id => SUBmunicipality name, if in main -datei file is only submunicipality-id, not the submunicipality name itself: Column 1 has fix municipality id, Column 2 has fix submunicipality id and Column 3 has fix submunicipality name. File is UTF-8, text-format and tab-separated");
			System.out.println("-streetidfile filename with street-id => streetname, if in main -datei file is only street-id, not the street name itself: Column 1 has fix id, Column 2 has fix streetname, file is UTF-8, text-format and tab-separated");
			System.out.println("-hausnummeradditionseparator zeichen");
			System.out.println("-hausnummeradditionseparator2 zeichen");
			System.out.println("-feldseparator zeichen");
			System.out.println("-subgebieteaktiv ja|nein (default nein)");
			System.out.println("-listurl  download hyperlink to housennumberlist");
			System.out.println("-copyright copyright Text for housenumberlist");
			System.out.println("-useage useage Text for the list");
			System.out.println("-listcontenttimestamp Content Timestamp in date format YYYY-MM-DD");
			System.out.println("-listfiletimestamp File Timestamp in date format YYYY-MM-DD");
			System.out.println("-CoordinatesOSMImportable are Coordinates in import file free importable in OSM (license compatible)");
			System.out.println("Liste enthält keine Stadtteilzuordnungen zur jeweiligen Hausnummer");
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
		String parameterQuellSrid = "";
		String parameterImportdateiname = "";
		boolean parameterSubareaActive = false;
		String parameterHousenumberadditionseparator = "";
		String parameterHousenumberadditionseparator2 = "-";
		String parameterSourcelistUrl = "";
		String parameterSourcelistCopyright = "";
		String parameterSourcelistUseage = "";
		Date parameterSourcelistContentdate = null;
		Date parameterSourcelistFiledate = null;
		boolean parameterOfficialgeocoordinates = false;

		List<String> municipalityUpperCaseList = new ArrayList<>();
		List<String> municipalityLowerCaseList = new ArrayList<>();
		
		if (args.length >= 1) {
			int argsOkCount = 0;
			for (int argsi = 0; argsi < args.length; argsi += 2) {
				System.out.print(" args pair analysing #: " + argsi + "  ===" + args[argsi] + "===");
				if (args.length > (argsi + 1)) {
					System.out.print("  args #+1: " + (argsi + 1) + "   ===" + args[argsi + 1] + "===");
				}
				System.out.println("");

				if (args[argsi].equals("-land")) {
					parameterLand = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-stadt")) {
					parameterStadt = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-ags")) {
					parameterAgs = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-koordsystem")) {
					parameterQuellSrid = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-datei")) {
					parameterImportdateiname = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-hausnummeradditionseparator")) {
					parameterHousenumberadditionseparator = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-hausnummeradditionseparator2")) {
					parameterHousenumberadditionseparator2 = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-subgebieteaktiv")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterSubareaActive = true;
					} else {
						parameterSubareaActive = false;
					}
					argsOkCount += 2;
				}
				if (args[argsi].equals("-feldseparator")) {
					fieldSeparator = args[argsi + 1];
					System.out.println("Info: explizit Feldseparator aktiviert ===" + fieldSeparator + "===");
					argsOkCount += 2;
				}				
				if (args[argsi].equals("-listurl")) {
					parameterSourcelistUrl = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-copyright")) {
					parameterSourcelistCopyright = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-useage")) {
					parameterSourcelistUseage = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-listcontenttimestamp")) {
					try {
						parameterSourcelistContentdate = dateformatUS.parse(args[argsi + 1]);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					argsOkCount += 2;
				}
				if (args[argsi].equals("-listfiletimestamp")) {
					try {
						parameterSourcelistFiledate = dateformatUS.parse(args[argsi + 1]);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					argsOkCount += 2;
				}
				if (args[argsi].equals("-CoordinatesOSMImportable")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterOfficialgeocoordinates = true;
					} else {
						parameterOfficialgeocoordinates = false;
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
			

			ShapeImportparameter importparameter = new ShapeImportparameter();
			
				// if importfile has no direct information about municipality name, 
				// but a reference id instead, and the extra file with 
				// municipality ref => municipality name is available, read it now
				// Code must reused from class CsvListImport, if neccessary

				// if importfile has no direct information about municipality subarea name, 
				// but a reference id instead, and the extra file with 
				// municipality subarea ref => municipality subarea name is available, read it now
				// Code must reused from class CsvListImport, if neccessary

				// if importfile has no direct information about street name, 
				// but a reference id instead, and the extra file with 
				// street ref => street name is available, read it now
				// Code must reused from class CsvListImport, if neccessary

			Municipality.connectDB(housenumberConn);

			try {
				importparameter.setCountrycode(Country.getCountryShortname(parameterLand));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Can't get countrycode for input parameter '" +
					parameterLand + " from DB, please check country name and repeat program.");
				return;
			}

/*			if(importparameter.getCountrycode().equals("IT")) {
				municipalityLowerCaseList.add("al");
				municipalityLowerCaseList.add("alla");
				municipalityLowerCaseList.add("alle");
				municipalityLowerCaseList.add("da");
				municipalityLowerCaseList.add("de");
				municipalityLowerCaseList.add("dei");
				municipalityLowerCaseList.add("del");
				municipalityLowerCaseList.add("dell");
				municipalityLowerCaseList.add("della");
				municipalityLowerCaseList.add("delle");
				municipalityLowerCaseList.add("destro");
				municipalityLowerCaseList.add("di");
				municipalityLowerCaseList.add("in");
				municipalityLowerCaseList.add("sinistro");
				
				municipalityUpperCaseList.add("II");
				
				importparameter.setMunicipalityUpperLowerList(municipalityUpperCaseList, municipalityLowerCaseList);
			}
*/			
			importparameter.setSubareaActive(parameterSubareaActive);
			importparameter.setSourceCoordinateSystem(parameterQuellSrid);
			importparameter.setImportfile(parameterImportdateiname, Charset.forName("UTF-8"));		 //"ISO-8859-1"  "UTF-8"
			importparameter.setHousenumberFieldseparators(parameterHousenumberadditionseparator,
				parameterHousenumberadditionseparator2);
			importparameter.setHeaderfield(HEADERFIELD.municipalityref, "ISTAT");
			importparameter.setHeaderfield(HEADERFIELD.housenumber, "CIVICO");
			importparameter.setHeaderfield(HEADERFIELD.street, "NOME");
			importparameter.setHeaderfield(HEADERFIELD.municipality, "COMUNE");
			importparameter.setHeaderfield(HEADERFIELD.postcode, "CAP");
			importparameter.setHeaderfield(HEADERFIELD.district, "PROVINCIA");
			importparameter.setHeaderfield(HEADERFIELD.region, "REGIONE");

			ShapeReader shapereader = new ShapeReader(importparameter);
			Map<Municipality, HousenumberList> housenumberlists = shapereader.execute();
			for (Map.Entry<Municipality, HousenumberList> listentry : housenumberlists.entrySet()) {
				Municipality municipality = listentry.getKey();
				HousenumberList housenumberlist = listentry.getValue();

				housenumberlist.setSourcelistUrl(parameterSourcelistUrl);
				housenumberlist.setSourcelistCopyright(parameterSourcelistCopyright);
				housenumberlist.setSourcelistUseage(parameterSourcelistUseage);
				housenumberlist.setContentDate(parameterSourcelistContentdate);
				housenumberlist.setFileDate(parameterSourcelistFiledate);
				housenumberlist.setOfficialgeocoordinates(parameterOfficialgeocoordinates);

				int numbermunicipalities = 0;
				try {
					numbermunicipalities = Municipality.search(municipality.getCountrycode(), 
						municipality.getName(), municipality.getOfficialRef(), "");
				} catch (Exception e1) {
					System.out.println("Error ocurred, when tried to get the municipality");
					System.out.println("Error details: " + e1.toString());
					return;
				}
				if(numbermunicipalities <= 1) {
					if(numbermunicipalities == 0) {
						try {
							municipality = municipality.store();
						} catch (Exception e) {
							System.out.println("Error ocurred, when tried to create municipality in database.");
							System.out.println("Error details: " + e.toString());
							return;
						}
					} else  {
						municipality = Municipality.next();
						System.out.println("municipality already exists, therefor import of liste can be done");
						System.out.println("here is the found municipality with its properties: " + municipality.toString());
					}
					housenumberlist.storeToDB();
				} else {
					while((municipality = Municipality.next()) != null) {
						System.out.println("Hit " + municipality.toString());
					}
					System.out.println("Error: municipality is more than once stored in DB, will be ignored. " +
						"Execuction continues with next municipality.");
					System.out.println(" (cont): ignored municipality is " + municipality.toString());
					continue;
				}

				MunicipalityArea newarea = null;
				MunicipalityJobs jobs = new MunicipalityJobs();
				jobs.setOverpassForEvaluation(true);
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
						continue;
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

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
