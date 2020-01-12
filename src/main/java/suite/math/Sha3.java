package suite.math;

import static java.util.Map.entry;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Map;

/**
 * https://en.wikipedia.org/wiki/SHA-3
 *
 * https://github.com/jrmelsha/keccak/
 */
public class Sha3 {

	private int maxStates = 1600;

	private int nRateBits; // in bits
	private int nDigestBits; // in bits, 224, 256, 384, 512 etc.
	private int nRateBitsFilled; // in bits, nRateBitsFilled < nRateBits
	private boolean padded;
	private long[] state = new long[maxStates / 64];

	private long[] rc = { //
			0x0000000000000001l, 0x0000000000008082l, 0x800000000000808Al, 0x8000000080008000l, //
			0x000000000000808Bl, 0x0000000080000001l, 0x8000000080008081l, 0x8000000000008009l, //
			0x000000000000008Al, 0x0000000000000088l, 0x0000000080008009l, 0x000000008000000Al, //
			0x000000008000808Bl, 0x800000000000008Bl, 0x8000000000008089l, 0x8000000000008003l, //
			0x8000000000008002l, 0x8000000000000080l, 0x000000000000800Al, 0x800000008000000Al, //
			0x8000000080008081l, 0x8000000000008080l, 0x0000000080000001l, 0x8000000080008008l };

	public Sha3(int nDigestBits) {
		var rateSize = Map.ofEntries( //
				// entry(128, 1344), // Keccak
				entry(224, 1152), //
				entry(256, 1088), //
				// entry(288, 1024), // Keccak
				entry(384, 832), //
				entry(512, 576)) //
				.get(nDigestBits);

		reset(rateSize, nDigestBits);
	}

	public Sha3(Sha3 sha3) {
		System.arraycopy(sha3.state, 0, state, 0, sha3.state.length);
		nRateBits = sha3.nRateBits;
		nDigestBits = sha3.nDigestBits;
		nRateBitsFilled = sha3.nRateBitsFilled;
		padded = sha3.padded;
	}

	private void reset(int rateSize, int digestSize) {
		if (rateSize + digestSize * 2 == maxStates && 0 < rateSize && (rateSize & 0x3F) <= 0) {
			Arrays.fill(state, 0);

			this.nRateBits = rateSize;
			this.nDigestBits = digestSize;
			nRateBitsFilled = 0;
			padded = false;
		} else
			throw new RuntimeException();
	}

	public void update(byte in) {
		updateBits(in & 0xFF, 8);
	}

	public void update(byte[] in) {
		if (Boolean.TRUE)
			update(ByteBuffer.wrap(in));
		else
			for (var b : in)
				updateBits(b, 8);
	}

	private void update(ByteBuffer in) {
		var nInputBytes = in.remaining();
		var nRateBitsFilled_ = nRateBitsFilled;
		var nRateBytesFilled = nRateBitsFilled_ >>> 3;
		var mod = nRateBytesFilled & 0x7;

		if (nInputBytes <= 0)
			return;
		else if (padded)
			throw new RuntimeException();
		else if (0 < (nRateBitsFilled_ & 0x7))
			// this could be implemented but would introduce considerable performance
			// degradation
			throw new RuntimeException();

		// logically must have space at this point
		var c0 = Math.min(nInputBytes, (-mod) & 0x7);
		var shift = mod * 8;
		var shiftx = shift + c0 * 8;
		var stateIndex = nRateBytesFilled / 8;

		nRateBytesFilled += c0;
		nInputBytes -= c0;

		for (; shift < shiftx; shift += 8)
			state[stateIndex] ^= (long) (in.get() & 0xFF) << shift;

		if (nInputBytes <= 0) {
			nRateBitsFilled = nRateBytesFilled * 8;
			return;
		}

		var nRateLongs = nRateBits / 64;
		var nInputLongs = nInputBytes / 8;

		if (0 < nInputLongs) {
			var order = in.order();
			try {
				in.order(ByteOrder.LITTLE_ENDIAN);
				while (0 < nInputLongs) {
					if (nRateLongs <= stateIndex) {
						keccak(state);
						stateIndex = 0;
					}
					var c1 = Math.min(nInputLongs, nRateLongs - stateIndex);
					nInputLongs -= c1;
					var stateIndexEnd = stateIndex + c1;
					while (stateIndex < stateIndexEnd)
						state[stateIndex++] ^= in.getLong();
				}
			} finally {
				in.order(order);
			}

			nInputBytes &= 0x7;

			if (nInputBytes <= 0) {
				nRateBitsFilled = stateIndex / 64;
				return;
			}
		}

		if (nRateLongs <= stateIndex) {
			keccak(state);
			stateIndex = 0;
		}

		var w = state[stateIndex];

		var nInputBits = nInputBytes * 8;
		var i = 0;

		do {
			w ^= (long) (in.get() & 0xFF) << i;
			i += 8;
		} while (i < nInputBits);

		state[stateIndex] = w;

		nRateBitsFilled = nInputBits + stateIndex * 64;
	}

