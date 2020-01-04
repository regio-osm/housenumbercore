-- Database table and view generation for application housenumber evaluations
--    The DB muss be exist already empty
--
-- V1.3, 2016-02-30, Dietmar Seifert
--            Table names still in german, sorry. Has to be changed in near future
--            Some table columns added in last days to enhance functionality on web frontend
--
-- V1.2, 2013-06-06, Dietmar Seifert
--            Tabelle gebiete, Spalte sub_id  Typ von int nach text geändert
--            Tabelle stadt_hausnummern,   Spalte teilgebiet_id  Name geändert in sub_id, Typ von int tnach text geändert
-- 
-- V1.1, 2013-06-27, Dietmar Seifert
--
--      in Tabelle stadt_hausnummern Spalte teilgebiet_id ergänzt. Dieses gibt optional (nicht besetzt = -1) die Referenz-Nr. des Stadtteils oder Stadtbezirks innerhalb einer größeren Stadt an,
--      wenn die ID in der Stadt Hausnummernliste mit enthalten ist (dann Teilgebiets-scharfes auswerten möglich und doppelte Straßen können berücksichtigt werden)
--
-- V1.0, 2013-05-03, Dietmar Seifert
--
--      Diese Datei wird neu aufgebaut auf Basis vorhandene DB auf eckhart-woerner.de Server
--      Dort war der DB-Name dietmar
--
--


-- Table: land

DROP TABLE land;

