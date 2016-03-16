package de.regioosm.housenumbercore.imports;

/*
 * V1.0, 26.06.2014, Dietmar Seifert
 * 
 * load a local osm file with admin boundaries and import them into hausnummern DB.
 * The admin boundaries are still missing in osm, this function close the gap to allow evaluations
	 
*/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.io.File;


import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.filter.common.IdTracker;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerFactory;
import org.openstreetmap.osmosis.core.filter.common.IdTrackerType;
import org.openstreetmap.osmosis.core.store.SimpleObjectStore;
import org.openstreetmap.osmosis.core.store.SingleClassObjectSerializationFactory;
import org.openstreetmap.osmosis.core.task.v0_6.*;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;
import org.openstreetmap.osmosis.pgsnapshot.common.NodeLocationStoreType;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.impl.WayGeometryBuilder;
import org.postgis.LineString;
import org.postgis.Point;
import org.postgis.Polygon;
import org.postgis.LinearRing;
import org.postgis.MultiPolygon;
import org.postgis.Geometry;







import de.regioosm.housenumbercore.util.Applicationconfiguration;

/**
 * Load a local osm file with admin boundaries and import them into hausnummern DB.
 * The admin boundaries are still missing in osm, this function close the gap to allow evaluations

 * @author Dietmar Seifert
 * @version 0.1
 * @since 2014-06-26
 *
 */
public class importlocalosmboundaries {
	static Integer nodes_count = 0;
	static Integer ways_count = 0;
	static Integer relations_count = 0;
	static IdTracker availableNodes = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	static IdTracker availableWays = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	static IdTracker availableRelations = IdTrackerFactory.createInstance(IdTrackerType.Dynamic);
	static SimpleObjectStore<EntityContainer> allNodes = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afnd", true);
	static SimpleObjectStore<EntityContainer> allWays = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afwy", true);
	static SimpleObjectStore<EntityContainer> allRelations = new SimpleObjectStore<EntityContainer>(new SingleClassObjectSerializationFactory(EntityContainer.class), "afrl", true);

	
	static TreeMap<Long, Way> gibmirways = new TreeMap<Long, Way>();
	static TreeMap<Long, Node> gibmirnodes = new TreeMap<Long, Node>();

	static String land = "";
	static Integer land_id = -1;
	static Integer stadt_id = -1;

	/**
	 * get all configuration items for the application: mostly DB-related and filesystem
	 */
	private static Applicationconfiguration configuration = new Applicationconfiguration();
	/**
	 * DB connection to Hausnummern
	 */
	private static Connection conHausnummern = null;

	
	public static Polygon createPolygon(List<Point> points) {
		
		LinearRing linearring = new LinearRing(points.toArray(new Point[]{}));
		LinearRing rings[] = new LinearRing[1];
		rings[0] = linearring;
		Polygon poly = new Polygon(rings);
		poly.srid = 4326;
		
		return poly;
	}

	
	private static boolean set_country(String countryname) {

		land = countryname;
		
		String select_countrystring = "SELECT id FROM land"
			+ " WHERE land = '" + countryname + "';";

		try {
			System.out.println("Select Country SQL-Statement ===" + select_countrystring + "===");

			Statement stmt_selectcountryid = conHausnummern.createStatement();

			ResultSet rs_selectgebiet = stmt_selectcountryid.executeQuery(select_countrystring);
			if( rs_selectgebiet.next() ) {
				land_id = rs_selectgebiet.getInt("id");
				return true;
			}
			return false;
		}
		catch( SQLException errorsql) {
			System.out.println("ERROR with sql select statement ===" + select_countrystring + "===");
			errorsql.printStackTrace();
			return false;
		}
	}

	
	
	private static boolean insert_municipality(Collection<Tag> tags) {


		String stadt = "";
		String officialkeys_id = "";
		
		for (Tag tag: tags) {
			String key = tag.getKey();
			String value = tag.getValue();
			System.out.println("Tag [" + key + "] ===" + value + "===");
			if(key.equals("name"))
				stadt = value;
			if(key.equals("subid"))
				officialkeys_id = value;
		}

		String insertmuncipality_sqlstring = "INSERT INTO stadt"
			+ " (stadt, officialkeys_id, land_id, subareasidentifyable, active_adminlevels, housenumberaddition_exactly, osm_hierarchy)"
			+ " VALUES ("
			+ " '" + stadt + "',"
			+ " '" + officialkeys_id + "',"
			+ " " + land_id + ","
			+ " 'n',"
			+ " ARRAY[8],"
			+ " 'n',"
			+ " '" + land + "');";

		try {
			Statement stmt_municipality = conHausnummern.createStatement();

			System.out.println("Municipality Insert statement ===" + insertmuncipality_sqlstring + "===");
			stmt_municipality.executeUpdate(insertmuncipality_sqlstring);

			String select_countrystring = "SELECT id FROM stadt WHERE land_id = " + land_id + " AND stadt = '" + stadt + "';";
			ResultSet rs_selectgebiet = stmt_municipality.executeQuery(select_countrystring);
			if( rs_selectgebiet.next() ) {
				stadt_id = rs_selectgebiet.getInt("id");
				return true;
			}
			return false;
		
		}
		catch( SQLException errorinsert) {
			System.out.println("Error when inserted municipality with sql insert statement === " + insertmuncipality_sqlstring + "===");
			errorinsert.printStackTrace();
			return false;
		}
	}


