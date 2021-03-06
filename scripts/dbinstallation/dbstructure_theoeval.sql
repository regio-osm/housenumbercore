--
-- V2.0, 2018-04-28, Dietmar Seifert
--     This script 
--     only creates optionally Tables and Views for
--     theoretical housenumber evaluation (only germany as of 2018-04).
--
--    The housenumber evaluation DB must already be created.
--    The base DB strucutre must already be created, see dbstructure.sql
--    The mandatory project specific DB functions must already be created, see dbfunctions.sql



-- ============================================================================================
--  Tables  for theoretical housenumber evaluation
-- ============================================================================================


-- Table: theoevaluations

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


-- ============================================================================================
--  Views  for theoretical housenumber evaluation
-- ============================================================================================

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
  FROM theoevaluations where tstamp > '2019-05-01' and tstamp < '2019-05-16';

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
  FROM theoevaluations where tstamp > '2019-06-01' and tstamp < '2019-06-06';

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
  FROM theoevaluations where tstamp > '2019-11-01' and tstamp < '2019-11-06';

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
  FROM theoevaluations where tstamp > '2019-12-01' and tstamp < '2019-12-06';

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

CREATE MATERIALIZED VIEW theoeval202001
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
  FROM theoevaluations where tstamp > '2020-02-01' and tstamp < '2020-02-15';

CREATE MATERIALIZED VIEW theoeval202002
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
  FROM theoevaluations where tstamp > '2020-03-01' and tstamp < '2020-03-15';

CREATE MATERIALIZED VIEW theoeval202003
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
  FROM theoevaluations where tstamp > '2020-04-01' and tstamp < '2020-04-15';

CREATE MATERIALIZED VIEW theoeval202004
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
  FROM theoevaluations where tstamp > '2020-05-01' and tstamp < '2020-05-15';

CREATE MATERIALIZED VIEW theoeval202005
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
  FROM theoevaluations where tstamp > '2020-06-01' and tstamp < '2020-06-15';

CREATE MATERIALIZED VIEW theoeval202006
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
  FROM theoevaluations where tstamp > '2020-07-01' and tstamp < '2020-07-05';

CREATE MATERIALIZED VIEW theoeval202007
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
  FROM theoevaluations where tstamp > '2020-08-01' and tstamp < '2020-08-05';

-- TODOMONTHLY


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

CREATE MATERIALIZED VIEW theoeval202001diff201912
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
  FROM theoeval202001 AS te2, theoeval201912 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval202002diff202001
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
  FROM theoeval202002 AS te2, theoeval202001 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval202003diff202002
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
  FROM theoeval202003 AS te2, theoeval202002 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval202004diff202003
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
  FROM theoeval202004 AS te2, theoeval202003 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval202005diff202004
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
  FROM theoeval202005 AS te2, theoeval202004 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

CREATE MATERIALIZED VIEW theoeval202006diff202005
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
  FROM theoeval202006 AS te2, theoeval202005 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';
  
CREATE MATERIALIZED VIEW theoeval202007diff202006
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
  FROM theoeval202007 AS te2, theoeval202006 AS te1
  WHERE te2.gemeinde_id = te1.gemeinde_id
  AND te2.land = te1.land
  AND te2.land = 'Bundesrepublik Deutschland';

  -- TODOMONTHLY



 -- Comparation yearly
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
