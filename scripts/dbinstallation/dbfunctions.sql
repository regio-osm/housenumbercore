--
-- V2.0, 2018-04-28, Dietmar Seifert
--    This script adds specific DB functions to allow multi-language sorting and DB driven distance calculations
--

--    The housenumber evaluation DB must be already created.
--    The Database tables and views will be defined in the other file dbstructure.sql



-- A primitive function in DB to correct sort for a number of special country characters
-- The list can be extended, but the function must exist, it will be used in various java classes and in web frontend

--origin from http://postgresql.1045698.n5.nabble.com/german-sort-is-wrong-td5582836.html
CREATE OR REPLACE FUNCTION correctorder(text)
  RETURNS text AS
  $BODY$ SELECT REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(lower($1),'ß','ss'),'ä','ae'),'ö','oe'),'ü','ue'),'â','a'),'Ä','ae'),'Ö','oe'),'Ü','ue') $BODY$
  LANGUAGE sql VOLATILE
  COST 100;
ALTER FUNCTION correctorder(text) OWNER TO postgres; 
-- if necessary, build an index with
-- CREATE INDEX correctorder_idx ON street (correctorder(col1));


-- A primitive function in DB to fast calculate SHORT distances.
-- The function must exist, it will be used in some java classes and in web frontend

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
