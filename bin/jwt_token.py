#!/usr/local/bin/python3

import jwt
import time
import sys
import json
from datetime import datetime
from datetime import timedelta

jwt_file_name = key_name = sys.argv[1]
key_name = sys.argv[2]
private_key = open(sys.argv[3], "r").read()
public_key = open(sys.argv[4], "r").read()
client_id = sys.argv[5]

JWT = """{
  "scope": [
    "rabbitmq.write:*/*/*",
    "rabbitmq.configure:*/*/*",
    "rabbitmq.read:*/*/*"
  ],
  "extra_scope": "rabbitmq.tag:management",
  "aud": [
    "rabbitmq"
  ]
}"""

with open(jwt_file_name) as json_file:
    decoded_jwt = json.load(json_file)

#decoded_jwt = json.loads(JWT)
#decoded_jwt["client_id"] = client_id
#decoded_jwt["cid"] = client_id
#decoded_jwt["sub"] = client_id

encoded = jwt.encode(decoded_jwt, private_key, algorithm="RS256",
  headers={"kid": key_name})
print(encoded)
