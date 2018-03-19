package de.regioosm.housenumbercore.imports;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.regioosm.housenumbercore.Reference;
import de.regioosm.housenumbercore.util.Applicationconfiguration;
import de.regioosm.housenumbercore.util.ImportAddress;


/**
 * Import a housenumber list from a municipality. File is in gml xml-Format. Until now (10/2014), 
 * the xml-tags are justified for Hamburg gml file. It could be necessary to justify for other municipality lists
 * 
 * @author Dietmar Seifert
 * @since 2014-10-17
 *
 */

public class import_stadtstrassenGENERICGml {
	static String gmlFeaturecollection = "";
	static String gmlFeaturemembers = "";
	static String gmlFeaturemember = "";
	static String gmlReferenceobject = "";
    static String AddressobjectnameXpathAbspos = "";
    static String PointXpathRelpos = "";
    static String MunicipalitynameXpathRelpos = "";
    static String MunicipalityidXpathRelpos = "";
    static String MunicipalityfilereferenceXpathRelpos = "";
	static String MunicipalityfilereferenceSolveXpathAbspos = "";
	static String MunicipalityfilereferenceSolveNameXpathRelpos = "";
	static String MunicipalityfilereferenceSolveIdXpathRelpos = "";
    static String SubareanameXpathRelpos = "";
    static String SubidXpathRelpos = "";
    static String PostcodeXpathRelpos = "";
    static String PlacenameXpathRelpos = "";
    static String StreetnameXpathRelpos = "";
    static String HousenumberXpathRelpos = "";
    static String HousenumberadditionXpathRelpos = "";
    static String HousenumberobjectidXpathRelpos = "";
	static String statusXpathRelpos = "";


	static StringBuffer outputbuffer = new StringBuffer();
	static StringBuffer osmoutputbuffer = new StringBuffer();

	static StringBuffer readerHeader = new StringBuffer();
//	static String readerReststring = "";
	static Integer readerStartpos = 0; 

	static Integer count_addresses = 0;

	
	private static HashMap<String, String> streetnamecorrectionmap = new HashMap<String, String>();

	private static Statement stmt = null;

	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	/**
	 * DB connection to Hausnummern
	 */
	private static Connection conHausnummern = null;


