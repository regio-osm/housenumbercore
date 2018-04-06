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
			System.out.println("-country name: local nation use of country name. if missing, default is 'Bundesrepublik Deutschland'");
			System.out.println("-municipality name: municipality name, if single file for one municipality and its information is not in file");
			System.out.println("-municipalityref xy: reference id for municipality. You must know, which reference within a country will be used. In Germany, it's amtlicher Gemeindeschluessel");
			System.out.println("-coordinatesystem srid: epsg code within postgresql DB for source coordinate system, e.g. 31468 for Gauß Krueger 4");
			System.out.println("-file importfilename");
			System.out.println("-filecharset ISO-8859-1|UTF-8: Character set used in importfile, default is UTF-8");
			System.out.println("-housenumberseparator character: one character or empty");
			System.out.println("-housenumberseparator2 character: one character or empty");
			System.out.println("-fieldseparator character: in importfile separator for fields, like colon, comma or tabulator");
			System.out.println("-subareaactive yes|no (default no)");
			System.out.println("-listurl url: download hyperlink to housennumberlist");
			System.out.println("-copyright text: legal copyright Text for housenumberlist");
			System.out.println("-useage text: legal (re)useage Text for the list");
			System.out.println("-listcontenttimestamp YYYY-MM-DD: Content Timestamp");
			System.out.println("-listfiletimestamp YYYY-MM-DD: technical File Timestamp, time of download");
			System.out.println("-coordinatesosmimportable yes|no: are Coordinates in import file free importable in OSM (license compatible)");
			System.out.println("-useoverpass  yes|no: should Osm Overpass used for getting data or local DB (default)");
			System.out.println("");
			System.out.println("Importfile must have commentline in line 1, starting with #");
			System.out.println("columns need values in any order:   Stadt	Straße   Hausnummer    Hausnummerzusatz   Hausnummerzusatz2   Bemerkung   Subid   Laengengrad   Breitengrad");
			return;
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			System.out.println("args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}



		String fieldSeparator = "\t";

		String paramCountry = "Bundesrepublik Deutschland";
		String paramMunicipality = "";
		String paramMunicipalityRef = "";
		boolean parameterUseOsmOverpass = false;
		String paramSourceSrid = "";
		String paramImportfileName = "";
		String parameterImportfileCharset = "UTF-8";
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

				if (args[argsi].equals("-country")) {
					paramCountry = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-municipality")) {
					paramMunicipality = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-municipalityref")) {
					paramMunicipalityRef = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-coordinatesystem")) {
					paramSourceSrid = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-useoverpass")) {
					if (!args[argsi + 1].equals("")) {
						String yesno = args[argsi + 1].toLowerCase().substring(0,1);
						if (yesno.equals("y") || yesno.equals("j")) {
							parameterUseOsmOverpass = true;
						} else {
							parameterUseOsmOverpass = false;
						}
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-file")) {
					paramImportfileName = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-filecharset")) {
					if (!args[argsi + 1].equals("")) {
						if (args[argsi + 1].toUpperCase().equals("ISO-8859-1"))
							parameterImportfileCharset = "ISO-8859-1";
						else if (args[argsi + 1].toUpperCase().equals("UTF-8"))
							parameterImportfileCharset = "UTF-8";
						else {
							System.out.println("Inputparameter -filecharset has invalid value '" + 
								args[argsi + 1] + ". Only values 'ISO-8859-1' and 'UTF-8' are allowed");
							return;
						}
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-housenumberseparator")) {
					parameterHousenumberadditionseparator = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-housenumberseparator2")) {
					parameterHousenumberadditionseparator2 = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-subareaactive")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterSubareaActive = true;
					} else {
						parameterSubareaActive = false;
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-fieldseparator")) {
					fieldSeparator = args[argsi + 1];
					System.out.println("Info: explicit set field separator to '" + fieldSeparator + "'");
					argsOkCount += 2;
				} else if (args[argsi].equals("-listurl")) {
					parameterSourcelistUrl = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-copyright")) {
					parameterSourcelistCopyright = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-useage")) {
					parameterSourcelistUseage = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-listcontenttimestamp")) {
					try {
						parameterSourcelistContentdate = dateformatUS.parse(args[argsi + 1]);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-listfiletimestamp")) {
					try {
						parameterSourcelistFiledate = dateformatUS.parse(args[argsi + 1]);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					argsOkCount += 2;
				} else if (args[argsi].equals("-coordinatesosmimportable")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterOfficialgeocoordinates = true;
					} else {
						parameterOfficialgeocoordinates = false;
					}
					argsOkCount += 2;
				} else {
					System.out.println("unknown program parameter '" + args[argsi] + "===");
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
				if(Country.getCountryShortname(paramCountry).equals("")) {
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
				importparameter.setCountrycode(Country.getCountryShortname(paramCountry));
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("Can't get countrycode for input parameter '" +
					paramCountry + " from DB, please check country name and repeat program.");
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
			importparameter.setSourceCoordinateSystem(paramSourceSrid);
			importparameter.setImportfile(paramImportfileName, Charset.forName(parameterImportfileCharset));
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
				jobs.setOverpassForEvaluation(parameterUseOsmOverpass);
				try {
					newarea = new MunicipalityArea(municipality);
					System.out.println("Processing municipality " + municipality.toString() + "===");
					if((newarea = newarea.generateMunicipalityPolygon(municipality, 0, true)) != null) {
						System.out.println("Processing newarea: " + newarea.toString() + "===");
						jobs.generateJob(newarea);
						Map<Street, OSMStreet> osmstreets = jobs.getOSMStreets(newarea);
						jobs.storeStreets(newarea, osmstreets);
						List<MunicipalityArea> subareas = newarea.generateSuburbPolygons(municipality, true);
						if(subareas != null) {
							for(int subareaindex = 0; subareaindex < subareas.size(); subareaindex++) {
								MunicipalityArea subarea = subareas.get(subareaindex);
								System.out.println("Processing subarea: " + subarea.toString() + "===");
								jobs.generateJob(subarea);
								osmstreets = jobs.getOSMStreets(subarea);
								jobs.storeStreets(subarea, osmstreets);
							}
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
