#!/bin/bash
cd /home/osm/apps/housenumbercore/data/Luxembourg
# wget: include check, if new version of file
wget https://openstreetmap.lu/luxembourg-addresses.csv
# first, extract first file line and check against expected header content
# if header is correct, set prefix # to header line, then sort file starting at file row 2
cat luxembourg-addresses.csv | sort -k12,12 -k3,3 -k1,1 -k2,2 -t, > luxembourg-addresses-sortiert.csv
# at filecontent-date to filename and add metafile for content file date and file technical date
# import_stadtstrassen
# gebiete_erstellen
# jobs_erstellen

