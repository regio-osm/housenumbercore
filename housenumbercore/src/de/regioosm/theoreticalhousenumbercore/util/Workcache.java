package de.regioosm.theoreticalhousenumbercore.util;
/*

	V1.0, 08.08.2013, Dietmar Seifert
		* works as a cache between table auswertung_hausnummern during the workflow phase auswertung_offline.java,
		* meaning 
		* - at program start (of a job) the existing content of table auswertung_hausnummern will be read into this cache
		* - in the table auswertung_hausnummern the rows will NOT be deleted furthermore
		* - the changes or unchanged calculation will be stored in this cache
		* - at the end, this cache will be written back to the table auswertung_hausnummern in this way
		* 	- untouched cache entries must be deleted in table, because they doesn't exists anymore
		* 	- unchanged cache entries doesn't cause any work
		* 	- changed cache entries will be written to the table back 
		* This cache has two goals
		* - time for program auswertung_offline.java should be dramatically lower, because most of time is insert of rows
		* - the auswertung for the actual job is not available during calculation, thats bad for end users


		Changes to Database: in procedure public void store_to_db(), records will be insert, updated or delete in table auswertung_hausnummern
*/

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import de.regioosm.housenumbercore.util.Applicationconfiguration;



public class Workcache {
	static Applicationconfiguration configuration = new Applicationconfiguration();
	static Connection con_hausnummern;

