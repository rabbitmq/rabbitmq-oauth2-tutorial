#!/usr/local/bin/python3

import jwt
import time
import sys
from datetime import datetime
from datetime import timedelta

private_key = open(sys.argv[1], "r").read()
public_key = open(sys.argv[2], "r").read()
audience = sys.argv[3]
new_scope_attribute = sys.argv[4]
encoded_jwt = sys.argv[5]

decoded_jwt = jwt.decode(encoded_jwt, public_key, audience=audience, algorithms=["RS256"])
#print(decoded_jwt)

headers = jwt.get_unverified_header(encoded_jwt)
#print(headers)

decoded_jwt[new_scope_attribute] = decoded_jwt['scope']

encoded = jwt.encode(decoded_jwt, private_key, algorithm="RS256")
print(encoded)