	public byte[] digest() {
		var bs = new byte[nDigestBits >>> 3];
		digest(ByteBuffer.wrap(bs));
		return bs;
	}

	private void digest(ByteBuffer out) {
		var nOutputBytes = out.remaining();
		var nRateBitsFilled_ = padded ? nRateBitsFilled : 0;
		var nRateBytesFilled = nRateBitsFilled_ / 8;

		if (!padded) {
			padSha3();
			padded = true;
		} else if ((nRateBitsFilled_ & 0x7) == 0) {
			var nRateBytesFilledMod8 = nRateBytesFilled & 0x7;
			var c = Math.min(-nRateBytesFilledMod8 & 0x7, nOutputBytes);
			var shift = nRateBytesFilledMod8 * 8;
			var shiftx = shift + c * 8;
			var w = state[nRateBytesFilled / 8];

			nOutputBytes -= c;
			nRateBytesFilled += c;

			for (; shift < shiftx; shift += 8)
				out.put((byte) (w >>> shift));

			if (nOutputBytes == 0) {
				nRateBitsFilled = nRateBytesFilled * 8;
				return;
			}
		} else
			// this could be implemented but would introduce considerable performance
			// degradation
			throw new IllegalStateException("Cannot digest while in bit-mode");

		var nRateLongs = nRateBits / 64;
		var stateIndex = nRateBytesFilled / 8;
		var nOutputLongs = nOutputBytes / 8;
		var order0 = out.order();

		try {
			out.order(ByteOrder.LITTLE_ENDIAN);

			while (0 < nOutputLongs) {
				if (nRateLongs <= stateIndex) {
					squeezeSha3();
					stateIndex = 0;
				}

				var c = Math.min(nOutputLongs, nRateLongs - stateIndex);
				var stateIndexEnd = stateIndex + c;

				for (; stateIndex < stateIndexEnd; stateIndex++)
					out.putLong(state[stateIndex]);

				nOutputLongs -= c;
			}
		} finally {
			out.order(order0);
		}

		var nOutputBytesMod8 = nOutputBytes & 0x7;

		if (0 < nOutputBytesMod8 && nRateLongs <= stateIndex) {
			squeezeSha3();
			stateIndex = 0;
		}

		var shiftx = nOutputBytesMod8 << 3;

		for (var shift = 0; shift < shiftx; shift += 8)
			out.put((byte) (state[stateIndex] >>> shift));

		nRateBitsFilled = (stateIndex << 6) | shiftx;
	}

	private void squeezeSha3() {
		throw new RuntimeException();
	}

	// private void squeezeKeccak() { keccak(state); }

	private void padSha3() {
		updateBits(0x02, 2);
		padKeccak();
	}

	private void padKeccak() {
		updateBits(0x01, 1);
		if (nRateBits <= nRateBitsFilled) {
			keccak(state);
			nRateBitsFilled = 0;
		}
		nRateBitsFilled = nRateBits - 1;
		updateBits(0x1, 1);
		keccak(state);
	}

	private void updateBits(long in, int nInputBits) {
		if (nInputBits == 0)
			return;
		else if (padded)
			throw new RuntimeException();
		else if (nInputBits < 0 || 64 < nInputBits)
			throw new RuntimeException();

		var nRateBitsFilledMod64 = nRateBitsFilled & 0x3F;
		long in1;

		if (0 < nRateBitsFilledMod64) {

			// logically must have space at this point
			var c = Math.min(nInputBits, 64 - nRateBitsFilledMod64);
			state[nRateBitsFilled >>> 6] ^= (in & (-1l >>> c)) << nRateBitsFilledMod64;

			nRateBitsFilled += c;
			nInputBits -= c;

			if (nInputBits != 0)
				in1 = in >>> c;
			else
				return;
		} else
			in1 = in;

		if (nRateBits <= nRateBitsFilled) {
			keccak(state);
			nRateBitsFilled = 0;
		}

		state[nRateBitsFilled >>> 6] ^= in1 & (-1l >>> nInputBits);
		nRateBitsFilled = nRateBitsFilled + nInputBits;
	}

