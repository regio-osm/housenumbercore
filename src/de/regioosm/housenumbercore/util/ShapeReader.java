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
import java.util.Collection;
import java.util.Date;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.PropertyType;
import org.opengis.filter.FilterFactory;
import org.opengis.geometry.DirectPosition;
import org.opengis.geometry.Geometry;


import de.regioosm.housenumbercore.util.ShapeImportparameter.HEADERFIELD;

/**
 * Import or update of a housenumber list from a municipality.
 * @author Dietmar Seifert
 *
 */
public class ShapeReader {

	private int lineno = 0;
	private DataStore dataStore = null;
	private FeatureIterator<SimpleFeature> addressRecords = null;
	//List<SimpleFeature> addressRecords = new ArrayList<>();

	private ShapeImportparameter importparameter = new ShapeImportparameter();

	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();


	/**
	 * Reader for a file in ESRI Shape-Format
	 * Parameter importlist must be filled out with a least importfile name and countrycode
	 * @param importlist
	 * @param charsetname	inputfile character set, for example ISO-8859-1 or UTF-8, see https://docs.oracle.com/javase/7/docs/api/java/nio/charset/Charset.html
	 */
	public ShapeReader(ShapeImportparameter importparameter) {
		if(	(importparameter.getImportfile() == null) ||
			importparameter.getImportfile().equals("")) {
			throw new IllegalArgumentException("At least Importfile must be specified in input parameter importparameter");
		}
		if(	(importparameter.getCountrycode() == null) ||
			importparameter.getCountrycode().equals("")) {
			throw new IllegalArgumentException("At least Country Code must be specified in input parameter importparameter");
		}

		this.importparameter = importparameter;

	}

	private String getFieldContent(SimpleFeature feature, HEADERFIELD field) {
		if((feature == null) || (feature.getProperties().size() == 0))
			return "";

		if(importparameter.getHeaderfieldColumn(field).equals("")) {
			return "";
		}
		String fieldname = importparameter.getHeaderfieldColumn(field);

		
        List<Object> attribute = feature.getAttributes();
        Collection<Property> properties = feature.getProperties();
        for(Property prop : properties) {
        	PropertyType propType = prop.getType();
        	if(propType.getClass().getSuperclass().getName().toString().equals("org.geotools.feature.type.PropertyTypeImpl")) {
            	if(prop.getName().toString().equals(fieldname) ) {
            		return prop.getValue().toString();
            	}
        	}
        }
		return "";
	}

