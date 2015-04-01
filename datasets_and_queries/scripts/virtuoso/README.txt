Setting up Virtuoso Server

- create a DB directory and copy the virtuoso.ini file
- make sure you have virtuoso-t and isql tool in the PATH environment setting
- make sure VIRTUOSO_HOME is properly set
- edit virtuoso.ini and setup properly the Striping section
- the NumberOfBuffers (8K each) should be adjusted if they more than 2/3 of available RAM memory 
- ditto ThreadsPerQuery and AsyncQueueMaxThreads according to number of available CPU cores

Loading the reference dataset and data in Virtuoso database

- start the Virtuoso instance in the directory prepared in previous steps 
- make sure test.properties have http://localhost:8890/sparql as a endpointURL and endpointUpdateURL also only generateCreativeWorks=true
- run the load_virt.sh <virtuoso_sql_port> <spb_driver_checkout>
Note the default SQL port is 1111 and HTTP port is 8890