CREATE TABLE land
(
  id                       bigserial      NOT NULL,
  land                     text           NOT NULL,
  countrycode              text, 
  gemeindeschluessel_key   text,                        -- fuer Gemeinden außerhalb Deutschland der OSM-Key, in dem der gemeindeschluessel Wert steht. Leer = 'de:amtlicher_gemeindeschluessel'
  boundary_polygon			geometry,
  CONSTRAINT pk_land PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
-- alter table land add column gemeindeschluessel_key text;
-- update land set gemeindeschluessel_key = 'de:amtlicher_gemeindeschluessel' where land = 'Bundesrepublik Deutschland';


-- Table: stadt

DROP TABLE stadt;

CREATE TABLE stadt
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL,
  stadt                           text           NOT NULL,
  officialkeys_id                 text           NOT NULL,
  osm_hierarchy                   text,
  active_adminlevels              integer[],               -- list of admin_level values, which are active main municipality area and active subareas
  housenumberaddition_exactly     text DEFAULT 'y',            -- 'y'(yes) or 'n'(no)
  subareasidentifyable            text,                     -- 'y'(yes) or 'n'(no)
  officialgeocoordinates			 text DEFAULT 'n'				-- 'y'(yes) or 'n'(no). New at 2015-02-09
  sourcelist_url                  text,            -- internet URL for info about the official housenumber file
  sourcelist_copyrighttext        text,            -- copyright text from delivery office
  sourcelist_useagetext           text,            -- usage info on website, told from delivery
  sourcelist_contentdate          date,            -- date of the housenumber list, if told from delivery administration office
  sourcelist_filedate             date,            -- technical file date
  parameters                      hstore,          -- new 2016-02-18 add municipality specific parameters for evaluations as key-value pairs  
                                                       -- 'listcoordosmuploadable=>yes'  official geocoordinates with license compatabile to osm odbl - 2016-02-18
                                                       -- 'listcoordosmuploadlimitcount=>n'  upload to osm limited to n housenumbers - 2016-02-18
                                                       -- 'listcoordforevaluation=>yes'  can officialcoordinates be used for evaluations (don't need to be ODbL-compatible)
  CONSTRAINT pk_stadt PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: gebiete

DROP TABLE gebiete;

CREATE TABLE gebiete
(
  id                              bigserial      NOT NULL,
  name                            text           NOT NULL,
  land_id                         bigint         NOT NULL,
  stadt_id                        bigint         NOT NULL,
  admin_level                     integer,
  sub_id                          text, 
  osm_id                          bigint         NOT NULL,
  polygon                         geometry,
  checkedtime                     timestamp without time zone,   -- timestamp of record creation or last check
  CONSTRAINT pk_gebiete_uniquefields PRIMARY KEY (land_id, stadt_id, admin_level, name)
)
WITH (OIDS=FALSE);


-- Table: jobs

DROP TABLE jobs;

CREATE TABLE jobs
(
  id                              bigserial      NOT NULL, 
  jobname                         text, 
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  gebiete_id                      bigint,
  schedule                        text[],							-- one or more strings with form 'day hh:mm:ss' for schedule date with a week
  checkedtime                     timestamp without time zone,   -- timestamp of record creation or last check
  CONSTRAINT pk_jobs PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: strasse

DROP TABLE strasse;

CREATE TABLE strasse
(
  id                              bigserial      NOT NULL,
  strasse                         text           NOT NULL,
  CONSTRAINT pk_strasse PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: jobs_strassen

DROP TABLE jobs_strassen;

CREATE TABLE jobs_strassen
(
  id                              bigserial      NOT NULL, 
  job_id                          bigint         NOT NULL, 
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  strasse_id                      bigint         NOT NULL, 
  osm_ids                         text,
  linestring                      geometry,
  CONSTRAINT pk_jobs_strassen_uniquefields PRIMARY KEY (job_id, land_id, stadt_id, strasse_id)
)
WITH (OIDS=FALSE);
CREATE INDEX jobsstrassen_jobid_idx ON jobs_strassen USING btree (job_id);
CREATE INDEX jobsstrassen_strasseid_idx ON jobs_strassen USING btree (strasse_id);


-- Table: jobs_strassen_blacklist

DROP TABLE jobs_strassen_blacklist;

CREATE TABLE jobs_strassen_blacklist
(
  id                              bigserial      NOT NULL, 
  job_id                          bigint         NOT NULL, 
  strasse_id                      bigint         NOT NULL, 
  osm_ids                         text,
  reason                          text,
  CONSTRAINT pk_jobs_strassen_blacklist_uniquefields PRIMARY KEY (job_id, strasse_id, osm_ids)
)
WITH (OIDS=FALSE);


-- Table: stadt_hausnummern   - Speicherung der offiziellen Hausnummern, geliefert von einer Liste

DROP TABLE stadt_hausnummern;

CREATE TABLE stadt_hausnummern
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  strasse_id                      bigint         NOT NULL, 
  postcode                        text,								-- new 2015-04-15
  hausnummer                      text           NOT NULL, 
  hausnummer_sortierbar           text           NOT NULL,
  hausnummer_bemerkung            text,
  extraosmtags                    hstore,
  sub_id                          text, 
  point                           geometry,                     -- Geocoordinate in coordsystem 4326 of the address. Source see in column pointsource. new at 2014-06-17  
  pointsource                     text,                         -- Description, where the geocoordinate comes from (normally from the official source like the housenumber list. new at 2014-06-17
  CONSTRAINT pk_stadt_hausnummern     PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
CREATE INDEX stadthausnummern_stadtid_idx ON stadt_hausnummern USING btree (stadt_id);
CREATE INDEX stadthausnummern_strasseid_idx ON stadt_hausnummern USING btree (strasse_id);


-- Table: osm_hausnummern   - in OSM vorhandene Hausnummern

DROP TABLE osm_hausnummern;

CREATE TABLE osm_hausnummern
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  strasse_id                      bigint         NOT NULL, 
  job_id                          bigint         NOT NULL, 
  postcode                        text,								-- new 2015-04-15
  hausnummer                      text           NOT NULL, 
  hausnummer_sortierbar           text           NOT NULL, 
  objektart                       text,                         -- holds the osm_tag in form "key"=>"value" including " !!! - old (up to 23.08.2013): value of osm tag building where found addr:housenumber=* 
                                                                -- 23.08.2013: delete now in table restriction NOT NULL for nodes without building=*
  osm_objektart                   text           NOT NULL,      -- "way", "node" or "relation"
  osm_id                          bigint,
  CONSTRAINT pk_osm_hausnummern_uniquefields PRIMARY KEY (land_id, stadt_id, strasse_id, job_id, hausnummer_sortierbar)
)
WITH (OIDS=FALSE);
CREATE INDEX pk_osm_hausnummern_id ON osm_hausnummern USING btree (id);


-- Table: notexisting_housenumbers   - in reality not existing housenumbers, source is a osm person

DROP TABLE notexisting_housenumbers;

CREATE TABLE notexisting_housenumbers
(
  id                              bigserial      NOT NULL,
  country                         text           NOT NULL,		 -- changed from countrycode to full name of country, name was countrycode until 2015-08-03 
  region                          text,                         -- from Enaikoon, could be Regierungsbezirk or similar
  city                            text           NOT NULL, 
  postcode                        text           ,
  street                          text           NOT NULL, 
  housenumber                     text           NOT NULL, 
  housenumbersorted               text,                         -- additional column
  housename                       text,
  gpspoint                        geometry,
  quality                         text,                         -- enumeration: 'digitized', 'donated', 'gps', 'linear interpolation'
  reason                          text,                         -- additional column: free text, why doesn't exists (client side: combi fix strings plus free input)
  nextcheckdate                   date,                         -- additional column: expected time in future for next survey check
  created_by                      text,                         -- up to now only the account, probable osm-account: perhaps system prefix like OSM: KPM: etc.
  CONSTRAINT pk_notexisting_housenumbers     PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
-- Migration statement for table notexisting_housenumbers
-- alter table notexisting_housenumbers add column  land_id bigint;
-- alter table notexisting_housenumbers add column  stadt_id bigint;
-- alter table notexisting_housenumbers add column  strasse_id bigint;
-- update notexisting_housenumbers set land_id = subquery.l_id, stadt_id = subquery.s_id, strasse_id = subquery.str_id  from (select neh.id as neh_id, l.id as l_id, s.id as s_id, str.id as str_id, country,region,city,neh.postcode,street,housenumber,housenumbersorted,housename,created_by from notexisting_housenumbers as neh, stadt_hausnummern as sh, stadt as s, land as l, strasse as str  where sh.stadt_id = s.id and s.land_id = l.id and sh.strasse_id = str.id and country = land and city = stadt and street = strasse order by country,city,street,housenumber) as subquery where id = subquery.neh_id;
-- alter table notexisting_housenumbers alter column  stadt_id set not null;ALTER TABLE
-- alter table notexisting_housenumbers alter column  land_id set not null;
-- alter table notexisting_housenumbers alter column  strasse_id set not null;


-- Table: evaluations

DROP TABLE evaluations;

CREATE TABLE evaluations
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  job_id                          bigint         NOT NULL, 
                                                                -- perhaps take an id for all evaluations at a day, like in listofstreets evaluation_overview_id int,
  number_target                   bigint NOT NULL,              -- Anzahl in Soll-Liste
  number_identical                bigint NOT NULL,              -- Identische, also in OSM vorhanden
  number_osmonly                  bigint NOT NULL,              -- Nur in OSM, nicht in Soll-Liste vorhanden
  tstamp                          timestamp without time zone NOT NULL,      -- timestamp of evaluation
  osmdb_tstamp                    timestamp without time zone,  -- timestamp of OSM DB at evaluation start time
  CONSTRAINT pk_evaluation PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


  -- Table: auswertung_hausnummern   - alle Hausnummern (ob singles oder nicht), für Anzeige auf website optimiert

DROP TABLE auswertung_hausnummern;

CREATE TABLE auswertung_hausnummern
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
copyland                        text, 
copystadt                       text, 
copystrasse                     text, 
  strasse_id                      bigint         NOT NULL, 
copyjobname                     text, 
  job_id                          bigint         NOT NULL, 
  postcode                        text,								-- new 2015-04-15
  hausnummer_sortierbar           text           NOT NULL, 
  treffertyp                      text           NOT NULL,
  hausnummer                      text           NOT NULL, 
  objektart                       text,                         -- holds the osm_tag in form "key"=>"value" including " !!! - 
  osm_objektart                   text, 
  osm_id                          bigint,
  point                           geometry,                     -- neu 18.11.2013. Koordinate der Adresse, Quelle siehe Spalte pointsource 
  pointsource                     text,                         -- neu 18.11.2013. Angabe, woher dei Koordinate der Adresse in Spalte point stammt (OSM, OSM Approximation, Google, etc.)
  hausnummer_bemerkung            text,                         -- neu 20.08.2014. Übernahme gleichnamige Tabellenspalte von stadt_hausnummern.
  street_osmids                   text,
  street_point                    geometry
                                                               -- deleted 2015-04-15  CONSTRAINT pk_auswertung_hausnummern_uniquefields PRIMARY KEY (land_id, stadt_id, strasse_id, job_id, treffertyp, hausnummer_sortierbar)
)
WITH (OIDS=FALSE);
CREATE INDEX idx_auswertung_hausnummern_jobid ON auswertung_hausnummern USING btree(job_id);
CREATE INDEX auswertunghausnummern_stadtid_idx ON auswertung_hausnummern USING btree (stadt_id);
CREATE INDEX auswertunghausnummern_strasseid_idx ON auswertung_hausnummern USING btree (strasse_id);


-- Table: exporthnr2shape   - Exporttabelle Auswertungsergebnis Hausnummernbezogen für nachfolgende Shapefile-Erstellung

DROP TABLE exporthnr2shape;

CREATE TABLE exporthnr2shape
(
  id                              bigserial      NOT NULL, 
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  job_id                          bigint         NOT NULL, 
  untersteebene                   text,                        -- new field at 04.09.2013 to check, if this is useful to get only the rows from lowest evaluation job level
  strasse                         text,
  hnr_soll                        bigint, 
  hnr_osm                         bigint, 
  hnr_fhlosm                      bigint, 
  hnr_nurosm                      bigint, 
  hnr_abdeck                      bigint, 
  hnr_liste                       text, 
  timestamp                       timestamp without time zone,
  geom                            geometry
)
WITH (OIDS=FALSE);
CREATE INDEX exporthnr2shape_geom_idx ON exporthnr2shape USING gist (geom);


-- Table: exportjobs2shape   - Exporttabelle Auswertungsergebnis Stadtteilbezogen für nachfolgende Shapefile-Erstellung

DROP TABLE exportjobs2shape;

CREATE TABLE exportjobs2shape
(
  id                              bigserial      NOT NULL, 
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  job_id                          bigint         NOT NULL, 
  stadtbezrk                      text,
  hnr_soll                        bigint, 
  hnr_osm                         bigint, 
  hnr_fhlosm                      bigint, 
  hnr_nurosm                      bigint, 
  hnr_abdeck                      bigint,
  timestamp                       timestamp without time zone,
  polygon                         geometry
)
WITH (OIDS=FALSE);
CREATE INDEX exportjobs2shape_geom_idx ON exportjobs2shape USING gist (polygon);


-- Table: jobqueue   - which next evaluations should run on timely regular base or by instant request

DROP TABLE jobqueue;

CREATE TABLE jobqueue
(
  id                              bigserial      NOT NULL,
  title                           text,
  countrycode                     text,
  adminhierarchy                  text,
  municipality                    text,
  jobname                         text,
  job_id                          bigint,
  requestreason                   text, 
  requesttime                     timestamp without time zone,
  scheduletime                    timestamp without time zone,
  finishedtime                    timestamp without time zone,
  state                           text DEFAULT 'open',
  priority                        int
)
WITH (OIDS=FALSE);


-- Table: theoevaluations

DROP TABLE theoevaluations;

CREATE TABLE theoevaluations
(
  id                                        bigserial      NOT NULL,
  land                                      text           NOT NULL, 
  stadt                                     text           NOT NULL, 
  gemeinde_id                               text, 
  flaechekm2                                real, 
  bevoelkerungszahl                         integer, 
  gliederungstadtland                       smallint, 
  anzahl_osmadressen                        bigint         NOT NULL, 
  anzahl_osmadressennodes                   bigint         NOT NULL, 
  anzahl_osmadressenways                    bigint         NOT NULL, 
  anzahl_osmadressenrels                    bigint         NOT NULL,
  anzahl_nodes_addrstreet_treffer           bigint,
  anzahl_ways_addrstreet_treffer            bigint,
  anzahl_polygons_addrstreet_treffer        bigint,
  anzahl_nodes_associatedstreet_treffer     bigint,
  anzahl_ways_associatedstreet_treffer      bigint,
  anzahl_polygons_associatedstreet_treffer  bigint,
  anzahl_osmadressenplaces                  bigint,
  anzahl_osmadressennodesunvollstaendig     bigint         NOT NULL, 
  anzahl_osmadressenwaysunvollstaendig      bigint         NOT NULL, 
  anzahl_osmadressenrelsunvollstaendig      bigint         NOT NULL, 
  tstamp                                    timestamp without time zone NOT NULL,   -- timestamp of evaluation
  osmdb_tstamp                              timestamp without time zone,            -- timestamp of OSM DB at evaluation start time
  polygon                                   geometry,
  CONSTRAINT pk_theoevaluation PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
CREATE INDEX idx_theoevaluations_gemeindeid ON theoevaluations USING btree(gemeinde_id);


CREATE OR REPLACE VIEW theoeval201407
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2014-08-10' and tstamp < '2014-08-29';

CREATE OR REPLACE VIEW theoeval201409
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2014-10-01' and tstamp < '2014-10-30';


CREATE OR REPLACE VIEW theoeval201410
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2014-11-01' and tstamp < '2014-11-30';


CREATE OR REPLACE VIEW theoeval201411
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2014-12-01' and tstamp < '2014-12-31';

  
CREATE OR REPLACE VIEW theoeval201412
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-01-01' and tstamp < '2015-01-31';


CREATE OR REPLACE VIEW theoeval201502
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-03-01' and tstamp < '2015-03-31';


CREATE OR REPLACE VIEW theoeval201503
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-04-01' and tstamp < '2015-04-30';


CREATE OR REPLACE VIEW theoeval201504
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-05-01' and tstamp < '2015-05-31';

  
CREATE OR REPLACE VIEW theoeval201505
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-06-01' and tstamp < '2015-06-30';


CREATE OR REPLACE VIEW theoeval201506
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-07-01' and tstamp < '2015-07-31';

  
CREATE OR REPLACE VIEW theoeval201507
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-08-01' and tstamp < '2015-08-30';


CREATE OR REPLACE VIEW theoeval201508
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-09-01' and tstamp < '2015-09-30';

  
CREATE OR REPLACE VIEW theoeval201509
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-10-01' and tstamp < '2015-10-31';


CREATE OR REPLACE VIEW theoeval201510
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-11-01' and tstamp < '2015-11-30';


CREATE OR REPLACE VIEW theoeval201511
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2015-12-01' and tstamp < '2015-12-31';

  
CREATE OR REPLACE VIEW theoeval201512
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-01-01' and tstamp < '2016-01-31';

  
CREATE OR REPLACE VIEW theoeval201601
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-02-01' and tstamp < '2016-02-28';


CREATE OR REPLACE VIEW theoeval201602
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-03-01' and tstamp < '2016-03-31';

CREATE OR REPLACE VIEW theoeval201603
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-04-01' and tstamp < '2016-04-05';


CREATE OR REPLACE VIEW theoeval201604
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-05-01' and tstamp < '2016-05-03';
  

 CREATE OR REPLACE VIEW theoeval201605
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-06-01' and tstamp < '2016-06-03';

 CREATE OR REPLACE VIEW theoeval201606
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-07-01' and tstamp < '2016-07-03';

CREATE OR REPLACE VIEW theoeval201607
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-08-01' and tstamp < '2016-08-03';

CREATE OR REPLACE VIEW theoeval201608
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-09-01' and tstamp < '2016-09-05';


CREATE OR REPLACE VIEW theoeval201609
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-10-01' and tstamp < '2016-10-05';

CREATE OR REPLACE VIEW theoeval201610
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-11-01' and tstamp < '2016-11-05';

CREATE OR REPLACE VIEW theoeval201611
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2016-12-01' and tstamp < '2016-12-06';

CREATE OR REPLACE VIEW theoeval201612
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-01-01' and tstamp < '2017-01-10';

CREATE OR REPLACE VIEW theoeval201701
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-02-01' and tstamp < '2017-02-05';

CREATE OR REPLACE VIEW theoeval201702
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-03-01' and tstamp < '2017-03-08';

CREATE OR REPLACE VIEW theoeval201703
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-04-01' and tstamp < '2017-04-05';

CREATE OR REPLACE VIEW theoeval201704
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-05-01' and tstamp < '2017-05-05';

CREATE OR REPLACE VIEW theoeval201705
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-06-01' and tstamp < '2017-06-05';

CREATE OR REPLACE VIEW theoeval201706
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-07-01' and tstamp < '2017-07-05';

CREATE OR REPLACE VIEW theoeval201707
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-08-01' and tstamp < '2017-08-05';

CREATE OR REPLACE VIEW theoeval201708
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-09-01' and tstamp < '2017-09-05';

CREATE OR REPLACE VIEW theoeval201709
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-10-01' and tstamp < '2017-10-05';

CREATE OR REPLACE VIEW theoeval201710
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-11-01' and tstamp < '2017-11-06';

CREATE OR REPLACE VIEW theoeval201711
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2017-12-01' and tstamp < '2017-12-06';

CREATE MATERIALIZED VIEW theoeval201712
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-01-01' and tstamp < '2018-01-06';

CREATE MATERIALIZED VIEW theoeval201801
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-02-01' and tstamp < '2018-02-06';

CREATE MATERIALIZED VIEW theoeval201802
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-03-01' and tstamp < '2018-03-03';

CREATE MATERIALIZED VIEW theoeval201803
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-04-01' and tstamp < '2018-04-04';

CREATE MATERIALIZED VIEW theoeval201804
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-05-01' and tstamp < '2018-05-04';

CREATE MATERIALIZED VIEW theoeval201805
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-06-01' and tstamp < '2018-06-13';

CREATE MATERIALIZED VIEW theoeval201806
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-07-01' and tstamp < '2018-07-06';

CREATE MATERIALIZED VIEW theoeval201807
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-08-01' and tstamp < '2018-08-06';

CREATE MATERIALIZED VIEW theoeval201808
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-09-01' and tstamp < '2018-09-06';

CREATE MATERIALIZED VIEW theoeval201809
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-10-01' and tstamp < '2018-10-06';

CREATE MATERIALIZED VIEW theoeval201810
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-11-01' and tstamp < '2018-11-06';

CREATE MATERIALIZED VIEW theoeval201811
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2018-12-01' and tstamp < '2018-12-06';

CREATE MATERIALIZED VIEW theoeval201812
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-01-01' and tstamp < '2019-01-13';

CREATE MATERIALIZED VIEW theoeval201901
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-02-01' and tstamp < '2019-02-04';

CREATE MATERIALIZED VIEW theoeval201902
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-03-01' and tstamp < '2019-03-06';

CREATE MATERIALIZED VIEW theoeval201903
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-04-01' and tstamp < '2019-04-06';

CREATE MATERIALIZED VIEW theoeval201904
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-05-01' and tstamp < '2019-05-15';

CREATE MATERIALIZED VIEW theoeval201905
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-06-01' and tstamp < '2019-06-07';

CREATE MATERIALIZED VIEW theoeval201906
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-07-01' and tstamp < '2019-07-05';

CREATE MATERIALIZED VIEW theoeval201907
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-08-01' and tstamp < '2019-08-12';

CREATE MATERIALIZED VIEW theoeval201908
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-09-01' and tstamp < '2019-09-03';

CREATE MATERIALIZED VIEW theoeval201909
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-10-01' and tstamp < '2019-10-03';

CREATE MATERIALIZED VIEW theoeval201910
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-11-01' and tstamp < '2019-11-03';

CREATE MATERIALIZED VIEW theoeval201911
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2019-12-01' and tstamp < '2019-12-03';

CREATE MATERIALIZED VIEW theoeval201912
  AS SELECT 
  id, land, stadt, gemeinde_id, flaechekm2, bevoelkerungszahl, gliederungstadtland, anzahl_osmadressen, 
  anzahl_osmadressennodes, anzahl_osmadressenways, anzahl_osmadressenrels, 
  (anzahl_osmadressennodesunvollstaendig + anzahl_osmadressenwaysunvollstaendig + anzahl_osmadressenrelsunvollstaendig) AS anzahl_osmadressenunvollstaendig, 
  anzahl_osmadressennodesunvollstaendig, anzahl_osmadressenwaysunvollstaendig, anzahl_osmadressenrelsunvollstaendig, 
  tstamp, osmdb_tstamp, 
   anzahl_nodes_addrstreet_treffer, anzahl_ways_addrstreet_treffer, anzahl_polygons_addrstreet_treffer,
   (anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer) AS anzahl_osmadressen_addrstreet,
  anzahl_nodes_associatedstreet_treffer, anzahl_ways_associatedstreet_treffer, anzahl_polygons_associatedstreet_treffer,
  (anzahl_nodes_associatedstreet_treffer + anzahl_ways_associatedstreet_treffer + anzahl_polygons_associatedstreet_treffer) AS anzahl_osmadressen_associatedstreet,
  anzahl_osmadressenplaces, polygon, 
  CASE WHEN anzahl_osmadressen = 0 THEN 0
      ELSE 100.0*(anzahl_nodes_addrstreet_treffer + anzahl_ways_addrstreet_treffer + anzahl_polygons_addrstreet_treffer)/anzahl_osmadressen
  END  AS anzahl_osmadressen_addrstreetanteil, 
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN round(bevoelkerungszahl * 1.052178115
 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN round(bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_anzahl_adressen,
  CASE   WHEN flaechekm2 > 0 AND bevoelkerungszahl > 0 AND bevoelkerungszahl <= 5000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000920645 * bevoelkerungszahl/flaechekm2 + 3.002091115))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 5000 AND bevoelkerungszahl <= 20000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000509945 * bevoelkerungszahl/flaechekm2 + 3.086196323))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 20000 AND bevoelkerungszahl <= 100000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000865691 * bevoelkerungszahl/flaechekm2 + 3.490431037))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 100000 AND bevoelkerungszahl <= 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.000728235 * bevoelkerungszahl/flaechekm2 + 3.394332878))
       WHEN flaechekm2 > 0 AND bevoelkerungszahl > 250000 THEN 100 * anzahl_osmadressen / (bevoelkerungszahl * 1.052178115 / (0.001400991 * bevoelkerungszahl/flaechekm2 + 3.46995551))
         ELSE 0
  END  AS theo_adressenabdeckung
  FROM theoevaluations where tstamp > '2020-01-01' and tstamp < '2020-01-03';

  // TODOMONTHLY


