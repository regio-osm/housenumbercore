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
import java.util.List;
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
public class CsvListImport {

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
			System.out.println("-municipalityidfile filename:  with municipalty-id => municipality name, if in main -file file is only municipality-id, not the municipality name itself: Column 1 has fix id, Column 2 has fix municipality name, file is UTF-8, text-format and tab-separated");
			System.out.println("-submunicipalityidfile filename:  with SUBmunicipalty-id => SUBmunicipality name, if in main -file file is only submunicipality-id, not the submunicipality name itself: Column 1 has fix municipality id, Column 2 has fix submunicipality id and Column 3 has fix submunicipality name. File is UTF-8, text-format and tab-separated");
			System.out.println("-streetidfile filename:  with street-id => streetname, if in main -file file is only street-id, not the street name itself: Column 1 has fix id, Column 2 has fix streetname, file is UTF-8, text-format and tab-separated");
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

		String paramCountry = "Bundesrepublik Deutschland";
		String paramMunicipality = "";
		String paramMunicipalityRef = "";
		boolean parameterUseOsmOverpass = false;
		String paramSourceSrid = "";
		String paramImportfileName = "";
		String parameterImportfileCharset = "UTF-8";
		String parameterMunicipalityIdListfilename = "";
		String parameterSubMunicipalityIdListfilename = "";
		String parameterStreetIdListfilename = "";
		boolean parameterSubareaActive = false;
		String parameterHousenumberadditionseparator = "";
		String parameterHousenumberadditionseparator2 = "-";
		String parameterSourcelistUrl = "";
		String parameterSourcelistCopyright = "";
		String parameterSourcelistUseage = "";
		Date parameterSourcelistContentdate = null;
		Date parameterSourcelistFiledate = null;
		boolean parameterOfficialgeocoordinates = false;


