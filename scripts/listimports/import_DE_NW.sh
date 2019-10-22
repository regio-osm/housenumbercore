#!/bin/bash
cd ~/apps/housenumbercore/data/Deutschland/Nordrhein-Westfalen/Gesamt/
mkdir 2019-07
cd 2019-07/
wget https://www.opengeodata.nrw.de/produkte/geobasis/lika/alkis_sek/gebref/gebref_EPSG4647_ASCII.zip
unzip gebref_EPSG4647_ASCII.zip


# process file, which references from muniid to municipality_name
#   bring 4 parts of muniid together, then municipality_name
gawk 'BEGIN { 
	FS=";"; 
	OFS="\t"; 
	print "GKZ\tGEMEINDENAME"; }
/^G/ {
	$1 = gensub(/^\xEF\xBB\xBF(.*)/, "\\1", "g", $1)
	printf("%s%s%s%s\t%s\n",$2,$3,$4,$5,$6)
}' gebref_schluessel.txt > DE_NW_gebref_schluessel_20191231.txt



# process main file
# remove optionally BOM-Header at file start
# change float-format of x and y geocoordinates from , (comma) to . (dot)
# bring 4 parts of muniid together, rest of fields stay originally (last field, date of actuality, will be removed)
gawk 'BEGIN { 
	FS=";"; 
	OFS="\t"; 
	print "#dummy\tid\tadresstyp\tmuniid\tid2\thausnummer\tx\ty\tstrasse";
}
// {
	$1 = gensub(/^\xEF\xBB\xBF(.*)/, "\\1", "g", $1)
	$12 = gensub(/([0-9]+),([0-9]*)/, "\\1\.\\2", "g", $12); 
	$13 = gensub(/([0-9]+),([0-9]*)/, "\\1\.\\2", "g", $13);

		# only export addresses of catagory A or B. C is POIs with a artifical address, not visible on ground
	if ( $3 == "A" || $3 == "B" ) printf("%s\t%s\t%s\t%s%s%s%s\t%s\t%s%s\t%s\t%s\t%s\n",$1,$2,$3,$4,$5,$6,$7,$9,$10,$11,$12,$13,$14)
}' gebref.txt > DE_NW_gebref_20191231.txt



cd ~/apps/housenumbercore/src/

java de.regioosm.housenumbercore.imports.CsvListImport -coordinatesystem 25832 -country "Bundesrepublik Deutschland" -file "~/apps/housenumbercore/data/Deutschland/Nordrhein-Westfalen/Gesamt/2019-07/DE_NW_gebref_20191231.txt" -filecharset UTF-8 -municipalityidfile "Deutschland/Nordrhein-Westfalen/Gesamt/2019-07/DE_NW_gebref_schluessel_20191231.txt" -copyright "© Nand NRW (2019)" -useage "Datenlizenz Deutschland - Namensnennung - Version 2.0 (<a href='http://www.govdata.de/dl-de/by-2-0'>www.govdata.de/dl-de/by-2-0</a>)" -listurl "https://www.opengeodata.nrw.de/produkte/geobasis/lika/alkis_sek/gebref/" -listcontenttimestamp 2019-07-01 -listfiletimestamp 2019-11-05 -useoverpass yes -coordinatesosmimportable no -subareaactive no -c 4=municipalityref -c 6=housenumber -c 7=lon -c 8=lat -c 9=street >Import_DE_NW.log

#psql -d hausnummern -U okilimu -c "update stadt set parameters = hstore(ARRAY['listcoordforevaluation','yes', 'listcoordosmuploadable','no']) where stadt = 'Leonberg' and officialkeys_id = '08115028' and land_id = (select id from land where land = 'Bundesrepublik Deutschland')"