CREATE OR REPLACE VIEW theoeval201410diff201409
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201410 AS te2, theoeval201409 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  

CREATE OR REPLACE VIEW theoeval201411diff201410
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201411 AS te2, theoeval201410 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201412diff201411
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201412 AS te2, theoeval201411 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  
  
CREATE OR REPLACE VIEW theoeval201502diff201412
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201502 AS te2, theoeval201412 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  
  
CREATE OR REPLACE VIEW theoeval201503diff201502
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201503 AS te2, theoeval201502 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  

  
CREATE OR REPLACE VIEW theoeval201504diff201503
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201504 AS te2, theoeval201503 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201505diff201504
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201505 AS te2, theoeval201504 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201506diff201505
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201506 AS te2, theoeval201505 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

  
CREATE OR REPLACE VIEW theoeval201507diff201506
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201507 AS te2, theoeval201506 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201508diff201507
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201508 AS te2, theoeval201507 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201509diff201508
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201509 AS te2, theoeval201508 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201510diff201509
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201510 AS te2, theoeval201509 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201511diff201510
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201511 AS te2, theoeval201510 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201512diff201511
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201512 AS te2, theoeval201511 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

  
CREATE OR REPLACE VIEW theoeval201601diff201512
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201601 AS te2, theoeval201512 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201602diff201601
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201602 AS te2, theoeval201601 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';


