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
			System.out.println("-convertstreetupperlower yes|no: convert street name to upper lower, if complete upper or complete lower case letters (default no)");
			System.out.println("-c columnno=destination: which column (columnno, starting with 1) contains which destination field, see below.");
			System.out.println("   This parameter can be used several times");;
			System.out.println("   destination            meaning");;
			System.out.println("   municipality           municipality name");
			System.out.println("   municipalityref        reference id for municipality. You must know, which reference within a country will be used.");
			System.out.println("   subarea                y");
			System.out.println("   subareaid              y");
			System.out.println("   street                 street name");
			System.out.println("   housenumber            housenumber (completely or only full number. For additions, see two more destination fields");
			System.out.println("   housenumberaddition    housenumberaddition: will be appended to housenumber, with separator, as defined in -housenumberseparator");
			System.out.println("   housenumberaddition2   second housenumberaddition: will be appended to housenumber and housenumberaddition, with separator, as defined in -housenumberseparator2");
			System.out.println("   postcode               postcode");
			System.out.println("   lon                    lon, in source coordinate system");
			System.out.println("   lat                    lat, in source coordinate system");
			System.out.println("   sourcesrid             source coordinate system");
			System.out.println("   note                   official note to an address, for example, special address, or house with address not built yet");
			
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
		boolean parameterConvertstreetupperlower = false;


		HashMap<String,String> municipalityIdList = new HashMap<String,String>();
		HashMap<String,String> submunicipalityIdList = new HashMap<String,String>();
		HashMap<String,String> streetIdList = new HashMap<String,String>();
		
		CsvImportparameter importparameter = new CsvImportparameter();
		
		if (args.length >= 1) {
			int argsOkCount = 0;
			String[] argsCopy = args.clone();
			for (int argsi = 0; argsi < args.length; argsi += 2) {
				System.out.print(" args pair analysing #: " + argsi + "  ===" + args[argsi] + "===");
				if (args.length > (argsi + 1)) {
					System.out.print("  args #+1: " + (argsi + 1) + "   ===" + args[argsi + 1] + "===");
				}
				System.out.println("");

				if (args[argsi].equals("-country")) {
					paramCountry = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-municipality")) {
					paramMunicipality = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-municipalityref")) {
					paramMunicipalityRef = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-coordinatesystem")) {
					paramSourceSrid = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
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
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-file")) {
					paramImportfileName = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
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
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-municipalityidfile")) {
					parameterMunicipalityIdListfilename = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-submunicipalityidfile")) {
					parameterSubMunicipalityIdListfilename = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-streetidfile")) {
					parameterStreetIdListfilename = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-housenumberseparator")) {
					parameterHousenumberadditionseparator = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-housenumberseparator2")) {
					parameterHousenumberadditionseparator2 = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-subareaactive")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterSubareaActive = true;
					} else {
						parameterSubareaActive = false;
					}
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-fieldseparator")) {
					fieldSeparator = args[argsi + 1];
					System.out.println("Info: explicit set field separator to '" + fieldSeparator + "'");
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-listurl")) {
					parameterSourcelistUrl = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-copyright")) {
					parameterSourcelistCopyright = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-useage")) {
					parameterSourcelistUseage = args[argsi + 1];
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-listcontenttimestamp")) {
					try {
						parameterSourcelistContentdate = dateformatUS.parse(args[argsi + 1]);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-listfiletimestamp")) {
					try {
						parameterSourcelistFiledate = dateformatUS.parse(args[argsi + 1]);
					} catch (ParseException e) {
						e.printStackTrace();
					}
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-coordinatesosmimportable")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterOfficialgeocoordinates = true;
					} else {
						parameterOfficialgeocoordinates = false;
					}
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-convertstreetupperlower")) {
					String yesno = args[argsi + 1].toLowerCase().substring(0,1);
					if (yesno.equals("y") || yesno.equals("j")) {
						parameterConvertstreetupperlower = true;
					} else {
						parameterConvertstreetupperlower = false;
					}
					argsOkCount += 2;
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
				} else if (args[argsi].equals("-c")) {
					String argvalue = args[argsi + 1].toLowerCase();
					if((argvalue.length() >= 2) && (argvalue.indexOf("=") != -1)) {
						int columnNo = Integer.parseInt(argvalue.substring(0, argvalue.indexOf("=")));
						String columnContent = argvalue.substring(argvalue.indexOf("=") + 1);
						if((columnNo > 0) && !columnContent.equals("")) {
							if(columnContent.equals("municipality"))
								importparameter.setHeaderfield(HEADERFIELD.municipality, columnNo - 1);
							else if(columnContent.equals("municipalityref"))
								importparameter.setHeaderfield(HEADERFIELD.municipalityref, columnNo - 1);
							else if(columnContent.equals("street"))
								importparameter.setHeaderfield(HEADERFIELD.street, columnNo - 1);
							else if(columnContent.equals("housenumber"))
								importparameter.setHeaderfield(HEADERFIELD.housenumber, columnNo - 1);
							else if(columnContent.equals("housenumberaddition"))
								importparameter.setHeaderfield(HEADERFIELD.housenumberaddition, columnNo - 1);
							else if(columnContent.equals("housenumberaddition2"))
								importparameter.setHeaderfield(HEADERFIELD.housenumberaddition2, columnNo - 1);
							else if(columnContent.equals("postcode"))
								importparameter.setHeaderfield(HEADERFIELD.postcode, columnNo - 1);
							else if(columnContent.equals("lon"))
								importparameter.setHeaderfield(HEADERFIELD.lon, columnNo - 1);
							else if(columnContent.equals("lat"))
								importparameter.setHeaderfield(HEADERFIELD.lat, columnNo - 1);
							else if(columnContent.equals("subarea"))
								importparameter.setHeaderfield(HEADERFIELD.subarea, columnNo - 1);
							else if(columnContent.equals("subareaid"))
								importparameter.setHeaderfield(HEADERFIELD.subareaid, columnNo - 1);
							else if(columnContent.equals("sourcesrid"))
								importparameter.setHeaderfield(HEADERFIELD.sourcesrid, columnNo - 1);
							else if(columnContent.equals("note"))
								importparameter.setHeaderfield(HEADERFIELD.note, columnNo - 1);
							else {
								System.out.println("unknown destination '" + columnContent +
									"', please correct and try again");
								return;
							}
						} else {
							System.out.println("invalid parameter value -c '" + argvalue + 
								"', either column no is below 1 or destination behind = is missing, please correct and try again");
							return;
						}
					}
					argsCopy[argsi] = null;
					argsCopy[argsi+1] = null;
					argsOkCount += 2;
				} else {
					System.out.println("unknown program parameter '" + args[argsi] + "===");
				}
			}
			if (argsOkCount != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, please check following unknown parameters");
				for (int argsi = 0; argsi < argsCopy.length; argsi += 2) {
					System.out.print(" args pair analysing #: " + argsi + "  =" + argsCopy[argsi] + ": ");
					if((argsCopy[argsi] == null) && (argsCopy[argsi + 1] == null)) {
						System.out.println(" successfully read");
						continue;
					}
					if (argsCopy[argsi] == null) 
						System.out.print("parameter key has been successfully read");
					if (argsCopy[argsi + 1] == null) 
							System.out.print("parameter value has been successfully read");
					System.out.println("");
				}
				return;
			}
		}
		System.out.println(importparameter.printHeaderfields());
		
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Postgres-Driver couldn't be found. Program STOPS");
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
				System.out.println("after Municipality.connectDB");

				try {
					importparameter.setCountrycode(Country.getCountryShortname(paramCountry));
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Can't get countrycode for input parameter '" +
						paramCountry + " from DB, please check country name and repeat program.");
					return;
				}
				System.out.println("after importparameter.setCountrycode");

				//Switzerland: nationwide: -country "Schweiz" -coordinatesystem 2056 -housenumberSeparator "" -subareaactive "n" -file /home/osm/apps/housenumbercore/data/Schweiz/CH-20180325-1253.csv -listurl "https://a.b.c/de.txt" -copyright "Copyright Text xyz" -useage "Nutzungsvereinbarung xy" -listcontenttimestamp 2018-03-01 -listfiletimestamp 2018-03-15 -coordinatesOSMImportable no -filecharset "UTF-8" -useoverpass yes
				//Switzerland Kanton Basel-Stadt: -land "Schweiz" -koordsystem 2056 -housenumberSeparator "" -subgebieteaktiv "n" -datei /home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Schweiz/Basel-Stadt/2018/DM_Datenmarkt/Datenmarkt.csv -listurl "https://basel.txt" -copyright "Copyright basel" -useage "Nutzungsvereinbarung basel" -listcontenttimestamp 2018-03-31 -listfiletimestamp 2018-04-01 -CoordinatesOSMImportable yes
				importparameter.setFieldSeparator(fieldSeparator);
				importparameter.setSubareaActive(parameterSubareaActive);
				importparameter.setSourceCoordinateSystem(paramSourceSrid);
				importparameter.setImportfile(paramImportfileName, Charset.forName(parameterImportfileCharset));
				importparameter.setHousenumberFieldseparators(parameterHousenumberadditionseparator,
					parameterHousenumberadditionseparator2);
				importparameter.convertStreetToUpperLower(parameterConvertstreetupperlower);
				if ( !paramMunicipality.equals("") )
					importparameter.setMunicipality(paramMunicipality);
				if ( !paramMunicipalityRef.equals("") )
					importparameter.setMunicipalityRef(paramMunicipalityRef);

				CsvReader csvreader = new CsvReader(importparameter);
				Map<Municipality, HousenumberList> housenumberlists = csvreader.execute();
				System.out.println("Number of read housenumber lists: " + housenumberlists.size());
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
System.out.println(" vorher getName ===" + municipality.getName() + "===, getref ===" + municipality.getOfficialRef() + "===");
						// if only one housenumberliste was read and there is no name for municipality
						// and no municipality ref, then check, if these two values were given as 
						// program parameter then set the properties to the parameter values
					if ((housenumberlists.size() == 1) && 
						municipality.getName().equals("") &&
						municipality.getOfficialRef().equals("") &
						!paramMunicipality.equals("") &&
						!paramMunicipalityRef.equals("")) {
						municipality.setName(paramMunicipality);
						municipality.setOfficialRef(paramMunicipalityRef);
					}
System.out.println("nachher getName ===" + municipality.getName() + "===, getref ===" + municipality.getOfficialRef() + "===");
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
								int newnumbermunicipalities = 0; 
								try {
									newnumbermunicipalities = Municipality.search(municipality.getCountrycode(), 
										municipality.getName(), municipality.getOfficialRef(), "");
								} catch (Exception e1) {
									System.out.println("Error ocurred, when tried to get new stored municipality");
									System.out.println("Error details: " + e1.toString());
									return;
								}
								if ( newnumbermunicipalities != 1 ) {
									System.out.println("new stored municipality couldn't be found exactly one time, but " + 
										newnumbermunicipalities + ", AN ERROR OCCURED, PROGRA STOPS");
										return;
								} else  {
									municipality = Municipality.next();
									System.out.println("here is the new stored municipality with its properties: " + municipality.toString());
								}								
							} catch (Exception e) {
								System.out.println("Error ocurred, when tried to create municipality in database.");
								System.out.println("Error details: " + e.toString());
								return;
							}
						} else  {
							municipality = Municipality.next();
							System.out.println("municipality already exists, therefor import of list can be done");
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
// ToDo: how to define table jobs, field schedule?
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
