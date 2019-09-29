#!/usr/bin/python
# https://github.com/Destiner/blocksmith/blob/master/blocksmith/ethereum.py
# sudo apt install python-ecdsa

import codecs
import hashlib
import ecdsa
import sys


def toAddress(public_key_hex):
	public_key_bytes = codecs.decode(public_key_hex, 'hex')

	# run SHA256 for the public key
	sha256_bpk_digest = hashlib.sha256(public_key_bytes).digest()

	# run ripemd160 for the SHA256
	ripemd160_bpk = hashlib.new('ripemd160')
	ripemd160_bpk.update(sha256_bpk_digest)
	ripemd160_bpk_digest = ripemd160_bpk.digest()
	ripemd160_bpk_hex = codecs.encode(ripemd160_bpk_digest, 'hex')

	# add network byte
	network_byte = b'00'
	network_btc_public_key_hex = network_byte + ripemd160_bpk_hex
	network_btc_public_key_bytes = codecs.decode(network_btc_public_key_hex, 'hex')

	# double SHA256 to get checksum
	sha256_nbpk0_digest = hashlib.sha256(network_btc_public_key_bytes).digest()
	sha256_nbpk1_digest = hashlib.sha256(sha256_nbpk0_digest).digest()
	sha256_nbpk1_hex = codecs.encode(sha256_nbpk1_digest, 'hex')
	checksum_hex = sha256_nbpk1_hex[:8]

	# concatenate public key and checksum to get the address
	wallet_address_hex = (network_btc_public_key_hex + checksum_hex).decode('utf-8')

	# convert to base58
	alphabet = '123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
	base58_string = ''

	# get the number of leading zeros and convert hex to decimal
	leading_zeros = len(wallet_address_hex) - len(wallet_address_hex.lstrip('0'))

	# convert hex to decimal
	wallet_address_int = int(wallet_address_hex, 16)

	# append digits to the start of string
	while 0 < wallet_address_int:
		digit = wallet_address_int % 58
		digit_char = alphabet[digit]
		base58_string = digit_char + base58_string
		wallet_address_int //= 58

	# add '1' for each 2 leading zeros
	ones = leading_zeros // 2
	for one in range(ones): base58_string = '1' + base58_string
	return base58_string

private_key_hex = sys.stdin.readline().strip()
private_key_bytes = codecs.decode(private_key_hex, 'hex')

# get ECDSA public key
public_key_bytes = ecdsa.SigningKey.from_string(private_key_bytes, curve=ecdsa.SECP256k1).verifying_key.to_string()
public_key_hex = codecs.encode(public_key_bytes, 'hex')

# add bitcoin byte
btc_byte = b'04'
btc_public_key_hex = btc_byte + public_key_hex

print ("btc_public_key_hex = " + btc_public_key_hex)
print ("btc_wallet_address = " + toAddress(btc_public_key_hex))

# get X from the key (first half)
half_length = len(public_key_hex) // 2
public_key_half_hex = public_key_hex[:half_length]

# add bitcoin byte: 0x02 if the last digit is even, 0x03 if the last digit is odd
last_byte = int(public_key_hex.decode('utf-8')[-1], 16)
compressed_btc_byte = b'02' if last_byte % 2 == 0 else b'03'
compressed_btc_public_key_hex = compressed_btc_byte + public_key_half_hex

print ("compressed_btc_public_key_hex = " + compressed_btc_public_key_hex)
print ("compressed_btc_wallet_address = " + toAddress(compressed_btc_public_key_hex))