CREATE OR REPLACE VIEW theoeval201603diff201602
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201603 AS te2, theoeval201602 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  

CREATE OR REPLACE VIEW theoeval201604diff201603
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201604 AS te2, theoeval201603 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201605diff201604
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201605 AS te2, theoeval201604 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201606diff201605
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201606 AS te2, theoeval201605 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201607diff201606
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201607 AS te2, theoeval201606 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201608diff201607
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201608 AS te2, theoeval201607 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201609diff201608
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201609 AS te2, theoeval201608 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  
CREATE OR REPLACE VIEW theoeval201610diff201609
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201610 AS te2, theoeval201609 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  
CREATE OR REPLACE VIEW theoeval201611diff201610
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201611 AS te2, theoeval201610 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201612diff201611
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201612 AS te2, theoeval201611 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201701diff201612
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201701 AS te2, theoeval201612 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201702diff201701
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201702 AS te2, theoeval201701 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201703diff201702
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201703 AS te2, theoeval201702 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201704diff201703
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201704 AS te2, theoeval201703 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201705diff201704
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201705 AS te2, theoeval201704 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201706diff201705
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201706 AS te2, theoeval201705 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201707diff201706
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201707 AS te2, theoeval201706 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201708diff201707
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201708 AS te2, theoeval201707 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201709diff201708
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201709 AS te2, theoeval201708 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201710diff201709
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201710 AS te2, theoeval201709 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE OR REPLACE VIEW theoeval201711diff201710
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201711 AS te2, theoeval201710 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201712diff201711
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201712 AS te2, theoeval201711 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201801diff201712
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201801 AS te2, theoeval201712 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201802diff201801
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201802 AS te2, theoeval201801 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201803diff201802
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201803 AS te2, theoeval201802 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201804diff201803
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201804 AS te2, theoeval201803 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201805diff201804
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201805 AS te2, theoeval201804 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201806diff201805
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201806 AS te2, theoeval201805 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201807diff201806
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201807 AS te2, theoeval201806 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201808diff201807
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201808 AS te2, theoeval201807 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201809diff201808
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201809 AS te2, theoeval201808 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201810diff201809
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201810 AS te2, theoeval201809 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201811diff201810
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201811 AS te2, theoeval201810 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201812diff201811
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201812 AS te2, theoeval201811 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201901diff201812
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201901 AS te2, theoeval201812 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201902diff201901
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201902 AS te2, theoeval201901 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201903diff201902
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201903 AS te2, theoeval201902 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201904diff201903
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201904 AS te2, theoeval201903 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201905diff201904
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201905 AS te2, theoeval201904 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201906diff201905
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201906 AS te2, theoeval201905 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201907diff201906
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201907 AS te2, theoeval201906 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201908diff201907
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201908 AS te2, theoeval201907 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201909diff201908
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201909 AS te2, theoeval201908 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201910diff201909
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201910 AS te2, theoeval201909 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201911diff201910
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201911 AS te2, theoeval201910 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval201912diff201911
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201912 AS te2, theoeval201911 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

  // TODOMONTHLY



 // jährliche Vergleiche
