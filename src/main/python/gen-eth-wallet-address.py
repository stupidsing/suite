#!/usr/bin/python
# https://github.com/Destiner/blocksmith/blob/master/blocksmith/ethereum.py
# sudo apt install python-ecdsa python-pip python-setuptools
# pip install --user pycryptodome wheel

import codecs
import ecdsa
from Crypto.Hash import keccak
import sys

private_key_hex = sys.stdin.readline().strip()
private_key_bytes = codecs.decode(private_key_hex, 'hex')

# get ECDSA public key
public_key_bytes = ecdsa.SigningKey.from_string(private_key_bytes, curve=ecdsa.SECP256k1).verifying_key.to_string()
public_key_hex = codecs.encode(public_key_bytes, 'hex')

keccak0_hash = keccak.new(digest_bits=256)
keccak0_hash.update(public_key_bytes)
keccak0_digest = keccak0_hash.hexdigest()

# take last 20 bytes
wallet_address_length = 40
wallet_address = keccak0_digest[-wallet_address_length:]
display_wallet_address = '0x' + wallet_address

# remove '0x' from the address
wallet_address_utf8 = wallet_address.encode('utf-8')

keccak1_hash = keccak.new(digest_bits=256)
keccak1_hash.update(wallet_address_utf8)
keccak1_digest = keccak1_hash.hexdigest()

checksum = '0x'

for i in range(len(wallet_address)):
	wallet_address_char = wallet_address[i]
	keccak1_digest_char = keccak1_digest[i]
	if 8 <= int(keccak1_digest_char, 16):
		checksum += wallet_address_char.upper()
	else:
		checksum += str(wallet_address_char)

print ("display_wallet_address = " + display_wallet_address)
print ("checksum = " + checksum)
