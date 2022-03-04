#!/usr/local/bin/python3

import jwt
import time
import sys
import json
from datetime import datetime
from datetime import timedelta

private_key = open(sys.argv[1], "r").read()
public_key = open(sys.argv[2], "r").read()
client_id = sys.argv[3]

JWT = """{
  "scope": [

  ],
  "extra_scope": [
    "rabbitmq.tag:management"
  ],
  "aud": [
    "rabbitmq"
  ]
}"""

decoded_jwt = json.loads(JWT)
#decoded_jwt["client_id"] = client_id
#decoded_jwt["cid"] = client_id
#decoded_jwt["sub"] = client_id

encoded = jwt.encode(decoded_jwt, private_key, algorithm="RS256")
print(encoded)