CREATE OR REPLACE VIEW theoeval201612diff201601
  AS SELECT 
  te2.land AS land, te2.stadt AS stadt,
  te2.gemeinde_id AS gemeinde_id, te2.flaechekm2 AS flaechekm2, te2.bevoelkerungszahl AS bevoelkerungszahl, te2.gliederungstadtland AS gliederungstadtland,
  (te2.anzahl_osmadressen - te1.anzahl_osmadressen) AS diff_anzahl_osmadressen,
  te2.anzahl_osmadressen AS neue_anzahl_osmadressen,
  te1.anzahl_osmadressen AS alte_anzahl_osmadressen,
  te2.theo_anzahl_adressen AS neue_theo_anzahl_adressen,
  te1.theo_anzahl_adressen AS alte_theo_anzahl_adressen,
  round(CAST(te2.theo_adressenabdeckung AS NUMERIC),1) AS neue_theo_adressenabdeckung,
  round(CAST(te1.theo_adressenabdeckung AS NUMERIC),1) AS alte_theo_adressenabdeckung,
  to_char(te2.tstamp, 'DD.MM.YYYY HH24:MI') AS neue_tstamp,
  to_char(te1.tstamp, 'DD.MM.YYYY HH24:MI') AS alte_tstamp,
  te2.polygon as polygon
  FROM theoeval201612 AS te2, theoeval201601 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

 
