#!/bin/bash

#path to properties file
pathToPropertiesFile="../../../$1"

#extract the endpoint URL for the CURL command
SPQRLEndpointURL=$(grep "endpointUpdateURL=" "$pathToPropertiesFile" | cut -d = -f 2)

#execute a CURL command to build lucene-connector index
curl -g -H 'Accept: application/xml' --data-urlencode 'update=

PREFIX luceneConnector: <http://www.ontotext.com/connectors/lucene#>
PREFIX inst: <http://www.ontotext.com/connectors/lucene/instance#>
INSERT DATA {
    inst:cwLuceneConnectorIndex luceneConnector:createConnector '''
{
  "types": [
    "http://www.bbc.co.uk/ontologies/creativework/CreativeWork"
  ],
  "fields": [
    {
      "fieldName": "title",
      "propertyChain": [
        "http://www.bbc.co.uk/ontologies/creativework/title"
      ],
      "facet": false,
      "stored": false
    },
    {
      "fieldName": "description",
      "propertyChain": [
        "http://www.bbc.co.uk/ontologies/creativework/description"
      ],
      "facet": false,
      "stored": false
    },
    {
      "fieldName": "dateModified",
      "propertyChain": [
        "http://www.bbc.co.uk/ontologies/creativework/dateModified"
      ],
      "stored": false,
      "facet": false
    }
  ]
}
'\'''\'''\'' .
}
' $SPQRLEndpointURL