	Pattern wildcardPattern = Pattern.compile("([A-ZÄÉÈÖÜĂÂÎŞŢ])([A-ZÄÉÈÖÜßĂÂÎŞŢ]*)");				// A to Z and all 5 special romania chars http://de.wikipedia.org/wiki/Rum%C3%A4nisches_Alphabet#Alphabet_und_Aussprache
	public String toUpperLowerCase(String text) {

		Matcher match = wildcardPattern.matcher(text);
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
/*				if(this.importparameter.getMunicipalityUpperLower.contains(foundstring.toLowerCase())) {
					replacetext = match.group(1).toLowerCase() + match.group(2).toLowerCase();
					matchFixToLowerCase = true;
				}
				if(uppercaselist.contains(foundstring.toUpperCase())) {
					replacetext = match.group(1).toUpperCase() + match.group(2).toUpperCase();
					matchFixToUpperCase = true;
				}
*/
				if(!matchFixToLowerCase && !matchFixToUpperCase)
					replacetext += match.group(2).toLowerCase();
			}
			match.appendReplacement(sb,replacetext);
			matchFind = match.find();
		}
		match.appendTail(sb);
		return sb.toString();
	}
	
	private boolean open() {
        try {
        	File file = new File(this.importparameter.getImportfile());
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("url", file.toURI().toURL());
            map.put("charset", this.importparameter.getImportfileFormat().toString());
            DataStore ds = DataStoreFinder.getDataStore(map);
            if (ds == null)
                throw new IllegalArgumentException("Cannot find DataStore at " + file);
            this.dataStore = ds;
        } catch (Exception e) {
	        e.printStackTrace();
	        this.dataStore = null;
	        return false;
        }
        return true;
	}

	
	private void fetchRecords() {
			// get features (all or filtered)
		if (this.dataStore == null)
			throw new IllegalArgumentException("DataStore cannot be null for getFeatureIterator");
	
		this.addressRecords = null;
		//FeatureIterator<SimpleFeature> filteredAddresses = new FeatureIterator<>();
		//List<SimpleFeature> filteredAddresses = new ArrayList<>();
		try {
			String typeName = this.dataStore.getTypeNames()[0];
			//FeatureSource<SimpleFeatureType, SimpleFeature> source = this.dataStore.getFeatureSource(typeName);
			FeatureSource source = this.dataStore.getFeatureSource(typeName);
			
			FilterFactory ff = CommonFactoryFinder.getFilterFactory( null );
Date vorher = new Date();
			//Filter filter = Filter.INCLUDE;	// 13.9 Mio (13903309)

			//Filter filter = ff.and(ff.equals(ff.property("REGIONE"), ff.literal("")),
			//	ff.notEqual(ff.property("COMUNE"), ff.literal(""))) ;	// 4.15 mio netto (3981177), brutto(4149410)
//Filter filter = ff.equals(ff.property("COMUNE"), ff.literal("MILANO"));
			//Filter filter = ff.and(ff.less(ff.property("ISTAT"), ff.literal("06000000")),
			//	ff.notEqual(ff.property("COMUNE"), ff.literal(""))) ;	// 4.15 mio netto (3981177), brutto(4149410)
			//Filter filter = ff.and(ff.between(ff.property("ISTAT"), ff.literal("06000000"), ff.literal("11999999")),
			//	ff.notEqual(ff.property("COMUNE"), ff.literal(""))) ;	// 3.8 mio netto (3798529), = brutto
			//Filter filter = ff.and(ff.between(ff.property("ISTAT"), ff.literal("12000000"), ff.literal("17999999")),
			//	ff.notEqual(ff.property("COMUNE"), ff.literal(""))) ;	// 3.6 mio netto (3595214), = brutto
			//Filter filter = ff.and(ff.greaterOrEqual(ff.property("ISTAT"), ff.literal("18000000")),
			//	ff.notEqual(ff.property("COMUNE"), ff.literal(""))) ;	// 2.4 mio netto (2360156), = brutto

			//FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures(filter);
FeatureCollection<SimpleFeatureType, SimpleFeature> collection = source.getFeatures();
System.out.println("Number Records in Collection: " + collection.size());
Date nachher = new Date();
System.out.println("Dauer in sek: " + (nachher.getTime() - vorher.getTime())/1000);
/*			FeatureIterator<SimpleFeature> iterator = (FeatureIterator<SimpleFeature>) collection.features();

			//Filter filter2 = ff.equals(ff.property("CAP"), ff.literal("92100"));
			try {
				int looper = 0;
				while (iterator.hasNext()) {
					looper++;
					if((looper % 100000) == 0)
						System.out.println("Loop " + looper);
					SimpleFeature next = iterator.next();
					if (filter2.evaluate(next)) {
						filteredAddresses.add(next);
					}
				}
			} finally {
				//Sha1SyncFilterFunction.clearThreadLocals();
				iterator.close();
			}
		System.out.println("Number Records in List: " + filteredAddresses.size());
*/			

			
			this.addressRecords = collection.features();
		} catch (Exception e) {
			e.printStackTrace();
			this.addressRecords = null;
		}
	}

	
	private ImportAddress next() throws Exception  {
		ImportAddress address = null;

		SimpleFeature addressFeature = null;
		Date vorher = new Date();
		int looper = 0;
        while (addressRecords.hasNext()) {
        	looper++;
            addressFeature = addressRecords.next();
if(!getFieldContent(addressFeature, HEADERFIELD.municipality).equals("MILANO"))
	continue;
            
Date nachher = new Date();
if ((looper > 5) || ((nachher.getTime() - vorher.getTime())/1000 > 5))
	System.out.println("Dauer in sek: " + (nachher.getTime() - vorher.getTime())/1000 + "   anzahl: " + looper);

			address = new ImportAddress();
			String region = "";
			String district = "";
			address.setCountrycode(importparameter.getCountrycode());
			
			address.setMunicipality(getFieldContent(addressFeature, HEADERFIELD.municipality));
			address.setMunicipality(toUpperLowerCase(address.getMunicipality()));

			address.setMunicipalityRef(getFieldContent(addressFeature, HEADERFIELD.municipalityref));
			if(	address.getMunicipality().equals("") && 
				(importparameter.getMunicipalityIDListEntry(getFieldContent(addressFeature, HEADERFIELD.municipalityref)) != null)) {
				address.setMunicipality(importparameter.getMunicipalityIDListEntry(getFieldContent(addressFeature, HEADERFIELD.municipalityref)));
			}

			address.setStreet(getFieldContent(addressFeature, HEADERFIELD.street));
//TODO Street_UpperLower(strasse)

			if(	getFieldContent(addressFeature, HEADERFIELD.streetid).equals("") &&
				(importparameter.getStreetIDListEntry(getFieldContent(addressFeature, HEADERFIELD.streetid)) != null)) {
				address.setStreet(importparameter.getStreetIDListEntry(getFieldContent(addressFeature, HEADERFIELD.streetid)));
//TODO Street_UpperLower(strasse)
			}
				
			address.setPostcode(getFieldContent(addressFeature, HEADERFIELD.postcode));

			String housenumber = getFieldContent(addressFeature, HEADERFIELD.housenumber);
			if(!importparameter.getHeaderfieldColumn(HEADERFIELD.housenumberaddition).equals("")) {
				housenumber += this.importparameter.getHousenumberFieldseparator();
				housenumber += getFieldContent(addressFeature, HEADERFIELD.housenumberaddition);
				if(!importparameter.getHeaderfieldColumn(HEADERFIELD.housenumberaddition2).equals("")) {
					housenumber += this.importparameter.getHousenumberFieldseparator2();
					housenumber += getFieldContent(addressFeature, HEADERFIELD.housenumberaddition2);
				}
			}
			address.setHousenumber(housenumber);

			if(!importparameter.getHeaderfieldColumn(HEADERFIELD.region).equals("")) {
				region = getFieldContent(addressFeature, HEADERFIELD.region);
				region = toUpperLowerCase(region);
			}

			if(!importparameter.getHeaderfieldColumn(HEADERFIELD.district).equals("")) {
				district = getFieldContent(addressFeature, HEADERFIELD.district);
				district = toUpperLowerCase(district);
			}

			if(!region.equals("") || !district.equals("")) {
				String adminhierarchy = Country.getCountryLongname(importparameter.getCountrycode());
				if (!region.equals(""))
					adminhierarchy += "," + region;
				if (!district.equals(""))
					adminhierarchy += "," + district;
				address.setAdminHierarchy(adminhierarchy);
			}

			address.setNote(getFieldContent(addressFeature, HEADERFIELD.note));

			if(!importparameter.getHeaderfieldColumn(HEADERFIELD.subarea).equals(""))
				address.setSubArea(getFieldContent(addressFeature, HEADERFIELD.subarea));

			if(	(!importparameter.getHeaderfieldColumn(HEADERFIELD.subareaid).equals("")) &&
				(importparameter.getSubareaMunicipalityIDListEntry(getFieldContent(addressFeature, HEADERFIELD.subareaid)) != null)) {
				address.setSubArea(importparameter.getStreetIDListEntry(getFieldContent(addressFeature, HEADERFIELD.subareaid)));
			}

				// handle Geometry, if available as point 
//TODO should be enhanced for other geometry type
        	String geomClass = addressFeature.getFeatureType().getGeometryDescriptor().getType().getName().toString();
        	if(!geomClass.equals("Point")) {
        		System.out.println("unexpected geomtry type ===" + geomClass + "===");
        	} else {
            	String actualsrid = addressFeature.getDefaultGeometryProperty().getType().getCoordinateReferenceSystem().getName().toString();
//TODO should be possible to handle other coordinate systems
            	if(actualsrid.equals("EPSG:WGS 84"))
            		address.setSourceSrid("4326");
            	else {
            		System.out.println("unknown Coordinate Reference System ===" + actualsrid + "===");
    				/*
    				 * some action to check srid value for text representation, which comes from Feature Object
    				String sourcesrid = importparameter.getSourceCoordinateSystem();
    				if(!importparameter.getHeaderfieldColumn(HEADERFIELD.sourcesrid).equals("")) {
    					sourcesrid = getFieldContent(addressFeature, HEADERFIELD.sourcesrid);
    					if(!sourcesrid.equals(importparameter.getSourceCoordinateSystem())) {
    						throw new IllegalArgumentException("coordinate system differs in addressFeature " + lineno + " '" + sourcesrid + "'" +
    							" from defined coordinate system '" + importparameter.getSourceCoordinateSystem() + "'");
    					}
    				}
    				address.setSourceSrid(sourcesrid);
    				*/
            	}

            	Geometry geometry = (Geometry) addressFeature.getDefaultGeometry();
            	DirectPosition centroidpos = geometry.getCentroid();
            	address.setLon(centroidpos.getOrdinate(0));
            	address.setLat(centroidpos.getOrdinate(1));
            	}

        	//TODO find out change of municipality (assuming, that input file is sorted by municipalities) and store after each municipality
//			alternatively, make an array of municipalities to hold all togehter in memory during import read process

			if (address.getStreet().equals("") || address.getHousenumber().equals("")) {
				if (address.getStreet().equals("")) {
					Importlog("Warning",  "Missing_Street", "no street name in fileline # " + 
						lineno + ", content ===" + addressFeature + "===, will be ignored");
				}
				if (address.getHousenumber().equals("")) {
					Importlog("Warning",  "Missing_Housenumber", "no housenumber in fileline # " + 
						lineno + ", content ===" + addressFeature + "===, will be ignored");
				}
			} else {
				break;
			}
		}
		if(addressFeature == null) {
			//TODO close file
			return null;
		}
		return address;
	}

	/**
	 * read complete input file containing house numbers for one or many municipalities.
	 * Please notify, that flag in municipality, if subareas are identifyable, could be set inside this method, depending on if input file contains a subarea column
	 * @return house numbers and possible municipality data in an array of municipalities with its house numbers
	 */
	public Map<Municipality, HousenumberList> execute() {
		Map<Municipality, HousenumberList> lists = new HashMap<>();

		int importedAddresses = 0;
		int doubledAdresses = 0;

		ImportAddress address = null;
		try {
			if(!open())
				return lists;
			fetchRecords();
			while((address = next()) != null) {
				importedAddresses++;
				if((importedAddresses % 1000) == 0)
					System.out.println("Address count: " + importedAddresses + ", Municipality count: " + lists.size());
				if(address.getCountrycode().equals("IT") && !address.getMunicipalityRef().equals("")) {
					address.setMunicipalityRef(address.getMunicipalityRef().substring(address.getMunicipalityRef().length() - 6, address.getMunicipalityRef().length()));
				}
				Municipality municipality = new Municipality(address.getCountrycode(), 
					address.getMunicipality(), address.getMunicipalityRef());
				if (!address.getSubArea().equals(Address.subareaUnset))
					municipality.setSubareasidentifyable(true);
				HousenumberList housenumberlist = null;
				if(lists.containsKey(municipality))
					housenumberlist = lists.get(municipality);
				else {
					housenumberlist = new HousenumberList(municipality);
					System.out.println("Address count: " + importedAddresses + ", Municipality count: " + lists.size());
				}
				if(!housenumberlist.contains(address)) {
					housenumberlist.addHousenumber(address);
					if ((address.getAdminHierarchy() != null) && !address.getAdminHierarchy().equals(""))
						housenumberlist.setHierarchy(address.getAdminHierarchy());
					lists.put(municipality,  housenumberlist);
				} else {
					System.out.println("Hausnummer doppelt in Zeile " + lineno + ": " + address.toString());
					doubledAdresses++;
				}
			}
			addressRecords.close();
			dataStore.dispose();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Anzahl Dateizeilen: " + lineno + ", davon mehrfache adressen: " + doubledAdresses);
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
}