CREATE OR REPLACE VIEW viewexportjobs2shape
  AS SELECT
  land AS country, stadt AS municipality, officialkeys_id, jobname, exportjobs2shape.id AS id, 
  land.id AS land_id, stadt.id AS stadt_id, job_id, stadtbezrk, hnr_soll, hnr_osm,
  hnr_fhlosm, hnr_abdeck, evallevel, polygon, countrycode
  FROM exportjobs2shape
  JOIN stadt ON exportjobs2shape.stadt_id = stadt.id
  JOIN land ON exportjobs2shape.land_id = land.id
  JOIN jobs ON exportjobs2shape.job_id = jobs.id;


CREATE OR REPLACE VIEW viewexporthnr2shape
  AS SELECT
  land AS country, stadt AS municipality, officialkeys_id, jobname, exporthnr2shape.id AS id, 
  land.id AS land_id, stadt.id AS stadt_id, job_id, untersteebene, strasse, hnr_soll, hnr_osm,
  hnr_fhlosm, hnr_abdeck, hnr_liste, geom
  FROM exporthnr2shape
  JOIN stadt ON exporthnr2shape.stadt_id = stadt.id
  JOIN land ON exporthnr2shape.land_id = land.id
  JOIN jobs ON exporthnr2shape.job_id = jobs.id;


