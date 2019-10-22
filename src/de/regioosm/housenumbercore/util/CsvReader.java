package de.regioosm.housenumbercore.util;
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
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.regioosm.housenumbercore.util.CsvImportparameter.HEADERFIELD;

/**
 * Import or update of a housenumber list from a municipality.
 * @author Dietmar Seifert
 *
 */
public class CsvReader {

	private int lineno = 0;
	private BufferedReader filereader = null;

	private CsvImportparameter importparameter = new CsvImportparameter();

	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();


	private Map <String, String> luxembourgLocalityList = new HashMap<> ();

	/**
	 * Reader for a file in CSV format.
	 * Parameter importlist must be filled out with a least importfile name and countrycode
	 * @param importlist
	 * @param charsetname	inputfile character set, for example ISO-8859-1 or UTF-8, see https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html
	 */
	public CsvReader(CsvImportparameter importparameter) {
		if(	(importparameter.getImportfile() == null) ||
			importparameter.getImportfile().equals("")) {
			throw new IllegalArgumentException("At least Importfile must be specified in input parameter importparameter");
		}
		if(	(importparameter.getCountrycode() == null) ||
			importparameter.getCountrycode().equals("")) {
			throw new IllegalArgumentException("At least Country Code must be specified in input parameter importparameter");
		}

		this.importparameter = importparameter;

		if(this.importparameter.getCountrycode().equals("LU"))
			initialiseLuxembourg();
	}

	protected String getLuxembourgMunicipalityforSubarea(String subareaname) {
		
		if(luxembourgLocalityList.get(subareaname) != null)
			return luxembourgLocalityList.get(subareaname);
		else
			return "";
	}
	
	
	private String getFieldContent(String line, HEADERFIELD field) {
		if((line == null) || line.equals(""))
			return "";

		String spalten[] = line.split(importparameter.getFieldSeparator());

		if(importparameter.getHeaderfieldColumn(field) == -1) {
			return "";
		}

		if(spalten.length > importparameter.getHeaderfieldColumn(field)) {
			String content = spalten[importparameter.getHeaderfieldColumn(field)];
			if(content.length() > 0)
				content = content.trim();
			return content;
		} else {
			System.out.println("WARNING: line #" + lineno + " has " + spalten.length + " columns, " +
				" field ===" + field.toString() + "===, is in column " +
				importparameter.getHeaderfieldColumn(field) + ", theres line will be ignored, is ===" +
				line + "===");
		}
		
		return "";
	}

	private String getFieldContent(String line, int columnno) {
		if((line == null) || line.equals(""))
			return "";

		String spalten[] = line.split(importparameter.getFieldSeparator());

		if( spalten.length > columnno ) {
			String content = spalten[columnno];
			if(content.length() > 0)
				content = content.trim();
			return content;
		} else {
			System.out.println("WARNING: line #" + lineno + " has " + spalten.length + " columns, " +
				" less than expected. Required column no # " + columnno + " is not available, sorry");
		}
		
		return "";
	}

	private int numberOfOccurences(String text, String search) {
		int count = 0;
		
		int startpos = 0;
		while(text.indexOf(search,startpos) != -1) {
			count++;
			startpos = text.indexOf(search,startpos) + 1;
		}
		return count;
	}



	
	private static ArrayList<String> uppercaselist = new ArrayList<>();
	private static ArrayList<String> lowercaselist = new ArrayList<>();


