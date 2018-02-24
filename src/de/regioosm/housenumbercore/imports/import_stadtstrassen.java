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
import java.sql.PreparedStatement;
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




import java.util.TreeMap;

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

	private static String getLuxembourgMunicipalityforSubarea(String subareaname) {

		TreeMap <String, String> locality = new TreeMap<String, String> ();
		locality.put("Beaufort", "Beaufort");
		locality.put("Dillingen", "Beaufort");
		locality.put("Grundhof (Beaufort)", "Beaufort");
		locality.put("Altrier", "Bech");
		locality.put("Bech", "Bech");
		locality.put("Blumenthal (Bech)", "Bech");
		locality.put("Geyershof (Bech)", "Bech");
		locality.put("Graulinster (Bech)", "Bech");
		locality.put("Hemstal", "Bech");
		locality.put("Hersberg", "Bech");
		locality.put("Kobenbour", "Bech");
		locality.put("Rippig", "Bech");
		locality.put("Zittig", "Bech");
		locality.put("Beckerich", "Beckerich");
		locality.put("Elvange", "Beckerich");
		locality.put("Hovelange", "Beckerich");
		locality.put("Huttange", "Beckerich");
		locality.put("Levelange", "Beckerich");
		locality.put("Noerdange", "Beckerich");
		locality.put("Oberpallen", "Beckerich");
		locality.put("Schweich", "Beckerich");
		locality.put("Berdorf", "Berdorf");
		locality.put("Bollendorf-Pont", "Berdorf");
		locality.put("Grundhof (Berdorf)", "Berdorf");
		locality.put("Kalkesbach (Berdorf)", "Berdorf");
		locality.put("Weilerbach", "Berdorf");
		locality.put("Bertrange", "Bertrange");
		locality.put("Abweiler", "Bettembourg");
		locality.put("Bettembourg", "Bettembourg");
		locality.put("Fennange", "Bettembourg");
		locality.put("Huncherange", "Bettembourg");
		locality.put("Noertzange", "Bettembourg");
		locality.put("Bettendorf", "Bettendorf");
		locality.put("Gilsdorf", "Bettendorf");
		locality.put("Moestroff", "Bettendorf");
		locality.put("Berg (Betzdorf)", "Betzdorf");
		locality.put("Betzdorf", "Betzdorf");
		locality.put("Mensdorf", "Betzdorf");
		locality.put("Olingen", "Betzdorf");
		locality.put("Roodt-sur-Syre", "Betzdorf");
		locality.put("Bissen", "Bissen");
		locality.put("Roost (Bissen)", "Bissen");
		locality.put("Biwer", "Biwer");
		locality.put("Biwerbach", "Biwer");
		locality.put("Boudler", "Biwer");
		locality.put("Boudlerbach", "Biwer");
		locality.put("Breinert", "Biwer");
		locality.put("Brouch (Wecker)", "Biwer");
		locality.put("Hagelsdorf", "Biwer");
		locality.put("Wecker", "Biwer");
		locality.put("Wecker-Gare", "Biwer");
		locality.put("Weydig", "Biwer");
		locality.put("Bill", "Boevange-sur-Attert");
		locality.put("Boevange-sur-Attert", "Boevange-sur-Attert");
		locality.put("Brouch (Mersch)", "Boevange-sur-Attert");
		locality.put("Buschdorf", "Boevange-sur-Attert");
		locality.put("Finsterthal", "Boevange-sur-Attert");
		locality.put("Grevenknapp", "Boevange-sur-Attert");
		locality.put("Openthalt", "Boevange-sur-Attert");
		locality.put("Baschleiden", "Boulaide");
		locality.put("Boulaide", "Boulaide");
		locality.put("Surré", "Boulaide");
		locality.put("Bourscheid", "Bourscheid");
		locality.put("Bourscheid Moulin", "Bourscheid");
		locality.put("Bourscheid-Plage", "Bourscheid");
		locality.put("Dirbach (Bourscheid)", "Bourscheid");
		locality.put("Flebour", "Bourscheid");
		locality.put("Friedbusch", "Bourscheid");
		locality.put("Goebelsmühle", "Bourscheid");
		locality.put("Kehmen", "Bourscheid");
		locality.put("Lipperscheid", "Bourscheid");
		locality.put("Michelau", "Bourscheid");
		locality.put("Scheidel", "Bourscheid");
		locality.put("Schlindermanderscheid", "Bourscheid");
		locality.put("Welscheid", "Bourscheid");
		locality.put("Assel", "Bous");
		locality.put("Bous", "Bous");
		locality.put("Erpeldange (Bous)", "Bous");
		locality.put("Rolling", "Bous");
		locality.put("Clervaux", "Clervaux");
		locality.put("Drauffelt", "Clervaux");
		locality.put("Eselborn", "Clervaux");
		locality.put("Fischbach (Clervaux)", "Clervaux");
		locality.put("Fossenhof", "Clervaux");
		locality.put("Grindhausen", "Clervaux");
		locality.put("Heinerscheid", "Clervaux");
		locality.put("Hupperdange", "Clervaux");
		locality.put("Kaesfurt (Heinerscheid)", "Clervaux");
		locality.put("Kalborn", "Clervaux");
		locality.put("Lausdorn (Heinerscheid)", "Clervaux");
		locality.put("Lieler", "Clervaux");
		locality.put("Marnach", "Clervaux");
		locality.put("Mecher (Clervaux)", "Clervaux");
		locality.put("Moulin de Kalborn", "Clervaux");
		locality.put("Munshausen", "Clervaux");
		locality.put("Reuler", "Clervaux");
		locality.put("Roder", "Clervaux");
		locality.put("Siebenaler", "Clervaux");
		locality.put("Tintesmühle", "Clervaux");
		locality.put("Urspelt", "Clervaux");
		locality.put("Weicherdange", "Clervaux");
		locality.put("Colmar-Berg", "Colmar-Berg");
		locality.put("Welsdorf", "Colmar-Berg");
		locality.put("Breidweiler", "Consdorf");
		locality.put("Colbette", "Consdorf");
		locality.put("Consdorf", "Consdorf");
		locality.put("Kalkesbach (Consdorf)", "Consdorf");
		locality.put("Marscherwald", "Consdorf");
		locality.put("Scheidgen", "Consdorf");
		locality.put("Wolper", "Consdorf");
		locality.put("Contern", "Contern");
		locality.put("Medingen", "Contern");
		locality.put("Moutfort", "Contern");
		locality.put("Oetrange", "Contern");
		locality.put("Dalheim", "Dalheim");
		locality.put("Filsdorf", "Dalheim");
		locality.put("Welfrange", "Dalheim");
		locality.put("Diekirch", "Diekirch");
		locality.put("Differdange", "Differdange");
		locality.put("Lasauvage", "Differdange");
		locality.put("Niederkorn", "Differdange");
		locality.put("Oberkorn", "Differdange");
		locality.put("Bettange-sur-Mess", "Dippach");
		locality.put("Dippach", "Dippach");
		locality.put("Schouweiler", "Dippach");
		locality.put("Sprinkange", "Dippach");
		locality.put("Dudelange", "Dudelange");
		locality.put("Echternach", "Echternach");
		locality.put("Colpach-Bas", "Ell");
		locality.put("Colpach-Haut", "Ell");
		locality.put("Ell", "Ell");
		locality.put("Petit-Nobressart", "Ell");
		locality.put("Roodt (Ell)", "Ell");
		locality.put("Burden", "Erpeldange-sur-Sûre");
		locality.put("Erpeldange-sur-Sûre", "Erpeldange-sur-Sûre");
		locality.put("Ingeldorf", "Erpeldange-sur-Sûre");
		locality.put("Esch-sur-Alzette", "Esch-sur-Alzette");
		locality.put("Bonnal", "Esch-sur-Sûre");
		locality.put("Dirbach", "Esch-sur-Sûre");
		locality.put("Esch-sur-Sûre", "Esch-sur-Sûre");
		locality.put("Eschdorf", "Esch-sur-Sûre");
		locality.put("Heiderscheid", "Esch-sur-Sûre");
		locality.put("Heiderscheidergrund", "Esch-sur-Sûre");
		locality.put("Hierheck", "Esch-sur-Sûre");
		locality.put("Insenborn", "Esch-sur-Sûre");
		locality.put("Lultzhausen", "Esch-sur-Sûre");
		locality.put("Merscheid", "Esch-sur-Sûre");
		locality.put("Neunhausen", "Esch-sur-Sûre");
		locality.put("Ringel", "Esch-sur-Sûre");
		locality.put("Tadler", "Esch-sur-Sûre");
		locality.put("Ettelbruck", "Ettelbruck");
		locality.put("Warken", "Ettelbruck");
		locality.put("Niederfeulen", "Feulen");
		locality.put("Oberfeulen", "Feulen");
		locality.put("Angelsberg", "Fischbach");
		locality.put("Fischbach (Mersch)", "Fischbach");
		locality.put("Koedange", "Fischbach");
		locality.put("Schiltzberg", "Fischbach");
		locality.put("Schoos", "Fischbach");
		locality.put("Stuppicht", "Fischbach");
		locality.put("Weyer (Junglinster)", "Fischbach");
		locality.put("Beyren", "Flaxweiler");
		locality.put("Flaxweiler", "Flaxweiler");
		locality.put("Gostingen", "Flaxweiler");
		locality.put("Niederdonven", "Flaxweiler");
		locality.put("Oberdonven", "Flaxweiler");
		locality.put("Aspelt", "Frisange");
		locality.put("Frisange", "Frisange");
		locality.put("Hellange", "Frisange");
		locality.put("Dahlem", "Garnich");
		locality.put("Garnich", "Garnich");
		locality.put("Hivange", "Garnich");
		locality.put("Kahler", "Garnich");
		locality.put("Bockholtz (Goesdorf)", "Goesdorf");
		locality.put("Buederscheid", "Goesdorf");
		locality.put("Dahl", "Goesdorf");
		locality.put("Dirbach (Goesdorf)", "Goesdorf");
		locality.put("Goebelsmühle (Goesdorf)", "Goesdorf");
		locality.put("Goesdorf", "Goesdorf");
		locality.put("Masseler", "Goesdorf");
		locality.put("Nocher", "Goesdorf");
		locality.put("Nocher Route", "Goesdorf");
		locality.put("Grevenmacher", "Grevenmacher");
		locality.put("Dellen", "Grosbous");
		locality.put("Grevels (Grosbous)", "Grosbous");
		locality.put("Grosbous", "Grosbous");
		locality.put("Lehrhof", "Grosbous");
		locality.put("Heffingen", "Heffingen");
		locality.put("Reuland", "Heffingen");
		locality.put("Alzingen", "Hesperange");
		locality.put("Fentange", "Hesperange");
		locality.put("Hesperange", "Hesperange");
		locality.put("Howald", "Hesperange");
		locality.put("Itzig", "Hesperange");
		locality.put("Eischen", "Hobscheid");
		locality.put("Hobscheid", "Hobscheid");
		locality.put("Altlinster", "Junglinster");
		locality.put("Beidweiler", "Junglinster");
		locality.put("Blumenthal", "Junglinster");
		locality.put("Bourglinster", "Junglinster");
		locality.put("Eisenborn", "Junglinster");
		locality.put("Eschweiler", "Junglinster");
		locality.put("Godbrange", "Junglinster");
		locality.put("Gonderange", "Junglinster");
		locality.put("Graulinster", "Junglinster");
		locality.put("Imbringen", "Junglinster");
		locality.put("Junglinster", "Junglinster");
		locality.put("Rodenbourg", "Junglinster");
		locality.put("Kayl", "Kayl");
		locality.put("Tétange", "Kayl");
		locality.put("Dondelange", "Kehlen");
		locality.put("Kehlen", "Kehlen");
		locality.put("Keispelt", "Kehlen");
		locality.put("Meispelt", "Kehlen");
		locality.put("Nospelt", "Kehlen");
		locality.put("Olm", "Kehlen");
		locality.put("Alscheid", "Kiischpelt");
		locality.put("Enscherange", "Kiischpelt");
		locality.put("Kautenbach", "Kiischpelt");
		locality.put("Lellingen", "Kiischpelt");
		locality.put("Merkholtz", "Kiischpelt");
		locality.put("Pintsch", "Kiischpelt");
		locality.put("Wilwerwiltz", "Kiischpelt");
		locality.put("Goeblange", "Koerich");
		locality.put("Goetzingen", "Koerich");
		locality.put("Koerich", "Koerich");
		locality.put("Windhof (Koerich)", "Koerich");
		locality.put("Bridel", "Kopstal");
		locality.put("Kopstal", "Kopstal");
		locality.put("Bascharage", "Käerjeng");
		locality.put("Clemency", "Käerjeng");
		locality.put("Fingig", "Käerjeng");
		locality.put("Hautcharage", "Käerjeng");
		locality.put("Linger", "Käerjeng");
		locality.put("Bavigne", "Lac de la Haute-Sûre");
		locality.put("Harlange", "Lac de la Haute-Sûre");
		locality.put("Kaundorf", "Lac de la Haute-Sûre");
		locality.put("Liefrange", "Lac de la Haute-Sûre");
		locality.put("Mecher (Haute-Sûre)", "Lac de la Haute-Sûre");
		locality.put("Nothum", "Lac de la Haute-Sûre");
		locality.put("Tarchamps", "Lac de la Haute-Sûre");
		locality.put("Watrange", "Lac de la Haute-Sûre");
		locality.put("Ernzen", "Larochette");
		locality.put("Larochette", "Larochette");
		locality.put("Meysembourg", "Larochette");
		locality.put("Canach", "Lenningen");
		locality.put("Lenningen", "Lenningen");
		locality.put("Leudelange", "Leudelange");
		locality.put("Gosseldange", "Lintgen");
		locality.put("Lintgen", "Lintgen");
		locality.put("Prettingen", "Lintgen");
		locality.put("Asselscheuer", "Lorentzweiler");
		locality.put("Blaschette", "Lorentzweiler");
		locality.put("Bofferdange", "Lorentzweiler");
		locality.put("Helmdange", "Lorentzweiler");
		locality.put("Hunsdorf", "Lorentzweiler");
		locality.put("Klingelscheuer", "Lorentzweiler");
		locality.put("Lorentzweiler", "Lorentzweiler");
		locality.put("Luxembourg", "Luxembourg");
		locality.put("Capellen", "Mamer");
		locality.put("Holzem", "Mamer");
		locality.put("Mamer", "Mamer");
		locality.put("Berbourg", "Manternach");
		locality.put("Lellig", "Manternach");
		locality.put("Manternach", "Manternach");
		locality.put("Münschecker", "Manternach");
		locality.put("Beringen", "Mersch");
		locality.put("Essingen", "Mersch");
		locality.put("Mersch", "Mersch");
		locality.put("Moesdorf (Mersch)", "Mersch");
		locality.put("Pettingen", "Mersch");
		locality.put("Reckange (Mersch)", "Mersch");
		locality.put("Rollingen", "Mersch");
		locality.put("Schoenfels", "Mersch");
		locality.put("Mertert", "Mertert");
		locality.put("Wasserbillig", "Mertert");
		locality.put("Mertzig", "Mertzig");
		locality.put("Born", "Mompach");
		locality.put("Boursdorf", "Mompach");
		locality.put("Givenich", "Mompach");
		locality.put("Herborn", "Mompach");
		locality.put("Lilien", "Mompach");
		locality.put("Moersdorf", "Mompach");
		locality.put("Mompach", "Mompach");
		locality.put("Bergem", "Mondercange");
		locality.put("Foetz", "Mondercange");
		locality.put("Mondercange", "Mondercange");
		locality.put("Pontpierre", "Mondercange");
		locality.put("Altwies", "Mondorf-les-Bains");
		locality.put("Ellange", "Mondorf-les-Bains");
		locality.put("Mondorf-les-Bains", "Mondorf-les-Bains");
		locality.put("Ernster", "Niederanven");
		locality.put("Hostert", "Niederanven");
		locality.put("Niederanven", "Niederanven");
		locality.put("Oberanven", "Niederanven");
		locality.put("Rameldange", "Niederanven");
		locality.put("Senningen", "Niederanven");
		locality.put("Senningerberg", "Niederanven");
		locality.put("Waldhof", "Niederanven");
		locality.put("Cruchten", "Nommern");
		locality.put("Niederglabach", "Nommern");
		locality.put("Nommern", "Nommern");
		locality.put("Oberglabach", "Nommern");
		locality.put("Schrondweiler", "Nommern");
		locality.put("Bockholtz", "Parc Hosingen");
		locality.put("Consthum", "Parc Hosingen");
		locality.put("Dorscheid", "Parc Hosingen");
		locality.put("Eisenbach", "Parc Hosingen");
		locality.put("Holzthum", "Parc Hosingen");
		locality.put("Hoscheid", "Parc Hosingen");
		locality.put("Hoscheid-Dickt", "Parc Hosingen");
		locality.put("Hosingen", "Parc Hosingen");
		locality.put("Neidhausen", "Parc Hosingen");
		locality.put("Oberschlinder", "Parc Hosingen");
		locality.put("Rodershausen", "Parc Hosingen");
		locality.put("Unterschlinder", "Parc Hosingen");
		locality.put("Wahlhausen", "Parc Hosingen");
		locality.put("Bettborn", "Préizerdaul");
		locality.put("Platen", "Préizerdaul");
		locality.put("Pratz", "Préizerdaul");
		locality.put("Reimberg", "Préizerdaul");
		locality.put("Bivels", "Putscheid");
		locality.put("Gralingen", "Putscheid");
		locality.put("Merscheid (Pütscheid)", "Putscheid");
		locality.put("Nachtmanderscheid", "Putscheid");
		locality.put("Pütscheid", "Putscheid");
		locality.put("Stolzembourg", "Putscheid");
		locality.put("Weiler (Pütscheid)", "Putscheid");
		locality.put("Lamadelaine", "Pétange");
		locality.put("Pétange", "Pétange");
		locality.put("Rodange", "Pétange");
		locality.put("Arsdorf", "Rambrouch");
		locality.put("Bigonville", "Rambrouch");
		locality.put("Bigonville-Poteau", "Rambrouch");
		locality.put("Bilsdorf", "Rambrouch");
		locality.put("Eschette", "Rambrouch");
		locality.put("Flatzbour", "Rambrouch");
		locality.put("Folschette", "Rambrouch");
		locality.put("Haut-Martelange", "Rambrouch");
		locality.put("Holtz", "Rambrouch");
		locality.put("Hostert (Rambrouch)", "Rambrouch");
		locality.put("Koetschette", "Rambrouch");
		locality.put("Perlé", "Rambrouch");
		locality.put("Rambrouch", "Rambrouch");
		locality.put("Riesenhof", "Rambrouch");
		locality.put("Rombach-Martelange", "Rambrouch");
		locality.put("Wolwelange", "Rambrouch");
		locality.put("Ehlange", "Reckange-sur-Mess");
		locality.put("Limpach", "Reckange-sur-Mess");
		locality.put("Pissange", "Reckange-sur-Mess");
		locality.put("Reckange-sur-Mess", "Reckange-sur-Mess");
		locality.put("Roedgen", "Reckange-sur-Mess");
		locality.put("Wickrange", "Reckange-sur-Mess");
		locality.put("Eltz", "Redange/Attert");
		locality.put("Lannen", "Redange/Attert");
		locality.put("Nagem", "Redange/Attert");
		locality.put("Niederpallen", "Redange/Attert");
		locality.put("Ospern", "Redange/Attert");
		locality.put("Redange/Attert", "Redange/Attert");
		locality.put("Reichlange", "Redange/Attert");
		locality.put("Bigelbach", "Reisdorf");
		locality.put("Hoesdorf", "Reisdorf");
		locality.put("Reisdorf", "Reisdorf");
		locality.put("Wallendorf-Pont", "Reisdorf");
		locality.put("Remich", "Remich");
		locality.put("Berchem", "Roeser");
		locality.put("Bivange", "Roeser");
		locality.put("Crauthem", "Roeser");
		locality.put("Kockelscheuer (Roeser)", "Roeser");
		locality.put("Livange", "Roeser");
		locality.put("Peppange", "Roeser");
		locality.put("Roeser", "Roeser");
		locality.put("Dickweiler", "Rosport");
		locality.put("Girst", "Rosport");
		locality.put("Girsterklaus", "Rosport");
		locality.put("Hinkel", "Rosport");
		locality.put("Osweiler", "Rosport");
		locality.put("Rosport", "Rosport");
		locality.put("Steinheim", "Rosport");
		locality.put("Rumelange", "Rumelange");
		locality.put("Calmus", "Saeul");
		locality.put("Ehner", "Saeul");
		locality.put("Kapweiler", "Saeul");
		locality.put("Saeul", "Saeul");
		locality.put("Schwebach", "Saeul");
		locality.put("Findel", "Sandweiler");
		locality.put("Sandweiler", "Sandweiler");
		locality.put("Belvaux", "Sanem");
		locality.put("Ehlerange", "Sanem");
		locality.put("Sanem", "Sanem");
		locality.put("Soleuvre", "Sanem");
		locality.put("Bech-Kleinmacher", "Schengen");
		locality.put("Burmerange", "Schengen");
		locality.put("Elvange (Schengen)", "Schengen");
		locality.put("Emerange", "Schengen");
		locality.put("Remerschen", "Schengen");
		locality.put("Schengen", "Schengen");
		locality.put("Schwebsingen", "Schengen");
		locality.put("Wellenstein", "Schengen");
		locality.put("Wintrange", "Schengen");
		locality.put("Colmar-Pont", "Schieren");
		locality.put("Schieren", "Schieren");
		locality.put("Schifflange", "Schifflange");
		locality.put("Münsbach", "Schuttrange");
		locality.put("Neuhaeusgen", "Schuttrange");
		locality.put("Schrassig", "Schuttrange");
		locality.put("Schuttrange", "Schuttrange");
		locality.put("Uebersyren", "Schuttrange");
		locality.put("Greisch", "Septfontaines");
		locality.put("Leesbach", "Septfontaines");
		locality.put("Roodt (Septfontaines)", "Septfontaines");
		locality.put("Septfontaines", "Septfontaines");
		locality.put("Simmerfarm", "Septfontaines");
		locality.put("Simmerschmelz", "Septfontaines");
		locality.put("Greiveldange", "Stadtbredimus");
		locality.put("Huettermuehle", "Stadtbredimus");
		locality.put("Stadtbredimus", "Stadtbredimus");
		locality.put("Grass", "Steinfort");
		locality.put("Hagen", "Steinfort");
		locality.put("Kleinbettingen", "Steinfort");
		locality.put("Steinfort", "Steinfort");
		locality.put("Heisdorf", "Steinsel");
		locality.put("Mullendorf", "Steinsel");
		locality.put("Steinsel", "Steinsel");
		locality.put("Strassen", "Strassen");
		locality.put("Bastendorf", "Tandel");
		locality.put("Bettel", "Tandel");
		locality.put("Brandenbourg", "Tandel");
		locality.put("Fouhren", "Tandel");
		locality.put("Hoscheidterhof", "Tandel");
		locality.put("Këppenhaff", "Tandel");
		locality.put("Landscheid", "Tandel");
		locality.put("Longsdorf", "Tandel");
		locality.put("Seltz", "Tandel");
		locality.put("Tandel", "Tandel");
		locality.put("Walsdorf", "Tandel");
		locality.put("Basbellain", "Troisvierges");
		locality.put("Biwisch", "Troisvierges");
		locality.put("Drinklange", "Troisvierges");
		locality.put("Goedange", "Troisvierges");
		locality.put("Hautbellain", "Troisvierges");
		locality.put("Huldange", "Troisvierges");
		locality.put("Troisvierges", "Troisvierges");
		locality.put("Wilwerdange", "Troisvierges");
		locality.put("Ansembourg", "Tuntange");
		locality.put("Bour", "Tuntange");
		locality.put("Hollenfels", "Tuntange");
		locality.put("Kuelbecherhaff", "Tuntange");
		locality.put("Marienthal", "Tuntange");
		locality.put("Tuntange", "Tuntange");
		locality.put("Everlange", "Useldange");
		locality.put("Rippweiler", "Useldange");
		locality.put("Schandel", "Useldange");
		locality.put("Useldange", "Useldange");
		locality.put("Eppeldorf", "Vallée de l'Ernz");
		locality.put("Ermsdorf", "Vallée de l'Ernz");
		locality.put("Folkendange", "Vallée de l'Ernz");
		locality.put("Keiwelbach", "Vallée de l'Ernz");
		locality.put("Medernach", "Vallée de l'Ernz");
		locality.put("Savelborn", "Vallée de l'Ernz");
		locality.put("Stegen", "Vallée de l'Ernz");
		locality.put("Vianden", "Vianden");
		locality.put("Michelbouch", "Vichten");
		locality.put("Vichten", "Vichten");
		locality.put("Brattert", "Wahl");
		locality.put("Buschrodt", "Wahl");
		locality.put("Grevels", "Wahl");
		locality.put("Heispelt", "Wahl");
		locality.put("Kuborn", "Wahl");
		locality.put("Rindschleiden", "Wahl");
		locality.put("Wahl", "Wahl");
		locality.put("Christnach", "Waldbillig");
		locality.put("Freckeisen", "Waldbillig");
		locality.put("Grundhof", "Waldbillig");
		locality.put("Haller", "Waldbillig");
		locality.put("Mullerthal", "Waldbillig");
		locality.put("Savelborn (Waldbillig)", "Waldbillig");
		locality.put("Waldbillig", "Waldbillig");
		locality.put("Ersange", "Waldbredimus");
		locality.put("Roedt", "Waldbredimus");
		locality.put("Trintange", "Waldbredimus");
		locality.put("Waldbredimus", "Waldbredimus");
		locality.put("Bereldange", "Walferdange");
		locality.put("Helmsange", "Walferdange");
		locality.put("Walferdange", "Walferdange");
		locality.put("Hassel", "Weiler-la-Tour");
		locality.put("Syren", "Weiler-la-Tour");
		locality.put("Weiler-la-Tour", "Weiler-la-Tour");
		locality.put("Beiler", "Weiswampach");
		locality.put("Binsfeld", "Weiswampach");
		locality.put("Breidfeld", "Weiswampach");
		locality.put("Holler", "Weiswampach");
		locality.put("Hollermühle", "Weiswampach");
		locality.put("Kaesfurt (Beiler)", "Weiswampach");
		locality.put("Kleemühle", "Weiswampach");
		locality.put("Lausdorn (Weiswampach)", "Weiswampach");
		locality.put("Leithum", "Weiswampach");
		locality.put("Maulusmühle (Weiswamp.)", "Weiswampach");
		locality.put("Rossmühle", "Weiswampach");
		locality.put("Weiswampach", "Weiswampach");
		locality.put("Wemperhardt", "Weiswampach");
		locality.put("Erpeldange (Eschweiler)", "Wiltz");
		locality.put("Eschweiler (Wiltz)", "Wiltz");
		locality.put("Knaphoscheid", "Wiltz");
		locality.put("Roullingen", "Wiltz");
		locality.put("Selscheid", "Wiltz");
		locality.put("Weidingen", "Wiltz");
		locality.put("Wiltz", "Wiltz");
		locality.put("Allerborn", "Wincrange");
		locality.put("Asselborn", "Wincrange");
		locality.put("Boevange", "Wincrange");
		locality.put("Boxhorn", "Wincrange");
		locality.put("Brachtenbach", "Wincrange");
		locality.put("Cinqfontaines", "Wincrange");
		locality.put("Crendal", "Wincrange");
		locality.put("Deiffelt", "Wincrange");
		locality.put("Derenbach", "Wincrange");
		locality.put("Doennange", "Wincrange");
		locality.put("Emeschbach Asselborn", "Wincrange");
		locality.put("Hachiville", "Wincrange");
		locality.put("Hamiville", "Wincrange");
		locality.put("Hinterhassel", "Wincrange");
		locality.put("Hoffelt", "Wincrange");
		locality.put("Lentzweiler", "Wincrange");
		locality.put("Lullange", "Wincrange");
		locality.put("Maulusmuehle", "Wincrange");
		locality.put("Niederwampach", "Wincrange");
		locality.put("Oberwampach", "Wincrange");
		locality.put("Rumlange", "Wincrange");
		locality.put("Sassel", "Wincrange");
		locality.put("Schimpach", "Wincrange");
		locality.put("Stockem", "Wincrange");
		locality.put("Troine", "Wincrange");
		locality.put("Troine-Route", "Wincrange");
		locality.put("Weiler", "Wincrange");
		locality.put("Wincrange", "Wincrange");
		locality.put("Berlé", "Winseler");
		locality.put("Doncols", "Winseler");
		locality.put("Gruemelscheid", "Winseler");
		locality.put("Noertrange", "Winseler");
		locality.put("Pommerloch", "Winseler");
		locality.put("Schleif", "Winseler");
		locality.put("Sonlez", "Winseler");
		locality.put("Winseler", "Winseler");
		locality.put("Ahn", "Wormeldange");
		locality.put("Dreiborn", "Wormeldange");
		locality.put("Ehnen", "Wormeldange");
		locality.put("Kapenacker", "Wormeldange");
		locality.put("Machtum", "Wormeldange");
		locality.put("Wormeldange", "Wormeldange");
		locality.put("Wormeldange-Haut", "Wormeldange");
		
		if(locality.get(subareaname) != null)
			return locality.get(subareaname);
		else
			return "";
	}


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
				String[] kopfspalten = null;

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
							kopfspalten = dateizeile.split(fieldSeparator);
							for (int spaltei = 0; spaltei < kopfspalten.length; spaltei++) {
								if(	kopfspalten[spaltei].toLowerCase().equals("stadt") || 
									kopfspalten[spaltei].toLowerCase().equals("addr:city") ||
									kopfspalten[spaltei].toLowerCase().equals("gemeinde") ||
									kopfspalten[spaltei].toLowerCase().equals("commune")) {
									spaltenindexStadt = spaltei;
								}
								if(	kopfspalten[spaltei].toLowerCase().equals("stadtid") ||
									kopfspalten[spaltei].toLowerCase().equals("gemeindeid") ||
									kopfspalten[spaltei].toLowerCase().equals("gemeinde_id") ||
									kopfspalten[spaltei].toLowerCase().equals("gemeinde-id")) {
									spaltenindexAgs = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("straße") ||
									kopfspalten[spaltei].toLowerCase().equals("strasse") ||
									kopfspalten[spaltei].toLowerCase().equals("rue")) {
									spaltenindexStrasse = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("straße-id") ||
									kopfspalten[spaltei].toLowerCase().equals("straßeid") ||
									kopfspalten[spaltei].toLowerCase().equals("strasseid") ||
									kopfspalten[spaltei].toLowerCase().equals("strasse-id") ||
									kopfspalten[spaltei].toLowerCase().equals("id_caclr_rue")) {
									spaltenindexStrasseId = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("straßeupper") ||
									kopfspalten[spaltei].toLowerCase().equals("strasseupper")) {
									spaltenindexStrasse = spaltei;
									strasseninhaltschreibweise = "upper";
								}
								if (kopfspalten[spaltei].toLowerCase().equals("postcode") ||
									kopfspalten[spaltei].toLowerCase().equals("plz") ||
									kopfspalten[spaltei].toLowerCase().equals("postleitzahl") ||
									kopfspalten[spaltei].toLowerCase().equals("code_postal")) {
									spaltenindexPostcode = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("hausnummer") ||
									kopfspalten[spaltei].toLowerCase().equals("numero")) {
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
									kopfspalten[spaltei].toLowerCase().equals("längengrad") ||
									kopfspalten[spaltei].toLowerCase().equals("lon_wgs84")) {
									spaltenindexLaengengrad = spaltei;
								}
								if (kopfspalten[spaltei].toLowerCase().equals("lat") ||
									kopfspalten[spaltei].toLowerCase().equals("hw") ||
									kopfspalten[spaltei].toLowerCase().equals("breitengrad") ||
									kopfspalten[spaltei].toLowerCase().equals("lat_wgs84")) {
									spaltenindexBreitengrad = spaltei;
								}
								if(	kopfspalten[spaltei].toLowerCase().equals("subid") ||
									kopfspalten[spaltei].toLowerCase().equals("sub_id") ||
									kopfspalten[spaltei].toLowerCase().equals("sub-id") ||
									kopfspalten[spaltei].toLowerCase().equals("localite")) {
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

					String[] spalten = dateizeile.split(fieldSeparator);

						// Zeile mit falscher Spaltenanzahl überspringen
					if (spalten.length < FILECOLUMNMINLENGTH) {
						Importlog("Info","Zeile_ungueltig", "zuwenig Spalten in Zeile # " + zeilenr + ", Dateizeile ===" + dateizeile + "===");
						continue;
					}

					OSMoutput(parameterQuellSrid, kopfspalten, spalten);

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

					if((parameterLand.equals("Luxembourg")) && stadt.equals("") && !subid.equals("")) {
						stadt = getLuxembourgMunicipalityforSubarea(subid);
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