--origin from http://postgresql.1045698.n5.nabble.com/german-sort-is-wrong-td5582836.html
CREATE OR REPLACE FUNCTION correctorder(text)
  RETURNS text AS
  $BODY$ SELECT REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(lower($1),'ß','ss'),'ä','ae'),'ö','oe'),'ü','ue'),'â','a'),'Ä','ae'),'Ö','oe'),'Ü','ue') $BODY$
  LANGUAGE sql VOLATILE
  COST 100;
ALTER FUNCTION correctorder(text) OWNER TO postgres; 
-- if necessary, build an index with
-- CREATE INDEX correctorder_idx ON street (correctorder(col1));


CREATE OR REPLACE FUNCTION lonlatdistance(lon1 double precision, lat1 double precision, lon2 double precision, lat2 double precision)
  RETURNS double precision AS $$
  DECLARE
    R numeric := 6371000;
    PI double precision := 3.1415926535897932384626433;
    deltagamma double precision := 0.0;
    sigma1 double precision := 0.0;
    sigma2 double precision := 0.0;
    x double precision := 0.0;
    y double precision := 0.0;
    d double precision := 0.0;
  BEGIN
    deltagamma := (lon2 - lon1) * PI / 180.0;
    sigma1 := lat1 * PI / 180.0;
    sigma2 := lat2 * PI / 180.0;

    x := deltagamma * cos((sigma1 + sigma2)/2);
    y := sigma2 - sigma1;
    d := sqrt(x*x + y*y) * R;
    return d;
  END;
  $$  LANGUAGE plpgsql;
ALTER FUNCTION lonlatdistance(double precision, double precision, double precision, double precision) OWNER TO postgres; 