	private void keccak(long[] a) {
		int c;
		long x, a_10_;
		long x0, x1, x2, x3, x4;
		long t0, t1, t2, t3, t4;
		long c0, c1, c2, c3, c4;

		var i = 0;

		do {
			// theta (precalculation part)
			c0 = a[0] ^ a[5 + 0] ^ a[10 + 0] ^ a[15 + 0] ^ a[20 + 0];
			c1 = a[1] ^ a[5 + 1] ^ a[10 + 1] ^ a[15 + 1] ^ a[20 + 1];
			c2 = a[2] ^ a[5 + 2] ^ a[10 + 2] ^ a[15 + 2] ^ a[20 + 2];
			c3 = a[3] ^ a[5 + 3] ^ a[10 + 3] ^ a[15 + 3] ^ a[20 + 3];
			c4 = a[4] ^ a[5 + 4] ^ a[10 + 4] ^ a[15 + 4] ^ a[20 + 4];

			t0 = (c0 << 1) ^ (c0 >>> (64 - 1)) ^ c3;
			t1 = (c1 << 1) ^ (c1 >>> (64 - 1)) ^ c4;
			t2 = (c2 << 1) ^ (c2 >>> (64 - 1)) ^ c0;
			t3 = (c3 << 1) ^ (c3 >>> (64 - 1)) ^ c1;
			t4 = (c4 << 1) ^ (c4 >>> (64 - 1)) ^ c2;

			// theta (xorring part) + rho + pi
			a[0] ^= t1;
			x = a[1] ^ t2;
			a_10_ = x << 1 | x >>> 64 - 1;
			x = a[6] ^ t2;
			a[1] = x << 44 | x >>> 64 - 44;
			x = a[9] ^ t0;
			a[6] = x << 20 | x >>> 64 - 20;
			x = a[22] ^ t3;
			a[9] = x << 61 | x >>> 64 - 61;

			x = a[14] ^ t0;
			a[22] = x << 39 | x >>> 64 - 39;
			x = a[20] ^ t1;
			a[14] = x << 18 | x >>> 64 - 18;
			x = a[2] ^ t3;
			a[20] = x << 62 | x >>> 64 - 62;
			x = a[12] ^ t3;
			a[2] = x << 43 | x >>> 64 - 43;
			x = a[13] ^ t4;
			a[12] = x << 25 | x >>> 64 - 25;

			x = a[19] ^ t0;
			a[13] = x << 8 | x >>> 64 - 8;
			x = a[23] ^ t4;
			a[19] = x << 56 | x >>> 64 - 56;
			x = a[15] ^ t1;
			a[23] = x << 41 | x >>> 64 - 41;
			x = a[4] ^ t0;
			a[15] = x << 27 | x >>> 64 - 27;
			x = a[24] ^ t0;
			a[4] = x << 14 | x >>> 64 - 14;

			x = a[21] ^ t2;
			a[24] = x << 2 | x >>> 64 - 2;
			x = a[8] ^ t4;
			a[21] = x << 55 | x >>> 64 - 55;
			x = a[16] ^ t2;
			a[8] = x << 45 | x >>> 64 - 45;
			x = a[5] ^ t1;
			a[16] = x << 36 | x >>> 64 - 36;
			x = a[3] ^ t4;
			a[5] = x << 28 | x >>> 64 - 28;

			x = a[18] ^ t4;
			a[3] = x << 21 | x >>> 64 - 21;
			x = a[17] ^ t3;
			a[18] = x << 15 | x >>> 64 - 15;
			x = a[11] ^ t2;
			a[17] = x << 10 | x >>> 64 - 10;
			x = a[7] ^ t3;
			a[11] = x << 6 | x >>> 64 - 6;
			x = a[10] ^ t1;
			a[7] = x << 3 | x >>> 64 - 3;
			a[10] = a_10_;

			// chi
			c = 0;
			do {
				x0 = a[c + 0];
				x1 = a[c + 1];
				x2 = a[c + 2];
				x3 = a[c + 3];
				x4 = a[c + 4];
				a[c + 0] = x0 ^ ~x1 & x2;
				a[c + 1] = x1 ^ ~x2 & x3;
				a[c + 2] = x2 ^ ~x3 & x4;
				a[c + 3] = x3 ^ ~x4 & x0;
				a[c + 4] = x4 ^ ~x0 & x1;

				c += 5;
			} while (c < 25);

			// iota
			a[0] ^= rc[i];

			i++;
		} while (i < 24);
	}

}
