#!/bin/sh

if [ $# -lt 2 ]
then
    echo "Usage: load_virt.sh <port> <SPB home directory>"
    exit
fi

PORT=${1-1111}
SPB=${2-~/ldbc_spb_bm_2.0}
TDIR=`pwd`

if [ -z "$VIRTUOSO_HOME" ]
then
    echo "Should run this script under Virtuoso environment"
    exit
fi

isql $PORT dba dba ns.sql
#isql $PORT dba dba spbcset.sql #enable when using feature/emergent branch
isql $PORT dba dba exec="ld_dir_all ('$SPB/dist/data/ontologies', '*.ttl', 'ldbc-onto')"
isql $PORT dba dba exec="ld_dir_all ('$SPB/dist/data/datasets', '*.ttl', 'ldbc')"
for i in {1..8}; do isql $PORT dba dba exec="rdf_loader_run()" & done
wait
isql $PORT dba dba exec="rdfs_rule_set ('ldbc', 'ldbc-onto')"
cd $SPB/dist
java -jar semantic_publishing_benchmark-basic-virtuoso.jar test.properties 
cd $TDIR
isql $PORT dba dba exec="ld_dir_all ('$SPB/dist/generated', '*.nq', 'ldbc')"
for i in {1..16}; do isql $PORT dba dba exec="rdf_loader_run()" & done
isql $PORT dba dba exec="VT_INC_INDEX_DB_DBA_RDF_OBJ()" &
isql $PORT dba dba exec="rdf_geo_fill ()"
wait
isql $PORT dba dba exec='grant "SPARQL_UPDATE" to "SPARQL"'
isql $PORT dba dba exec="checkpoint"
