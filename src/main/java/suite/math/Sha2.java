package suite.math;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Base64;

import primal.primitive.IntLngSink;
import primal.primitive.IntVerbs.CopyInt;
import primal.primitive.adt.Bytes;

/**
 * https://en.wikipedia.org/wiki/SHA-2
 *
 * @author ywsing
 */
public class Sha2 {

	private int[] k = {
			0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
			0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
			0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
			0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
			0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
			0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
			0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
			0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2, };

	public byte[] hmacSha256(byte[] key0, byte[] bs0) {
		var blockSize = 64;
		var key1 = blockSize < key0.length ? sha256(key0) : key0;
		var key2 = Arrays.copyOf(key1, blockSize);

		// input and output key pads
		var ikp = new byte[blockSize];
		var okp = new byte[blockSize];

		for (var i = 0; i < blockSize; i++) {
			ikp[i] = (byte) (key2[i] ^ 0x36);
			okp[i] = (byte) (key2[i] ^ 0x5c);
		}

		var h0 = sha256(Bytes.concat(Bytes.of(ikp), Bytes.of(bs0)).toArray());
		return sha256(Bytes.concat(Bytes.of(okp), Bytes.of(h0)).toArray());
	}

	public String sha256Str(byte[] bs) {
		return Base64.getEncoder().encodeToString(sha256(bs));
	}

	public byte[] sha256(byte[] bs0) {
		var s512 = 512;
		int h0 = 0x6a09e667, h1 = 0xbb67ae85, h2 = 0x3c6ef372, h3 = 0xa54ff53a;
		int h4 = 0x510e527f, h5 = 0x9b05688c, h6 = 0x1f83d9ab, h7 = 0x5be0cd19;
		var L0 = bs0.length * 8;
		var L1 = L0 + 1;
		var L2 = L1 + 64;
		var L3 = L2 + s512 - 1 & ~(s512 - 1);
		var K = L3 - L2;

		var is = new int[L3 / 32];

		for (var i = 0; i < bs0.length; i++) {
			var div4 = i / 4;
			var mod4 = i % 4;
			is[div4] |= Byte.toUnsignedInt(bs0[i]) << 24 - mod4 * 8;
		}

		IntLngSink set = (pos, b) -> {
			var div32 = pos / 32;
			var mod32 = pos % 32;
			is[div32] |= (b & 1) << 31 - mod32;
		};

		set.sink2(L0, 1);

		for (var i = 0; i < 64; i++)
			set.sink2(L1 + K + i, (long) L0 >>> 63 - i);

		var w = new int[64];

		for (var pos = 0; pos < is.length; pos += s512 / 32) {
			CopyInt.array(is, pos, w, 0, s512 / 32);

			for (var i = s512 / 32; i < w.length; i++) {
				var wi02 = w[i - 02];
				var wi15 = w[i - 15];
				var s0 = Integer.rotateRight(wi15, 07) ^ Integer.rotateRight(wi15, 18) ^ wi15 >>> 03;
				var s1 = Integer.rotateRight(wi02, 17) ^ Integer.rotateRight(wi02, 19) ^ wi02 >>> 10;
				w[i] = w[i - 16] + s0 + w[i - 07] + s1;
			}

			int a = h0, b = h1, c = h2, d = h3, e = h4, f = h5, g = h6, h = h7;

			for (var i = 0; i < w.length; i++) {
				var s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
				var ch = e & f ^ ~e & g;
				var temp1 = h + s1 + ch + k[i] + w[i];
				var s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
				var maj = a & b ^ a & c ^ b & c;
				var temp2 = s0 + maj;

				h = g;
				g = f;
				f = e;
				e = d + temp1;
				d = c;
				c = b;
				b = a;
				a = temp1 + temp2;
			}

			h0 += a;
			h1 += b;
			h2 += c;
			h3 += d;
			h4 += e;
			h5 += f;
			h6 += g;
			h7 += h;
		}

		var bs1 = new byte[256 / 8];
		var bb = ByteBuffer.wrap(bs1);
		bb.putInt(h0);
		bb.putInt(h1);
		bb.putInt(h2);
		bb.putInt(h3);
		bb.putInt(h4);
		bb.putInt(h5);
		bb.putInt(h6);
		bb.putInt(h7);

		return bs1;
	}

}