		HashMap<String,String> municipalityIdList = new HashMap<String,String>();
		HashMap<String,String> submunicipalityIdList = new HashMap<String,String>();
		HashMap<String,String> streetIdList = new HashMap<String,String>();
		
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
				} else if (args[argsi].equals("-municipalityidfile")) {
					parameterMunicipalityIdListfilename = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-submunicipalityidfile")) {
					parameterSubMunicipalityIdListfilename = args[argsi + 1];
					argsOkCount += 2;
				} else if (args[argsi].equals("-streetidfile")) {
					parameterStreetIdListfilename = args[argsi + 1];
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


			CsvImportparameter importparameter = new CsvImportparameter();
			
			try {
					// if importfile has no direct information about municipality name, 
					// but a reference id instead, and the extra file with 
					// municipality ref => municipality name is available, read it now
				if(!parameterMunicipalityIdListfilename.equals("")) {
					String municipalityIdlistfilename = configuration.application_datadir + "/" + parameterMunicipalityIdListfilename;
					BufferedReader municipalityIdlistfilereader = new BufferedReader(new FileReader(municipalityIdlistfilename));
					String municipalityIdlistline = "";
					Integer filelineno = 0;
					while ((municipalityIdlistline = municipalityIdlistfilereader.readLine()) != null) {
						filelineno++;
						if(municipalityIdlistline.equals(""))
							continue;
						if(municipalityIdlistline.equals("GKZ	GEMEINDENAME"))
							continue;
						String[] municipalityIdlistlinecolumns = municipalityIdlistline.split("\t");
						if (municipalityIdList.get(municipalityIdlistlinecolumns[0]) == null) {
							municipalityIdList.put(municipalityIdlistlinecolumns[0], municipalityIdlistlinecolumns[1]);
						} else {
							System.out.println("ERROR: duplicate municipalityIdList-Entry in line #" + filelineno + ":  ===" + municipalityIdlistline + "===");
						}
					}
					municipalityIdlistfilereader.close();
					importparameter.setMunicipalityIDList(municipalityIdList);
				}

					// if importfile has no direct information about municipality subarea name, 
					// but a reference id instead, and the extra file with 
					// municipality subarea ref => municipality subarea name is available, read it now
				if(!parameterSubMunicipalityIdListfilename.equals("")) {
					String submunicipalityIdlistfilename = configuration.application_datadir + "/" + parameterSubMunicipalityIdListfilename;
					BufferedReader submunicipalityIdlistfilereader = new BufferedReader(new FileReader(submunicipalityIdlistfilename));
					String submunicipalityIdlistline = "";
					Integer filelineno = 0;
					while ((submunicipalityIdlistline = submunicipalityIdlistfilereader.readLine()) != null) {
						filelineno++;
						if(submunicipalityIdlistline.equals(""))
							continue;
						if(submunicipalityIdlistline.equals("GKZ	OKZ	ORTSNAME"))
							continue;
						String[] submunicipalityIdlistlinecolumns = submunicipalityIdlistline.split("\t");
						String submunicipalitykey = submunicipalityIdlistlinecolumns[0] + submunicipalityIdlistlinecolumns[1];
						if (submunicipalityIdList.get(submunicipalitykey) == null) {
							submunicipalityIdList.put(submunicipalitykey, submunicipalityIdlistlinecolumns[2]);
						} else {
							System.out.println("ERROR: duplicate submunicipalityIdList-Entry " + submunicipalitykey
								+ " in line #" + filelineno + ":  ===" + submunicipalityIdlistline + "===");
						}
					}
					submunicipalityIdlistfilereader.close();
					importparameter.setSubareaMunicipalityIDList(submunicipalityIdList);
				}

					// if importfile has no direct information about street name, 
					// but a reference id instead, and the extra file with 
					// street ref => street name is available, read it now
				if(!parameterStreetIdListfilename.equals("")) {
					String streetidlistfilename = configuration.application_datadir + "/" + parameterStreetIdListfilename;
					BufferedReader streetidlistfilereader = new BufferedReader(new FileReader(streetidlistfilename));
					String streetidlistline = "";
					Integer filelineno = 0;
					while ((streetidlistline = streetidlistfilereader.readLine()) != null) {
						filelineno++;
						if(streetidlistline.equals(""))
							continue;
						if(streetidlistline.equals("strasseid	STRASSENNAME"))
							continue;
						String[] streetidlistlinecolumns = streetidlistline.split("\t");
						if (streetIdList.get(streetidlistlinecolumns[0]) == null) {
							streetIdList.put(streetidlistlinecolumns[0], streetidlistlinecolumns[1]);
						} else {
							System.out.println("ERROR: duplicate streetidList-Entry in line #" + filelineno + ":  ===" + streetidlistline + "===");
						}
					}
					streetidlistfilereader.close();
					importparameter.setStreetIDList(streetIdList);
				}

				Municipality.connectDB(housenumberConn);

				try {
					importparameter.setCountrycode(Country.getCountryShortname(paramCountry));
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Can't get countrycode for input parameter '" +
						paramCountry + " from DB, please check country name and repeat program.");
					return;
				}
				//Switzerland: nationwide: -country "Schweiz" -coordinatesystem 2056 -housenumberSeparator "" -subareaactive "n" -file /home/osm/apps/housenumbercore/data/Schweiz/CH-20180325-1253.csv -listurl "https://a.b.c/de.txt" -copyright "Copyright Text xyz" -useage "Nutzungsvereinbarung xy" -listcontenttimestamp 2018-03-01 -listfiletimestamp 2018-03-15 -coordinatesOSMImportable no -filecharset "UTF-8" -useoverpass yes
				//Switzerland Kanton Basel-Stadt: -land "Schweiz" -koordsystem 2056 -housenumberSeparator "" -subgebieteaktiv "n" -datei /home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Schweiz/Basel-Stadt/2018/DM_Datenmarkt/Datenmarkt.csv -listurl "https://basel.txt" -copyright "Copyright basel" -useage "Nutzungsvereinbarung basel" -listcontenttimestamp 2018-03-31 -listfiletimestamp 2018-04-01 -CoordinatesOSMImportable yes
				importparameter.setSubareaActive(parameterSubareaActive);
				importparameter.setSourceCoordinateSystem(paramSourceSrid);
				importparameter.setImportfile(paramImportfileName, Charset.forName(parameterImportfileCharset));
				importparameter.setHousenumberFieldseparators(parameterHousenumberadditionseparator,
					parameterHousenumberadditionseparator2);
				importparameter.setHeaderfield(HEADERFIELD.municipalityref, 3);
				importparameter.setHeaderfield(HEADERFIELD.municipality, 4);
				importparameter.setHeaderfield(HEADERFIELD.street, 5);
				importparameter.setHeaderfield(HEADERFIELD.housenumber, 6);
				importparameter.setHeaderfield(HEADERFIELD.postcode, 7);
				importparameter.setHeaderfield(HEADERFIELD.lon, 10);
				importparameter.setHeaderfield(HEADERFIELD.lat, 11);
				
				CsvReader csvreader = new CsvReader(importparameter);
				Map<Municipality, HousenumberList> housenumberlists = csvreader.execute();
				for (Map.Entry<Municipality, HousenumberList> listentry : housenumberlists.entrySet()) {
					Municipality municipality = listentry.getKey();
					if (1 == 0) {	// actualy hard coded for Switzerland Kanton Basel-Stadt in 2018-03:
					if(municipality.getName().equals("Basel")) 
						municipality.setOfficialRef("2701");
					else if(municipality.getName().equals("Bettingen")) 
						municipality.setOfficialRef("2702");
					else if(municipality.getName().equals("Riehen"))
						municipality.setOfficialRef("2703");
					else if(municipality.getName().equals("Münchenstein"))
						continue;
					else
						System.out.println("unknown City: " + municipality.getName());
					}
					HousenumberList housenumberlist = listentry.getValue();
					if (1 == 0) {	// actualy hard coded for Switzerland Kanton Basel-Stadt in 2018-03:
									// expand the abbreviated street names
						Map<String, ImportAddress> housenumbers = housenumberlist.getHousenumbers();
						for(Map.Entry<String, ImportAddress> housenumberentry : housenumbers.entrySet()) {
							String key = housenumberentry.getKey();
							ImportAddress housenumber = housenumberentry.getValue();
							if(housenumber.getStreet().endsWith("str."))
								housenumber.setStreet(housenumber.getStreet().replace("str.",  "strasse"));
							if(housenumber.getStreet().endsWith("Str."))
								housenumber.setStreet(housenumber.getStreet().replace("Str.",  "Strasse"));
						}
					}
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

		    } catch (FileNotFoundException e) {
		      e.printStackTrace();
		    } catch (IOException e) {
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