	private static boolean insert_gebiete(Collection<Tag> tags, String polygon) {

		String insertgebiet_sqlstring = "";

		try {

			String stadt = "";
			int adminlevel = 0;
			
			for (Tag tag: tags) {
				String key = tag.getKey();
				String value = tag.getValue();
				System.out.println("Tag [" + key + "] ===" + value + "===");
				if(key.equals("name"))
					stadt = value;
				if(key.equals("admin_level"))
					adminlevel = Integer.parseInt(value);
			}

			
			insertgebiet_sqlstring = "INSERT INTO gebiete"
				+ " (name, land_id, stadt_id, admin_level, sub_id, osm_id, polygon)"
				+ " VALUES ("
				+ " '" + stadt + "',"
				+ " " + land_id + ","
				+ " " + stadt_id + ","
				+ " " + adminlevel + ","
				+ " '-1',"
				+ " -1,"
				+ " ST_Transform(ST_GeomFromText('" + polygon + "'),900913)"
				+ ");";
			
			/*
			insertgebiet_sqlstring = "UPDATE gebiete"
					+ " set polygon = ST_Transform(ST_GeomFromText('" + polygon + "'),900913)"
					+ " WHERE"
					+ " name = '" + stadt + "'"
					+ " AND land_id = " + land_id
					+ " AND stadt_id = " + stadt_id
					+ " AND admin_level = " + adminlevel
					+ ";";
			*/
			Statement stmt_insertgebiet = conHausnummern.createStatement();

			System.out.println("Gebiet-Insertbefehl ===" + insertgebiet_sqlstring + "===");
			stmt_insertgebiet.executeUpdate( insertgebiet_sqlstring );
			return true;
		}
		catch( SQLException errorinsert) {
			System.out.println("Error when tried to insert gebiet with insert statement ===" + insertgebiet_sqlstring + "===");
			errorinsert.printStackTrace();
			return false;
		}
	}
	
	
	
