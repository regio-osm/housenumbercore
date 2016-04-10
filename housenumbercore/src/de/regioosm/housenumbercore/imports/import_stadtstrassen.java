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

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;


import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.ImportHausnummerliste;

/**
 * Import or update of a housenumber list from a municipality.
 * @author Dietmar Seifert
 *
 */
public class import_stadtstrassen {

	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	/**
	 * DB connection to Hausnummern
	 */
	private static Connection conHausnummern = null;
	/**
	 * collect all housenumbers, as found in input file. Later, the housenumbers will be stored from this structure to DB. 
	 */
	private static ImportHausnummerliste housenumberlist = new ImportHausnummerliste();
	/**
	 * signals not set the id of a city.
	 */
	private static final long STADTIDUNSET = -1L;
	/**
	 * signal not set of the id of a country.
	 */
	private static final long LANDIDUNSET = -1L;
	/**
	 * minimum number of columns in the input file.
	 */
	private static final int FILECOLUMNMINLENGTH = 2; 

	private static void Importlog(String protocolType, String shortReason, String description) {
		PrintWriter logfile = null;
		String protocolfilename = configuration.application_datadir + File.separator + "import-" + protocolType + ".log";
		String outputtext = "";
		try {
			logfile = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(protocolfilename, true),StandardCharsets.UTF_8)));
			outputtext += new java.util.Date().toString() 
				+ "\t" + shortReason
				+ "\t" + description;
			logfile.println(outputtext);
			logfile.close();
		} catch (IOException ioerror) {
			System.out.println("ERROR: couldn't open file to write, filename was ===" + protocolfilename + "===");
			ioerror.printStackTrace();
		}
	}

	
	private static ArrayList<String> uppercaselist = new ArrayList<>();
	private static ArrayList<String> lowercaselist = new ArrayList<>();

	
	
	public static String Street_UpperLower(String street) {
		Pattern wildcardPattern = Pattern.compile("([A-ZÄÉÈÖÜĂÂÎŞŢ])([A-ZÄÉÈÖÜßĂÂÎŞŢ]*)");				// A to Z and all 5 special romania chars http://de.wikipedia.org/wiki/Rum%C3%A4nisches_Alphabet#Alphabet_und_Aussprache

		Matcher match = wildcardPattern.matcher(street);
		StringBuffer sb = new StringBuffer();
		boolean matchFind = match.find();
		String foundstring = "";
		boolean matchFixToLowerCase = false;
		boolean matchFixToUpperCase = false;
		while(matchFind) {
			matchFixToLowerCase = false;
			matchFixToUpperCase = false;
			foundstring = match.group(1);
			String replacetext = match.group(1);
			if(match.groupCount() >= 2) {
				foundstring += match.group(2);
				if(lowercaselist.contains(foundstring.toLowerCase())) {
					replacetext = match.group(1).toLowerCase() + match.group(2).toLowerCase();
					matchFixToLowerCase = true;
				}
				if(uppercaselist.contains(foundstring.toUpperCase())) {
					replacetext = match.group(1).toUpperCase() + match.group(2).toUpperCase();
					matchFixToUpperCase = true;
				}
				if(!matchFixToLowerCase && !matchFixToUpperCase)
					replacetext += match.group(2).toLowerCase();
			}
			match.appendReplacement(sb,replacetext);
			matchFind = match.find();
		}
		match.appendTail(sb);
		return sb.toString();
	}

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
		String parameterAktiveStadtteileYesno = "n";
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
						parameterAktiveStadtteileYesno = "y";
					} else {
						parameterAktiveStadtteileYesno = "n";
					}
					argsOkCount += 2;
				}
			}
			if (argsOkCount != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}

		if(parameterLand.equals("Italia")) {
			lowercaselist.add("al");
			lowercaselist.add("alla");
			lowercaselist.add("alle");
			lowercaselist.add("da");
			lowercaselist.add("de");
			lowercaselist.add("dei");
			lowercaselist.add("del");
			lowercaselist.add("dell");
			lowercaselist.add("della");
			lowercaselist.add("delle");
			lowercaselist.add("destro");
			lowercaselist.add("di");
			lowercaselist.add("in");
			lowercaselist.add("sinistro");
			
			uppercaselist.add("II");
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
			conHausnummern = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);

			Statement stmt = conHausnummern.createStatement();

			String dateipfadname = "Stadtvermessungsamt-Hausnummern.txt";
			//String dateipfadname = "Stadt-Kaufbeuren-Hausnummern.txt";
			if (parameterImportdateiname != null) {
				dateipfadname = parameterImportdateiname;
			}
			dateipfadname = configuration.application_datadir + "/" + dateipfadname;

			try {
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
				}

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
				}

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
				}

				BufferedReader filereader = new BufferedReader(new FileReader(dateipfadname));

					// Hol landId. Wenn nicht vorhanden, erzeuge Datensatz und hol dann die landId
				long landId = LANDIDUNSET;
				
				String sqlbefehlLand = "SELECT id  FROM land WHERE land = '" + parameterLand + "'";
				System.out.println("Select-Anfrage ===" + sqlbefehlLand + "=== ...");
				ResultSet rsLand = stmt.executeQuery(sqlbefehlLand);
				if (rsLand.next()) {
					landId = rsLand.getLong("id");
				} else {
					stmt.executeUpdate("INSERT INTO land (land) VALUES ('" + parameterLand + "');");
					rsLand = stmt.executeQuery(sqlbefehlLand);
					if (rsLand.next()) {
						landId = rsLand.getLong("id");
					}
				}

	
				if (landId == LANDIDUNSET) {
					System.out.println("FEHLER FEHLER: landId konnte nicht ermittelt werden, ABBRUCH");
					filereader.close();
					return;
				}
				housenumberlist.land = parameterLand;
				housenumberlist.land_dbid = landId;
				housenumberlist.subbereichescharf = parameterAktiveStadtteileYesno;
				housenumberlist.hausnummerzusatzgrosskleinrelevant = "n";
				
				String dateizeile;
				long zeilenr = 0;

				boolean geocoordindatesavailable = false;
				long strasseId = 0;
				int spaltenindexStadt = -1;
				int spaltenindexAgs = -1;
				int spaltenindexStrasse = -1;
				int spaltenindexStrasseId = -1;
				int spaltenindexPostcode = -1;
				int spaltenindexHausnummer = -1;
				int spaltenindexHausnummerzusatz = -1;
				int spaltenindexHausnummerzusatz2 = -1;
				int spaltenindexBemerkung = -1;
				int spaltenindexSubid = -1;
				int spaltenindexQuellSrid = -1;
				int spaltenindexLaengengrad = -1;
				int spaltenindexBreitengrad = -1;
				String stadt_zuletzt = "";
				String stadt = "";
				String ags = "";
				String ags_zuletzt = "";
				String strasse = "";
				String postcode = "";
				String hausnummer = "";
				String bemerkung = "";
				String quellSrid = "";
				double laengengrad = 0f;
				double breitengrad = 0f;
				boolean addedHousenumber = false;

				String subid = "-1";
				while ((dateizeile = filereader.readLine()) != null) {
					zeilenr++;
					
					if (dateizeile.equals("")) {
						continue;
					}
					//System.out.println("Position header: " + dateizeile.indexOf("#"));
					//System.out.println(" zeichen 0: " + dateizeile.codePointAt(0) + "   " + dateizeile.codePointAt(1) + "   " + dateizeile.codePointAt(2));
					if (dateizeile.indexOf("#") == 0) {
						if (zeilenr == 1L) {
							dateizeile = dateizeile.substring(1);
							String[] kopfspalten = dateizeile.split(fieldSeparator);
							for (int spaltei = 0; spaltei < kopfspalten.length; spaltei++) {
								if(		kopfspalten[spaltei].toLowerCase().equals("stadt")
									||	kopfspalten[spaltei].toLowerCase().equals("addr:city")
									||	kopfspalten[spaltei].toLowerCase().equals("gemeinde")) {
									spaltenindexStadt = spaltei;
								}
								if(		kopfspalten[spaltei].toLowerCase().equals("stadtid")
									||	kopfspalten[spaltei].toLowerCase().equals("gemeindeid")
									||	kopfspalten[spaltei].toLowerCase().equals("gemeinde_id")
									||	kopfspalten[spaltei].toLowerCase().equals("gemeinde-id")) {
									spaltenindexAgs = spaltei;
								}
								if (	kopfspalten[spaltei].toLowerCase().equals("straße")
									||	kopfspalten[spaltei].toLowerCase().equals("strasse")) {
									spaltenindexStrasse = spaltei;
								}
								if (	kopfspalten[spaltei].toLowerCase().equals("straße-id")
									||	kopfspalten[spaltei].toLowerCase().equals("straßeid")
									||	kopfspalten[spaltei].toLowerCase().equals("strasseid")
									||	kopfspalten[spaltei].toLowerCase().equals("strasse-id")) {
										spaltenindexStrasseId = spaltei;
								}
								if (	kopfspalten[spaltei].toLowerCase().equals("straßeupper")
									||	kopfspalten[spaltei].toLowerCase().equals("strasseupper")
									||	kopfspalten[spaltei].toLowerCase().equals("strasse")) {
										spaltenindexStrasse = spaltei;
										strasseninhaltschreibweise = "upper";
								}
								if (	kopfspalten[spaltei].toLowerCase().equals("postcode")
									||	kopfspalten[spaltei].toLowerCase().equals("plz")
									||	kopfspalten[spaltei].toLowerCase().equals("postleitzahl")) {
									spaltenindexPostcode = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("hausnummer")) {
									spaltenindexHausnummer = spaltei;
								}
								if(		(kopfspalten[spaltei].toLowerCase().equals("hausnummerzusatz"))
									||	(kopfspalten[spaltei].toLowerCase().equals("hausnummernzusatz"))) {
									spaltenindexHausnummerzusatz = spaltei;
								}
								if(		(kopfspalten[spaltei].toLowerCase().equals("hausnummerzusatz2"))
									||	(kopfspalten[spaltei].toLowerCase().equals("hausnummernzusatz2"))) {
									spaltenindexHausnummerzusatz2 = spaltei;
								}
								if (	kopfspalten[spaltei].toLowerCase().equals("bemerkung")
									||	kopfspalten[spaltei].toLowerCase().equals("bemerkungen")) {
									spaltenindexBemerkung = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("koordindatensystem") ||
									kopfspalten[spaltei].toLowerCase().equals("epsg") ||
									kopfspalten[spaltei].toLowerCase().equals("srid")) {
									spaltenindexQuellSrid = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("lon") ||
									kopfspalten[spaltei].toLowerCase().equals("rw") ||
									kopfspalten[spaltei].toLowerCase().equals("laengengrad") ||
									kopfspalten[spaltei].toLowerCase().equals("längengrad")) {
									spaltenindexLaengengrad = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("lat") ||
									kopfspalten[spaltei].toLowerCase().equals("hw") ||
									kopfspalten[spaltei].toLowerCase().equals("breitengrad")) {
									spaltenindexBreitengrad = spaltei;
								}
								if(		kopfspalten[spaltei].toLowerCase().equals("subid")
									|| 	kopfspalten[spaltei].toLowerCase().equals("sub_id")
									|| 	kopfspalten[spaltei].toLowerCase().equals("sub-id")) {
									spaltenindexSubid = spaltei;
								}
							}

							System.out.print("Kopfzeile analysiert ...");
							System.out.print("      Spalte-Stadt: " + spaltenindexStadt);
							System.out.print("      Spalte-Stadtid: " + spaltenindexAgs);
							System.out.print("      Spalte-Straße: " + spaltenindexStrasse);
							if(spaltenindexStrasseId != -1)
								System.out.print("  Spalte-Straße-Id: " + spaltenindexStrasseId);
							if(spaltenindexPostcode != -1)
								System.out.print("  Spalte-Postcode: " + spaltenindexPostcode);
							System.out.print("  Spalte-Hausnummer: " + spaltenindexHausnummer);
							if(spaltenindexHausnummerzusatz != -1)
								System.out.print("  Spalte-Hausnummerzusatz: " + spaltenindexHausnummerzusatz);
							if(spaltenindexHausnummerzusatz2 != -1)
								System.out.print("  Spalte-Hausnummerzusatz2: " + spaltenindexHausnummerzusatz2);
							System.out.println("   Spalte-Subid: " + spaltenindexSubid);
							if(spaltenindexLaengengrad != -1)
								System.out.println("   Spalte-Längengrad: " + spaltenindexLaengengrad);
							if(spaltenindexBreitengrad != -1)
								System.out.println("   Spalte-Breitengrad: " + spaltenindexBreitengrad);
							if(spaltenindexBemerkung != -1)
								System.out.println("   Spalte-Bemerkungen: " + spaltenindexBemerkung);
						} else {
							System.out.println("Dateizeile #" + zeilenr + "  war sonstige Kommentarzeile ===" + dateizeile + "===");
						}
						continue;
					}

					System.out.println("Dateizeile # " + zeilenr + " ===" + dateizeile + "===");

					String[] spalten = dateizeile.split("\t");

						// Zeile mit falscher Spaltenanzahl überspringen
					if (spalten.length < FILECOLUMNMINLENGTH) {
						Importlog("Info","Zeile_ungueltig", "zuwenig Spalten in Zeile # " + zeilenr + ", Dateizeile ===" + dateizeile + "===");
						continue;
					}

					stadt_zuletzt = stadt;
					stadt = "";
					if((spalten.length > spaltenindexStadt) && (spaltenindexStadt != -1)) {
						stadt = spalten[spaltenindexStadt].trim();
						if( (! stadt.equals(stadt_zuletzt)) && (! stadt_zuletzt.equals(""))) {
							if(housenumberlist.count() > 0) {
								housenumberlist.stadt = stadt_zuletzt;
								if(! ags_zuletzt.equals(""))
									housenumberlist.stadt_id = ags_zuletzt;
								if(geocoordindatesavailable)
									housenumberlist.officialgeocoordinates = "y";
								else
									housenumberlist.officialgeocoordinates = "n";
								housenumberlist.storeToDB();
								housenumberlist.clear();
							}
						}
					}

					ags_zuletzt = ags;
					ags = "";
					if((spalten.length > spaltenindexAgs) && (spaltenindexAgs != -1)) {
						ags = spalten[spaltenindexAgs].trim();
						if(stadt.equals("") && !parameterMunicipalityIdListfilename.equals("")) {
							if (municipalityIdList.get(ags) != null) {
								stadt = municipalityIdList.get(ags).trim();
							} else {
								System.out.println("ERROR: missing municipality ID entry for Id ===" + ags + "===");
							}
						}
						if( (! ags.equals(ags_zuletzt)) && (! ags_zuletzt.equals(""))) {
							if(housenumberlist.count() > 0) {
								housenumberlist.stadt = stadt_zuletzt;
								if(! ags_zuletzt.equals(""))
									housenumberlist.stadt_id = ags_zuletzt;
								if(geocoordindatesavailable)
									housenumberlist.officialgeocoordinates = "y";
								else
									housenumberlist.officialgeocoordinates = "n";
								housenumberlist.storeToDB();
								housenumberlist.clear();
							}
						}
					}

					strasse = "";
					if((spalten.length > spaltenindexStrasse) && (spaltenindexStrasse != -1)) {
						strasse = spalten[spaltenindexStrasse].trim();
						if(strasseninhaltschreibweise.equals("upper"))
							strasse = Street_UpperLower(strasse);
					}
					if((spalten.length > spaltenindexStrasseId) && (spaltenindexStrasseId != -1)) {
						String strasseidkey = spalten[spaltenindexStrasseId];			//TODO specific to Austria, must be defined due to configuration: ags +
						if(!parameterStreetIdListfilename.equals("")) {
							if (streetIdList.get(strasseidkey) != null) {
								strasse = streetIdList.get(strasseidkey).trim();
								if(strasseninhaltschreibweise.equals("upper"))
									strasse = Street_UpperLower(strasse);
							} else {
								System.out.println("ERROR: missing street ID entry for Id ===" + spaltenindexStrasseId + "===");
							}
						}
					}
					postcode = "";
					if((spalten.length > spaltenindexPostcode) && (spaltenindexPostcode != -1)) {
						postcode = spalten[spaltenindexPostcode].trim();
					}
					hausnummer = "";
					if((spalten.length > spaltenindexHausnummer) && (spaltenindexHausnummer != -1)) {
						hausnummer = spalten[spaltenindexHausnummer].trim();
					}
					if((spalten.length > spaltenindexHausnummerzusatz) && (spaltenindexHausnummerzusatz != -1)) {
						if(! spalten[spaltenindexHausnummerzusatz].trim().equals(""))
							hausnummer += parameterHousenumberadditionseparator + spalten[spaltenindexHausnummerzusatz].trim();
					}
					if((spalten.length > spaltenindexHausnummerzusatz2) && (spaltenindexHausnummerzusatz2 != -1)) {
						if(! spalten[spaltenindexHausnummerzusatz2].trim().equals(""))
							hausnummer += parameterHousenumberadditionseparator2 + spalten[spaltenindexHausnummerzusatz2].trim();
					}
					
					bemerkung = "";
					if((spalten.length > spaltenindexBemerkung) && (spaltenindexBemerkung != -1)) {
						bemerkung = spalten[spaltenindexBemerkung].trim();
					}

					subid = "-1";
					if ((spalten.length > spaltenindexSubid) && (spaltenindexSubid != -1)) {
						subid = spalten[spaltenindexSubid];
						if(!parameterSubMunicipalityIdListfilename.equals("")) {
							if(!ags.equals("")) {
								String submunicipalitykey = ags + subid;
								if (submunicipalityIdList.get(submunicipalitykey) != null) {
									subid = submunicipalityIdList.get(submunicipalitykey);
								}
							} else {
								subid = "-1";
								System.out.println("Error: ags is empty, when tried to get submunicipality entry and subid was ===" + subid + "===");
							}
						}
					}

					quellSrid = parameterQuellSrid;
					if ((spalten.length > spaltenindexQuellSrid) && (spaltenindexQuellSrid != -1)) {
						quellSrid = spalten[spaltenindexQuellSrid];
					}
					laengengrad = 0d;
					if ((spalten.length > spaltenindexLaengengrad) && (spaltenindexLaengengrad != -1)) {
						String localstring = spalten[spaltenindexLaengengrad].trim();
						if(!localstring.equals("")) {
							try {
								localstring = localstring.replace(",",".");
								laengengrad = Double.parseDouble(localstring);
								geocoordindatesavailable = true;
							} catch (NumberFormatException nofloat) {
								System.out.println("Warning: cannot convert input Längengrad value as float ===" + spalten[spaltenindexLaengengrad].trim() + "===  preconvert to number ==="+localstring +"===");
								System.out.println(" (cont) error stack follows " + nofloat.toString());
							}
						}
					}
					breitengrad = 0d;
					if ((spalten.length > spaltenindexBreitengrad) && (spaltenindexBreitengrad != -1)) {
						String localstring = spalten[spaltenindexBreitengrad].trim();
						if(!localstring.equals("")) {
							try {
								localstring = localstring.replace(",",".");
								breitengrad = Double.parseDouble(localstring);
								geocoordindatesavailable = true;
							} catch (NumberFormatException nofloat) {
								System.out.println("Warning: cannot convert input Breitengrad value as float ===" + spalten[spaltenindexBreitengrad].trim() + "===  preconvert to number ==="+localstring +"===");
								System.out.println(" (cont) error stack follows " + nofloat.toString());
							}
						}
					}
					
						// Zeile mit richtiger Spaltenanzahl, aber leere Spalte(n) überspringen
					if (stadt.equals("") && (parameterStadt.equals(""))) {
//						Importlog("Error",  "Missing_Municipality", "no municipality name in fileline # " + zeilenr + ", content ===" + dateizeile + "===, will be ignored");
//						continue;
					}
					if (strasse.equals("")) {
						if(!strasse.equals("")) {
							strasse = stadt;
							Importlog("Warning",  "Missing_Street_Added", "no street name in fileline # " + zeilenr + ", content ===" + dateizeile + "===, work around");
						} else {
							Importlog("Warning",  "Missing_Street", "no street name in fileline # " + zeilenr + ", content ===" + dateizeile + "===, will be ignored");
							continue;
						}
					}
					if (hausnummer.equals("")) {
						Importlog("Warning",  "Missing_Housenumber", "no housenumber in fileline # " + zeilenr + ", content ===" + dateizeile + "===, will be ignored");
						continue;
					}
					System.out.print("# " + zeilenr + "  Straße ===" + strasse);
					System.out.print(";   subid ===" + subid + "===");
					System.out.println(";   Hausnummer ===" + hausnummer + "===");

					strasseId = 0L;
					addedHousenumber = housenumberlist.addHousenumber(strasse,  strasseId, subid,  postcode, hausnummer, bemerkung, laengengrad, breitengrad, quellSrid, parameterQuellText);
					if(! addedHousenumber) {
						Importlog("Warning",  "Identical_Address", "duplicated Address in fileline # " + zeilenr + ", Dateizeile ===" + dateizeile + "===");
					}
				}

				if(housenumberlist.count() > 0) {
					if(! stadt.equals(""))
						housenumberlist.stadt = stadt;
					else
						housenumberlist.stadt = parameterStadt;
					if(! ags.equals(""))
						housenumberlist.stadt_id = ags;
					else
						housenumberlist.stadt_id  = parameterAgs;
					if(geocoordindatesavailable)
						housenumberlist.officialgeocoordinates = "y";
					else
						housenumberlist.officialgeocoordinates = "n";
					housenumberlist.storeToDB();
					housenumberlist.clear();
				}

				filereader.close();


		    } catch (FileNotFoundException e) {
		      e.printStackTrace();
		    } catch (IOException e) {
		      e.printStackTrace();
		    }

			stmt.close();
			conHausnummern.close();
		}
		catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}


}
