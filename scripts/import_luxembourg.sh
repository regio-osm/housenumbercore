#!/bin/bash
cd ~/apps/housenumbercore/data/Luxembourg
# wget: include check, if new version of file
wget https://openstreetmap.lu/luxembourg-addresses.csv
# first, extract first file line and check against expected header content
# if header is correct, sort file starting at file row 2
cat luxembourg-addresses.csv | (head -n 1 && tail -n +2 | sort -k12,12 -k3,3 -k1,1 -k2,2 -t, ) > luxembourg-addresses-sortiert.csv
# at filecontent-date to filename and add metafile for content file date and file technical date

# import_stadtstrassen
# gebiete_erstellen
java -cp .:/home/osm/apps/housenumbercore/target/classes/main/resources/commons-compress-1.4.jar:/home/osm/apps/housenumbercore/target/classes/main/resources/osmosis-xml--SNAPSHOT.jar:/home/osm/apps/housenumbercore/target/classes/main/resources/osmosis-core--SNAPSHOT.jar:/home/osm/apps/housenumbercore/target/classes/main/resources/postgresql-9.4-1204-jdbc4.jar de/regioosm/housenumbercore/imports/CsvListImport -country "Italia" -municipality "Milano" -municipalityref "015146" -coordinatesystem 4326 -subareaactive "yes" -listurl "http://dati.comune.milano.it/dataset/ds634-numeri-civici-municipio-coordinate" -copyright "Copyright Lux xyz" -useage "Nutzungsvereinbarung Lux xy" -listcontenttimestamp 2018-03-01 -listfiletimestamp 2018-03-15 -coordinatesosmimportable yes -useoverpass yes  -c 12=municipality -c 3=subarea -c 1=street -c 2=housenumber -c 4=postcode -c 8=lon -c 7=lat -file /home/osm/apps/housenumbercore/data/Luxembourg/luxembourg-addresses-TEST-sortiert.csv >import_Luxembourg-TEST.log 2>&1 &


# jobs_erstellen

