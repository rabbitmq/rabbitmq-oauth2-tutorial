# Use PingFederate as OAuth 2.0 server

You are going to test one OAuth flow:
1. Access management ui via a browser

## Prerequisites to follow this guide

- Docker
- make
- A license key downloaded from [PingFederate](https://docs.pingidentity.com/r/en-us/pingfederate-103/imb1564002973616)

## Deploy PingFederate

First, deploy **PingFederate** by running the following command. However, you must modify the location of the license key file in the environment variable `LICENSE_KEY_FILE`. 
```
export LICENSE_KEY_FILE="/Users/mrosales/Documents/pingfedeerate/PingFederate-12.0.1-Development.lic" 
make start-pingfederate
```

To access PingFederate management interface go to https://0.0.0.0:9999/. 

**IN PROGESS**