package suite.math;

import java.nio.ByteBuffer;
import java.util.Arrays;

import suite.primitive.IntIntSink;
import suite.primitive.Ints_;

/**
 * https://en.wikipedia.org/wiki/SHA-2
 *
 * @author ywsing
 */
public class Sha2 {

	int[] k = { //
			0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5, //
			0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174, //
			0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da, //
			0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967, //
			0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85, //
			0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070, //
			0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3, //
			0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2, };

	public byte[] sha256(byte[] bs0) {
		int h0 = 0x6a09e667, h1 = 0xbb67ae85, h2 = 0x3c6ef372, h3 = 0xa54ff53a;
		int h4 = 0x510e527f, h5 = 0x9b05688c, h6 = 0x1f83d9ab, h7 = 0x5be0cd19;
		var L0 = bs0.length * 8;
		var L1 = L0 + 1;
		var L2 = L1 + 64;
		var L3 = L2 + 511 & 0xFFFFFE00;
		var K = L3 - L2;

		int[] is = Ints_.toArray(bs0.length / 4, i -> {
			var i4 = i * 4;
			return bs0[i4] + (bs0[i4 + 1] << 8) + (bs0[i4 + 2] << 16) + (bs0[i4 + 3] << 24);
		});

		IntIntSink set = (pos, b) -> is[pos / 32] |= b << pos % 32;

		set.sink2(L1, 1);

		for (int i = 0; i < 64; i++)
			set.sink2(L1 + K, L0 >> (63 - i) & 1);

		for (int pos = 0; pos < is.length;) {
			var i0 = pos;
			int[] w = Arrays.copyOfRange(is, i0, pos += 512 / 32);

			for (int i = 16; i < 64; i++) {
				var wi2 = w[i - 2];
				var wi15 = w[i - 15];
				int s0 = Integer.rotateRight(wi15, 7) ^ Integer.rotateRight(wi15, 18) ^ wi15 >> 3;
				int s1 = Integer.rotateRight(wi2, 17) ^ Integer.rotateRight(wi2, 19) ^ wi2 >> 10;
				w[i] = w[i - 16] + s0 + w[i - 7] + s1;
			}

			int a = h0, b = h1, c = h2, d = h3;
			int e = h4, f = h5, g = h6, h = h7;

			for (int i = 0; i < 64; i++) {
				int s1 = Integer.rotateRight(e, 6) ^ Integer.rotateRight(e, 11) ^ Integer.rotateRight(e, 25);
				var ch = e & f ^ ~e & g;
				var temp1 = h + s1 + ch + k[i] + w[i];
				int s0 = Integer.rotateRight(a, 2) ^ Integer.rotateRight(a, 13) ^ Integer.rotateRight(a, 22);
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

		byte[] bs1 = new byte[256];
		ByteBuffer bb = ByteBuffer.wrap(bs1);
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
