#!/bin/bash

# CAUTOIN: file format is ISO-8859-1, not standard format UTF-8
# Info: import file contains postcode and municipality in german HK_DE format


# dynamic variables, please check and set for every download
CONTENTDATE="2020-05-31"  #Date in form yyyy-mm-dd
FILEDOWNLOADDATE="2020-06-21"  #Date in form yyyy-mm-dd
DOWNLOADFILE="http://offenedaten.frankfurt.de/dataset/9c902fd2-dd17-40cc-9fab-52c9c28aea3c/resource/59eae6ec-8788-4347-8760-257f50aaced7/download/zprojekteopen-datadatenamt-62hausskoordinatendatensatze20209.-stand-31.05.2020-erledigtadressen.csv"


DATAROOT="/home/osm/apps/housenumbercore/data"
AREADIRNAME="Deutschland/Hessen/FrankfurtamMain"
TODAYDIRNAME=`date +%Y%m%d`


echo "Datum als Variable ===${TODAYDIRNAME}==="

cd "${DATAROOT}/${AREADIRNAME}"
mkdir "${TODAYDIRNAME}"
cd "${TODAYDIRNAME}/"
wget "${DOWNLOADFILE}" --no-check-certificate -O ./DE_HS_FrankfurtamMain.csv



# process main file

# first, sort file in order: municipality-id (columns 4 up to 7), street, housenumber, housenumberaddition
cat DE_HS_FrankfurtamMain.csv |  sort -k4,4n -k5,5n -k6,6n -k7,7n -k14,14 -k10,10n -k11,11 -t";"  --output=DE_HS_FrankfurtamMain_temp.csv

# remove optionally UTF-8 BOM-Header at file start
# change float-format of x and y geocoordinates from , (comma) to . (dot)
# bring 4 parts of muniid together, rest of fields stay originally (last field, date of actuality, will be removed)
gawk 'BEGIN { 
	FS=";"; 
	OFS="\t"; 
	print "#dummy\tid\tadresstyp\tmuniid\tid2\thausnummer\tx\ty\tstrasse\tplz\tgemeinde";
}
// {
		# remove optionally UTF-8 BOM-Header at file start (ok on every line beginning)
	$1 = gensub(/^\xEF\xBB\xBF(.*)/, "\\1", "g", $1)
		# change float-format of x and y geocoordinates from , (comma) to . (dot)
	$12 = gensub(/([0-9]+),([0-9]*)/, "\\1\.\\2", "g", $12); 
	$13 = gensub(/([0-9]+),([0-9]*)/, "\\1\.\\2", "g", $13);
		# expand abbreviated version of street name
	$14 = gensub(/Str\.$/, "Straße", "g", $14);
	$14 = gensub(/str\.$/, "straße", "g", $14);


		# only export addresses of category A or B. Category C are POIs with artifical addresses, not visible on ground
	if ( $3 == "A" || $3 == "B" ) printf("%s\t%s\t%s\t%s%s%s%s\t%s\t%s%s\t%s\t%s\t%s\t%s\t%s\n",$1,$2,$3,$4,$5,$6,$7,$9,$10,$11,$12,$13,$14,$15,$16)
}' DE_HS_FrankfurtamMain_temp.csv> "DE_HS_FrankfurtamMain_${TODAYDIRNAME}.txt"


cd /home/osm/apps/housenumbercore/src/

java de.regioosm.housenumbercore.imports.CsvListImport \
-coordinatesystem 25832 -country "Bundesrepublik Deutschland" \
-file "${DATAROOT}/${AREADIRNAME}/${TODAYDIRNAME}/DE_HS_FrankfurtamMain_${TODAYDIRNAME}.txt" -filecharset ISO-8859-1 \
-copyright "© Stadtvermessungsamt Frankfurt am Main" \
-useage "Datenlizenz Deutschland - Namensnennung - Version 2.0 (<a href='http://www.govdata.de/dl-de/by-2-0'>www.govdata.de/dl-de/by-2-0</a>)" \
-listurl "https://offenedaten.frankfurt.de/dataset/hauskoordinaten-franfurt" \
-listcontenttimestamp "${CONTENTDATE}" -listfiletimestamp "${FILEDOWNLOADDATE}" \
-useoverpass yes -coordinatesosmimportable no -subareaactive no \
-c 4=municipalityref -c 6=housenumber -c 7=lon -c 8=lat -c 9=street -c 10=postcode -c 11=municipality >"Import_DE_HS_FrankfurtamMain_${TODAYDIRNAME}.log"

psql -d hausnummern -U okilimu -c "update stadt set active_adminlevels = '{6}', parameters = hstore(ARRAY['listcoordforevaluation','yes', 'listcoordosmuploadable','no']) where stadt = 'Frankfurt am Main' and officialkeys_id = '06412000' and land_id = (select id from land where land = 'Bundesrepublik Deutschland')"