	/**
	 * Main Program to import a new housenumber list from a municipality.
	 *
	 * @param args
	 * @author Dietmar Seifert
	 */
	public static void main(final String[] args) {

		try {
			System.out.println("ok, jetzt Class.forName Aufruf ...");
			Class.forName("org.postgresql.Driver");
			System.out.println("ok, nach Class.forName Aufruf!");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}

		String dateipfadname = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Island/2014/OSM-Daten/Island_Boundaries___TESTDATEN___work.osm";
//		String dateipfadname = "/home/openstreetmap/NASI/OSMshare/Projekte-Zusammenarbeiten/Hausnummernlisten/Island/OSM-Daten/Island_Boundaries___work.osm";
		//String dateipfadname = "/home/openstreetmap/NASI/OSMshare/db-support/import_db/iceland.osm";

		try {
			String urlHausnummern = configuration.db_application_url;
			conHausnummern = DriverManager.getConnection(urlHausnummern, 
				configuration.db_application_username, configuration.db_application_password);

			String countryname = "Ísland";

			set_country(countryname);

			Sink sinkImplementation = new Sink() {
				
			Polygon[] polys = null;
				
				@Override
				public void release() {
					// TODO Auto-generated method stub
					System.out.println("hallo Sink.release   aktiv !!!");
					
				}
				
				@Override
				public void complete() {
					// TODO Auto-generated method stub
					System.out.println("hallo Sink.complete  aktiv:    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);
					
				}
				
				@Override
				public void initialize(Map<String, Object> metaData) {
					// TODO Auto-generated method stub
					System.out.println("hallo Sink.initialize aktiv !!!");
				}
				
				@Override
				public void process(EntityContainer entityContainer) {
					System.out.println("hallo Sink.process  aktiv:    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);
					// TODO Auto-generated method stub
			        Entity entity = entityContainer.getEntity();
			        if (entity instanceof Node) {
			            //do something with the node
			        	nodes_count++;

		    			//allNodes.add(entityContainer);
		    			availableNodes.set(entity.getId());

						NodeContainer nodec = (NodeContainer) entityContainer;
						Node node = nodec.getEntity();
						System.out.println("Node lon: "+node.getLongitude() + "  lat: "+node.getLatitude()+"===");

						gibmirnodes.put(entity.getId(), node);
			        } else if (entity instanceof Way) {
			        	ways_count++;
			        	
		    			//allWays.add(entityContainer);
		    			availableWays.set(entity.getId());

						WayContainer wayc = (WayContainer) entityContainer;
						Way way = wayc.getEntity();
						//System.out.println("Weg "+way.getWayNodes()+"===");
			        
		    			gibmirways.put(entity.getId(), way);
			        
			        } else if (entity instanceof Relation) {
			    		Integer polygonanzahl = 0;
			            //do something with the relation
			        	relations_count++;
			        	System.out.println("Relation   " + entity.toString());
			        	List<RelationMember> relmembers =  ((Relation) entity).getMembers();

			        	polys = new Polygon[relmembers.size()];
			    		LinearRing rings[] = new LinearRing[relmembers.size()];
			    		String rings_role[] = new String[relmembers.size()];
		        		Collection<Tag> relationtags = entity.getTags();
						for (Tag tag: relationtags) {
		        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
						}

						insert_municipality(relationtags);
						System.out.println("  Anzahl Member: "+relmembers.size());


			        	for(int memberi = 0; memberi < relmembers.size(); memberi++) {
if(memberi > 0)
	System.out.println("mehr als 1 Member, aktiv: "+memberi);
			        		RelationMember actmember = relmembers.get(memberi);
			        		EntityType memberType = actmember.getMemberType();
			        		long memberId = actmember.getMemberId();

			        		System.out.println("relation member ["+memberi+"]  Typ: "+memberType+"   ==="+actmember.toString()+"===   Role ==="+actmember.getMemberRole()+"===");

			        	
			        		if (EntityType.Node.equals(memberType)) {
			    				if (availableNodes.get(memberId)) {
			    					System.out.println("in Relation Member vom Type   NODE enthalten  ==="+gibmirnodes.get(memberId).toString()+"===");
					        		System.out.println("  Hier die Tags des Node:  "+gibmirnodes.get(memberId).getTags().toString()+"===");
					        		Collection<Tag> nodetags = gibmirnodes.get(memberId).getTags();
									for (Tag tag: nodetags) {
					        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
			    				}
			    			} else if (EntityType.Way.equals(memberType)) {
			    				if (availableWays.get(memberId)) {
			    					System.out.println("in Relation Member vom Type   WAY 0enthalten  ==="+gibmirways.get(memberId).toString()+"===");
			    					Collection<Tag> waytags = gibmirways.get(memberId).getTags();
									for (Tag tag: waytags) {
					        			System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
									}
									Way actway = gibmirways.get(memberId);
									List<WayNode> actwaynodes = actway.getWayNodes();
									System.out.println("Weg enthält Anzahl knoten: "+actwaynodes.size());
									Integer lfdnr = 0;
									List<Point> points = new LinkedList<Point>();
									for (WayNode waynode: actwaynodes) {
										Node actnode = gibmirnodes.get(waynode.getNodeId());
										Point actpoint = new Point(actnode.getLongitude(), actnode.getLatitude());
										points.add(actpoint);
										//System.out.println(" Node # " + lfdnr + "    id: " + actnode.getId() + "    lon: " + actnode.getLongitude() + "   lat: "+actnode.getLatitude());
										lfdnr++;
									}

									WayGeometryBuilder waygeombuilder = new WayGeometryBuilder(NodeLocationStoreType.TempFile);
									//LineString linestring = waygeombuilder.createWayLinestring(actway);
									LineString linestring = waygeombuilder.createLinestring(points);
									System.out.println("erstellter Linestring ==="+linestring.toString()+"===");

						    		LinearRing linearring = new LinearRing(points.toArray(new Point[]{}));
						    		
						    		rings[memberi] = linearring;
						    		rings_role[memberi] = actmember.getMemberRole();

						    		LinearRing temprings[] = new LinearRing[1];
						    		temprings[0] = linearring;
						    		
						    		polys[polygonanzahl] = new Polygon(temprings);
						    		//polys[polygonanzahl] = new Polygon(rings);
						        	polys[polygonanzahl].srid = 4326;
						        	polygonanzahl++;
									System.out.println("erstelltes Polygon ==="+polys.toString()+"===");
			    				}
			    			}
			        	
			        	}

			        	Geometry multipolygon = new MultiPolygon(polys);
			        	multipolygon.srid = 4326;

			        	//Geometry geom = new GeometryCollection(rings);
			        	//PGgeometry otti = new PGgeometry(geom);
			        	
System.out.println("otti  .getType() ==="+multipolygon.getType()+"===  type: "+multipolygon.getTypeString());
System.out.println("otti  .toString() ==="+multipolygon.toString()+"===");
//			        	ComposedGeom multipoly = new MultiPolygon(polygons);
//						System.out.println("erstelltes Multipolygon ==="+multipoly.toString()+"===");
						insert_gebiete(relationtags, multipolygon.toString());

						
						
			    		
			    		//allRelations.add(entityContainer);
		    			availableRelations.set(entity.getId());
			        }
					
				}
			};
			
			RunnableSource osmfilereader;
			
			File xmlfile = new File(dateipfadname);
			osmfilereader = new XmlReader(xmlfile, true, CompressionMethod.None);

			osmfilereader.setSink(sinkImplementation);

			Thread readerThread = new Thread(osmfilereader);
			readerThread.start();

			while (readerThread.isAlive()) {
		        readerThread.join();
			}


			conHausnummern.close();
	    } catch (InterruptedException e) {
	        /* do nothing */
	    }
		catch (SQLException e) {
			e.printStackTrace();
			return;
	    }
		catch (OsmosisRuntimeException osmosiserror) {
			System.out.println("es folgt ein osmosis runtime fehler ...");
			osmosiserror.printStackTrace();
		}
	}
}
