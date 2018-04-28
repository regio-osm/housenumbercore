--
-- V2.0, 2018-04-28, Dietmar Seifert
--     This script creates Tables and View for base funcionality of osm housenumber evaluation DB.
--
-- V1.3, 2016-02-30, Dietmar Seifert
--            Table names still in german, sorry. Has to be changed in near future
--            Some table columns added in last days to enhance functionality on web frontend
--
--

--    The housenumber evaluation DB must be already created.

--    Mandatory project specific DB functions will be defined in the other file dbfunctions.sql

--    Optionally tables and view creations for theoretical housenumber evaluation (only germany as of 2018-04) see dbstructure_theoeval.sql


-- ============================================================================================
--  Tables and Views for storing of official lists and their evaluation against OSM data
-- ============================================================================================

-- Table: land (country)

CREATE TABLE land
(
  id                       bigserial      NOT NULL,
  land                     text           NOT NULL,
  countrycode              text,			-- two-letter ISO-3166 Code, see https://en.wikipedia.org/wiki/ISO_3166-1_alpha-2
  gemeindeschluessel_key   text, 		-- OSM key, which unique identifies the municipality within the country. In Germany for example 'de:amtlicher_gemeindeschluessel'
  boundary_polygon			geometry,	-- optionally: add a polygon, if no gemeindeschluessel_key available, to limit hits within a country polygon border
  CONSTRAINT pk_land PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: stadt (municipality)

CREATE TABLE stadt
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL,
  stadt                           text           NOT NULL,
  officialkeys_id                 text           NOT NULL,   -- unique value for key, as defined in table land, column gemeinedeschluessel_key
  osm_hierarchy                   text,             -- country,state,x,y - wil be set by osm admin hierarchy
  active_adminlevels              integer[],        -- list of admin_level values, which are active main municipality area and active subareas
  housenumberaddition_exactly     text DEFAULT 'y', -- 'y'(yes) or 'n'(no)
  subareasidentifyable            text,             -- 'y'(yes) or 'n'(no)
  officialgeocoordinates			 text DEFAULT 'n', -- 'y'(yes) or 'n'(no). New at 2015-02-09
  sourcelist_url                  text,             -- internet URL for info about the official housenumber file
  sourcelist_copyrighttext        text,             -- copyright text from list delivery office
  sourcelist_useagetext           text,             -- usage info on website, told from delivery office
  sourcelist_contentdate          date,             -- date of the housenumber list, if told from delivery administration office
  sourcelist_filedate             date,             -- technical file date
  parameters                      hstore,           -- new 2016-02-18 add municipality specific parameters for evaluations as key-value pairs  
                                                       -- 'listcoordosmuploadable=>yes'  official geocoordinates with license compatabile to osm odbl - 2016-02-18
                                                       -- 'listcoordosmuploadlimitcount=>n'  upload to osm limited to n housenumbers - 2016-02-18
                                                       -- 'listcoordforevaluation=>yes'  can officialcoordinates be used for evaluations (don't need to be ODbL-compatible)
  CONSTRAINT pk_stadt PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: gebiete  (subarea)

CREATE TABLE gebiete
(
  id                              bigserial      NOT NULL,
  name                            text           NOT NULL,
  land_id                         bigint         NOT NULL,
  stadt_id                        bigint         NOT NULL,
  admin_level                     integer,                 -- osm relation, value of key "admin_level"
  sub_id                          text,                    -- official name or id from subarea, as in housenumber list from delivery office and in OSM
  osm_id                          bigint         NOT NULL, -- osm relation id from subarea
  polygon                         geometry,
  checkedtime                     timestamp without time zone,   -- timestamp of record creation or last check
  CONSTRAINT pk_gebiete_uniquefields PRIMARY KEY (land_id, stadt_id, admin_level, name)
)
WITH (OIDS=FALSE);


-- Table: jobs

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


-- Table: strasse (street)

CREATE TABLE strasse
(
  id                              bigserial      NOT NULL,
  strasse                         text           NOT NULL,
  CONSTRAINT pk_strasse PRIMARY KEY (id)
)
WITH (OIDS=FALSE);


-- Table: jobs_strassen (jobs_streets)

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


-- Table: jobs_strassen_blacklist (jobs_streets_blacklist)

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


-- Table: stadt_hausnummern (official_housenumbers)

CREATE TABLE stadt_hausnummern
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  strasse_id                      bigint         NOT NULL, 
  postcode                        text,
  hausnummer                      text           NOT NULL, 
  hausnummer_sortierbar           text           NOT NULL,
  hausnummer_bemerkung            text,
  extraosmtags                    hstore,                -- additional information about housenumber, as OSM key=value pairs (new in 2018-03)
  sub_id                          text, 
  point                           geometry,                     -- Geocoordinate in coordsystem 4326 of the address. Source see in column pointsource. new at 2014-06-17  
  pointsource                     text,                         -- Description, where the geocoordinate comes from (normally from the official source like the housenumber list. new at 2014-06-17
  CONSTRAINT pk_stadt_hausnummern     PRIMARY KEY (id)
)
WITH (OIDS=FALSE);
CREATE INDEX stadthausnummern_stadtid_idx ON stadt_hausnummern USING btree (stadt_id);
CREATE INDEX stadthausnummern_strasseid_idx ON stadt_hausnummern USING btree (strasse_id);


-- Table: osm_hausnummern (osm_housenumbers)

CREATE TABLE osm_hausnummern
(
  id                              bigserial      NOT NULL,
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  strasse_id                      bigint         NOT NULL, 
  job_id                          bigint         NOT NULL, 
  postcode                        text,
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


-- Table: notexisting_housenumbers   - official housenumbers, which are not existing, according to osm users

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


-- Table: evaluations

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


-- Table: auswertung_hausnummern (evaluation_housenumbers)  - all evaluated housenumbers, (if official, identical or osm-singles, optimized for web frontend

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
  postcode                        text,
  hausnummer_sortierbar           text           NOT NULL, 
  treffertyp                      text           NOT NULL,
  hausnummer                      text           NOT NULL, 
  objektart                       text,                         -- holds the osm_tag in form "key"=>"value" including " !!! - 
  osm_objektart                   text, 
  osm_id                          bigint,
  point                           geometry,                -- address coordinate, source see column pointsource
  pointsource                     text,                    -- source of address coordinate in column pointsource,  (OSM, OSM Approximation, Google, etc.)
  hausnummer_bemerkung            text,                    -- identical content from same column in table stadt_hausnummern
  street_osmids                   text,
  street_point                    geometry
)
WITH (OIDS=FALSE);
CREATE INDEX idx_auswertung_hausnummern_jobid ON auswertung_hausnummern USING btree(job_id);
CREATE INDEX auswertunghausnummern_stadtid_idx ON auswertung_hausnummern USING btree (stadt_id);
CREATE INDEX auswertunghausnummern_strasseid_idx ON auswertung_hausnummern USING btree (strasse_id);


-- Table: jobqueue   - which next evaluations should run on timely regular base or by instant request

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



-- ============================================================================================
--  Tables and Views for result representation
-- ============================================================================================



-- Table: exportjobs2shape   - evaluation result on municipality level, for use as WMS layer, lower zoom levels

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
  evallevel                       integer,
  polygon                         geometry
)
WITH (OIDS=FALSE);
CREATE INDEX exportjobs2shape_geom_idx ON exportjobs2shape USING gist (polygon);
CREATE INDEX exportjobs2shape_landid_idx ON exportjobs2shape USING btree (land_id);
CREATE INDEX exportjobs2shape_stadtid_idx ON exportjobs2shape USING btree (stadt_id);
CREATE INDEX exportjobs2shape_jobid_idx ON exportjobs2shape USING btree (job_id);


-- Table: exporthnr2shape   - evaluation result on street level, for use as WMS layer, zoomed deep in

CREATE TABLE exporthnr2shape
(
  id                              bigserial      NOT NULL, 
  land_id                         bigint         NOT NULL, 
  stadt_id                        bigint         NOT NULL, 
  job_id                          bigint         NOT NULL, 
  untersteebene                   text,                     -- to check, if this is useful to get only the rows from lowest evaluation job level
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
CREATE INDEX exporthnr2shape_landid_idx ON exporthnr2shape USING btree (land_id);
CREATE INDEX exporthnr2shape_stadtid_idx ON exporthnr2shape USING btree (stadt_id);
CREATE INDEX exporthnr2shape_jobid_idx ON exporthnr2shape USING btree (job_id);



-- View: viewexportjobs2shape   - collect evaluation result on municipality level with more details for web frontend and WMS layer

CREATE OR REPLACE VIEW viewexportjobs2shape
  AS SELECT
  land AS country, stadt AS municipality, officialkeys_id, jobname, exportjobs2shape.id AS id, 
  land.id AS land_id, stadt.id AS stadt_id, job_id, stadtbezrk, hnr_soll, hnr_osm,
  hnr_fhlosm, hnr_abdeck, evallevel, polygon, countrycode
  FROM exportjobs2shape
  JOIN stadt ON exportjobs2shape.stadt_id = stadt.id
  JOIN land ON exportjobs2shape.land_id = land.id
  JOIN jobs ON exportjobs2shape.job_id = jobs.id;

  
-- View: viewexporthnr2shape   - collect evaluation result on street level with more details for web frontend and WMS layer

CREATE OR REPLACE VIEW viewexporthnr2shape
  AS SELECT
  land AS country, stadt AS municipality, officialkeys_id, jobname, exporthnr2shape.id AS id, 
  land.id AS land_id, stadt.id AS stadt_id, job_id, untersteebene, strasse, hnr_soll, hnr_osm,
  hnr_fhlosm, hnr_abdeck, hnr_liste, geom
  FROM exporthnr2shape
  JOIN stadt ON exporthnr2shape.stadt_id = stadt.id
  JOIN land ON exporthnr2shape.land_id = land.id
  JOIN jobs ON exporthnr2shape.job_id = jobs.id;