	static HashMap<String, Reference> references = new HashMap<String, Reference>();
	


//TODO use original form import_stadtstrassen
		// copied 2015-05-24 from import_stadtstrassen.java
	private static ArrayList<String> uppercaselist = new ArrayList<>();
	private static ArrayList<String> lowercaselist = new ArrayList<>();
	private static String capitalize(String street) {
		Pattern wildcardPattern = Pattern.compile("([A-ZÄÖÜĂÂÎŞŢÉËÚ])([A-ZÄÖÜßĂÂÎŞŢÉËÚ]*)");				// A to Z and all 5 special romania chars http://de.wikipedia.org/wiki/Rum%C3%A4nisches_Alphabet#Alphabet_und_Aussprache
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


	private static Integer analyseMunicipalityReference(Document xmldocument) {

		Date method_starttime = new Date();
		Date method_endtime = new Date();
		Date evaluate_starttime = new Date();
		Date evaluate_endtime = new Date();
		Long xpathms = 0L;
    	Long bufferoutputms = 0L;
		
		evaluate_starttime = new Date();
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression referencesxpath;

		
		Integer numberrecords = 0;
		try {
			String munirefxpathstring = MunicipalityfilereferenceSolveXpathAbspos;
			referencesxpath = xpath.compile(munirefxpathstring);		///gml:/

			NodeList referencesnodes = (NodeList) referencesxpath.evaluate(xmldocument, XPathConstants.NODESET);
			evaluate_endtime = new Date();

			for (int referenceindex = 0; referenceindex < referencesnodes.getLength(); referenceindex++) {

				Reference reference = new Reference();
				String referenceId = "";

		    	String actxpathstring;
				NodeList actnodes;
				Date xpath_starttime = new Date();
				Date xpath_endtime = new Date();
				
				Node actreference = (Node) referencesnodes.item(referenceindex);
		    	//System.out.println(" reference # " + referenceindex + "  [" + actreference.getNodeName() + "] === " + actreference.getTextContent() + "===");

	    		NamedNodeMap act_attributes = actreference.getAttributes();
				for(int attri = 0; attri < act_attributes.getLength(); attri++) {
					Attr act_attribute = (Attr) act_attributes.item(attri);
					if(act_attribute.getName().equals("gml:id")) {
						referenceId = act_attribute.getValue().trim();
					}
				}

		    	if(!MunicipalityfilereferenceSolveNameXpathRelpos.isEmpty()) {
			    	actxpathstring = MunicipalityfilereferenceSolveNameXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xpath.evaluate(actxpathstring, actreference, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
				    	reference.setName(actnode.getTextContent().trim());
				    }
		    	}

		    	if(!MunicipalityfilereferenceSolveIdXpathRelpos.isEmpty()) {
			    	actxpathstring = MunicipalityfilereferenceSolveIdXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xpath.evaluate(actxpathstring, actreference, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
				    	reference.setId(actnode.getTextContent().trim());
				    }
		    	}
String temp_name = "poziom";
String hierarchy = "";
		    	if(!temp_name.isEmpty()) {
			    	actxpathstring = temp_name;
					xpath_starttime = new Date();
					actnodes = (NodeList) xpath.evaluate(actxpathstring, actreference, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
				    	hierarchy = actnode.getTextContent().trim();	// 4poziom = municipality, less are admin hierarchies above
				    	if(!hierarchy.equals(""))
				    		hierarchy = hierarchy.substring(0,1);
				    }
		    	}

				if(hierarchy.equals("4")) {
					references.put(referenceId, reference);
					numberrecords++;
				}
		    } // end of loop over all address objects
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Error error) {
			error.printStackTrace();
		}

		
		method_endtime = new Date();
		System.out.println("");
		System.out.println(" method time in ms: " + (method_endtime.getTime() - method_starttime.getTime()));
		System.out.println(" xpath-evaluate time in ms: " + (evaluate_endtime.getTime() -  evaluate_starttime.getTime()));
		System.out.println("  xpath-compile time in ms: " + xpathms);
		System.out.println(" bufferoutputms time in ms: " + bufferoutputms);

		return numberrecords;
	}


	private static Integer analyseAddresses(Document xmldocument) {
		ImportAddress address = new ImportAddress();
		Integer numberAddresses = 0;

		Date method_starttime = new Date();
		Date method_endtime = new Date();
		Date evaluate_starttime = new Date();
		Date evaluate_endtime = new Date();
		Date xpath_starttime = new Date();
		Date xpath_endtime = new Date();
		Long xpathms = 0L;
		Date bufferoutput_starttime = new Date();
    	Date bufferoutput_endtime = new Date();
    	Long bufferoutputms = 0L;
		
		evaluate_starttime = new Date();
		XPath xpath = XPathFactory.newInstance().newXPath();
		XPathExpression addressesxpath;
		try {
			String addressesxpathstring = AddressobjectnameXpathAbspos;
			addressesxpath = xpath.compile(addressesxpathstring);		///gml:/

			NodeList addresses = (NodeList) addressesxpath.evaluate(xmldocument, XPathConstants.NODESET);
			evaluate_endtime = new Date();

	    	String actxpathstring;
			NodeList actnodes;

		    XPathFactory xpf = XPathFactory.newInstance();
		    XPath xp = xpf.newXPath();
			
			for (int addressindex = 0; addressindex < addresses.getLength(); addressindex++) {
				Integer muninameindex = -1;

				Node actaddress = (Node) addresses.item(addressindex);
		    	//System.out.println(" address # " + addressindex + "  [" + actaddress.getNodeName() + "] === " + actaddress.getTextContent() + "===");

				// following line was active, but no clue, why and for what
				//actaddress.getParentNode().removeChild(actaddress);
				
		    	if(!MunicipalitynameXpathRelpos.isEmpty()) {
			    	actxpathstring = MunicipalitynameXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
				    	muninameindex = nodesindex;
		  	    		address.setMunicipality(actnode.getTextContent().trim());
		  	    		address.setMunicipality(capitalize(address.getMunicipality()));
				    }
		    	}

		    	if(!MunicipalityfilereferenceXpathRelpos.isEmpty()) {
			    	actxpathstring = MunicipalityfilereferenceXpathRelpos;

					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
			    	String referenceId = "";
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
						if(muninameindex == -1) {
				    		NamedNodeMap act_attributes = actnode.getAttributes();
							for(int attri = 0; attri < act_attributes.getLength(); attri++) {
								Attr act_attribute = (Attr) act_attributes.item(attri);
								if(act_attribute.getName().equals("xlink:href")) {
									referenceId = act_attribute.getValue().trim();
								}
							}
					    	if(referenceId.indexOf("/") != -1)
					    		referenceId = referenceId.substring(referenceId.lastIndexOf("/") + 1);
							if(references.get(referenceId) != null) {
					    		Reference munireference = references.get(referenceId);
					    		address.setMunicipalityId(munireference.getId());
					    	}
						} else if (muninameindex == nodesindex) {
				    		NamedNodeMap act_attributes = actnode.getAttributes();
							for(int attri = 0; attri < act_attributes.getLength(); attri++) {
								Attr act_attribute = (Attr) act_attributes.item(attri);
								if(act_attribute.getName().equals("xlink:href")) {
									referenceId = act_attribute.getValue().trim();
								}
							}
					    	if(referenceId.indexOf("/") != -1)
					    		referenceId = referenceId.substring(referenceId.lastIndexOf("/") + 1);
					    	if(references.get(referenceId) != null) {
					    		Reference munireference = references.get(referenceId);
					    		address.setMunicipalityId(munireference.getId());
					    	} else {
					    		System.out.println("Municipality-ID is not available in list, id ===" + referenceId + "===");
					    	}
						}
				    }
		    	}

		    	if(!MunicipalityidXpathRelpos.isEmpty()) {
			    	actxpathstring = MunicipalityidXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setMunicipalityId(actnode.getTextContent().trim());
				    }
		    	}

		    	if(!SubareanameXpathRelpos.isEmpty()) {
			    	actxpathstring = SubareanameXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setSubArea(actnode.getTextContent().trim());
				    }
		    	}

