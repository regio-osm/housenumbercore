package de.regioosm.housenumbercore.util;


import java.io.BufferedReader;
import java.io.File;
import java.sql.Connection;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.*;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.xml.common.CompressionMethod;
import org.openstreetmap.osmosis.xml.v0_6.XmlReader;


public class OsmReader {

	protected static Connection housenumberConn = null;
	
	private static OsmImportparameter importparameter = new OsmImportparameter();

	
	public OsmReader(OsmImportparameter importparameter) {
		OsmReader.importparameter = importparameter;
	}


	public static OsmImportparameter getImportparameter() {
		return importparameter;
	}
	
	public static void connectDB(Connection housenumberConn) {
		OsmReader.housenumberConn = housenumberConn;
	}

	
	public Map<Municipality, HousenumberList> execute() {
		Map<Municipality, HousenumberList> lists = new HashMap<>();
	
	
		Map<Long, Node> gibmirnodes = new HashMap<Long, Node>();
		Map<Long, Way> gibmirways = new HashMap<Long, Way>();
		Map<Long, Relation> gibmirrelations = new HashMap<Long, Relation>();

	
		
		

		try {

			Sink sinkImplementation = new Sink() {
				OsmImportparameter importparameter = OsmReader.getImportparameter();
				List<String> extraosmkeys = importparameter.getExtrakeys();

				Integer nodes_count = 0;
				Integer ways_count = 0;
				Integer relations_count = 0;

				int doubletaddresses = 0;

				@Override
				public void release() {
					// TODO Auto-generated method stub
					System.out.println("hallo Sink.release   aktiv !!!");
				}
				
				@Override
				public void complete() {
					System.out.println("hallo Sink.complete  aktiv:    nodes #"+nodes_count+"   ways #"+ways_count+"   relations #"+relations_count);

					ImportAddress.connectDB(housenumberConn);

					// loop over all osm node objects
	    	    	for (Map.Entry<Long, Node> nodemap: gibmirnodes.entrySet()) {
						ImportAddress housenumber = new ImportAddress();
						housenumber.setSourceSrid(importparameter.getSourceCoordinateSystem());
						String countrycode = importparameter.getCountrycode();

						Long objectid = nodemap.getKey();
	    				Collection<Tag> tags = nodemap.getValue().getTags();
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			System.out.println("node #" + objectid + ": Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(tag.getKey().equals("addr:country")) {
			        			countrycode = tag.getValue().trim();
				        		try {
									housenumber.setCountrycode(countrycode);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			        		}
			        		if(tag.getKey().equals("addr:city"))
			        			housenumber.setMunicipality(tag.getValue().trim());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place")) {
		        				housenumber.setStreet(tag.getValue().trim());
			        		}
			        		if(tag.getKey().equals("addr:postcode"))
			        			housenumber.setPostcode(tag.getValue().trim());
			        		if(tag.getKey().equals("addr:housenumber")) {
			        			housenumber.setHousenumber(tag.getValue().trim());
								if ((housenumber.getHousenumber().indexOf(",") != -1) ||
									(housenumber.getHousenumber().indexOf(";") != -1)) {
									System.out.println("WARNING: in housenumber special character " +
										"for more than one housenumber found, will not be resolved to separate housenumbers");
								}
			        		}
			        		if(extraosmkeys.contains(tag.getKey())) {
			        			housenumber.getOSMTagList().add(tag.getKey().trim(), tag.getValue().trim());
			        		}
						}
						System.out.println( "raw node with housenumber ===" + housenumber.getHousenumber() + 
							"=== in street ===" + housenumber.getStreet() + "===, node id #" + objectid + "===");
						if (!housenumber.getHousenumber().equals("")) {
							if (!housenumber.getStreet().equals("")) {
								housenumber.setLonLat(nodemap.getValue().getLongitude(), nodemap.getValue().getLatitude());
																
								Municipality municipality = null;
								try {
									municipality = new Municipality(countrycode,
										housenumber.getMunicipality(), "");
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								HousenumberList housenumberlist = null;
								if(lists.containsKey(municipality))
									housenumberlist = lists.get(municipality);
								else {
									housenumberlist = new HousenumberList(municipality);
								}
								if(!housenumberlist.contains(housenumber)) {
									housenumberlist.addHousenumber(housenumber);
									lists.put(municipality,  housenumberlist);
								} else {
									System.out.println("housenumber doublet at osm id " + objectid);
									doubletaddresses++;
								}
							} else {
								System.out.println("WARNING: OSM Node has a housenumber, but no street or place Information and will be ignored. OSM-Node id is " + objectid);
							}
						}
	    			}

	    	    		// loop over all osm way objects
	    	    	for (Map.Entry<Long, Way> waymap: gibmirways.entrySet()) {
						ImportAddress housenumber = new ImportAddress();
						housenumber.setSourceSrid(importparameter.getSourceCoordinateSystem());
						String countrycode = importparameter.getCountrycode();

						Long objectid = waymap.getKey();
		        		Collection<Tag> tags = waymap.getValue().getTags();
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(tag.getKey().equals("addr:country")) {
			        			countrycode = tag.getValue().trim();
				        		try {
									housenumber.setCountrycode(countrycode);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			        		}
			        		if(tag.getKey().equals("addr:city"))
			        			housenumber.setMunicipality(tag.getValue().trim());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place")) {
		        				housenumber.setStreet(tag.getValue().trim());
			        		}
			        		if(tag.getKey().equals("addr:postcode"))
			        			housenumber.setPostcode(tag.getValue().trim());
			        		if(tag.getKey().equals("addr:housenumber")) {
			        			housenumber.setHousenumber(tag.getValue().trim());
								if ((housenumber.getHousenumber().indexOf(",") != -1) ||
									(housenumber.getHousenumber().indexOf(";") != -1)) {
									System.out.println("WARNING: in housenumber special character " +
										"for more than one housenumber found, will not be resolved to separate housenumbers");
								}
			        		}
			        		if(tag.getKey().equals("centroid_lon"))
			        			housenumber.setLon(Double.parseDouble(tag.getValue()));
			        		if(tag.getKey().equals("centroid_lat"))
			        			housenumber.setLat(Double.parseDouble(tag.getValue()));
			        		if(extraosmkeys.contains(tag.getKey())) {
			        			housenumber.getOSMTagList().add(tag.getKey().trim(), tag.getValue().trim());
			        		}
		        		}
						System.out.println( "raw way with housenumber ===" + housenumber.getHousenumber() + 
							"=== in street ===" + housenumber.getStreet() + "===, node id #" + objectid + "===");
						if (!housenumber.getHousenumber().equals("")) {
							if (!housenumber.getStreet().equals("")) {
//osmhousenumber.set_osm_tag(keyvalues);
								Municipality municipality = null;
								try {
									municipality = new Municipality(countrycode,
										housenumber.getMunicipality(), "");
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								HousenumberList housenumberlist = null;
								if(lists.containsKey(municipality))
									housenumberlist = lists.get(municipality);
								else {
									housenumberlist = new HousenumberList(municipality);
								}
								if(!housenumberlist.contains(housenumber)) {
									housenumberlist.addHousenumber(housenumber);
									lists.put(municipality,  housenumberlist);
								} else {
									System.out.println("housenumber doublet at osm id " + objectid);
									doubletaddresses++;
								}
							} else {
								System.out.println("WARNING: OSM Way has a housenumber, but no street or place Information and will be ignored. OSM-Wayid is " + objectid);
							}
						}
	    			}
		
	    	    		// loop over all osm relation objects with addr:housenumber Tag
	    	    	for (Map.Entry<Long, Relation> relationmap: gibmirrelations.entrySet()) {
						ImportAddress housenumber = new ImportAddress();
						housenumber.setSourceSrid(importparameter.getSourceCoordinateSystem());
						String countrycode = importparameter.getCountrycode();

						Long objectid = relationmap.getKey();
		        		Collection<Tag> tags = relationmap.getValue().getTags();
		        		HashMap<String,String> keyvalues = new HashMap<String,String>();
		        		for (Tag tag: tags) {
		        			keyvalues.put(tag.getKey(), tag.getValue());
			        		if(tag.getKey().equals("addr:country")) {
				        			countrycode = tag.getValue().trim();
				        		try {
									housenumber.setCountrycode(countrycode);
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
			        		}
			        		if(tag.getKey().equals("addr:city"))
			        			housenumber.setMunicipality(tag.getValue().trim());
			        		if(		tag.getKey().equals("addr:street")
			        			||	tag.getKey().equals("addr:place")) {
		        				housenumber.setStreet(tag.getValue().trim());
			        		}
			        		if(tag.getKey().equals("addr:postcode"))
			        			housenumber.setPostcode(tag.getValue().trim());
			        		if(tag.getKey().equals("addr:housenumber")) {
			        			housenumber.setHousenumber(tag.getValue().trim());
								if ((housenumber.getHousenumber().indexOf(",") != -1) ||
									(housenumber.getHousenumber().indexOf(";") != -1)) {
									System.out.println("WARNING: in housenumber special character " +
										"for more than one housenumber found, will not be resolved to separate housenumbers");
								}
			        		}
			        		if(tag.getKey().equals("centroid_lon"))
			        			housenumber.setLon(Double.parseDouble(tag.getValue()));
			        		if(tag.getKey().equals("centroid_lat"))
			        			housenumber.setLat(Double.parseDouble(tag.getValue()));
			        		if(extraosmkeys.contains(tag.getKey())) {
			        			housenumber.getOSMTagList().add(tag.getKey().trim(), tag.getValue().trim());
			        		}
			        	}
						System.out.println( "raw relation with housenumber ===" + housenumber.getHousenumber() + 
							"=== in street ===" + housenumber.getStreet() + "===, node id #" + objectid + "===");
						if (!housenumber.getHousenumber().equals("")) {
							if (!housenumber.getStreet().equals("")) {
//osmhousenumber.set_osm_tag(keyvalues);
								Municipality municipality = null;
								try {
									municipality = new Municipality(countrycode,
										housenumber.getMunicipality(), "");
								} catch (Exception e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								HousenumberList housenumberlist = null;
								if(lists.containsKey(municipality))
									housenumberlist = lists.get(municipality);
								else {
									housenumberlist = new HousenumberList(municipality);
								}
								if(!housenumberlist.contains(housenumber)) {
									housenumberlist.addHousenumber(housenumber);
									lists.put(municipality,  housenumberlist);
								} else {
									System.out.println("housenumber doublet at osm id " + objectid);
									doubletaddresses++;
								}
							} else {
								System.out.println("WARNING: OSM Relation has a housenumber, " +
									"but no street or place Information and will be ignored. OSM-relationid is " + objectid);
							}
						}
	    			}
				}

				@Override
				public void initialize(Map<String, Object> metaData) {
				}

				@Override
				public void process(EntityContainer entityContainer) {

			        Entity entity = entityContainer.getEntity();
			        if (entity instanceof Node) {
			            //do something with the node
			        	nodes_count++;

		    			//availableNodes.set(entity.getId());

						NodeContainer nodec = (NodeContainer) entityContainer;
						Node node = nodec.getEntity();
						//System.out.println("Node lon: "+node.getLongitude() + "  lat: "+node.getLatitude()+"===");

						gibmirnodes.put(entity.getId(), node);
			        } else if (entity instanceof Way) {
			        	ways_count++;
			        	

						WayContainer wayc = (WayContainer) entityContainer;
						Way way = wayc.getEntity();
						List<WayNode> actwaynodes = way.getWayNodes();
						Integer lfdnr = 0;
						Double lon_sum = 0.0D;
						Double lat_sum = 0.0D;
						for (WayNode waynode: actwaynodes) {
							Node actnode = gibmirnodes.get(waynode.getNodeId());
							lon_sum += actnode.getLongitude();
							lat_sum += actnode.getLatitude();
							lfdnr++;
						}
		        		Collection<Tag> waytags = way.getTags();
						Double centroid_lon = lon_sum / lfdnr;
						Double centroid_lat = lat_sum / lfdnr;
						waytags.add(new Tag("centroid_lon", centroid_lon.toString()));
						waytags.add(new Tag("centroid_lat", centroid_lat.toString()));
			        
		    			gibmirways.put(entity.getId(), way);
			        
			        } else if (entity instanceof Relation) {
			            //do something with the relation
			        	relations_count++;
			        	//System.out.println("Relation   " + entity.toString());
			        	List<RelationMember> relmembers =  ((Relation) entity).getMembers();

						RelationContainer relationc = (RelationContainer) entityContainer;
						Relation relation = relationc.getEntity();
			        	
		        		Collection<Tag> relationtags = entity.getTags();
		        		String relationType = "";
		        		String relationName = "";
		        		boolean relationContainsAddrhousenumber = false;
						for (Tag tag: relationtags) {
		        			//System.out.println("Tag [" + tag.getKey() + "] ==="+tag.getValue()+"===");
		        			if(	tag.getKey().equals("type"))
		        				relationType = tag.getValue();
		        			if(	tag.getKey().equals("name"))
		        				relationName = tag.getValue();
		        			if(tag.getKey().equals("addr:housenumber"))
		        				relationContainsAddrhousenumber = true;
						}

						if(		! relationType.equals("associatedStreet")
							&& 	! relationType.equals("multipolygon")) {
							System.out.println("WARNING: Relation is not of type associatedStreet, instead ===" + 
								relationType + "===, OSM-Id ===" + entity.getId() + "===");
							return;
			        	}
						if(		relationType.equals("associatedStreet")
							&&	(relationName.equals(""))) {
							System.out.println("WARNING: Relation has no name Tag, will be ignored, OSM-Id ===" + entity.getId() + "===");
							return;
			        	}

						Integer lfdnr = 0;
						Double lon_sum = 0.0D;
						Double lat_sum = 0.0D;
			        	for(int memberi = 0; memberi < relmembers.size(); memberi++) {
			        		RelationMember actmember = relmembers.get(memberi);
			        		EntityType memberType = actmember.getMemberType();
			        		long memberId = actmember.getMemberId();

			        		//System.out.println("relation member ["+memberi+"]  Typ: "+memberType+"   ==="+actmember.toString()+"===   Role ==="+actmember.getMemberRole()+"===");

			        		if(actmember.getMemberRole().equals("street")) {		// ignore relation member with role street
								System.out.println("WARNING: Relation is of type=street, will be ignored, OSM-Id ===" + entity.getId() + "===");
			        			continue;
			        		}

			        		if (EntityType.Node.equals(memberType)) {
			    				if (gibmirnodes.get(memberId) != null) {
			    					System.out.println("Info: in Relation Member vom Type   NODE enthalten  ==="+gibmirnodes.get(memberId).toString()+"===");
			    					System.out.println("Info:   Hier die Tags des Node:  "+gibmirnodes.get(memberId).getTags().toString()+"===");
					        		Collection<Tag> nodetags = gibmirnodes.get(memberId).getTags();
									nodetags.add(new Tag("addr:street", relationName));
			    				}
			    			} else if (EntityType.Way.equals(memberType)) {
			    				if (gibmirways.get(memberId) != null) {
			    					Collection<Tag> waytags = gibmirways.get(memberId).getTags();
									waytags.add(new Tag("addr:street", relationName));

									Way actway = gibmirways.get(memberId);
									List<WayNode> actwaynodes = actway.getWayNodes();
									for (WayNode waynode: actwaynodes) {
										Node actnode = gibmirnodes.get(waynode.getNodeId());
										if(!actmember.getMemberRole().equals("inner")) {
											lon_sum += actnode.getLongitude();
											lat_sum += actnode.getLatitude();
											lfdnr++;
										}
									}
			    				}
			    			}
			        	} // loop over alle relation members
			        		// if relation contains a housenumber (so its not an assocatedStreet relation, but a multipolygon with address)
			        	if(relationContainsAddrhousenumber) {
							Double centroid_lon = lon_sum / lfdnr;
							Double centroid_lat = lat_sum / lfdnr;
							relationtags.add(new Tag("centroid_lon", centroid_lon.toString()));
							relationtags.add(new Tag("centroid_lat", centroid_lat.toString()));
			    			gibmirrelations.put(entity.getId(), relation);
			        	}
			        }
				}
			};
			
			RunnableSource osmfilereader;

			if(importparameter.getImportfile() == null)
				return null;
			File filehandle = new File(importparameter.getImportfile());
			if(!filehandle.isFile() || !filehandle.canRead())
				return null;
			

			osmfilereader = new XmlReader(filehandle, false, CompressionMethod.None);

			osmfilereader.setSink(sinkImplementation);

			Thread readerThread = new Thread(osmfilereader);
			readerThread.start();

			while (readerThread.isAlive()) {
		        readerThread.join();
			}
			
		} catch (OsmosisRuntimeException osmosiserror) {
			System.out.println("ERROR: Osmosis runtime Error ...");
			System.out.println("ERROR: " +osmosiserror.toString());
	    } catch (InterruptedException e) {
	    	System.out.println("WARNING: Execution of type InterruptedException occured, details follows ...");
	    	System.out.println("WARNING: " + e.toString());
	        /* do nothing */
		}
		return lists;
	}
}
