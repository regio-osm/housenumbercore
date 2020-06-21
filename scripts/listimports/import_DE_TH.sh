#!/bin/bash

# dynamic variables, please check and set for every download
CONTENTDATE="2019-03-25"  #Date in form yyyy-mm-dd
FILEDOWNLOADDATE="2019-03-25"  #Date in form yyyy-mm-dd
DOWNLOADFILE="https://geoportal.geoportal-th.de/hausko_umr/HK-TH.zip"


DATAROOT="/home/osm/apps/housenumbercore/data"
AREADIRNAME="Deutschland/Thüringen/Gesamt"
TODAYDIRNAME=`date +%Y%m%d`


echo "Datum als Variable ===${TODAYDIRNAME}==="

cd "${DATAROOT}/${AREADIRNAME}"
mkdir "${TODAYDIRNAME}"
cd "${TODAYDIRNAME}/"
wget "${DOWNLOADFILE}" -O ./HK-TH.zip
unzip HK-TH.zip


# process file, which references from muniid to municipality_name
#   bring 4 parts of muniid together, then municipality_name
gawk 'BEGIN { 
        FS=";"; 
        OFS="\t"; 
        RS="\r\n";
        print "GKZ\tGEMEINDENAME"; }
/^G/ {
        $1 = gensub(/^\xEF\xBB\xBF(.*)/, "\\1", "g", $1)
        printf("%s%s%s%s\t%s\n",$2,$3,$4,$5,$6)
}' schluessel-th.txt > DE_TH_gebref_schluessel.txt



# process main file

# first, sort file in order: municipality-id (columns 4 up to 7), street, housenumber, housenumberaddition
cat adressen-th.txt |  sort -k4,4n -k5,5n -k6,6n -k7,7n -k14,14 -k10,10n -k11,11 -t";"  --output=adressen-th_temp.txt

# remove optionally UTF-8 BOM-Header at file start
# change float-format of x and y geocoordinates from , (comma) to . (dot)
# bring 4 parts of muniid together, rest of fields stay originally (last field, date of actuality, will be removed)
gawk 'BEGIN { 
	FS=";"; 
	OFS="\t"; 
	print "#dummy\tid\tadresstyp\tmuniid\tid2\thausnummer\tx\ty\tstrasse";
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
	if ( $3 == "A" || $3 == "B" ) printf("%s\t%s\t%s\t%s%s%s%s\t%s\t%s%s\t%s\t%s\t%s\n",$1,$2,$3,$4,$5,$6,$7,$9,$10,$11,$12,$13,$14)
}' adressen-th_temp.txt > "DE_TH_gebref_${TODAYDIRNAME}.txt"


cd /home/osm/apps/housenumbercore/src/

java de.regioosm.housenumbercore.imports.CsvListImport \
-coordinatesystem 25832 -country "Bundesrepublik Deutschland" \
-file "${DATAROOT}/${AREADIRNAME}/${TODAYDIRNAME}/DE_TH_gebref_${TODAYDIRNAME}.txt" -filecharset UTF-8 \
-municipalityidfile "${AREADIRNAME}/${TODAYDIRNAME}/DE_TH_gebref_schluessel.txt" \
-copyright "© GDI-Th" \
-useage "Datenlizenz Deutschland - Namensnennung - Version 2.0 (<a href='http://www.govdata.de/dl-de/by-2-0'>www.govdata.de/dl-de/by-2-0</a>)" \
-listurl "https://www.geoportal-th.de/de-de/Downloadbereiche/Download-Offene-Geodaten-Th%C3%BCringen" \
-listcontenttimestamp "${CONTENTDATE}" -listfiletimestamp "${FILEDOWNLOADDATE}" \
-useoverpass yes -coordinatesosmimportable no -subareaactive no \
-c 4=municipalityref -c 6=housenumber -c 7=lon -c 8=lat -c 9=street >"Import_DE_TH_${TODAYDIRNAME}.log"

#psql -d hausnummern -U okilimu -c "update stadt set parameters = hstore(ARRAY['listcoordforevaluation','yes', 'listcoordosmuploadable','no']) where stadt = 'Leonberg' and officialkeys_id = '08115028' and land_id = (select id from land where land = 'Bundesrepublik Deutschland')"
