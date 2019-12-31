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
java -cp .:/home/osm/apps/housenumbercore/target/classes/main/resources/commons-compress-1.4.jar:/home/osm/apps/housenumbercore/target/classes/main/resources/osmosis-xml--SNAPSHOT.jar:/home/osm/apps/housenumbercore/target/classes/main/resources/osmosis-core--SNAPSHOT.jar:/home/osm/apps/housenumbercore/target/classes/main/resources/postgresql-9.4-1204-jdbc4.jar de/regioosm/housenumbercore/imports/CsvListImport -country "Italia" -municipality "Milano" -municipalityref "015146" -coordinatesystem 4326 -subareaactive "yes" -listurl "http://dati.comune.milano.it/dataset/ds634-numeri-civici-municipio-coordinate" -copyright "Copyright Comune di Milano" -useage "<a href='http://www.opendefinition.org/licenses/cc-by'>Creative Commons Attribution License</a>" -listcontenttimestamp 2018-07-18 -listfiletimestamp 2018-07-26 -fieldseparator ";" -coordinatesosmimportable yes -useoverpass yes -c 6=subarea -c 3=street -c 4=housenumber -c 15=lon -c 14=lat -c 5=note -convertstreetupperlower yes -file /home/osm/apps/housenumbercore/data/Italia/IT_Milan_ds634_civici_municipio_coordinategeografiche_20180718_mod.csv >import_IT_Milan.log 2>&1 &


# jobs_erstellen

