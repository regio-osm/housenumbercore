package de.regioosm.housenumbercore.imports;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;



/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/



/**
 * @author Dietmar Seifert
 *
 * evaluate the housenumbers in a municipality or a subregion of it, check againsts an available housenumerlist
 * 	
*/




public class HousenumberList {
	public static final Logger logger = Logger.getLogger(HousenumberList.class.getName());


	public HousenumberList() {
		Handler handler = new ConsoleHandler();
		handler.setFormatter(new Formatter() {
	         public String format(LogRecord rec) {
	            StringBuffer buf = new StringBuffer(1000);
	            //buf.append(new java.util.Date());
	            //buf.append(" ");
	            buf.append(rec.getLevel());
	            buf.append(": ");
	            buf.append(formatMessage(rec));
	            buf.append("\n");
	            return buf.toString();
	         }
		});
		handler.setLevel(Level.ALL);
		logger.addHandler(handler);
		FileHandler fhandler = null;
		try {
			fhandler = new FileHandler("HousenumberList_logger.log");
			final String dir = System.getProperty("user.dir");
			System.out.println("current dir = " + dir);

		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		fhandler.setFormatter(new Formatter() {
	         public String format(LogRecord rec) {
	            StringBuffer buf = new StringBuffer(1000);
	            buf.append(new java.util.Date());
	            buf.append(" ");
	            buf.append(rec.getLevel());
	            buf.append(" ");
	            buf.append(formatMessage(rec));
	            buf.append("\n");
	            return buf.toString();
	         }
		});
		fhandler.setLevel(Level.ALL);
		logger.addHandler(fhandler);
		logger.setLevel(Level.ALL);
		
	}

	protected static void ReadData() {
		URL                url; 
		URLConnection      urlConn; 
		BufferedReader     dis;


		
		//String overpass_url = "http://www.stadtplan.hagen.de/StrVz/Hauskoordinaten.csv";
		//String overpass_url = "http://openstreetmap.lu/luxembourg-addresses.csv";
		String overpass_url = "http://daten-hamburg.de/geographie_geologie_geobasisdaten/ALKIS_Adressen/ALKIS_Adressen_HH_2018-01-02.zip";
/*		String overpass_queryurl = "interpreter?data=";
		String overpass_query = "[timeout:3600][maxsize:1073741824]\n"
			+ "[out:xml];\n"
			+ "area(" + 3600000000L + ")->.boundaryarea;\n"
			+ "(\n"
			+ "node(area.boundaryarea)[\"addr:housenumber\"];\n"
			+ "way(area.boundaryarea)[\"addr:housenumber\"];>;\n"
			+ "rel(area.boundaryarea)[\"addr:housenumber\"];>>;\n"
			+ "rel(area.boundaryarea)[\"type\"=\"associatedStreet\"];>>;\n"
			+ ");\n"
			+ "out meta;";
		logger.log(Level.FINE, "OSM Overpass Query ===" + overpass_query + "===");
*/
		String url_string = "";
		File osmFile = null;

		try {
			url_string = overpass_url;
/*			String overpass_query_encoded = URLEncoder.encode(overpass_query, "UTF-8");
			overpass_query_encoded = overpass_query_encoded.replace("%28","(");
			overpass_query_encoded = overpass_query_encoded.replace("%29",")");
			overpass_query_encoded = overpass_query_encoded.replace("+","%20");
			url_string = overpass_url + overpass_queryurl + overpass_query_encoded;
			logger.log(Level.INFO, "Request for Overpass-API to get housenumbers ...");
			logger.log(Level.FINE, "Overpass Request URL to get housenumbers ===" + url_string + "===");
*/
			StringBuffer osmresultcontent = new StringBuffer();

			InputStream overpassResponse = null; 
			String responseContentEncoding = "";
			try {
				url = new URL(url_string);
				
				urlConn = url.openConnection(); 
				urlConn.setDoInput(true); 
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
				urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
				
				overpassResponse = urlConn.getInputStream(); 
	
				Integer headeri = 1;
				logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
				while(urlConn.getHeaderFieldKey(headeri) != null) {
					logger.log(Level.FINE, "  Header # " + headeri 
						+ ":  [" + urlConn.getHeaderFieldKey(headeri)
						+ "] ===" + urlConn.getHeaderField(headeri) + "===");
					System.out.println("  Header # " + headeri 
							+ ":  [" + urlConn.getHeaderFieldKey(headeri)
							+ "] ===" + urlConn.getHeaderField(headeri) + "===");
					if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
						responseContentEncoding = urlConn.getHeaderField(headeri);
					headeri++;
				}
			} catch (MalformedURLException mue) {
				logger.log(Level.WARNING, "Overpass API request produced a malformed Exception, Request URL was ===" + url_string + "===, Details follows ...");					
				logger.log(Level.WARNING, mue.toString());
				return;
				//logger.log(Level.WARNING, "sleeping now for " + (2 * numberfailedtries) + " seconds before Overpass-Query will be tried again");
				//TimeUnit.SECONDS.sleep(2 * numberfailedtries);
			} catch( ConnectException conerror) {

				url_string = overpass_url + "status";
				System.out.println("url to get overpass status for this server requests ===" + url_string + "===");
				url = new URL(url_string);
				urlConn = url.openConnection(); 
				urlConn.setDoInput(true); 
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
				urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
				
				overpassResponse = urlConn.getInputStream(); 
	
				Integer headeri = 1;
				logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
				while(urlConn.getHeaderFieldKey(headeri) != null) {
					logger.log(Level.FINE, "  Header # " + headeri 
						+ ":  [" + urlConn	.getHeaderFieldKey(headeri)
						+ "] ===" + urlConn.getHeaderField(headeri) + "===");
					if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
						responseContentEncoding = urlConn.getHeaderField(headeri);
					headeri++;
				}
				if(responseContentEncoding.equals("gzip")) {
					dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
				} else {
					dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
				}
				String inputline = "";
				while ((inputline = dis.readLine()) != null)
				{ 
					System.out.println("Content ===" + inputline + "===\n");
				}
				dis.close();
			
			} catch (IOException ioe) {
				logger.log(Level.WARNING, "Overpass API request produced an Input/Output Exception (Request URL was ===" + url_string + "===, Details follows ...");					
				logger.log(Level.WARNING, ioe.toString());

				url_string = overpass_url + "status";
				System.out.println("url to get overpass status for this server requests ===" + url_string + "===");
				url = new URL(url_string);
				urlConn = url.openConnection(); 
				urlConn.setDoInput(true); 
				urlConn.setUseCaches(false);
				urlConn.setRequestProperty("User-Agent", "regio-osm.de Housenumber Evaluation Client, contact: strassenliste@diesei.de");
				urlConn.setRequestProperty("Accept-Encoding", "gzip, compress");
				
				overpassResponse = urlConn.getInputStream(); 
	
				Integer headeri = 1;
				logger.log(Level.FINE, "Overpass URL Response Header-Fields ...");
				while(urlConn.getHeaderFieldKey(headeri) != null) {
					logger.log(Level.FINE, "  Header # " + headeri 
						+ ":  [" + urlConn.getHeaderFieldKey(headeri)
						+ "] ===" + urlConn.getHeaderField(headeri) + "===");
					if(urlConn.getHeaderFieldKey(headeri).equals("Content-Encoding"))
						responseContentEncoding = urlConn.getHeaderField(headeri);
					headeri++;
				}
				if(responseContentEncoding.equals("gzip")) {
					dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"UTF-8"));
				} else {
					dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
				}
				String inputline = "";
				while ((inputline = dis.readLine()) != null)
				{ 
					System.out.println("Content ===" + inputline + "===\n");
				}
				dis.close();
			
			
			}

	
			String inputline = "";
			if(responseContentEncoding.equals("gzip")) {
				dis = new BufferedReader(new InputStreamReader(new GZIPInputStream(overpassResponse),"ISO-8859-1"));	//UTF-8   "ISO-8859-1"
			} else {
				dis = new BufferedReader(new InputStreamReader(overpassResponse,"UTF-8"));
			}
			long bytecount = 0L;
			int linecount = 0;
			while ((inputline = dis.readLine()) != null)
			{ 
				osmresultcontent.append(inputline + "\n");
				linecount++;
				bytecount += inputline.length();
				if((linecount % 1000) == 0)
					System.out.println("active   Download, now   " + linecount + " Lines,    " + bytecount/1000 +  "kb Download");
			}
			dis.close();
			System.out.println("finished Download,       " + linecount + " Lines,    " + bytecount/1000 +  "kb Download");
			
				// first, save upload data as local file, just for checking or for history
			DateFormat time_formatter = new SimpleDateFormat("yyyyMMdd-HHmmss'Z'");
			String downloadtime = time_formatter.format(new Date());
			
			byte[] bytesOfMessage = osmresultcontent.toString().getBytes("UTF-8");
			MessageDigest md;
			try {
				md = MessageDigest.getInstance("MD5");
				byte[] thedigest = md.digest(bytesOfMessage);
				System.out.println("MD5-Sum ===" + thedigest + "===");
				System.out.println("MD5-Sum tostring ===" + thedigest.toString() + "===");
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			String filename = "download.txt"; 

			try {
				osmFile = new File(filename);
				PrintWriter osmOutput = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(filename),StandardCharsets.ISO_8859_1))); //UTF_8  ISO_8859_1
				osmOutput.println(osmresultcontent.toString());
				osmOutput.close();
				logger.log(Level.INFO, "Saved Overpass OSM Data Content to file " + filename);
			} catch (IOException ioe) {
				logger.log(Level.SEVERE, "Error, when tried to save Overpass OSM Data in file " + filename);
				logger.log(Level.SEVERE, ioe.toString());
			}
				// ok, osm result is in osmresultcontent.toString() available
			logger.log(Level.FINE, "Dateilänge nach optionalem Entpacken in Bytes: " + osmresultcontent.toString().length());


	    } catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
	    	logger.log(Level.SEVERE, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.SEVERE, e.toString());
		} catch (IOException e) {
			// TODO Auto-generated catch block
	    	logger.log(Level.SEVERE, "Execution of type InterruptedException occured, details follows ...");
			logger.log(Level.SEVERE, e.toString());
		}
		return;
	}


	public static void main(final String[] args) {
		ReadData();
	}


}