		    	if(!SubidXpathRelpos.isEmpty()) {
			    	actxpathstring = SubidXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setSubareaId(actnode.getTextContent().trim());
				    }
		    	}
		    	
		    	if(!StreetnameXpathRelpos.isEmpty()) {
			    	actxpathstring = StreetnameXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setStreet(actnode.getTextContent().trim());
		  	    		address.setStreet(capitalize(address.getStreet()));
//TODO in Italia, check, if german streetname or italian one "STR." => "STRASSE " or "STR." => "STRADA "

		  	    		for (String actregexp : streetnamecorrectionmap.keySet()) {
		  					String actreplace = streetnamecorrectionmap.get(actregexp);
		  	    			address.setStreet(address.getStreet().replaceAll(actregexp, actreplace));
		  	    		}		  	    		
				    }
		    	}

		    	if(!PlacenameXpathRelpos.isEmpty()) {
			    	actxpathstring = PlacenameXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setPlace(actnode.getTextContent().trim());
		  	    		if(address.getPlace().indexOf("Ulica") != -1)
		  	    			address.setPlace(address.getPlace().replaceFirst("^Ulica ", ""));
				    }
		    	}

		    	if(!HousenumberXpathRelpos.isEmpty()) {
			    	actxpathstring = HousenumberXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setHousenumber(actnode.getTextContent().trim().toLowerCase());
				    }
		    	}

		    	if(!HousenumberadditionXpathRelpos.isEmpty()) {
			    	actxpathstring = HousenumberadditionXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.setHousenumberaddition(actnode.getTextContent().trim());
				    }
		    	}

		    	if(!PostcodeXpathRelpos.isEmpty()) {
			    	actxpathstring = PostcodeXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
//??? in BL or NL necessary???		  	    		address.hausnummer = actnode.getTextContent().trim();
		  	    	    address.setPostcode(actnode.getTextContent().trim());
				    }
		    	}

		    	if(!HousenumberobjectidXpathRelpos.isEmpty()) {
			    	actxpathstring = HousenumberobjectidXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.addKeyvalue("temp_iip_identyfikator", actnode.getTextContent().trim());
				    }
		    	}

		    	if(!statusXpathRelpos.isEmpty()) {
			    	actxpathstring = statusXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actnode = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actnode.getNodeName() + "] === " + actnode.getTextContent() + "===");
		  	    		address.addKeyvalue("temp_status", actnode.getTextContent().trim());
				    }
		    	}

		    	if(!PointXpathRelpos.isEmpty()) {
			    	actxpathstring = PointXpathRelpos;
					xpath_starttime = new Date();
					actnodes = (NodeList) xp.evaluate(actxpathstring, actaddress, XPathConstants.NODESET);
					xpath_endtime = new Date();
					xpathms += (xpath_endtime.getTime() - xpath_starttime.getTime());
				    for (int nodesindex = 0; nodesindex < actnodes.getLength(); nodesindex++) {
				    	Node actpoint = (Node) actnodes.item(nodesindex);
				    	//System.out.println(" child # " + nodesindex + "  [" + actpoint.getNodeName() + "] === " + actpoint.getTextContent() + "===");
	
	    	    		NamedNodeMap act_point_attributes = actpoint.getAttributes();
						for(int attri = 0; attri < act_point_attributes.getLength(); attri++) {
							Attr act_attribute = (Attr) act_point_attributes.item(attri);
							if(act_attribute.getName().equals("srsName")) {
								address.setSourceSrid(act_attribute.getValue().trim().substring(act_attribute.getValue().trim().lastIndexOf(":") + 1));
							}
						}
	
						NodeList points_subnodes = actpoint.getChildNodes();
					    int numpointssubnodes = points_subnodes.getLength();
				    	Double xpos = 0.0D;
				    	Double ypos = 0.0D;
					    for (int subpointi = 0; subpointi < numpointssubnodes; subpointi++) {
					    	Node act_subpoint = (Node) points_subnodes.item(subpointi);
					    	//System.out.println("act_subpoint ===" + act_subpoint.getNodeName() + "===");
							if(		act_subpoint.getNodeName().equals("gml:pos")
								||	act_subpoint.getNodeName().equals("gml:coordinates")) {
						    		//<gml:pos>575834.471000001 5942460.453</gml:pos>
									//<gml:coordinates ts=" " cs="," decimal=".">206453.35,533875.990000001		</gml:coordinates>		in Poland at 2015/04
								String pointstring = act_subpoint.getTextContent();
								String pointparts[] = null;
								if(pointstring.indexOf(",") != -1)
									pointparts = pointstring.split(",");
								else if(pointstring.indexOf(" ") != -1)
										pointparts = pointstring.split(" ");
								else
									System.out.println("neither space nor colon were found to separate lon and lat ===" + pointstring + "===");
								if(pointparts.length == 2) {
									xpos = Double.parseDouble(pointparts[1]);
									ypos = Double.parseDouble(pointparts[0]);
								}
						    } else if(act_subpoint.getNodeName().equals("gml:coord")) {
	
								NodeList gmlpos_subnodes = act_subpoint.getChildNodes();
							    int numgmlpossubnodes = gmlpos_subnodes.getLength();
						    	for (int subgmlpos = 0; subgmlpos < numgmlpossubnodes; subgmlpos++) {
							    	Node act_subgmlpos = (Node) gmlpos_subnodes.item(subgmlpos);
								    if(act_subgmlpos.getNodeName().equals("gml:X")) {
								    	xpos = Double.parseDouble(act_subgmlpos.getTextContent());
								    } else if(act_subgmlpos.getNodeName().equals("gml:Y")) {
								    	ypos = Double.parseDouble(act_subgmlpos.getTextContent());
								    }
							    }
							} else {
			    	    		if(! act_subpoint.getNodeName().equals("#text")) {
			    	    			System.out.println("ignorierter act_subpoint ===" + act_subpoint.getNodeName() + "===");
			    	    		}
							}
					    	if(!xpos.equals("") && !ypos.equals("")) {
								String sql_latlon = "";				    						
								try {
									sql_latlon = "SELECT"				    						
										+ " ST_X(ST_Transform(ST_Setsrid(ST_Makepoint(?, ?), ?), 4326)) AS lon,"				    						
										+ " ST_Y(ST_Transform(ST_Setsrid(ST_Makepoint(?, ?), ?), 4326)) AS lat;";
									PreparedStatement coordstmt = conHausnummern.prepareStatement(sql_latlon);
									
				    				coordstmt.setDouble(1, xpos);
				    				coordstmt.setDouble(2, ypos);
				    				coordstmt.setInt(3, Integer.parseInt(address.getSourceSrid()));
				    				coordstmt.setDouble(4, xpos);
				    				coordstmt.setDouble(5, ypos);
				    				coordstmt.setInt(6, Integer.parseInt(address.getSourceSrid()));
				    				ResultSet rs = coordstmt.executeQuery();
				    				if (rs.next()) {
										address.setLocation(rs.getDouble("lon"), rs.getDouble("lat"));
				    				}
				    				rs.close();
								} catch (SQLException e) {
									System.out.println("sql_latlon ===" + sql_latlon + "===");
									e.printStackTrace();
								}
					    	}
					    }
				    }
			    }

		    	if(address.getStreet().equals("") && address.getPlace().equals(""))
		    		address.setStreet(address.getSubArea());
  	    			
		    	
		    	bufferoutput_starttime = new Date();
	    		osmoutputbuffer.append(address.printosm());
	    		outputbuffer.append(address.printtxt());
		    	bufferoutput_endtime = new Date();
		    	bufferoutputms += (bufferoutput_endtime.getTime() - bufferoutput_starttime.getTime());
	    		numberAddresses++;
	    		System.out.println("Number of Adresses: " + numberAddresses);
		    } // end of loop over all address objects
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Error error) {
			error.printStackTrace();
		}

		
		method_endtime = new Date();
		System.out.println("");
		System.out.println(" method time in ms: " + (method_endtime.getTime() - method_starttime.getTime()));
		System.out.println(" xpath-evaluate time in ms: " + (evaluate_endtime.getTime() -  evaluate_starttime.getTime()));
		System.out.println("  xpath-compile time in ms: " + xpathms);
		System.out.println(" bufferoutputms time in ms: " + bufferoutputms);
		
	
		return numberAddresses;
	}


	static Long bytesreadtotal = 0L;
	static StringBuffer readerRestBuffer = new StringBuffer();
	
	
	static void initRecordsReader() {
		bytesreadtotal = 0L;
		readerStartpos = 0;
		readerHeader = new StringBuffer();
		readerRestBuffer = new StringBuffer();
	}
	
	static String getNextRecords(InputStreamReader filereader, String membername, Integer numberNextGmlRecords) {	
		Integer numberRecords = 0;
		boolean headeraktiv = false;
		boolean finished = false;
		Integer bytesread = 0;
		Integer bytesreadTotal = 0;
		Integer BUFFERMAXTOHOLD = 10000;		// set a value to more than maximum length of a record - if a record start tag will not be found in text, limit to this value
												// for next read. This prevents to hold a lot of unneccessary MB in memory

		StringBuffer outputbuffer = new StringBuffer();

		String aktText = "";
		char[] charbuffer = new char[3000];

		System.out.println("Start reading xml-File ...");
		try {
			do {
				aktText = "";

				bytesread = filereader.read(charbuffer, 0, charbuffer.length);

				if((bytesreadtotal/1000000) != ((bytesreadtotal+bytesread)/1000000)) {
					System.out.println(" read " + ((bytesreadtotal+bytesread)/1000000) + " MB of file content");
				}
				bytesreadtotal += charbuffer.length;

				if(readerRestBuffer.length() > 0) {
					aktText = readerRestBuffer.toString();
				}

				if(bytesread > -1) {
					aktText += new String(charbuffer);
					readerStartpos += bytesread;
					bytesreadTotal += bytesread;
				} else {
					finished = true;
				}

				if(aktText.indexOf("<?xml") != -1) {
					headeraktiv = true;
				}
				if(headeraktiv) {
					String headerEndString = "<" + gmlFeaturemembers;
					if(aktText.indexOf(headerEndString) != -1) {
						Integer startpos = 0;
						if(readerHeader.length() == 0) {
							Integer codezeichen = aktText.codePointAt(0);
							if(codezeichen == 65279)	// found BOM, ignore first character in file
								startpos = 1;
							System.out.println("Codezeichen: " + codezeichen);
						}
						Integer endpos = aktText.indexOf(headerEndString);
						endpos = aktText.indexOf(">", endpos) + 1;
						readerHeader.append(aktText.substring(startpos,endpos));
						headeraktiv = false;
						aktText = aktText.substring(endpos);
					} else {
						readerRestBuffer = new StringBuffer(aktText);
						aktText = "";
						continue;
					}
				}
				Integer startpos = -1;
				Integer endpos = -1;
				readerRestBuffer = new StringBuffer();
				if(! headeraktiv) {
					String recordStartString = "<" + membername;
						// if start tag will not be found, directly reduce active text to hold further to just number of search start tags, because if could be in text partly
					if((aktText.indexOf(recordStartString) == -1) && (aktText.length() > BUFFERMAXTOHOLD)) {
						Integer local_old_length = aktText.length();
						aktText = aktText.substring(aktText.length() - BUFFERMAXTOHOLD);
						System.out.println("purged text buffer with unusable content from " + local_old_length + " to " + aktText.length());
					}
					while(aktText.indexOf(recordStartString) != -1) {
						startpos = aktText.indexOf(recordStartString);
						String recordEndString = "</" + membername + ">";
						if(aktText.indexOf(recordEndString) != -1) {
							endpos = aktText.indexOf(recordEndString) + recordEndString.length();
if(endpos < startpos)
	System.out.println("ERROR: wrong values for startpos: " + startpos + " and endpos: " + endpos);
							outputbuffer.append(aktText.substring(startpos,endpos) + "\n");
							aktText= aktText.substring(endpos);

							numberRecords++;
							if(numberRecords >= numberNextGmlRecords) {
								readerRestBuffer.append(aktText);
								aktText = "";
								break;
							}
						} else {
							readerRestBuffer.append(aktText);
							aktText = "";
							break;
						}
					}
					if((outputbuffer.length() > 0)) {
						readerRestBuffer.append(aktText);
						aktText = "";
					}
				}
				if(numberRecords >= numberNextGmlRecords) {
					readerRestBuffer.append(aktText);
					aktText = "";
					finished = true;
				}
			} while(! finished);
		} catch (IOException ioerror) {
			System.out.println("ERROR: IO-Exception during parsing of xml-content");
			ioerror.printStackTrace();
			return outputbuffer.toString();
		}

		System.out.println("End reading xml-File, number of characters: " + bytesreadTotal);

		if(outputbuffer.length() > 0) {
			if(!gmlFeaturemembers.equals("") && outputbuffer.indexOf("</" + gmlFeaturemembers + ">") == -1)
				outputbuffer.append("</" + gmlFeaturemembers + ">\n");
			if(!gmlFeaturecollection.equals("") && outputbuffer.indexOf("</" + gmlFeaturecollection + ">") == -1)
				outputbuffer.append("</" + gmlFeaturecollection + ">\n");
			return readerHeader.toString() + outputbuffer;
		} else
			return "";
	}

	public static void main(final String[] args) {
		Date timeProgramStarttime = new Date();
		Date timeProgramEndtime;
		Date nowtime;
		Date time_debug_starttime = null;
		Date time_debug_endtime = null;
		Date looptime_debug_starttime = null;
		Date looptime_debug_endtime = null;

		String parameterLand = "Bundesrepublik Deutschland";
		String parameterImportdateiname = "";
	
		if ((args.length >= 1) && (args[0].equals("-h"))) {
			System.out.println("-country Name of Country, as store on evaluation Website or in Admin Polygon in OSM. It not used, 'Bundesrepublik Deutschland' will be used");
			System.out.println("-municipality Name of Municipality");
			System.out.println("-municipalityref Municipality Reference-Id for unique Identification of the municipality in OSM. In Germany value of de:amtlicher_gemeindeschluessel");
			System.out.println("-file importfilename");
			System.out.println("");
			System.out.println("Importfile must be in gml XML-Format. Direct support only for Belgium, Italy and Poland Lists. For other countries, source code musst be changed first.");
			return;
		}

		for (int lfdnr = 0; lfdnr < args.length; lfdnr++) {
			System.out.println("args[" + lfdnr + "] ===" + args[lfdnr] + "===");
		}

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
				} else if (args[argsi].equals("-file")) {
					parameterImportdateiname = args[argsi + 1];
					argsOkCount += 2;
				} else {
					System.out.println("ERROR: unknown program parameter ===" + args[argsi] + "===");
					return;
				}
			}
			if (argsOkCount != args.length) {
				System.out.println("ERROR: not all programm parameters were valid, STOP");
				return;
			}
		}

		String filecontent = "";

		if(parameterLand.equals("Poland")) {
			gmlFeaturecollection = "gml:FeatureCollection";
			gmlFeaturemembers = "gml:featureMembers";
			gmlFeaturemember = "prg-ad:PRG_PunktAdresowy";
			AddressobjectnameXpathAbspos = "/FeatureCollection/featureMembers/PRG_PunktAdresowy";
			PointXpathRelpos = "pozycja/Point";
			MunicipalitynameXpathRelpos = "jednostkaAdmnistracyjna";  // admin_level: 8=miejscowosc, 7=jednostkaAdmnistracyjna
			MunicipalityidXpathRelpos = ""; 		//jednostkaAdministracyjna[1]/PRG_JednostkaAdministracyjnaNazwa/idTERYT";
			MunicipalityfilereferenceXpathRelpos = "komponent";
			MunicipalityfilereferenceSolveXpathAbspos = "/FeatureCollection/featureMembers/PRG_JednostkaAdministracyjnaNazwa";
			gmlReferenceobject = "prg-ad:PRG_JednostkaAdministracyjnaNazwa";
			MunicipalityfilereferenceSolveNameXpathRelpos = "nazwa";
			MunicipalityfilereferenceSolveIdXpathRelpos = "idTERYT";
	        SubareanameXpathRelpos = "miejscowosc";
	        SubidXpathRelpos = "";
			PostcodeXpathRelpos = "kodPocztowy";
			PlacenameXpathRelpos = "miejscowosc";
			StreetnameXpathRelpos = "ulica";
			HousenumberXpathRelpos = "numerPorzadkowy";
			HousenumberadditionXpathRelpos = "";
			HousenumberobjectidXpathRelpos = "idIIP/BT_Identyfikator/lokalnyId";
statusXpathRelpos = "status";
		} else if(parameterLand.equals("België")) {
			gmlFeaturecollection = "agiv:FeatureCollection";
			gmlFeaturemembers = "";
			gmlFeaturemember = "gml:featureMember";
			AddressobjectnameXpathAbspos = "/FeatureCollection/featureMember/CrabAdr";
			PointXpathRelpos = "pointProperty/Point";
			MunicipalitynameXpathRelpos = "GEMEENTE";
			MunicipalityidXpathRelpos = "";
			PostcodeXpathRelpos = "POSTCODE";
			StreetnameXpathRelpos = "STRAATNM";
			HousenumberXpathRelpos = "HUISNR";
			HousenumberadditionXpathRelpos = "";
			HousenumberobjectidXpathRelpos = "ID";
			statusXpathRelpos = "";
		} else if(parameterLand.equals("Italia")) {
			if(1==0) {		// Trento, Italia
			gmlFeaturecollection = "ogr:FeatureCollection";
						gmlFeaturemembers = "";
			gmlFeaturemember = "gml:featureMember";
			AddressobjectnameXpathAbspos = "/FeatureCollection/featureMember/civici_web";
			PointXpathRelpos = "geometryProperty/Point";
			MunicipalitynameXpathRelpos = "sobborgo";
						MunicipalityidXpathRelpos = "";
			PostcodeXpathRelpos = "cap";
			StreetnameXpathRelpos = "desvia";
			HousenumberXpathRelpos = "civico_alf";		// complete housenumber, including suffixes. Example: 117/A - housenumber without suffix would be ogr:civico_num
			HousenumberadditionXpathRelpos = "";				// housenumber addition would be ogr:civico_let
						HousenumberobjectidXpathRelpos = "";
						statusXpathRelpos = "";
			} else {
				gmlFeaturecollection = "gml2:FeatureCollection";
				gmlFeaturemembers = "";
				gmlFeaturemember = "gml:featureMember";
				AddressobjectnameXpathAbspos = "/FeatureCollection/featureMember/Addresses";
				PointXpathRelpos = "pointProperty/Point";
					// IT Gemeindenname: GEHA_GEMEINDE_IT
				MunicipalitynameXpathRelpos = "GEHA_GEMEINDE_DE";
				MunicipalityidXpathRelpos = "GEHA_GEMEINDE_ISTAT";
				PostcodeXpathRelpos = "GEHA_POSTLEITZAHL";
					// IT Straßenname: GEHA_STRASSE_IT
				StreetnameXpathRelpos = "GEHA_STRASSE_DE";
				HousenumberXpathRelpos = "GEHA_NUMMER";	// housenumber pur, without suffixes
				HousenumberadditionXpathRelpos = "GEHA_NENNER";
HousenumberobjectidXpathRelpos = "";
statusXpathRelpos = "";
			}
		} else if(parameterLand.equals("Bundesrepublik Deutschland")) {		// genauer gesagt Sachsen in 01/2016
			gmlFeaturecollection = "gml:FeatureCollection";
			gmlFeaturemembers = "";
			gmlFeaturemember = "gml:featureMember";
			AddressobjectnameXpathAbspos = "ad:Address";
			PointXpathRelpos = "ad:position/ad:GeographicPosition/ad:geometry/gml:Point";
							MunicipalitynameXpathRelpos = "agiv:GEMEENTE"; // <ad:component xlink:href="#AddressAreaName_0033-AddressAreaName-0030-010"/> => gml:featureMember/ad:AddressAreaName/ad:name/gn:GeographicalName/gn:spelling/gn:SpellingOfName/gn:text
							MunicipalityidXpathRelpos = "";
						PostcodeXpathRelpos = "agiv:POSTCODE";  // <ad:component xlink:href="#PostalDescriptor_0033-PostalDescriptor-01219"/> => gml:featureMember/ad:PostalDescriptor gml:id=PostalDescriptor_0033-PostalDescriptor-01219/ad:postCode
							StreetnameXpathRelpos = "agiv:STRAATNM"; // <ad:component xlink:href="#ThoroughfareName_0033-ThoroughfareName-000-0030-Richard-Strauss-Platz"/> => gml:featureMember/ad:ThoroughfareName gml:id="ThoroughfareName_0033-ThoroughfareName-000-0030-Richard-Strauss-Platz"/ad:name/gn:GeographicalName/gn:spelling/gn:SpellingOfName/gn:text
			HousenumberXpathRelpos = "ad:designator";
							HousenumberadditionXpathRelpos = "";
				// AddressAreaName = Ortsteil???
/*  adresse mit -Zusatz (in Freiberg, Am Krönerstolln)
							<ad:AddressLocator>
                            <ad:designator>
                                    <ad:LocatorDesignator>
                                            <ad:designator>63</ad:designator>
                                            <ad:type>addressNumber</ad:type>
                                    </ad:LocatorDesignator>
                            </ad:designator>
                            <ad:designator>
                                    <ad:LocatorDesignator>
                                            <ad:designator>B</ad:designator>
                                            <ad:type>addressNumberExtension</ad:type>
                                    </ad:LocatorDesignator>
                            </ad:designator>
                            <ad:level>siteLevel</ad:level>
                            <ad:withinScopeOf nilReason="Unpopulated" xsi:nil="true"/>
                    </ad:AddressLocator>
*/
							HousenumberobjectidXpathRelpos = "agiv:ID";
							statusXpathRelpos = "";
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
		

		if(parameterLand.equals("Poland")) {
			streetnamecorrectionmap.put("^Al. ", "Aleja ");
			streetnamecorrectionmap.put("^dr ", "Doktora ");
			streetnamecorrectionmap.put("^Gen. ", "Generała ");
			streetnamecorrectionmap.put("^gen. ", "Generała ");
			streetnamecorrectionmap.put("^ks. ", "Księdza ");
			streetnamecorrectionmap.put("^Ks. ", "Księdza ");
			streetnamecorrectionmap.put("^Os. ", "Osiedle ");
			streetnamecorrectionmap.put("^Pl. ", "Plac ");
			streetnamecorrectionmap.put("^płk. ", "Pułkownika ");
			streetnamecorrectionmap.put("^Ulica ", "");
		}

		if(parameterLand.equals("Italia")) {
			streetnamecorrectionmap.put("Str\\.", "Straße ");
			streetnamecorrectionmap.put("str\\.$", "straße");
			streetnamecorrectionmap.put(" Str$", " Straße");
			streetnamecorrectionmap.put("\\-Str$", "-Straße");
			streetnamecorrectionmap.put("^St\\.", "St. ");
			streetnamecorrectionmap.put("^St\\. +", "St. ");
			streetnamecorrectionmap.put("^St\\. -", "St.-");		// ok, correct previous entry, when - follows, like St.-Cyriak-Weg  
			streetnamecorrectionmap.put("Strasse", "Straße");
			streetnamecorrectionmap.put("strasse", "straße");
			streetnamecorrectionmap.put("Grosser ", "Großer ");
			streetnamecorrectionmap.put("Grosse ", "Große ");
			streetnamecorrectionmap.put("^Frak\\. ", "Fraktion ");
			streetnamecorrectionmap.put("^Frakt\\.", "Fraktion ");
			streetnamecorrectionmap.put("^Fraktion +", "Fraktion ");
			streetnamecorrectionmap.put("Handwerkerstr\\.N", "Handwerkerstraße N");
			streetnamecorrectionmap.put("Handwerkerstr\\.S", "Handwerkerstraße S");
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

			stmt = conHausnummern.createStatement();

			String dateipfad = "";
			String dateipfadname = "";
			String output_dateipfadname = "";
			String osmoutput_dateipfadname = "";
			String logs_dateipfadname = "";
			if (parameterImportdateiname != null) {
				dateipfadname = parameterImportdateiname;
			}

			boolean continuesmode = false;
			Integer startoffsetrecords = 0;

			startoffsetrecords = 0;

			if(startoffsetrecords > 0)
				continuesmode = true;			
//Integer numberNextGmlRecords = 2;
Integer numberNextGmlRecords = 500;
String bezirk = "";
			if(parameterLand.equals("Poland")) {
				bezirk = "2016_03_18_07_31_08__28_warmińsko-mazurskie";
				//dateipfad = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Polen/2016-03-23";
				dateipfad = "/home/openstreetmap/temp/PolenHnr";
				dateipfadname = dateipfad + File.separator + bezirk + ".xml";
				output_dateipfadname = dateipfad + File.separator + "temp" + File.separator + bezirk + ".txt";
				osmoutput_dateipfadname = dateipfad + File.separator + "temp" + File.separator + bezirk + ".osm";
				logs_dateipfadname = dateipfad + File.separator + "temp" + File.separator + bezirk + ".log";
			} else if(parameterLand.equals("België")) {
				dateipfadname = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Belgien/Vlandern-Teil---CRAB/2016-02-20/CRAB_Adressenlijst/GML/CrabAdr.gml";
				output_dateipfadname = dateipfadname + ".txt";
				osmoutput_dateipfadname = dateipfadname + ".osm";
				logs_dateipfadname = dateipfadname + ".log";
			} else if(parameterLand.equals("Italia")) {
				//dateipfadname = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/offen/IT-Trento/civici_web.gml";
				dateipfadname = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/offen/IT-Südtirol/GeokatalogExport.gml";
				output_dateipfadname = dateipfadname + ".txt";
				osmoutput_dateipfadname = dateipfadname + ".osm";
				logs_dateipfadname = dateipfadname + ".log";
			} else if(parameterLand.equals("Bundesrepublik Deutschland")) {
				dateipfadname = "/home/openstreetmap/NASI/OSMshare/programme/git/saendner_osm-hausnummern/data_hausnummern/sn_ad_25833.gml";
				output_dateipfadname = dateipfadname + ".txt";
				osmoutput_dateipfadname = dateipfadname + ".osm";
				logs_dateipfadname = dateipfadname + ".log";
			}

			DocumentBuilderFactory factory = null;
			DocumentBuilder builder = null;
			Document xml_document = null;

			//System.out.println("Info: got this xml-content ===" + article_xml_content + "===");

			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();

			InputStreamReader filereader = new InputStreamReader(new FileInputStream(dateipfadname),"UTF-8");
			initRecordsReader();
			
			if(!gmlReferenceobject.equals("")) {
				boolean contentavailable = true;

				while(contentavailable) {
					looptime_debug_starttime = new Date();
					time_debug_starttime = new Date();
					filecontent = getNextRecords(filereader, gmlReferenceobject, numberNextGmlRecords);
					time_debug_endtime = new Date();
					System.out.println(" getNextGmlRecords took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));
					if(filecontent.length() == 0) {
						contentavailable = false;
						continue;
					}

					Integer count_references = 0;
					try {
						// parse xml-document.
						time_debug_starttime = new Date();
						xml_document = builder.parse(new InputSource(new StringReader(filecontent)));
						time_debug_endtime = new Date();
						System.out.println(" builder.parse took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));

						time_debug_starttime = new Date();
						count_references = analyseMunicipalityReference(xml_document);
					    time_debug_endtime = new Date();
						System.out.println(" getchildnodes took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));

						System.out.println("Number of characters read: " + readerStartpos + "    Number References: " + count_references);

					} catch (org.xml.sax.SAXException saxerror) {
						System.out.println("ERROR: SAX-Exception during parsing of xml-content");
						saxerror.printStackTrace();
						System.out.println("Number of characters read: " + readerStartpos + "    Number References: " + count_references);
						System.out.println("CONTENT START ===\n" + filecontent + "\n=== CONTENT END");
						saxerror.printStackTrace();

						PrintWriter logfilehandle = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
								new FileOutputStream(logs_dateipfadname, true),StandardCharsets.UTF_8)));
						logfilehandle.println("Number of characters read: " + readerStartpos + "    Number References: " + count_references);
						logfilehandle.println(saxerror.toString());
						logfilehandle.println("CONTENT START ===\n" + filecontent + "\n=== CONTENT END");
						logfilehandle.println(saxerror.toString());
						logfilehandle.close();
					}
					looptime_debug_endtime = new Date();
					System.out.println(" loop time in ms: " + (looptime_debug_endtime.getTime() -  looptime_debug_starttime.getTime()));
				}
				filereader.close();
			}

			initRecordsReader();
			
			filereader = new InputStreamReader(new FileInputStream(dateipfadname),"UTF-8");

		    PrintWriter output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(output_dateipfadname, continuesmode),StandardCharsets.UTF_8)));
		    output.println("#temp_datensatznr	temp_strasseid	Straße	Hausnummer	Hausnummerzusatz	plz	gemeindeid	gemeinde	subarea	subid	temp_koordinatensystem	lon	lat");
			output.close();

		    PrintWriter osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(osmoutput_dateipfadname, continuesmode),StandardCharsets.UTF_8)));
		    osmOutput.println("<?xml version='1.0' encoding='UTF-8'?>");
    		osmOutput.println("<osm version='0.6' upload='false' generator='OSM_Hausnummerauswertung'>");
			osmOutput.close();

			boolean contentavailable = true;

			if(startoffsetrecords > 0) {
				while(contentavailable && (count_addresses < startoffsetrecords)) {
					filecontent = getNextRecords(filereader, gmlFeaturemember, numberNextGmlRecords);
					if(filecontent.length() == 0) {
						contentavailable = false;
						continue;
					}
				}
			}

			while(contentavailable) {
				looptime_debug_starttime = new Date();
				time_debug_starttime = new Date();
				filecontent = getNextRecords(filereader, gmlFeaturemember, numberNextGmlRecords);
				time_debug_endtime = new Date();
				System.out.println(" getNextGmlRecords took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));
				if(filecontent.length() == 0) {
					contentavailable = false;
					continue;
				}


				try {
					// parse xml-document.
					time_debug_starttime = new Date();
					xml_document = builder.parse(new InputSource(new StringReader(filecontent)));
					time_debug_endtime = new Date();
					System.out.println(" builder.parse took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));

					time_debug_starttime = new Date();
					count_addresses += analyseAddresses(xml_document);
				    time_debug_endtime = new Date();
					System.out.println(" getchildnodes took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));

					System.out.println("Anzahl Zeichen gelesen: " + readerStartpos + "    Anzahl Adressen: " + count_addresses);

					time_debug_starttime = new Date();
				    output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(output_dateipfadname, true),StandardCharsets.UTF_8)));
					output.print(outputbuffer.toString());
					output.close();

					outputbuffer = new StringBuffer();


				    osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(osmoutput_dateipfadname, true),StandardCharsets.UTF_8)));
					osmOutput.print(osmoutputbuffer.toString());
					osmOutput.close();
					time_debug_endtime = new Date();
					System.out.println(" writing to file took time in ms: " + (time_debug_endtime.getTime() -  time_debug_starttime.getTime()));
				
					osmoutputbuffer = new StringBuffer();
				} catch (org.xml.sax.SAXException saxerror) {
					System.out.println("ERROR: SAX-Exception during parsing of xml-content");
					saxerror.printStackTrace();
					System.out.println("Anzahl Zeichen gelesen: " + readerStartpos + "    Anzahl Adressen: " + count_addresses);
					System.out.println("CONTENT BEGIN ===\n" + filecontent + "\n=== CONTENT END");

					PrintWriter logfilehandle = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
							new FileOutputStream(logs_dateipfadname, true),StandardCharsets.UTF_8)));
					logfilehandle.println("Anzahl Zeichen gelesen: " + readerStartpos + "    Anzahl Adressen: " + count_addresses);
					logfilehandle.println(saxerror.toString());
					logfilehandle.println("CONTENT START ===\n" + filecontent + "\n=== CONTENT END");
					logfilehandle.println(saxerror.toString());
					logfilehandle.close();
				}
				looptime_debug_endtime = new Date();
				System.out.println(" loop time in ms: " + (looptime_debug_endtime.getTime() -  looptime_debug_starttime.getTime()));
			}
			filereader.close();

		    osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(osmoutput_dateipfadname, true),StandardCharsets.UTF_8)));
    		osmOutput.println("</osm>");
			osmOutput.close();
			
		    nowtime = new Date();
			System.out.println("Programm-Dauer bis Ende Dateiverarbeitung in sek: " + (nowtime.getTime() - timeProgramStarttime.getTime()) / 1000);



		    nowtime = new Date();
			System.out.println("Programm-Dauer bis Ende rausschreiben txt-Datei in sek: " + (nowtime.getTime() - timeProgramStarttime.getTime()) / 1000);
			
		    nowtime = new Date();
			System.out.println("Programm-Dauer bis Ende DB-Speicherung in sek: " + (nowtime.getTime() - timeProgramStarttime.getTime()) / 1000);
			
			stmt.close();
			conHausnummern.close();

			timeProgramEndtime = new Date();
			System.out.println("Programm-Dauer in sek: " + (timeProgramEndtime.getTime() - timeProgramStarttime.getTime()) / 1000);
			
		} 
		catch (IOException ioerror) {
			System.out.println("ERROR: IO-Exception during parsing of xml-content");
			ioerror.printStackTrace();
			return;
		}
		catch (ParserConfigurationException parseerror) {
			System.out.println("ERROR: fail to get new Instance from DocumentBuilderFactory");
			parseerror.printStackTrace();
		}
		catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
}
