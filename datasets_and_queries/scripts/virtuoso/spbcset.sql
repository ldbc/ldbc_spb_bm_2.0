

rdf_cset ('cwork', properties => vector (
  'http://www.w3.org/1999/02/22-rdf-syntax-ns#type',
vector ('http://www.bbc.co.uk/ontologies/creativework/dateModified', 'index'),
vector ('http://www.bbc.co.uk/ontologies/creativework/description', 'index'),
'http://www.bbc.co.uk/ontologies/creativework/liveCoverage',
'http://www.bbc.co.uk/ontologies/creativework/shortTitle',
'http://www.bbc.co.uk/ontologies/creativework/thumbnail',
vector ('http://www.bbc.co.uk/ontologies/creativework/title', 'index'),
'http://www.bbc.co.uk/ontologies/creativework/altText',
'http://www.bbc.co.uk/ontologies/creativework/audience',
'http://www.bbc.co.uk/ontologies/creativework/category',
'http://www.bbc.co.uk/ontologies/creativework/dateCreated'
),
  types => vector ());



cset_iri_pattern ('cwork', 'http://www.bbc.co.uk/things/%%#id', vector (vector (0, 10000000000)));


rdf_cset ('geoname', properties => vector (
'http://www.geonames.org/ontology#name',
'http://www.geonames.org/ontology#parentFeature', 
'http://www.geonames.org/ontology#countryCode',
'http://www.geonames.org/ontology#featureClass',
'http://www.geonames.org/ontology#featureCode', 
'http://www.geonames.org/ontology#parentCountry',
'http://www.geonames.org/ontology#nearbyFeatures'),
  types => vector ()); 





cset_iri_pattern ('geoname', 'http://sws.geonames.org/%%/', vector (vector (0, 10000000)));