	private String StreetToUpperLower(String street) {
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
	
	private void analyseHeaderline(String line) {


		if((line == null) || line.equals(""))
			return;

		if(line.indexOf("#") == 0)
			line = line.substring(1);

		String[] kopfspalten = line.split(importparameter.getFieldSeparator());
		for (int spaltei = 0; spaltei < kopfspalten.length; spaltei++) {
			if(	kopfspalten[spaltei].toLowerCase().equals("stadt") || 
				kopfspalten[spaltei].toLowerCase().equals("addr:city") ||
				kopfspalten[spaltei].toLowerCase().equals("gemeinde") ||
				kopfspalten[spaltei].toLowerCase().equals("commune")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.municipality) == -1)
					importparameter.setHeaderfield(HEADERFIELD.municipality, spaltei);
			} else if(	kopfspalten[spaltei].toLowerCase().equals("stadtid") ||
				kopfspalten[spaltei].toLowerCase().equals("gemeindeid") ||
				kopfspalten[spaltei].toLowerCase().equals("gemeinde_id") ||
				kopfspalten[spaltei].toLowerCase().equals("gemeinde-id")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.municipalityref) == -1)
					importparameter.setHeaderfield(HEADERFIELD.municipalityref, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("straße") ||
				kopfspalten[spaltei].toLowerCase().equals("strasse") ||
				kopfspalten[spaltei].toLowerCase().equals("rue")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.street) == -1)
					importparameter.setHeaderfield(HEADERFIELD.street, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("straße-id") ||
				kopfspalten[spaltei].toLowerCase().equals("straßeid") ||
				kopfspalten[spaltei].toLowerCase().equals("strasseid") ||
				kopfspalten[spaltei].toLowerCase().equals("strasse-id") ||
				kopfspalten[spaltei].toLowerCase().equals("id_caclr_rue")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.streetid) == -1)
					importparameter.setHeaderfield(HEADERFIELD.streetid, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("postcode") ||
				kopfspalten[spaltei].toLowerCase().equals("plz") ||
				kopfspalten[spaltei].toLowerCase().equals("postleitzahl") ||
				kopfspalten[spaltei].toLowerCase().equals("code_postal")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.postcode) == -1)
					importparameter.setHeaderfield(HEADERFIELD.postcode, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("hausnummer") ||
				kopfspalten[spaltei].toLowerCase().equals("numero")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.housenumber) == -1)
					importparameter.setHeaderfield(HEADERFIELD.housenumber, spaltei);
			} else if(	kopfspalten[spaltei].toLowerCase().equals("hausnummerzusatz") ||
				kopfspalten[spaltei].toLowerCase().equals("hausnummernzusatz")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.housenumberaddition) == -1)
					importparameter.setHeaderfield(HEADERFIELD.housenumberaddition, spaltei);
			} else if(	kopfspalten[spaltei].toLowerCase().equals("hausnummerzusatz2") ||	
				kopfspalten[spaltei].toLowerCase().equals("hausnummernzusatz2")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.housenumberaddition2) == -1)
					importparameter.setHeaderfield(HEADERFIELD.housenumberaddition2, spaltei);
			} else if (	kopfspalten[spaltei].toLowerCase().equals("bemerkung") ||
				kopfspalten[spaltei].toLowerCase().equals("bemerkungen")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.note) == -1)
					importparameter.setHeaderfield(HEADERFIELD.note, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("koordindatensystem") ||
				kopfspalten[spaltei].toLowerCase().equals("epsg") ||
				kopfspalten[spaltei].toLowerCase().equals("srid")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.sourcesrid) == -1)
					importparameter.setHeaderfield(HEADERFIELD.sourcesrid, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("lon") ||
				kopfspalten[spaltei].toLowerCase().equals("rw") ||
				kopfspalten[spaltei].toLowerCase().equals("laengengrad") ||
				kopfspalten[spaltei].toLowerCase().equals("längengrad") ||
				kopfspalten[spaltei].toLowerCase().equals("rechtswert") ||
				kopfspalten[spaltei].toLowerCase().equals("lon_wgs84")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.lon) == -1)
					importparameter.setHeaderfield(HEADERFIELD.lon, spaltei);
			} else if (kopfspalten[spaltei].toLowerCase().equals("lat") ||
				kopfspalten[spaltei].toLowerCase().equals("hw") ||
				kopfspalten[spaltei].toLowerCase().equals("breitengrad") ||
				kopfspalten[spaltei].toLowerCase().equals("hochwert") ||
				kopfspalten[spaltei].toLowerCase().equals("lat_wgs84")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.lat) == -1)
					importparameter.setHeaderfield(HEADERFIELD.lat, spaltei);
			} else if(	kopfspalten[spaltei].toLowerCase().equals("sub") ||
				kopfspalten[spaltei].toLowerCase().equals("subarea") ||
				kopfspalten[spaltei].toLowerCase().equals("localite")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.subarea) == -1)
					importparameter.setHeaderfield(HEADERFIELD.subarea, spaltei);
			} else if(	kopfspalten[spaltei].toLowerCase().equals("subid") ||
				kopfspalten[spaltei].toLowerCase().equals("subarea_id") ||
				kopfspalten[spaltei].toLowerCase().equals("subarea-id") ||
				kopfspalten[spaltei].toLowerCase().equals("subareaid")) {
				if(importparameter.getHeaderfieldColumn(HEADERFIELD.subareaid) == -1)
					importparameter.setHeaderfield(HEADERFIELD.subareaid, spaltei);
			}
		}

		System.out.print("analysed Header line ...");
		System.out.print("   municipality: " + importparameter.getHeaderfieldColumn(HEADERFIELD.municipality));
		System.out.print("   municipality REF: " + importparameter.getHeaderfieldColumn(HEADERFIELD.municipalityref));
		System.out.print("   municipality ID: " + importparameter.getHeaderfieldColumn(HEADERFIELD.municipalityid));
		System.out.print("   street: " + importparameter.getHeaderfieldColumn(HEADERFIELD.street));
		System.out.print("   street ID: " + importparameter.getHeaderfieldColumn(HEADERFIELD.streetid));
		System.out.print("   postcode: " + importparameter.getHeaderfieldColumn(HEADERFIELD.postcode));
		System.out.print("   housenumber: " + importparameter.getHeaderfieldColumn(HEADERFIELD.housenumber));
		System.out.print("   housenumberaddition: " + importparameter.getHeaderfieldColumn(HEADERFIELD.housenumberaddition));
		System.out.print("   housenumberaddition2: " + importparameter.getHeaderfieldColumn(HEADERFIELD.housenumberaddition2));
		System.out.print("   subarea: " + importparameter.getHeaderfieldColumn(HEADERFIELD.subarea));
		System.out.println("   subarea ID: " + importparameter.getHeaderfieldColumn(HEADERFIELD.subareaid));
		System.out.println("   lon: " + importparameter.getHeaderfieldColumn(HEADERFIELD.lon));
		System.out.println("   lat: " + importparameter.getHeaderfieldColumn(HEADERFIELD.lat));
		System.out.println("   note: " + importparameter.getHeaderfieldColumn(HEADERFIELD.note));
	}
	
	private boolean open() {
		if(this.importparameter.getImportfile() == null)
			return false;
		File filehandle = new File(this.importparameter.getImportfile());
		if(!filehandle.isFile() || !filehandle.canRead())
			return false;
		
		try {
			this.filereader = new BufferedReader(new InputStreamReader(
				new FileInputStream(this.importparameter.getImportfile()), 
				this.importparameter.getImportfileFormat()));
		} catch (FileNotFoundException e) {
			return false;
		}
		return true;
	}

	/**
	 * read next line of file. In fist line, analyse header
	 * @return
	 * @throws Exception
	 */
	private String readln() throws Exception {
		String line = null;
		
		if(this.filereader == null) {
			if(!open()) {
				return null;
			}
		}

		try {
			line = this.filereader.readLine();
			if(line == null) {
				return line;
			}
		} catch (IOException e) {
			throw e;
		}
		lineno++;
//TODO give use more flexiblity, how many heading lines and if it should be analysed or user defines content itself
		if(lineno == 1) {
			int char0 = line.codePointAt(0);
			int char1 = line.codePointAt(1);
			if((char0 == 239) && (char1 == 187)) {
				line = line.substring(3);
			}
			if(importparameter.getFieldSeparator().equals("")) {
				int hitcount = 0;
				if (numberOfOccurences(line, "\t") > hitcount) {
					importparameter.setFieldSeparator("\t");
					hitcount = numberOfOccurences(line, "\t");
				}
				if (numberOfOccurences(line, ";") > hitcount) {
					importparameter.setFieldSeparator(";");
					hitcount = numberOfOccurences(line, ";");
				}
				if (numberOfOccurences(line, ",") > hitcount) {
					importparameter.setFieldSeparator(",");
					hitcount = numberOfOccurences(line, ",");
				}
				if (hitcount == 0) {
					throw new Exception("Field separator couldn't be examined, " +
						"you must set it explicit with setFieldSeparator");
				}
			}
			analyseHeaderline(line);
				// lets get read another line to get a data line and return this one
			line = this.filereader.readLine();
			if(line == null) {
				this.filereader.close();
				return line;
			}
		}
		return line;
	}
	

	private ImportAddress next() throws Exception  {
		ImportAddress address = null;

		
//TODO activate code
		if(this.importparameter.getCountrycode().equals("IT")) {
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

			// get custom header fields
		Map<Integer, String> customheaderfields = importparameter.getCustomHeaderfields();

		
		String line = "";
		while ((line = readln()) != null) {
			address = new ImportAddress();
			address.setCountrycode(importparameter.getCountrycode());

			if ( importparameter.hasField(HEADERFIELD.municipality) )
				address.setMunicipality(getFieldContent(line, HEADERFIELD.municipality));
			else if (importparameter.getMunicipality() != null )
				address.setMunicipality(importparameter.getMunicipality());

			if ( importparameter.hasField(HEADERFIELD.municipalityref) )
				address.setMunicipalityRef(getFieldContent(line, HEADERFIELD.municipalityref));
			else if (importparameter.getMunicipalityRef() != null )
				address.setMunicipalityRef(importparameter.getMunicipalityRef());

			if ( ( address.getMunicipality() == null ) && 
				( importparameter.getMunicipalityIDListEntry(getFieldContent(line, HEADERFIELD.municipalityref)) != null) ) {
				address.setMunicipality(importparameter.getMunicipalityIDListEntry(getFieldContent(line, HEADERFIELD.municipalityref)));
			}

			if (importparameter.convertStreetToUpperLower()) {
				address.setStreet(StreetToUpperLower(getFieldContent(line, HEADERFIELD.street)));
			} else {
				address.setStreet(getFieldContent(line, HEADERFIELD.street));
			}

			if ( importparameter.hasField(HEADERFIELD.streetid) &&
				 getFieldContent(line, HEADERFIELD.streetid).equals("") &&
				 (importparameter.getStreetIDListEntry(getFieldContent(line, HEADERFIELD.streetid)) != null)) {
				if (importparameter.convertStreetToUpperLower()) {
					address.setStreet(StreetToUpperLower(importparameter.getStreetIDListEntry(getFieldContent(line, HEADERFIELD.streetid))));
				} else {
					address.setStreet(importparameter.getStreetIDListEntry(getFieldContent(line, HEADERFIELD.streetid)));
				}
			}
				
			if ( importparameter.hasField(HEADERFIELD.postcode) )
				address.setPostcode(getFieldContent(line, HEADERFIELD.postcode));

			String housenumber = getFieldContent(line, HEADERFIELD.housenumber);
			if ( importparameter.hasField(HEADERFIELD.housenumberaddition ) ) {
				housenumber += this.importparameter.getHousenumberFieldseparator();
				housenumber += getFieldContent(line, HEADERFIELD.housenumberaddition);
				if(importparameter.hasField(HEADERFIELD.housenumberaddition2) ) {
					housenumber += this.importparameter.getHousenumberFieldseparator2();
					housenumber += getFieldContent(line, HEADERFIELD.housenumberaddition2);
				}
			}
			address.setHousenumber(housenumber);

			if ( importparameter.hasField(HEADERFIELD.note) )
			address.setNote(getFieldContent(line, HEADERFIELD.note));

			if ( importparameter.hasField(HEADERFIELD.subarea) )
				address.setSubArea(getFieldContent(line, HEADERFIELD.subarea));

			if(	(importparameter.hasField(HEADERFIELD.subareaid) ) &&
				(importparameter.getSubareaMunicipalityIDListEntry(getFieldContent(line, HEADERFIELD.subareaid)) != null)) {
				address.setSubArea(importparameter.getStreetIDListEntry(getFieldContent(line, HEADERFIELD.subareaid)));
			}

			if(	importparameter.getCountrycode().equals("LU") &&
				address.getMunicipality().equals("") &&
				!address.getSubArea().equals("")) {
				address.setMunicipality(getLuxembourgMunicipalityforSubarea(address.getSubArea()));
			}

				// work on custom header fields, if defined
			for (Map.Entry<Integer, String> customfieldentry : customheaderfields.entrySet()) {
				int columnno = customfieldentry.getKey();
				String osmkey = customfieldentry.getValue();
				String columncontent = getFieldContent(line, columnno);
				if ( !columncontent.equals("") )
					address.setOSMTag(osmkey, columncontent);
			}

			String sourcesrid = importparameter.getSourceCoordinateSystem();
			if(importparameter.hasField(HEADERFIELD.sourcesrid) ) {
				sourcesrid = getFieldContent(line, HEADERFIELD.sourcesrid);
				if(!sourcesrid.equals(importparameter.getSourceCoordinateSystem())) {
					throw new IllegalArgumentException("coordinate system differs in line " + lineno + " '" + sourcesrid + "'" +
						" from defined coordinate system '" + importparameter.getSourceCoordinateSystem() + "'");
				}
			}
			address.setSourceSrid(sourcesrid);

			
			if ( importparameter.hasField(HEADERFIELD.lon) && importparameter.hasField(HEADERFIELD.lat) ) { 
				String lon = getFieldContent(line, HEADERFIELD.lon);
				String lat = getFieldContent(line, HEADERFIELD.lat);
				if(!lon.equals("") && !lat.equals("")) {
					try {
						lon = lon.replace(",",".");
						address.setLon(Double.parseDouble(lon));
						if(	this.importparameter.getSourceCoordinateSystem().equals("25832") && 
							(address.getLon() > 32000000))
							address.setLon(address.getLon() - 32000000.0);
	
					} catch (NumberFormatException nofloat) {
						System.out.println("Warning: cannot convert input lon value '" + 
							getFieldContent(line, HEADERFIELD.lon) + "'");
						address.setLon(ImportAddress.lonUnset);
						address.setLat(ImportAddress.latUnset);
					}
					try {
						lat = lat.replace(",",".");
						address.setLat(Double.parseDouble(lat));
					} catch (NumberFormatException nofloat) {
						System.out.println("Warning: cannot convert input lat value '" + 
							getFieldContent(line, HEADERFIELD.lat) + "'"); 
						address.setLon(ImportAddress.lonUnset);
						address.setLat(ImportAddress.latUnset);
					}
				}
			}
			//TODO find out change of municipality (assuming, that input file is sorted by municipalities) and store after each municipality
//			alternatively, make an array of municipalities to hold all togehter in memory during import read process

			if (address.getStreet().equals("") || address.getHousenumber().equals("")) {
				if (address.getStreet().equals("")) {
					Importlog("Warning",  "Missing_Street", "no street name in fileline # " + 
						lineno + ", content ===" + line + "===, will be ignored");
				}
				if (address.getHousenumber().equals("")) {
					Importlog("Warning",  "Missing_Housenumber", "no housenumber in fileline # " + 
						lineno + ", content ===" + line + "===, will be ignored");
				}
			} else {
				break;
			}
		}
		if(line == null) {
			this.filereader.close();
			return null;
		}
		return address;
	}

	public Map<Municipality, HousenumberList> execute() {
		Map<Municipality, HousenumberList> lists = new HashMap<>();

		int doppelteadressen = 0;

		ImportAddress address = null;
		try {
			if(!open())
				return lists;
			while((address = next()) != null) {
				Municipality municipality = new Municipality(address.getCountrycode(), 
					address.getMunicipality(), address.getMunicipalityRef());
				HousenumberList housenumberlist = null;
				if(lists.containsKey(municipality))
					housenumberlist = lists.get(municipality);
				else {
					housenumberlist = new HousenumberList(municipality);
				}
				if(!housenumberlist.contains(address)) {
					housenumberlist.addHousenumber(address);
					lists.put(municipality,  housenumberlist);
				} else {
					System.out.println("Hausnummer doppelt in Zeile " + lineno + ": " + address.toString());
					doppelteadressen++;
				}
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Anzahl Dateizeilen: " + lineno + ", davon mehrfache adressen: " + doppelteadressen);
		return lists;
	}
	
	// ============================================================================================================================================
	// ============================================================================================================================================
	// ============================================================================================================================================

	
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

/*
	static long osmnodeid = -1;
	private static void OSMoutput(String parameterQuellSrid, String headercolumns[], String actualcolumns[]) {
		PrintWriter osmfile = null;
		String protocolfilename = configuration.application_datadir + File.separator + "import" + ".osm";
		
		String sourcesrid = parameterQuellSrid;
		Double sourcecoordx = 0.0D;
		Double sourcecoordy = 0.0D;
		for(Integer columnindex = 0; columnindex < headercolumns.length; columnindex++) {
			if (headercolumns[columnindex].toLowerCase().equals("koordindatensystem") ||
				headercolumns[columnindex].toLowerCase().equals("epsg") ||
				headercolumns[columnindex].toLowerCase().equals("srid")) {
				sourcesrid = actualcolumns[columnindex];
			}
			if (headercolumns[columnindex].toLowerCase().equals("lon") ||
				headercolumns[columnindex].toLowerCase().equals("rw") ||
				headercolumns[columnindex].toLowerCase().equals("laengengrad") ||
				headercolumns[columnindex].toLowerCase().equals("längengrad")) {
				if(!actualcolumns[columnindex].equals(""))
					sourcecoordx = Double.parseDouble(actualcolumns[columnindex].replace(",","."));
			}
			if (headercolumns[columnindex].toLowerCase().equals("lat") ||
				headercolumns[columnindex].toLowerCase().equals("hw") ||
				headercolumns[columnindex].toLowerCase().equals("breitengrad")) {
				if(!actualcolumns[columnindex].equals(""))
					sourcecoordy = Double.parseDouble(actualcolumns[columnindex].replace(",","."));
			}
		}
		if((sourcecoordx == 0.0D) || (sourcecoordy == 0.0D))
			return;

		String outputtext = "";
		try {
			
			String selectLonLatSql = "SELECT ST_X(ST_Transform(ST_Setsrid(ST_Makepoint(?, ?), ?), 4326)) as lon,"
				+ " ST_Y(ST_Transform(ST_Setsrid(ST_Makepoint(?, ?), ?), 4326)) as lat;";
			PreparedStatement selectLonLatStmt = conHausnummern.prepareStatement(selectLonLatSql);
			selectLonLatStmt.setDouble(1, sourcecoordx);
			selectLonLatStmt.setDouble(2, sourcecoordy);
			selectLonLatStmt.setInt(3, Integer.parseInt(sourcesrid));
			selectLonLatStmt.setDouble(4, sourcecoordx);
			selectLonLatStmt.setDouble(5, sourcecoordy);
			selectLonLatStmt.setInt(6, Integer.parseInt(sourcesrid));
			ResultSet selectLonLatRS = selectLonLatStmt.executeQuery();
			if (selectLonLatRS.next()) {
				Double lon = selectLonLatRS.getDouble("lon");
				Double lat = selectLonLatRS.getDouble("lat");

				osmfile = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(protocolfilename, true),StandardCharsets.UTF_8)));
				outputtext += "<node id=\"" + osmnodeid-- + "\" action=\"modify\" visible=\"true\" lat=\"" + lat + "\" lon=\"" + lon + "\">\n";
				for(Integer columnindex = 0; columnindex < actualcolumns.length; columnindex++) {
					if(!headercolumns[columnindex].equals("") && !actualcolumns[columnindex].equals("")) {
						outputtext += "<tag k=\"" + headercolumns[columnindex] +  "\" v=\"" + actualcolumns[columnindex] + "\" />\n";
					}
				}
				outputtext += "</node>\n";
				osmfile.println(outputtext);
				osmfile.close();
			}
			
		} catch (IOException ioerror) {
			System.out.println("ERROR: couldn't open file to write, filename was ===" + protocolfilename + "===");
			ioerror.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NumberFormatException numbexception) {
			numbexception.printStackTrace();
		}
	}

*/
	
	public void initialiseLuxembourg() {
		luxembourgLocalityList.put("Beaufort", "Beaufort");
		luxembourgLocalityList.put("Dillingen", "Beaufort");
		luxembourgLocalityList.put("Grundhof (Beaufort)", "Beaufort");
		luxembourgLocalityList.put("Altrier", "Bech");
		luxembourgLocalityList.put("Bech", "Bech");
		luxembourgLocalityList.put("Blumenthal (Bech)", "Bech");
		luxembourgLocalityList.put("Geyershof (Bech)", "Bech");
		luxembourgLocalityList.put("Graulinster (Bech)", "Bech");
		luxembourgLocalityList.put("Hemstal", "Bech");
		luxembourgLocalityList.put("Hersberg", "Bech");
		luxembourgLocalityList.put("Kobenbour", "Bech");
		luxembourgLocalityList.put("Rippig", "Bech");
		luxembourgLocalityList.put("Zittig", "Bech");
		luxembourgLocalityList.put("Beckerich", "Beckerich");
		luxembourgLocalityList.put("Elvange", "Beckerich");
		luxembourgLocalityList.put("Hovelange", "Beckerich");
		luxembourgLocalityList.put("Huttange", "Beckerich");
		luxembourgLocalityList.put("Levelange", "Beckerich");
		luxembourgLocalityList.put("Noerdange", "Beckerich");
		luxembourgLocalityList.put("Oberpallen", "Beckerich");
		luxembourgLocalityList.put("Schweich", "Beckerich");
		luxembourgLocalityList.put("Berdorf", "Berdorf");
		luxembourgLocalityList.put("Bollendorf-Pont", "Berdorf");
		luxembourgLocalityList.put("Grundhof (Berdorf)", "Berdorf");
		luxembourgLocalityList.put("Kalkesbach (Berdorf)", "Berdorf");
		luxembourgLocalityList.put("Weilerbach", "Berdorf");
		luxembourgLocalityList.put("Bertrange", "Bertrange");
		luxembourgLocalityList.put("Abweiler", "Bettembourg");
		luxembourgLocalityList.put("Bettembourg", "Bettembourg");
		luxembourgLocalityList.put("Fennange", "Bettembourg");
		luxembourgLocalityList.put("Huncherange", "Bettembourg");
		luxembourgLocalityList.put("Noertzange", "Bettembourg");
		luxembourgLocalityList.put("Bettendorf", "Bettendorf");
		luxembourgLocalityList.put("Gilsdorf", "Bettendorf");
		luxembourgLocalityList.put("Moestroff", "Bettendorf");
		luxembourgLocalityList.put("Berg (Betzdorf)", "Betzdorf");
		luxembourgLocalityList.put("Betzdorf", "Betzdorf");
		luxembourgLocalityList.put("Mensdorf", "Betzdorf");
		luxembourgLocalityList.put("Olingen", "Betzdorf");
		luxembourgLocalityList.put("Roodt-sur-Syre", "Betzdorf");
		luxembourgLocalityList.put("Bissen", "Bissen");
		luxembourgLocalityList.put("Roost (Bissen)", "Bissen");
		luxembourgLocalityList.put("Biwer", "Biwer");
		luxembourgLocalityList.put("Biwerbach", "Biwer");
		luxembourgLocalityList.put("Boudler", "Biwer");
		luxembourgLocalityList.put("Boudlerbach", "Biwer");
		luxembourgLocalityList.put("Breinert", "Biwer");
		luxembourgLocalityList.put("Brouch (Wecker)", "Biwer");
		luxembourgLocalityList.put("Hagelsdorf", "Biwer");
		luxembourgLocalityList.put("Wecker", "Biwer");
		luxembourgLocalityList.put("Wecker-Gare", "Biwer");
		luxembourgLocalityList.put("Weydig", "Biwer");
		luxembourgLocalityList.put("Bill", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Boevange-sur-Attert", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Brouch (Mersch)", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Buschdorf", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Finsterthal", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Grevenknapp", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Openthalt", "Boevange-sur-Attert");
		luxembourgLocalityList.put("Baschleiden", "Boulaide");
		luxembourgLocalityList.put("Boulaide", "Boulaide");
		luxembourgLocalityList.put("Surré", "Boulaide");
		luxembourgLocalityList.put("Bourscheid", "Bourscheid");
		luxembourgLocalityList.put("Bourscheid Moulin", "Bourscheid");
		luxembourgLocalityList.put("Bourscheid-Plage", "Bourscheid");
		luxembourgLocalityList.put("Dirbach (Bourscheid)", "Bourscheid");
		luxembourgLocalityList.put("Flebour", "Bourscheid");
		luxembourgLocalityList.put("Friedbusch", "Bourscheid");
		luxembourgLocalityList.put("Goebelsmühle", "Bourscheid");
		luxembourgLocalityList.put("Kehmen", "Bourscheid");
		luxembourgLocalityList.put("Lipperscheid", "Bourscheid");
		luxembourgLocalityList.put("Michelau", "Bourscheid");
		luxembourgLocalityList.put("Scheidel", "Bourscheid");
		luxembourgLocalityList.put("Schlindermanderscheid", "Bourscheid");
		luxembourgLocalityList.put("Welscheid", "Bourscheid");
		luxembourgLocalityList.put("Assel", "Bous");
		luxembourgLocalityList.put("Bous", "Bous");
		luxembourgLocalityList.put("Erpeldange (Bous)", "Bous");
		luxembourgLocalityList.put("Rolling", "Bous");
		luxembourgLocalityList.put("Clervaux", "Clervaux");
		luxembourgLocalityList.put("Drauffelt", "Clervaux");
		luxembourgLocalityList.put("Eselborn", "Clervaux");
		luxembourgLocalityList.put("Fischbach (Clervaux)", "Clervaux");
		luxembourgLocalityList.put("Fossenhof", "Clervaux");
		luxembourgLocalityList.put("Grindhausen", "Clervaux");
		luxembourgLocalityList.put("Heinerscheid", "Clervaux");
		luxembourgLocalityList.put("Hupperdange", "Clervaux");
		luxembourgLocalityList.put("Kaesfurt (Heinerscheid)", "Clervaux");
		luxembourgLocalityList.put("Kalborn", "Clervaux");
		luxembourgLocalityList.put("Lausdorn (Heinerscheid)", "Clervaux");
		luxembourgLocalityList.put("Lieler", "Clervaux");
		luxembourgLocalityList.put("Marnach", "Clervaux");
		luxembourgLocalityList.put("Mecher (Clervaux)", "Clervaux");
		luxembourgLocalityList.put("Moulin de Kalborn", "Clervaux");
		luxembourgLocalityList.put("Munshausen", "Clervaux");
		luxembourgLocalityList.put("Reuler", "Clervaux");
		luxembourgLocalityList.put("Roder", "Clervaux");
		luxembourgLocalityList.put("Siebenaler", "Clervaux");
		luxembourgLocalityList.put("Tintesmühle", "Clervaux");
		luxembourgLocalityList.put("Urspelt", "Clervaux");
		luxembourgLocalityList.put("Weicherdange", "Clervaux");
		luxembourgLocalityList.put("Colmar-Berg", "Colmar-Berg");
		luxembourgLocalityList.put("Welsdorf", "Colmar-Berg");
		luxembourgLocalityList.put("Breidweiler", "Consdorf");
		luxembourgLocalityList.put("Colbette", "Consdorf");
		luxembourgLocalityList.put("Consdorf", "Consdorf");
		luxembourgLocalityList.put("Kalkesbach (Consdorf)", "Consdorf");
		luxembourgLocalityList.put("Marscherwald", "Consdorf");
		luxembourgLocalityList.put("Scheidgen", "Consdorf");
		luxembourgLocalityList.put("Wolper", "Consdorf");
		luxembourgLocalityList.put("Contern", "Contern");
		luxembourgLocalityList.put("Medingen", "Contern");
		luxembourgLocalityList.put("Moutfort", "Contern");
		luxembourgLocalityList.put("Oetrange", "Contern");
		luxembourgLocalityList.put("Dalheim", "Dalheim");
		luxembourgLocalityList.put("Filsdorf", "Dalheim");
		luxembourgLocalityList.put("Welfrange", "Dalheim");
		luxembourgLocalityList.put("Diekirch", "Diekirch");
		luxembourgLocalityList.put("Differdange", "Differdange");
		luxembourgLocalityList.put("Lasauvage", "Differdange");
		luxembourgLocalityList.put("Niederkorn", "Differdange");
		luxembourgLocalityList.put("Oberkorn", "Differdange");
		luxembourgLocalityList.put("Bettange-sur-Mess", "Dippach");
		luxembourgLocalityList.put("Dippach", "Dippach");
		luxembourgLocalityList.put("Schouweiler", "Dippach");
		luxembourgLocalityList.put("Sprinkange", "Dippach");
		luxembourgLocalityList.put("Dudelange", "Dudelange");
		luxembourgLocalityList.put("Echternach", "Echternach");
		luxembourgLocalityList.put("Colpach-Bas", "Ell");
		luxembourgLocalityList.put("Colpach-Haut", "Ell");
		luxembourgLocalityList.put("Ell", "Ell");
		luxembourgLocalityList.put("Petit-Nobressart", "Ell");
		luxembourgLocalityList.put("Roodt (Ell)", "Ell");
		luxembourgLocalityList.put("Burden", "Erpeldange-sur-Sûre");
		luxembourgLocalityList.put("Erpeldange-sur-Sûre", "Erpeldange-sur-Sûre");
		luxembourgLocalityList.put("Ingeldorf", "Erpeldange-sur-Sûre");
		luxembourgLocalityList.put("Esch-sur-Alzette", "Esch-sur-Alzette");
		luxembourgLocalityList.put("Bonnal", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Dirbach", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Esch-sur-Sûre", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Eschdorf", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Heiderscheid", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Heiderscheidergrund", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Hierheck", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Insenborn", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Lultzhausen", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Merscheid", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Neunhausen", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Ringel", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Tadler", "Esch-sur-Sûre");
		luxembourgLocalityList.put("Ettelbruck", "Ettelbruck");
		luxembourgLocalityList.put("Warken", "Ettelbruck");
		luxembourgLocalityList.put("Niederfeulen", "Feulen");
		luxembourgLocalityList.put("Oberfeulen", "Feulen");
		luxembourgLocalityList.put("Angelsberg", "Fischbach");
		luxembourgLocalityList.put("Fischbach (Mersch)", "Fischbach");
		luxembourgLocalityList.put("Koedange", "Fischbach");
		luxembourgLocalityList.put("Schiltzberg", "Fischbach");
		luxembourgLocalityList.put("Schoos", "Fischbach");
		luxembourgLocalityList.put("Stuppicht", "Fischbach");
		luxembourgLocalityList.put("Weyer (Junglinster)", "Fischbach");
		luxembourgLocalityList.put("Beyren", "Flaxweiler");
		luxembourgLocalityList.put("Flaxweiler", "Flaxweiler");
		luxembourgLocalityList.put("Gostingen", "Flaxweiler");
		luxembourgLocalityList.put("Niederdonven", "Flaxweiler");
		luxembourgLocalityList.put("Oberdonven", "Flaxweiler");
		luxembourgLocalityList.put("Aspelt", "Frisange");
		luxembourgLocalityList.put("Frisange", "Frisange");
		luxembourgLocalityList.put("Hellange", "Frisange");
		luxembourgLocalityList.put("Dahlem", "Garnich");
		luxembourgLocalityList.put("Garnich", "Garnich");
		luxembourgLocalityList.put("Hivange", "Garnich");
		luxembourgLocalityList.put("Kahler", "Garnich");
		luxembourgLocalityList.put("Bockholtz (Goesdorf)", "Goesdorf");
		luxembourgLocalityList.put("Buederscheid", "Goesdorf");
		luxembourgLocalityList.put("Dahl", "Goesdorf");
		luxembourgLocalityList.put("Dirbach (Goesdorf)", "Goesdorf");
		luxembourgLocalityList.put("Goebelsmühle (Goesdorf)", "Goesdorf");
		luxembourgLocalityList.put("Goesdorf", "Goesdorf");
		luxembourgLocalityList.put("Masseler", "Goesdorf");
		luxembourgLocalityList.put("Nocher", "Goesdorf");
		luxembourgLocalityList.put("Nocher Route", "Goesdorf");
		luxembourgLocalityList.put("Grevenmacher", "Grevenmacher");
		luxembourgLocalityList.put("Dellen", "Grosbous");
		luxembourgLocalityList.put("Grevels (Grosbous)", "Grosbous");
		luxembourgLocalityList.put("Grosbous", "Grosbous");
		luxembourgLocalityList.put("Lehrhof", "Grosbous");
		luxembourgLocalityList.put("Heffingen", "Heffingen");
		luxembourgLocalityList.put("Reuland", "Heffingen");
		luxembourgLocalityList.put("Alzingen", "Hesperange");
		luxembourgLocalityList.put("Fentange", "Hesperange");
		luxembourgLocalityList.put("Hesperange", "Hesperange");
		luxembourgLocalityList.put("Howald", "Hesperange");
		luxembourgLocalityList.put("Itzig", "Hesperange");
		luxembourgLocalityList.put("Eischen", "Hobscheid");
		luxembourgLocalityList.put("Hobscheid", "Hobscheid");
		luxembourgLocalityList.put("Altlinster", "Junglinster");
		luxembourgLocalityList.put("Beidweiler", "Junglinster");
		luxembourgLocalityList.put("Blumenthal", "Junglinster");
		luxembourgLocalityList.put("Bourglinster", "Junglinster");
		luxembourgLocalityList.put("Eisenborn", "Junglinster");
		luxembourgLocalityList.put("Eschweiler", "Junglinster");
		luxembourgLocalityList.put("Godbrange", "Junglinster");
		luxembourgLocalityList.put("Gonderange", "Junglinster");
		luxembourgLocalityList.put("Graulinster", "Junglinster");
		luxembourgLocalityList.put("Imbringen", "Junglinster");
		luxembourgLocalityList.put("Junglinster", "Junglinster");
		luxembourgLocalityList.put("Rodenbourg", "Junglinster");
		luxembourgLocalityList.put("Kayl", "Kayl");
		luxembourgLocalityList.put("Tétange", "Kayl");
		luxembourgLocalityList.put("Dondelange", "Kehlen");
		luxembourgLocalityList.put("Kehlen", "Kehlen");
		luxembourgLocalityList.put("Keispelt", "Kehlen");
		luxembourgLocalityList.put("Meispelt", "Kehlen");
		luxembourgLocalityList.put("Nospelt", "Kehlen");
		luxembourgLocalityList.put("Olm", "Kehlen");
		luxembourgLocalityList.put("Alscheid", "Kiischpelt");
		luxembourgLocalityList.put("Enscherange", "Kiischpelt");
		luxembourgLocalityList.put("Kautenbach", "Kiischpelt");
		luxembourgLocalityList.put("Lellingen", "Kiischpelt");
		luxembourgLocalityList.put("Merkholtz", "Kiischpelt");
		luxembourgLocalityList.put("Pintsch", "Kiischpelt");
		luxembourgLocalityList.put("Wilwerwiltz", "Kiischpelt");
		luxembourgLocalityList.put("Goeblange", "Koerich");
		luxembourgLocalityList.put("Goetzingen", "Koerich");
		luxembourgLocalityList.put("Koerich", "Koerich");
		luxembourgLocalityList.put("Windhof (Koerich)", "Koerich");
		luxembourgLocalityList.put("Bridel", "Kopstal");
		luxembourgLocalityList.put("Kopstal", "Kopstal");
		luxembourgLocalityList.put("Bascharage", "Käerjeng");
		luxembourgLocalityList.put("Clemency", "Käerjeng");
		luxembourgLocalityList.put("Fingig", "Käerjeng");
		luxembourgLocalityList.put("Hautcharage", "Käerjeng");
		luxembourgLocalityList.put("Linger", "Käerjeng");
		luxembourgLocalityList.put("Bavigne", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Harlange", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Kaundorf", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Liefrange", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Mecher (Haute-Sûre)", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Nothum", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Tarchamps", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Watrange", "Lac de la Haute-Sûre");
		luxembourgLocalityList.put("Ernzen", "Larochette");
		luxembourgLocalityList.put("Larochette", "Larochette");
		luxembourgLocalityList.put("Meysembourg", "Larochette");
		luxembourgLocalityList.put("Canach", "Lenningen");
		luxembourgLocalityList.put("Lenningen", "Lenningen");
		luxembourgLocalityList.put("Leudelange", "Leudelange");
		luxembourgLocalityList.put("Gosseldange", "Lintgen");
		luxembourgLocalityList.put("Lintgen", "Lintgen");
		luxembourgLocalityList.put("Prettingen", "Lintgen");
		luxembourgLocalityList.put("Asselscheuer", "Lorentzweiler");
		luxembourgLocalityList.put("Blaschette", "Lorentzweiler");
		luxembourgLocalityList.put("Bofferdange", "Lorentzweiler");
		luxembourgLocalityList.put("Helmdange", "Lorentzweiler");
		luxembourgLocalityList.put("Hunsdorf", "Lorentzweiler");
		luxembourgLocalityList.put("Klingelscheuer", "Lorentzweiler");
		luxembourgLocalityList.put("Lorentzweiler", "Lorentzweiler");
		luxembourgLocalityList.put("Luxembourg", "Luxembourg");
		luxembourgLocalityList.put("Capellen", "Mamer");
		luxembourgLocalityList.put("Holzem", "Mamer");
		luxembourgLocalityList.put("Mamer", "Mamer");
		luxembourgLocalityList.put("Berbourg", "Manternach");
		luxembourgLocalityList.put("Lellig", "Manternach");
		luxembourgLocalityList.put("Manternach", "Manternach");
		luxembourgLocalityList.put("Münschecker", "Manternach");
		luxembourgLocalityList.put("Beringen", "Mersch");
		luxembourgLocalityList.put("Essingen", "Mersch");
		luxembourgLocalityList.put("Mersch", "Mersch");
		luxembourgLocalityList.put("Moesdorf (Mersch)", "Mersch");
		luxembourgLocalityList.put("Pettingen", "Mersch");
		luxembourgLocalityList.put("Reckange (Mersch)", "Mersch");
		luxembourgLocalityList.put("Rollingen", "Mersch");
		luxembourgLocalityList.put("Schoenfels", "Mersch");
		luxembourgLocalityList.put("Mertert", "Mertert");
		luxembourgLocalityList.put("Wasserbillig", "Mertert");
		luxembourgLocalityList.put("Mertzig", "Mertzig");
		luxembourgLocalityList.put("Born", "Mompach");
		luxembourgLocalityList.put("Boursdorf", "Mompach");
		luxembourgLocalityList.put("Givenich", "Mompach");
		luxembourgLocalityList.put("Herborn", "Mompach");
		luxembourgLocalityList.put("Lilien", "Mompach");
		luxembourgLocalityList.put("Moersdorf", "Mompach");
		luxembourgLocalityList.put("Mompach", "Mompach");
		luxembourgLocalityList.put("Bergem", "Mondercange");
		luxembourgLocalityList.put("Foetz", "Mondercange");
		luxembourgLocalityList.put("Mondercange", "Mondercange");
		luxembourgLocalityList.put("Pontpierre", "Mondercange");
		luxembourgLocalityList.put("Altwies", "Mondorf-les-Bains");
		luxembourgLocalityList.put("Ellange", "Mondorf-les-Bains");
		luxembourgLocalityList.put("Mondorf-les-Bains", "Mondorf-les-Bains");
		luxembourgLocalityList.put("Ernster", "Niederanven");
		luxembourgLocalityList.put("Hostert", "Niederanven");
		luxembourgLocalityList.put("Niederanven", "Niederanven");
		luxembourgLocalityList.put("Oberanven", "Niederanven");
		luxembourgLocalityList.put("Rameldange", "Niederanven");
		luxembourgLocalityList.put("Senningen", "Niederanven");
		luxembourgLocalityList.put("Senningerberg", "Niederanven");
		luxembourgLocalityList.put("Waldhof", "Niederanven");
		luxembourgLocalityList.put("Cruchten", "Nommern");
		luxembourgLocalityList.put("Niederglabach", "Nommern");
		luxembourgLocalityList.put("Nommern", "Nommern");
		luxembourgLocalityList.put("Oberglabach", "Nommern");
		luxembourgLocalityList.put("Schrondweiler", "Nommern");
		luxembourgLocalityList.put("Bockholtz", "Parc Hosingen");
		luxembourgLocalityList.put("Consthum", "Parc Hosingen");
		luxembourgLocalityList.put("Dorscheid", "Parc Hosingen");
		luxembourgLocalityList.put("Eisenbach", "Parc Hosingen");
		luxembourgLocalityList.put("Holzthum", "Parc Hosingen");
		luxembourgLocalityList.put("Hoscheid", "Parc Hosingen");
		luxembourgLocalityList.put("Hoscheid-Dickt", "Parc Hosingen");
		luxembourgLocalityList.put("Hosingen", "Parc Hosingen");
		luxembourgLocalityList.put("Neidhausen", "Parc Hosingen");
		luxembourgLocalityList.put("Oberschlinder", "Parc Hosingen");
		luxembourgLocalityList.put("Rodershausen", "Parc Hosingen");
		luxembourgLocalityList.put("Unterschlinder", "Parc Hosingen");
		luxembourgLocalityList.put("Wahlhausen", "Parc Hosingen");
		luxembourgLocalityList.put("Bettborn", "Préizerdaul");
		luxembourgLocalityList.put("Platen", "Préizerdaul");
		luxembourgLocalityList.put("Pratz", "Préizerdaul");
		luxembourgLocalityList.put("Reimberg", "Préizerdaul");
		luxembourgLocalityList.put("Bivels", "Putscheid");
		luxembourgLocalityList.put("Gralingen", "Putscheid");
		luxembourgLocalityList.put("Merscheid (Pütscheid)", "Putscheid");
		luxembourgLocalityList.put("Nachtmanderscheid", "Putscheid");
		luxembourgLocalityList.put("Pütscheid", "Putscheid");
		luxembourgLocalityList.put("Stolzembourg", "Putscheid");
		luxembourgLocalityList.put("Weiler (Pütscheid)", "Putscheid");
		luxembourgLocalityList.put("Lamadelaine", "Pétange");
		luxembourgLocalityList.put("Pétange", "Pétange");
		luxembourgLocalityList.put("Rodange", "Pétange");
		luxembourgLocalityList.put("Arsdorf", "Rambrouch");
		luxembourgLocalityList.put("Bigonville", "Rambrouch");
		luxembourgLocalityList.put("Bigonville-Poteau", "Rambrouch");
		luxembourgLocalityList.put("Bilsdorf", "Rambrouch");
		luxembourgLocalityList.put("Eschette", "Rambrouch");
		luxembourgLocalityList.put("Flatzbour", "Rambrouch");
		luxembourgLocalityList.put("Folschette", "Rambrouch");
		luxembourgLocalityList.put("Haut-Martelange", "Rambrouch");
		luxembourgLocalityList.put("Holtz", "Rambrouch");
		luxembourgLocalityList.put("Hostert (Rambrouch)", "Rambrouch");
		luxembourgLocalityList.put("Koetschette", "Rambrouch");
		luxembourgLocalityList.put("Perlé", "Rambrouch");
		luxembourgLocalityList.put("Rambrouch", "Rambrouch");
		luxembourgLocalityList.put("Riesenhof", "Rambrouch");
		luxembourgLocalityList.put("Rombach-Martelange", "Rambrouch");
		luxembourgLocalityList.put("Wolwelange", "Rambrouch");
		luxembourgLocalityList.put("Ehlange", "Reckange-sur-Mess");
		luxembourgLocalityList.put("Limpach", "Reckange-sur-Mess");
		luxembourgLocalityList.put("Pissange", "Reckange-sur-Mess");
		luxembourgLocalityList.put("Reckange-sur-Mess", "Reckange-sur-Mess");
		luxembourgLocalityList.put("Roedgen", "Reckange-sur-Mess");
		luxembourgLocalityList.put("Wickrange", "Reckange-sur-Mess");
		luxembourgLocalityList.put("Eltz", "Redange/Attert");
		luxembourgLocalityList.put("Lannen", "Redange/Attert");
		luxembourgLocalityList.put("Nagem", "Redange/Attert");
		luxembourgLocalityList.put("Niederpallen", "Redange/Attert");
		luxembourgLocalityList.put("Ospern", "Redange/Attert");
		luxembourgLocalityList.put("Redange/Attert", "Redange/Attert");
		luxembourgLocalityList.put("Reichlange", "Redange/Attert");
		luxembourgLocalityList.put("Bigelbach", "Reisdorf");
		luxembourgLocalityList.put("Hoesdorf", "Reisdorf");
		luxembourgLocalityList.put("Reisdorf", "Reisdorf");
		luxembourgLocalityList.put("Wallendorf-Pont", "Reisdorf");
		luxembourgLocalityList.put("Remich", "Remich");
		luxembourgLocalityList.put("Berchem", "Roeser");
		luxembourgLocalityList.put("Bivange", "Roeser");
		luxembourgLocalityList.put("Crauthem", "Roeser");
		luxembourgLocalityList.put("Kockelscheuer (Roeser)", "Roeser");
		luxembourgLocalityList.put("Livange", "Roeser");
		luxembourgLocalityList.put("Peppange", "Roeser");
		luxembourgLocalityList.put("Roeser", "Roeser");
		luxembourgLocalityList.put("Dickweiler", "Rosport");
		luxembourgLocalityList.put("Girst", "Rosport");
		luxembourgLocalityList.put("Girsterklaus", "Rosport");
		luxembourgLocalityList.put("Hinkel", "Rosport");
		luxembourgLocalityList.put("Osweiler", "Rosport");
		luxembourgLocalityList.put("Rosport", "Rosport");
		luxembourgLocalityList.put("Steinheim", "Rosport");
		luxembourgLocalityList.put("Rumelange", "Rumelange");
		luxembourgLocalityList.put("Calmus", "Saeul");
		luxembourgLocalityList.put("Ehner", "Saeul");
		luxembourgLocalityList.put("Kapweiler", "Saeul");
		luxembourgLocalityList.put("Saeul", "Saeul");
		luxembourgLocalityList.put("Schwebach", "Saeul");
		luxembourgLocalityList.put("Findel", "Sandweiler");
		luxembourgLocalityList.put("Sandweiler", "Sandweiler");
		luxembourgLocalityList.put("Belvaux", "Sanem");
		luxembourgLocalityList.put("Ehlerange", "Sanem");
		luxembourgLocalityList.put("Sanem", "Sanem");
		luxembourgLocalityList.put("Soleuvre", "Sanem");
		luxembourgLocalityList.put("Bech-Kleinmacher", "Schengen");
		luxembourgLocalityList.put("Burmerange", "Schengen");
		luxembourgLocalityList.put("Elvange (Schengen)", "Schengen");
		luxembourgLocalityList.put("Emerange", "Schengen");
		luxembourgLocalityList.put("Remerschen", "Schengen");
		luxembourgLocalityList.put("Schengen", "Schengen");
		luxembourgLocalityList.put("Schwebsingen", "Schengen");
		luxembourgLocalityList.put("Wellenstein", "Schengen");
		luxembourgLocalityList.put("Wintrange", "Schengen");
		luxembourgLocalityList.put("Colmar-Pont", "Schieren");
		luxembourgLocalityList.put("Schieren", "Schieren");
		luxembourgLocalityList.put("Schifflange", "Schifflange");
		luxembourgLocalityList.put("Münsbach", "Schuttrange");
		luxembourgLocalityList.put("Neuhaeusgen", "Schuttrange");
		luxembourgLocalityList.put("Schrassig", "Schuttrange");
		luxembourgLocalityList.put("Schuttrange", "Schuttrange");
		luxembourgLocalityList.put("Uebersyren", "Schuttrange");
		luxembourgLocalityList.put("Greisch", "Septfontaines");
		luxembourgLocalityList.put("Leesbach", "Septfontaines");
		luxembourgLocalityList.put("Roodt (Septfontaines)", "Septfontaines");
		luxembourgLocalityList.put("Septfontaines", "Septfontaines");
		luxembourgLocalityList.put("Simmerfarm", "Septfontaines");
		luxembourgLocalityList.put("Simmerschmelz", "Septfontaines");
		luxembourgLocalityList.put("Greiveldange", "Stadtbredimus");
		luxembourgLocalityList.put("Huettermuehle", "Stadtbredimus");
		luxembourgLocalityList.put("Stadtbredimus", "Stadtbredimus");
		luxembourgLocalityList.put("Grass", "Steinfort");
		luxembourgLocalityList.put("Hagen", "Steinfort");
		luxembourgLocalityList.put("Kleinbettingen", "Steinfort");
		luxembourgLocalityList.put("Steinfort", "Steinfort");
		luxembourgLocalityList.put("Heisdorf", "Steinsel");
		luxembourgLocalityList.put("Mullendorf", "Steinsel");
		luxembourgLocalityList.put("Steinsel", "Steinsel");
		luxembourgLocalityList.put("Strassen", "Strassen");
		luxembourgLocalityList.put("Bastendorf", "Tandel");
		luxembourgLocalityList.put("Bettel", "Tandel");
		luxembourgLocalityList.put("Brandenbourg", "Tandel");
		luxembourgLocalityList.put("Fouhren", "Tandel");
		luxembourgLocalityList.put("Hoscheidterhof", "Tandel");
		luxembourgLocalityList.put("Këppenhaff", "Tandel");
		luxembourgLocalityList.put("Landscheid", "Tandel");
		luxembourgLocalityList.put("Longsdorf", "Tandel");
		luxembourgLocalityList.put("Seltz", "Tandel");
		luxembourgLocalityList.put("Tandel", "Tandel");
		luxembourgLocalityList.put("Walsdorf", "Tandel");
		luxembourgLocalityList.put("Basbellain", "Troisvierges");
		luxembourgLocalityList.put("Biwisch", "Troisvierges");
		luxembourgLocalityList.put("Drinklange", "Troisvierges");
		luxembourgLocalityList.put("Goedange", "Troisvierges");
		luxembourgLocalityList.put("Hautbellain", "Troisvierges");
		luxembourgLocalityList.put("Huldange", "Troisvierges");
		luxembourgLocalityList.put("Troisvierges", "Troisvierges");
		luxembourgLocalityList.put("Wilwerdange", "Troisvierges");
		luxembourgLocalityList.put("Ansembourg", "Tuntange");
		luxembourgLocalityList.put("Bour", "Tuntange");
		luxembourgLocalityList.put("Hollenfels", "Tuntange");
		luxembourgLocalityList.put("Kuelbecherhaff", "Tuntange");
		luxembourgLocalityList.put("Marienthal", "Tuntange");
		luxembourgLocalityList.put("Tuntange", "Tuntange");
		luxembourgLocalityList.put("Everlange", "Useldange");
		luxembourgLocalityList.put("Rippweiler", "Useldange");
		luxembourgLocalityList.put("Schandel", "Useldange");
		luxembourgLocalityList.put("Useldange", "Useldange");
		luxembourgLocalityList.put("Eppeldorf", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Ermsdorf", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Folkendange", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Keiwelbach", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Medernach", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Savelborn", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Stegen", "Vallée de l'Ernz");
		luxembourgLocalityList.put("Vianden", "Vianden");
		luxembourgLocalityList.put("Michelbouch", "Vichten");
		luxembourgLocalityList.put("Vichten", "Vichten");
		luxembourgLocalityList.put("Brattert", "Wahl");
		luxembourgLocalityList.put("Buschrodt", "Wahl");
		luxembourgLocalityList.put("Grevels", "Wahl");
		luxembourgLocalityList.put("Heispelt", "Wahl");
		luxembourgLocalityList.put("Kuborn", "Wahl");
		luxembourgLocalityList.put("Rindschleiden", "Wahl");
		luxembourgLocalityList.put("Wahl", "Wahl");
		luxembourgLocalityList.put("Christnach", "Waldbillig");
		luxembourgLocalityList.put("Freckeisen", "Waldbillig");
		luxembourgLocalityList.put("Grundhof", "Waldbillig");
		luxembourgLocalityList.put("Haller", "Waldbillig");
		luxembourgLocalityList.put("Mullerthal", "Waldbillig");
		luxembourgLocalityList.put("Savelborn (Waldbillig)", "Waldbillig");
		luxembourgLocalityList.put("Waldbillig", "Waldbillig");
		luxembourgLocalityList.put("Ersange", "Waldbredimus");
		luxembourgLocalityList.put("Roedt", "Waldbredimus");
		luxembourgLocalityList.put("Trintange", "Waldbredimus");
		luxembourgLocalityList.put("Waldbredimus", "Waldbredimus");
		luxembourgLocalityList.put("Bereldange", "Walferdange");
		luxembourgLocalityList.put("Helmsange", "Walferdange");
		luxembourgLocalityList.put("Walferdange", "Walferdange");
		luxembourgLocalityList.put("Hassel", "Weiler-la-Tour");
		luxembourgLocalityList.put("Syren", "Weiler-la-Tour");
		luxembourgLocalityList.put("Weiler-la-Tour", "Weiler-la-Tour");
		luxembourgLocalityList.put("Beiler", "Weiswampach");
		luxembourgLocalityList.put("Binsfeld", "Weiswampach");
		luxembourgLocalityList.put("Breidfeld", "Weiswampach");
		luxembourgLocalityList.put("Holler", "Weiswampach");
		luxembourgLocalityList.put("Hollermühle", "Weiswampach");
		luxembourgLocalityList.put("Kaesfurt (Beiler)", "Weiswampach");
		luxembourgLocalityList.put("Kleemühle", "Weiswampach");
		luxembourgLocalityList.put("Lausdorn (Weiswampach)", "Weiswampach");
		luxembourgLocalityList.put("Leithum", "Weiswampach");
		luxembourgLocalityList.put("Maulusmühle (Weiswamp.)", "Weiswampach");
		luxembourgLocalityList.put("Rossmühle", "Weiswampach");
		luxembourgLocalityList.put("Weiswampach", "Weiswampach");
		luxembourgLocalityList.put("Wemperhardt", "Weiswampach");
		luxembourgLocalityList.put("Erpeldange (Eschweiler)", "Wiltz");
		luxembourgLocalityList.put("Eschweiler (Wiltz)", "Wiltz");
		luxembourgLocalityList.put("Knaphoscheid", "Wiltz");
		luxembourgLocalityList.put("Roullingen", "Wiltz");
		luxembourgLocalityList.put("Selscheid", "Wiltz");
		luxembourgLocalityList.put("Weidingen", "Wiltz");
		luxembourgLocalityList.put("Wiltz", "Wiltz");
		luxembourgLocalityList.put("Allerborn", "Wincrange");
		luxembourgLocalityList.put("Asselborn", "Wincrange");
		luxembourgLocalityList.put("Boevange", "Wincrange");
		luxembourgLocalityList.put("Boxhorn", "Wincrange");
		luxembourgLocalityList.put("Brachtenbach", "Wincrange");
		luxembourgLocalityList.put("Cinqfontaines", "Wincrange");
		luxembourgLocalityList.put("Crendal", "Wincrange");
		luxembourgLocalityList.put("Deiffelt", "Wincrange");
		luxembourgLocalityList.put("Derenbach", "Wincrange");
		luxembourgLocalityList.put("Doennange", "Wincrange");
		luxembourgLocalityList.put("Emeschbach Asselborn", "Wincrange");
		luxembourgLocalityList.put("Hachiville", "Wincrange");
		luxembourgLocalityList.put("Hamiville", "Wincrange");
		luxembourgLocalityList.put("Hinterhassel", "Wincrange");
		luxembourgLocalityList.put("Hoffelt", "Wincrange");
		luxembourgLocalityList.put("Lentzweiler", "Wincrange");
		luxembourgLocalityList.put("Lullange", "Wincrange");
		luxembourgLocalityList.put("Maulusmuehle", "Wincrange");
		luxembourgLocalityList.put("Niederwampach", "Wincrange");
		luxembourgLocalityList.put("Oberwampach", "Wincrange");
		luxembourgLocalityList.put("Rumlange", "Wincrange");
		luxembourgLocalityList.put("Sassel", "Wincrange");
		luxembourgLocalityList.put("Schimpach", "Wincrange");
		luxembourgLocalityList.put("Stockem", "Wincrange");
		luxembourgLocalityList.put("Troine", "Wincrange");
		luxembourgLocalityList.put("Troine-Route", "Wincrange");
		luxembourgLocalityList.put("Weiler", "Wincrange");
		luxembourgLocalityList.put("Wincrange", "Wincrange");
		luxembourgLocalityList.put("Berlé", "Winseler");
		luxembourgLocalityList.put("Doncols", "Winseler");
		luxembourgLocalityList.put("Gruemelscheid", "Winseler");
		luxembourgLocalityList.put("Noertrange", "Winseler");
		luxembourgLocalityList.put("Pommerloch", "Winseler");
		luxembourgLocalityList.put("Schleif", "Winseler");
		luxembourgLocalityList.put("Sonlez", "Winseler");
		luxembourgLocalityList.put("Winseler", "Winseler");
		luxembourgLocalityList.put("Ahn", "Wormeldange");
		luxembourgLocalityList.put("Dreiborn", "Wormeldange");
		luxembourgLocalityList.put("Ehnen", "Wormeldange");
		luxembourgLocalityList.put("Kapenacker", "Wormeldange");
		luxembourgLocalityList.put("Machtum", "Wormeldange");
		luxembourgLocalityList.put("Wormeldange", "Wormeldange");
		luxembourgLocalityList.put("Wormeldange-Haut", "Wormeldange");
	}
		
}
