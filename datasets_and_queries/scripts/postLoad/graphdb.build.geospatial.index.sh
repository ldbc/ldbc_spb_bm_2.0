#!/bin/bash

#path to properties file
pathToPropertiesFile="../../../$1"

#extract the endpoint URL for the CURL command
SPQRLEndpointURL=$(grep "endpointUpdateURL=" "$pathToPropertiesFile" | cut -d = -f 2)

#execute a CURL command to build the geo-spatial index
curl -g -H 'Accept: application/xml' --data-urlencode 'update=
PREFIX ontogeo: <http://www.ontotext.com/owlim/geo#> 
  INSERT DATA { _:b1 ontogeo:createIndex _:b2. }' $SPQRLEndpointURL
