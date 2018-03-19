package de.regioosm.housenumbercore.imports;
/*
 * OFFEN 17.06.2014: Koordinaten übernehmen aus Inputfile, 
 * 					z.B. Feiberg: select st_astext(st_transform(st_setsrid(st_point(4594534.685,5644172.396),31468),4326));   //GK4
 * 
 * TODO OFFEN: manchmal werden Suffixe in Klammern ergänzt, um mehrfache Straßennamen abzugrenzen, siehe aktuell in DB
 * select st.stadt, strasse  from strasse as s, stadt as st, stadt_hausnummern as sh where st.id = sh.stadt_id and s.id = sh.strasse_id and strasse like '%(%' group by st.stadt,strasse order by st.stadt,strasse;
 * 
	V1.1, 16.02.2011, Dietmar Seifert
		*	Anpassung der Tabellen für allgemeine Nutzung; Ergänzung Tabellen land und stadt
		*	Datensätze in land und stadt werden ergänzt, wenn noch nicht vorhanden

	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Augsburg" "09761" "Stadtvermessungsamt-Hausnummern.txt"
	Aufruf: java import_stadtstrassen "Bundesrepublik Deutschland" "Kaufbeuren" "09762" "Stadt-Kaufbeuren-Hausnummern.txt"
	
	
	
	03.02.2014: Import Berlin: Anpassungen in der Datei: 
		ISO8859-1 => UTF-8
		; => \t
		Überschriftszeile ergänzt:
		 
*/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.Country;
import de.regioosm.housenumbercore.util.CsvReader;
import de.regioosm.housenumbercore.util.HousenumberList;
import de.regioosm.housenumbercore.util.ImportAddress;
import de.regioosm.housenumbercore.util.Municipality;
import de.regioosm.housenumbercore.util.CsvReader.HEADERFIELD;

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

		if ((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-land 'Bundesrepublik Deutschland' wenn fehlend");
			System.out.println("-stadt Stadtname");
			System.out.println("-ags 8stelligeramtlicherGemeindeschlüssel z.B. 09761000");
			System.out.println("-koordsystem srid z.B. 31468 für GK4");
			System.out.println("-koordquelle z.B. 'Stadt xy'");
			System.out.println("-datei importdateiname");
			System.out.println("-municipalityidfile filename with municipalty-id => municipality name, if in main -datei file is only municipality-id, not the municipality name itself: Column 1 has fix id, Column 2 has fix municipality name, file is UTF-8, text-format and tab-separated");
			System.out.println("-submunicipalityidfile filename with SUBmunicipalty-id => SUBmunicipality name, if in main -datei file is only submunicipality-id, not the submunicipality name itself: Column 1 has fix municipality id, Column 2 has fix submunicipality id and Column 3 has fix submunicipality name. File is UTF-8, text-format and tab-separated");
			System.out.println("-streetidfile filename with street-id => streetname, if in main -datei file is only street-id, not the street name itself: Column 1 has fix id, Column 2 has fix streetname, file is UTF-8, text-format and tab-separated");
			System.out.println("-hausnummeradditionseparator zeichen");
			System.out.println("-hausnummeradditionseparator2 zeichen");
			System.out.println("-feldseparator zeichen");
			System.out.print("-subgebieteaktiv ja|nein (default nein)");
			System.out.println("Liste enthält keine Stadtteilzuordnungen zur jeweiligen Hausnummer");
			System.out.println("");
			System.out.println("Importfile must have commentline in line 1, starting with #");
			System.out.println("columns need values in any order:   Stadt	Straße   Hausnummer    Hausnummerzusatz   Hausnummerzusatz2   Bemerkung   Subid   Laengengrad   Breitengrad");
			return;
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			System.out.println("args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}

		String strasseninhaltschreibweise = "";

		String fieldSeparator = "\t";

		String parameterLand = "Bundesrepublik Deutschland";
		String parameterStadt = "";
		String parameterAgs = "";
		String parameterQuellSrid = "";
		String parameterQuellText = "";
		String parameterImportdateiname = "";
		String parameterMunicipalityIdListfilename = "";
		String parameterSubMunicipalityIdListfilename = "";
		String parameterStreetIdListfilename = "";
		boolean parameterSubareaActive = false;
		String parameterHousenumberadditionseparator = "";
		String parameterHousenumberadditionseparator2 = "-";

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
				if (args[argsi].equals("-koordquelle")) {
					parameterQuellText = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-datei")) {
					parameterImportdateiname = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-municipalityidfile")) {
					parameterMunicipalityIdListfilename = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-submunicipalityidfile")) {
					parameterSubMunicipalityIdListfilename = args[argsi + 1];
					argsOkCount += 2;
				}
				if (args[argsi].equals("-streetidfile")) {
					parameterStreetIdListfilename = args[argsi + 1];
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
			}
			if (argsOkCount != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}


		
		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");
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
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}

			Municipality municipality = null;
			Municipality.connectDB(housenumberConn);

			int numbermunicipalities;
			try {
				numbermunicipalities = Municipality.search(parameterLand, parameterStadt, parameterAgs, "");
			} catch (Exception e1) {
				System.out.println("Error ocurred, when tried to get the municipality");
				System.out.println("Error details: " + e1.toString());
				return;
			}
			if(numbermunicipalities == 0) {
				try {
					municipality = new Municipality(parameterLand, parameterStadt, parameterAgs);
					municipality.store();
				} catch (Exception e) {
					System.out.println("Error ocurred, when tried to create municipality in database.");
					System.out.println("Error details: " + e.toString());
					return;
				}
			} else if(numbermunicipalities > 1) {
				while((municipality = Municipality.next()) != null) {
					System.out.println("Hit " + municipality.toString());
				}
				return;
			}

			municipality = Municipality.next();
			System.out.println("municipality already exists, therefor import of liste can be done");
			System.out.println("here is the found municipality with its properties: " + municipality.toString());

			HousenumberList housenumberlist = new HousenumberList();
			HousenumberList.connectDB(housenumberConn);
			
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
					housenumberlist.setMunicipalityIDList(municipalityIdList);
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
					housenumberlist.setSubareaMunicipalityIDList(submunicipalityIdList);
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
					housenumberlist.setStreetIDList(streetIdList);
				}

				housenumberlist.setMunicipality(municipality);
				housenumberlist.setSubareaActive(parameterSubareaActive);
				housenumberlist.setSourceCoordinateSystem(parameterQuellSrid);
				housenumberlist.setSourceGeocoordinateText(parameterQuellText);
				housenumberlist.setImportfile(parameterImportdateiname);
				housenumberlist.setFieldseparators(parameterHousenumberadditionseparator,
					parameterHousenumberadditionseparator2);
				
				CsvReader csvreader = new CsvReader(housenumberlist, "ISO-8859-1");	// "UTF-8"
csvreader.setHeaderfield(HEADERFIELD.street, 3);
csvreader.setHeaderfield(HEADERFIELD.housenumber, 4);
csvreader.setHeaderfield(HEADERFIELD.housenumberaddition, 5);
				ImportAddress address = null;
				try {
					while((address = csvreader.next()) != null) {
						housenumberlist.addHousenumber(address);
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				housenumberlist.storeToDB();

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
