

Configure CORS 

To deploy PingFederate 

docker run --name ping  -v  ~/Documents/PingFederate-12.0.1-Development.lic:/opt/out/instance/server/default/conf/pingfederate.lic -p 9031:9031 -p 9999:9999 pingidentity/pingfederate