	Workcache_Entry cache[] = new Workcache_Entry[500000];
	public boolean debug = false;
	public int cache_count = 0;
	private boolean housenumberaddition_exactly = true;
	String land = "";
	String stadt = "";
	String jobname = "";
	
	
	private void workcache_debug(String outputtext) {
		PrintWriter debug_output = null;
		String workcache_debugfile = "workcache_debug.txt";

		if(!debug) {
			return;
		} else {
			try {
				debug_output = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(workcache_debugfile, true),"UTF-8")));
				debug_output.println(outputtext);
				System.out.println("***" + outputtext);
				debug_output.close();
			} catch (IOException ioerror) {
				System.out.println("ERROR: couldn't open file to write, filename was ==="+workcache_debugfile+"===");
				ioerror.printStackTrace();
			}
		}
	}

	/**
	 * @return the housenumberaddition_exactly
	 */
	public boolean isHousenumberaddition_exactly() {
		return housenumberaddition_exactly;
	}

	/**
	 * @param housenumberaddition_exactly the housenumberaddition_exactly to set
	 */
	public void setHousenumberaddition_exactly(
			boolean housenumberaddition_exactly) {
		this.housenumberaddition_exactly = housenumberaddition_exactly;
	}

	public int count() {
		return cache_count;
	}

	public void clear() {
		for(int cacheentries = 0; cacheentries < this.count(); cacheentries++) {
			cache[cacheentries] = null;
		}
		cache_count = 0;
	}
	
	public int count_unchanged() {
		int count = 0;
		for(int entryindex = 0; entryindex < cache_count; entryindex++) {
			if(cache[entryindex].getstate().equals("unchanged"))
				count++;
		}
		return count;
	}

	public int countTreffertyp(Workcache_Entry.Treffertyp treffertyp) {
		int count = 0;
		for(int entryindex = 0; entryindex < cache_count; entryindex++) {
			if(cache[entryindex].getTreffertyp() == treffertyp)
				count++;
		}
		return count;
	}


	// set an entry, which is from database table, so not a real new entry
	public Workcache_Entry add_dbentry(Workcache_Entry entry) {
		Workcache_Entry newentry = new Workcache_Entry(this.isHousenumberaddition_exactly());
		newentry.set(entry);
		newentry.setstate("dbloaded");
		newentry.toStringlong();
		cache[cache_count++] = newentry;
		
		return newentry;
	}

		// add a real new entry
	public void add_newentry( Workcache_Entry in_newentry) {
		Workcache_Entry newentry = new Workcache_Entry(this.isHousenumberaddition_exactly());
		newentry.set(in_newentry);
		newentry.setstate("new");
		newentry.toStringlong();
		cache[cache_count++] = newentry;
	}

	
	private int find_entry_in_cache(Workcache_Entry findentry) {
		if(		(findentry.getStrasseId() == -1L) 
			||	((findentry.getHausnummerSortierbar() == null) || findentry.getHausnummerSortierbar().equals(""))
			||	((findentry.getHausnummerNormalisiert() == null) || findentry.getHausnummerNormalisiert().equals(""))
			||	((findentry.getOsmTag() != null) && (findentry.getOsmTag().indexOf("=>") != -1))) {
			findentry.normalize();
		}

		for(int entryindex = 0; entryindex < cache_count; entryindex++) {
			if(		(cache[entryindex].getLandId() == findentry.getLandId())
				&&	(cache[entryindex].getStadtId() == findentry.getStadtId()) 
				&&	(cache[entryindex].getStrasseId() == findentry.getStrasseId()) 
				&&	(cache[entryindex].getJobId() == findentry.getJobId())) {
				if(		this.isHousenumberaddition_exactly()
					&& cache[entryindex].getHausnummer().equals(findentry.getHausnummer())) {
					return entryindex;
				} else if(!this.isHousenumberaddition_exactly()) {
					if(cache[entryindex].getHausnummer().toLowerCase().equals(findentry.getHausnummer().toLowerCase())) {
						if(!cache[entryindex].getHausnummer().equals(findentry.getHausnummer())) {
							System.out.println("Hausnummer nur identisch, weil grosskleinirrelevant findentry.getHausnummer()===" + findentry.getHausnummer() + "===  cache[entryindex].getHausnummer() ===" + cache[entryindex].getHausnummer() + "===");
						}
						return entryindex;
					}
				}
			}
		}
		return -1;
	}
	
	/**
	 * search the findentry housenumber object, if it is known in Workcache and optionaly return the found object.
	 * The only one optionaly attribute in findentry can be treffertyp (set "" to ignore)
	 * @param findentry: housenumber object, which will be searched in structure, if available
	 * @return:			 the found housenumber object (only one) or null
	 */
	public  Workcache_Entry get_entry_in_cache(Workcache_Entry findentry) {	// public version of find_entry, but slightly different works
		if(		(findentry.getStrasseId() == -1L) 
			||	((findentry.getHausnummerSortierbar() == null) || findentry.getHausnummerSortierbar().equals(""))
			||	((findentry.getHausnummerNormalisiert() == null) || findentry.getHausnummerNormalisiert().equals(""))
			||	((findentry.getOsmTag() != null) && (findentry.getOsmTag().indexOf("=>") != -1))) {
			findentry.normalize();
		}

		for(int entryindex = 0; entryindex < this.count(); entryindex++) {
			if(		(this.cache[entryindex].getLandId() == findentry.getLandId())
				&&	(this.cache[entryindex].getStadtId() == findentry.getStadtId()) 
				&&	(this.cache[entryindex].getStrasseId() == findentry.getStrasseId()) 
				&&	(this.cache[entryindex].getJobId() == findentry.getJobId())
				&&	(this.cache[entryindex].getHausnummerNormalisiert().equals(findentry.getHausnummerNormalisiert()))
				&&	((findentry.getTreffertyp() == Workcache_Entry.Treffertyp.UNSET) || (this.cache[entryindex].getTreffertyp() == findentry.getTreffertyp()))	// treffertyp optional if set
				) {
				return this.cache[entryindex];
			}
		}
		return null;
	}

	
	/**
	 * get the cacheentry object with the given index
	 * @param index: index of the cacheentry
	 * @return:			 the found housenumber object (only one) or null
	 */
	public Workcache_Entry get_entry_in_cache(int index) {	// public version of find_entry, but slightly different works
		if(index > cache_count) {
			return null;
		} else { 
			return cache[index];
		}
	}
	

	
		// update in this cache
	public void update(Workcache_Entry updateentry) {
		boolean debug_output = true;
		if(debug_output) System.out.println("start of .update of class Workcache ...");
		if(	(updateentry.getStrasseId() == -1L) 
			|| ((updateentry.getHausnummerSortierbar() == null) || updateentry.getHausnummerSortierbar().equals(""))
			||	((updateentry.getHausnummerNormalisiert() == null) || updateentry.getHausnummerNormalisiert().equals(""))
			|| 	((updateentry.getOsmTag() != null) && (updateentry.getOsmTag().indexOf("=>") != -1))
		) {
			if(debug_output) System.out.println("in .update of class Workcache: necessary to call .normalize...");
			updateentry.normalize();
		}

		if(debug_output) System.out.println("in .update of class Workcache: find entry ...");
		int entry_index = find_entry_in_cache(updateentry);
		if(debug_output) System.out.println("found? "+(entry_index > -1));
		if(entry_index > -1) {
			try {
				if(debug_output) System.out.println("in .update of class Workcache: before call .update of class Entry ...");
				cache[entry_index].update(updateentry);
				if(debug_output) System.out.println("in .update of class Workcache: after call .update of class Entry ...");
			} catch (HausnummernException e) {
				System.out.println("Böser Fehler in Workcache, Fkt. update: offenbar brachte find_entry_in_cache einen entry zurück, der nicht passte, bitte prüfen");
				e.printStackTrace();
				return;
			}
		} else {
			if(debug_output) System.out.println("in .update of class Workcache: before call add_newentry ...");
			add_newentry(updateentry);
			if(debug_output) System.out.println("in .update of class Workcache: after call add_newentry ...");
		}
		if(debug_output) System.out.println("end of .update of class Workcache ...");
	}

	
	public void store_to_table_osm_hausnummern() {
			// only difference to ...auswertung_hausnummern is, of course, other table name and non existing column treffertyp


//TODO	deleted housenumber in case of changing strasse_id or similiar seems to be a problem
//		then old housenumber object will not be deleted
//     vorübergehende löschen der Fehler und einiger weiterer Datensätze wg Hausnummer 56-58:
//		delete from osm_hausnummern where id in (select oh.id from osm_hausnummern as oh, (select count(*) as anzahl, osm_id, job_id from osm_hausnummern  group by osm_id,job_id order by count(osm_id) desc) as foo where foo.anzahl > 1 and oh.osm_id = foo.osm_id and foo.getJobId() = oh.getJobId());

		try {
			java.util.Date timeMethodStart = new java.util.Date();

			Class.forName("org.postgresql.Driver");

			String url_hausnummern = configuration.db_application_url;
			con_hausnummern = DriverManager.getConnection(url_hausnummern, configuration.db_application_username, configuration.db_application_password);
			con_hausnummern.setAutoCommit(true);

			Statement stmt_osmhausnummern = con_hausnummern.createStatement();
			HashMap <String,Integer> stateCounter = new HashMap<String,Integer>();
			System.out.println("before update cache to table osm_hausnummern: cache count: "+cache_count);
			for(int entryindex = 0; entryindex < cache_count; entryindex++) {
				Workcache_Entry actentry = cache[entryindex];
				String actstate = actentry.getstate();
				if (stateCounter.get(actstate) == null) {
					stateCounter.put(actstate, 1);
				} else {
					stateCounter.put(actstate, stateCounter.get(actstate) + 1);
				}
				if( actstate.equals("unchanged")) {			// entry hasn't been changed, but acknowledged
					// nothing to do
				}
				else if(		actstate.equals("new")) {
						// insert new record
					String insertbefehl_osmhausnummern = "";
					insertbefehl_osmhausnummern = "INSERT INTO osm_hausnummern ";
					insertbefehl_osmhausnummern += "(land_id, stadt_id, strasse_id, job_id, hausnummer, hausnummer_sortierbar";
					insertbefehl_osmhausnummern += ", objektart, osm_objektart, osm_id";
					insertbefehl_osmhausnummern += ", point, pointsource";			// new at 04.03.2014
					insertbefehl_osmhausnummern += ") VALUES (";
					insertbefehl_osmhausnummern += actentry.getLandId();
					insertbefehl_osmhausnummern += ", " + actentry.getStadtId();
					insertbefehl_osmhausnummern += ", " + actentry.getStrasseId();
					insertbefehl_osmhausnummern += ", " + actentry.getJobId();
					insertbefehl_osmhausnummern += ", '" + actentry.getHausnummer() + "'";
					insertbefehl_osmhausnummern += ", '" + actentry.getHausnummerSortierbar() + "'";
					if((actentry.getOsmTag() == null) || actentry.getOsmTag().equals("null"))
						insertbefehl_osmhausnummern += ", null";
					else
						insertbefehl_osmhausnummern += ", '" + actentry.getOsmTag() + "'";
					if((actentry.getOsmObjektart() == null) || actentry.getOsmObjektart().equals("null"))
						insertbefehl_osmhausnummern += ", null";
					else
						insertbefehl_osmhausnummern += ", '" + actentry.getOsmObjektart() + "'";
					insertbefehl_osmhausnummern += ", " + actentry.getOsmId() + "";
					if((actentry.getLonlat() == null) || actentry.getLonlat().equals(""))							// new at 04.03.2014
						insertbefehl_osmhausnummern  += ", null";
					else
						if(actentry.getLonlat().indexOf("POINT") != -1) {
							insertbefehl_osmhausnummern += ", St_GeomFromText(\'" + actentry.getLonlat() + "\',4326)";
						} else {
							insertbefehl_osmhausnummern += ", St_GeomFromText(\'POINT(" + actentry.getLonlat() + ")\',4326)";
						}
					if((actentry.getLonlat_source() == null) || actentry.getLonlat_source().equals(""))			// new at 04.03.2014
						insertbefehl_osmhausnummern += ", ''";
					else
						insertbefehl_osmhausnummern += ", '" + actentry.getLonlat_source() + "'";
					insertbefehl_osmhausnummern += ");";
					System.out.println("insertbefehl_osmhausnummern ==="+insertbefehl_osmhausnummern+"===");
					//workcache_debug("store_to_db: insertbefehl + ===" + insertbefehl_osmhausnummern + "===");

					try {
						java.util.Date time_beforeinsert = new java.util.Date();
						stmt_osmhausnummern.executeUpdate( insertbefehl_osmhausnummern );
						java.util.Date time_afterinsert = new java.util.Date();
						System.out.println("TIME for insert record osmhausnummern_hausnummern (identical ones), in ms. "+(time_afterinsert.getTime()-time_beforeinsert.getTime()));
					}
					catch( SQLException e) {
						System.out.println("ERROR: during insert in table osmhausnummern_hausnummern identische, insert code was ==="+insertbefehl_osmhausnummern+"===");
						e.printStackTrace();
					}
				} // end of actual entry is new
				else if(actstate.equals("changed")) { 		// entry has been changed
						// update record
					String updatebefehl_osmhausnummern = "";
					updatebefehl_osmhausnummern = "UPDATE osm_hausnummern";
					updatebefehl_osmhausnummern += " set land_id = " + actentry.getLandId();
					updatebefehl_osmhausnummern += ", stadt_id = " + actentry.getStadtId();
					updatebefehl_osmhausnummern += ", strasse_id = " + actentry.getStrasseId();
					updatebefehl_osmhausnummern += ", job_id = " + actentry.getJobId();
					updatebefehl_osmhausnummern += ", hausnummer = '" + actentry.getHausnummer() + "'";
					updatebefehl_osmhausnummern += ", hausnummer_sortierbar = '" + actentry.getHausnummerSortierbar() + "'";
					if((actentry.getOsmTag() == null) || actentry.getOsmTag().equals("null"))
						updatebefehl_osmhausnummern += ", objektart = null";
					else
						updatebefehl_osmhausnummern += ", objektart = '" + actentry.getOsmTag() + "'";
					if((actentry.getOsmObjektart() == null) || actentry.getOsmObjektart().equals("null"))
						updatebefehl_osmhausnummern += ", osm_objektart = null";
					else
						updatebefehl_osmhausnummern += ", osm_objektart = '" + actentry.getOsmObjektart() + "'";
					updatebefehl_osmhausnummern += ", osm_id = " + actentry.getOsmId() + "";
					if((actentry.getLonlat() == null) || actentry.getLonlat().equals(""))							// new at 04.03.2014
						updatebefehl_osmhausnummern += ", point = null";
					else {
						if(actentry.getLonlat().indexOf("POINT") != -1) {
							updatebefehl_osmhausnummern += ", point = St_GeomFromText(\'" + actentry.getLonlat() + "\',4326)";
						} else {
							updatebefehl_osmhausnummern += ", point = St_GeomFromText(\'POINT(" + actentry.getLonlat() + ")\',4326)";
						}
					}
					if((actentry.getLonlat_source() == null) || actentry.getLonlat_source().equals(""))			// new at 04.03.2014
						updatebefehl_osmhausnummern += ", pointsource = ''";
					else
						updatebefehl_osmhausnummern += ", pointsource = '" + actentry.getLonlat_source() + "'";
					updatebefehl_osmhausnummern += " WHERE id = " + actentry.getId() + ";";
					System.out.println("updatebefehl_osmhausnummern ==="+updatebefehl_osmhausnummern+"===");
					//workcache_debug("store_to_db: insertbefehl + ===" + updatebefehl_osmhausnummern + "===");
					try {
						java.util.Date time_beforeupdate = new java.util.Date();
						stmt_osmhausnummern.executeUpdate( updatebefehl_osmhausnummern );
						java.util.Date time_afterupdate = new java.util.Date();
						System.out.println("TIME for update record osmhausnummern_hausnummern (identical ones), in ms. "+(time_afterupdate.getTime()-time_beforeupdate.getTime()));
					}
					catch( SQLException e) {
						System.out.println("ERROR: during update in table osmhausnummern_hausnummern identische, update code was ==="+updatebefehl_osmhausnummern+"===");
						e.printStackTrace();
					}
				}
				else if(	actstate.equals("deleted") ||			// active set to delete. Should be seldom. More often, a row will be not re-set aber set to dbloaded for being invalide
							actstate.equals("dbloaded"))	 { 		// loaded from db. If later not set to other state, then the entry is no longer valid
					// delete record
					String deletebefehl_osmhausnummern = "";
					deletebefehl_osmhausnummern = "DELETE FROM osm_hausnummern";
					deletebefehl_osmhausnummern += " WHERE id = " + actentry.getId() + ";";
					System.out.println("deletebefehl_osmhausnummern ==="+deletebefehl_osmhausnummern+"===");
					//workcache_debug("store_to_db: insertbefehl + ===" + deletebefehl_osmhausnummern + "===");
					try {
						java.util.Date time_beforedelete = new java.util.Date();
						stmt_osmhausnummern.executeUpdate( deletebefehl_osmhausnummern );
						java.util.Date time_afterdelete = new java.util.Date();
						System.out.println("TIME for delete record osmhausnummern_hausnummern (identical ones), in ms. "+(time_afterdelete.getTime()-time_beforedelete.getTime()));
					}
					catch( SQLException e) {
						System.out.println("ERROR: during delete in table osmhausnummern_hausnummern identische, delete code was ==="+deletebefehl_osmhausnummern+"===");
						e.printStackTrace();
					}
				}
				else
				{
					System.out.println("ERROR ERROR other actstate ===" + actstate + "===, please check !!!");
				}
				
				
			} // end of loop over all entries of cache
			stmt_osmhausnummern.close();
			con_hausnummern.close();

			java.util.Date timeMethodEnd = new java.util.Date();
			System.out.println("TIME for complete store_to_osm_hausnummern in sek "+(timeMethodEnd.getTime()-timeMethodStart.getTime())/1000);

			System.out.println("Ausgabe Verteilung Objektzustand nach Ende store_to_osm_hausnummern ...");
			for (String key : stateCounter.keySet()) {
			    System.out.println("Key [" + key + "] ==="+stateCounter.get(key)+"===");
			}

		
		} // end of try to connect to DB and operate with DB
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch( SQLException e) {
			e.printStackTrace();
			try {
				con_hausnummern.close();
			} catch( SQLException innere) {
				System.out.println("inner sql-exception (tried to to close connection ...");
				innere.printStackTrace();
			}
			return;
		}
	}	// end of procedure store_to_table_osm_hausnummern


	public void store_to_table_auswertung_hausnummern() {

	
		try {
			java.util.Date timeMethodStart = new java.util.Date();

			Class.forName("org.postgresql.Driver");

			String url_hausnummern = configuration.db_application_url;
			con_hausnummern = DriverManager.getConnection(url_hausnummern, configuration.db_application_username, configuration.db_application_password);
			con_hausnummern.setAutoCommit(true);

			
			Statement stmt_auswertung = con_hausnummern.createStatement();
			HashMap <String,Integer> stateCounter = new HashMap<String,Integer>();
			for(int entryindex = 0; entryindex < cache_count; entryindex++) {
				Workcache_Entry actentry = cache[entryindex];
				String actstate = actentry.getstate();
				if (stateCounter.get(actstate) == null) {
					stateCounter.put(actstate, 1);
				} else {
					stateCounter.put(actstate, stateCounter.get(actstate) + 1);
				}

				if( actstate.equals("unchanged")) {			// entry hasn't been changed, but acknowledged
					// nothing to do
				}
				else if(		actstate.equals("new")) {
						// insert new record
					String insertbefehl_auswertung = "";
					insertbefehl_auswertung = "INSERT INTO auswertung_hausnummern ";
					insertbefehl_auswertung += "(copyland, copystadt, copystrasse, copyjobname, land_id, stadt_id, strasse_id, job_id, hausnummer, hausnummer_sortierbar";
					insertbefehl_auswertung += ", objektart, osm_objektart, osm_id, treffertyp";
					insertbefehl_auswertung += ", point, pointsource";			// new at 19.11.2013
					insertbefehl_auswertung += ") VALUES (";
					insertbefehl_auswertung += "'" + this.land + "'";
					insertbefehl_auswertung += ", '" + this.stadt+ "'";
					insertbefehl_auswertung += ", '" + actentry.getStrasse() + "'";
					insertbefehl_auswertung += ", '" + this.jobname + "'";
					insertbefehl_auswertung += ", " + actentry.getLandId();
					insertbefehl_auswertung += ", " + actentry.getStadtId();
					insertbefehl_auswertung += ", " + actentry.getStrasseId();
					insertbefehl_auswertung += ", " + actentry.getJobId();
					insertbefehl_auswertung += ", '" + actentry.getHausnummer() + "'";
					insertbefehl_auswertung += ", '" + actentry.getHausnummerSortierbar() + "'";
					if(actentry.getOsmTag() == null)
						insertbefehl_auswertung += ", null";
					else
						insertbefehl_auswertung += ", '" + actentry.getOsmTag() + "'";
					if(actentry.getOsmObjektart() == null)
						insertbefehl_auswertung += ", null";
					else
						insertbefehl_auswertung += ", '" + actentry.getOsmObjektart() + "'";
					insertbefehl_auswertung += ", " + actentry.getOsmId() + "";
					insertbefehl_auswertung += ", '" + actentry.getTreffertypText() + "'";
					if((actentry.getLonlat() == null) || actentry.getLonlat().equals(""))							// new at 19.11.2013
						insertbefehl_auswertung += ", null";
					else
						if(actentry.getLonlat().indexOf("POINT") != -1) {
							insertbefehl_auswertung += ", St_GeomFromText(\'" + actentry.getLonlat() + "\',4326)";
						} else {
							insertbefehl_auswertung += ", St_GeomFromText(\'POINT(" + actentry.getLonlat() + ")\',4326)";
						}
					if((actentry.getLonlat_source() == null) || actentry.getLonlat_source().equals(""))			// new at 19.11.2013
						insertbefehl_auswertung += ", ''";
					else
						insertbefehl_auswertung += ", '" + actentry.getLonlat_source() + "'";
					insertbefehl_auswertung += ");";
					System.out.println("insertbefehl_auswertung ==="+insertbefehl_auswertung+"===");
					System.out.println("insert-cacheobject ==="+actentry.toStringlong()+"===");
					//workcache_debug("store_to_db: insertbefehl + ===" + insertbefehl_auswertung + "===");

					try {
						java.util.Date time_beforeinsert = new java.util.Date();
						stmt_auswertung.executeUpdate( insertbefehl_auswertung );
						java.util.Date time_afterinsert = new java.util.Date();
						System.out.println("TIME for insert record auswertung_hausnummern (identical ones), in ms. "+(time_afterinsert.getTime()-time_beforeinsert.getTime()));
					}
					catch( SQLException e) {
						System.out.println("ERROR: during insert in table auswertung_hausnummern identische, insert code was ==="+insertbefehl_auswertung+"===");
						e.printStackTrace();
					}
				} // end of actual entry is new
				else if(actstate.equals("changed")) { 		// entry has been changed
						// update record
					String updatebefehl_auswertung = "";
					updatebefehl_auswertung = "UPDATE auswertung_hausnummern";
					updatebefehl_auswertung += " set";
					updatebefehl_auswertung += " copyland = '" + this.land + "'";
					updatebefehl_auswertung += ", copystadt = '" + this.stadt + "'";
					updatebefehl_auswertung += ", copyjobname = '" + this.jobname + "'";
					updatebefehl_auswertung += ", copystrasse = '" + actentry.getStrasse() + "'";
					updatebefehl_auswertung += ", land_id = " + actentry.getLandId();
					updatebefehl_auswertung += ", stadt_id = " + actentry.getStadtId();
					updatebefehl_auswertung += ", strasse_id = " + actentry.getStrasseId();
					updatebefehl_auswertung += ", job_id = " + actentry.getJobId();
					updatebefehl_auswertung += ", hausnummer = '" + actentry.getHausnummer() + "'";
					updatebefehl_auswertung += ", hausnummer_sortierbar = '" + actentry.getHausnummerSortierbar() + "'";
					updatebefehl_auswertung += ", objektart = '" + actentry.getOsmTag() + "'";
					updatebefehl_auswertung += ", osm_objektart = '" + actentry.getOsmObjektart() + "'";
					updatebefehl_auswertung += ", osm_id = " + actentry.getOsmId() + "";
					updatebefehl_auswertung += ", treffertyp = '" + actentry.getTreffertypText() + "'";
					if((actentry.getLonlat() == null) || actentry.getLonlat().equals(""))							// new at 19.11.2013
						updatebefehl_auswertung += ", point = null";
					else
						if(actentry.getLonlat().indexOf("POINT") != -1) {
							updatebefehl_auswertung += ", point = St_GeomFromText(\'" + actentry.getLonlat() + "\',4326)";
						} else {
							updatebefehl_auswertung += ", point = St_GeomFromText(\'POINT(" + actentry.getLonlat() + ")\',4326)";
						}
					if((actentry.getLonlat_source() == null) || actentry.getLonlat_source().equals(""))			// new at 19.11.2013
						updatebefehl_auswertung += ", pointsource = ''";
					else
						updatebefehl_auswertung += ", pointsource = '" + actentry.getLonlat_source() + "'";
					updatebefehl_auswertung += " WHERE id = " + actentry.getId() + ";";
					System.out.println("updatebefehl_auswertung ==="+updatebefehl_auswertung+"===");
					System.out.println("update-cacheobject ==="+actentry.toStringlong()+"===");
					//workcache_debug("store_to_db: insertbefehl + ===" + updatebefehl_auswertung + "===");
					try {
						java.util.Date time_beforeupdate = new java.util.Date();
						stmt_auswertung.executeUpdate( updatebefehl_auswertung );
						java.util.Date time_afterupdate = new java.util.Date();
						System.out.println("TIME for update record auswertung_hausnummern (identical ones), in ms. "+(time_afterupdate.getTime()-time_beforeupdate.getTime()));
					}
					catch( SQLException e) {
						System.out.println("ERROR: during update in table auswertung_hausnummern identische, update code was ==="+updatebefehl_auswertung+"===");
						e.printStackTrace();
					}
				}
				else if(	actstate.equals("deleted") ||			// active set to delete. Should be seldom. More often, a row will be not re-set aber set to dbloaded for being invalide
							actstate.equals("dbloaded"))	 { 		// loaded from db. If later not set to other state, then the entry is no longer valid
					// delete record
					String deletebefehl_auswertung = "";
					deletebefehl_auswertung = "DELETE FROM auswertung_hausnummern";
					deletebefehl_auswertung += " WHERE id = " + actentry.getId() + ";";
					System.out.println("deletebefehl_auswertung ==="+deletebefehl_auswertung+"===");
					System.out.println("delete-cacheobject ==="+actentry.toStringlong()+"===");
					//workcache_debug("store_to_db: insertbefehl + ===" + deletebefehl_auswertung + "===");
					try {
						java.util.Date time_beforedelete = new java.util.Date();
						stmt_auswertung.executeUpdate( deletebefehl_auswertung );
						java.util.Date time_afterdelete = new java.util.Date();
						System.out.println("TIME for delete record auswertung_hausnummern (identical ones), in ms. "+(time_afterdelete.getTime()-time_beforedelete.getTime()));
					}
					catch( SQLException e) {
						System.out.println("ERROR: during delete in table auswertung_hausnummern identische, delete code was ==="+deletebefehl_auswertung+"===");
						e.printStackTrace();
					}
				}
				else
				{
					System.out.println("other actstate ===" + actstate + "===, please check !!!");
				}
						
			} // end of loop over all entries of cache
			stmt_auswertung.close();
			con_hausnummern.close();

			java.util.Date timeMethodEnd = new java.util.Date();
			System.out.println("TIME for complete store_to_auswertung_hausnummern in sek "+(timeMethodEnd.getTime()-timeMethodStart.getTime())/1000);

			System.out.println("Ausgabe Verteilung Objektzustand nach Ende store_to_auswertung ...");
			for (String key : stateCounter.keySet()) {
			    System.out.println("Key [" + key + "] ==="+stateCounter.get(key)+"===");
			}
			
		} // end of try to connect to DB and operate with DB
		catch(ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(1);
		}
		catch( SQLException e) {
			e.printStackTrace();
			try {
				con_hausnummern.close();
			} catch( SQLException innere) {
				System.out.println("inner sql-exception (tried to close connection ...");
				innere.printStackTrace();
			}
			return;
		}
	}	// end of procedure store_to_table_auswertung_hausnummern
}

