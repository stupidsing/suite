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

	private int nDigestBits; // in bits, 224, 256, 384, 512 etc.
	private int nRateBits; // in bits
	private int nRateBitsFilled; // in bits, nRateBitsFilled < nRateBits
	private boolean padded;
	private long[] states = new long[maxStates / 64];

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
		System.arraycopy(sha3.states, 0, states, 0, sha3.states.length);
		nDigestBits = sha3.nDigestBits;
		nRateBits = sha3.nRateBits;
		nRateBitsFilled = sha3.nRateBitsFilled;
		padded = sha3.padded;
	}

	private void reset(int nRateBits, int nDigestBits) {
		if (nRateBits + nDigestBits * 2 == maxStates && 0 < nRateBits && (nRateBits & 0x3F) == 0) {
			Arrays.fill(states, 0);

			this.nDigestBits = nDigestBits;
			this.nRateBits = nRateBits;
			nRateBitsFilled = 0;
			padded = false;
		} else
			throw new RuntimeException();
	}

	public void update(byte in) {
		updateBits(in, 8);
	}

	public void update(byte[] in) {
		if (Boolean.TRUE)
			update(ByteBuffer.wrap(in));
		else if (Boolean.FALSE)
			for (var b : in)
				updateBits(b, 8);
		else
			for (var b : in)
				for (var i = 0; i < 8; i++)
					updateBits(b >> i, 1);
	}

	private void update(ByteBuffer in) {
		var nInputBytes = in.remaining();
		var nRateBytesFilled0 = nRateBitsFilled / 8;
		var nRateBytesFilledMod8 = nRateBytesFilled0 & 0x07;

		if (padded)
			throw new RuntimeException();
		else if ((nRateBitsFilled & 0x07) != 0) // bad alignment
			throw new RuntimeException();

		// logically must have space at this point
		var c0 = Math.min(nInputBytes, -nRateBytesFilledMod8 & 0x07);
		var shift = nRateBytesFilledMod8 * 8;
		var shiftx = shift + c0 * 8;
		var stateIndex0 = nRateBytesFilled0 / 8;

		nRateBytesFilled0 += c0;
		nInputBytes -= c0;

		for (; shift < shiftx; shift += 8)
			states[stateIndex0] ^= (long) (in.get() & 0xFF) << shift;

		nRateBitsFilled = nRateBytesFilled0 * 8;
		keccakIfRequired();

		var nRateBytesFilled1 = nRateBitsFilled / 8;
		var stateIndex1 = nRateBytesFilled1 / 8;
		var nInputLongs = nInputBytes / 8;
		var nRateLongs = nRateBits / 64;
		var order = in.order();

		try {
			in.order(ByteOrder.LITTLE_ENDIAN);

			while (0 < nInputLongs) {
				var c1 = Math.min(nInputLongs, nRateLongs - stateIndex1);
				nInputLongs -= c1;
				var stateIndexEnd = stateIndex1 + c1;

				while (stateIndex1 < stateIndexEnd)
					states[stateIndex1++] ^= in.getLong();

				if (nRateLongs <= stateIndex1) {
					keccak();
					stateIndex1 = 0;
				}
			}
		} finally {
			in.order(order);
		}

		var nInputBits = (nInputBytes & 0x07) * 8;

		for (var i = 0; i < nInputBits; i += 8)
			states[stateIndex1] ^= (long) (in.get() & 0xFF) << i;

		nRateBitsFilled = nInputBits + stateIndex1 * 64;
	}

	public byte[] digest() {
		var bs = new byte[nDigestBits >>> 3];
		digest(ByteBuffer.wrap(bs));
		return bs;
	}

	private void digest(ByteBuffer out) {
		if (!padded)
			padSha3();

		var nOutputBytesLeft0 = out.remaining();
		var nRateBytesFilled0 = nRateBitsFilled / 8;
		var nRateBytesFilledMod8 = nRateBytesFilled0 & 0x07;
		var c0 = Math.min(-nRateBytesFilledMod8 & 0x07, nOutputBytesLeft0);
		var shift0 = nRateBytesFilledMod8 * 8;
		var shiftx0 = shift0 + c0 * 8;
		var w = states[nRateBytesFilled0 / 8];

		if ((nRateBitsFilled & 0x07) == 0)
			for (; shift0 < shiftx0; shift0 += 8)
				out.put((byte) (w >>> shift0));
		else // bad alignment
			throw new RuntimeException();

		var nOutputBytesLeft1 = nOutputBytesLeft0 - c0;
		var nRateBytesFilled1 = nRateBytesFilled0 + c0;
		nRateBitsFilled = nRateBytesFilled1 * 8;

		if (nOutputBytesLeft1 != 0) {
			var nRateLongs = nRateBits / 64;
			var stateIndex = nRateBitsFilled / 64;
			var nOutputLongsLeft = nOutputBytesLeft1 / 8;

			if (nRateLongs <= stateIndex) {
				squeezeSha3();
				stateIndex = 0;
			}

			var order0 = out.order();

			try {
				out.order(ByteOrder.LITTLE_ENDIAN);

				while (0 < nOutputLongsLeft) {
					var c = Math.min(nOutputLongsLeft, nRateLongs - stateIndex);
					var stateIndexEnd = stateIndex + c;

					for (; stateIndex < stateIndexEnd; stateIndex++)
						out.putLong(states[stateIndex]);

					nOutputLongsLeft -= c;

					if (nRateLongs <= stateIndex) {
						squeezeSha3();
						stateIndex = 0;
					}
				}
			} finally {
				out.order(order0);
			}

			var nOutputBytesMod8 = nOutputBytesLeft1 & 0x07;

			var shiftx1 = nOutputBytesMod8 * 8;

			for (var shift1 = 0; shift1 < shiftx1; shift1 += 8)
				out.put((byte) (states[stateIndex] >>> shift1));

			nRateBitsFilled = shiftx1 + stateIndex * 64;
		}
	}

	public void updateBits(long in0, int nInputBits) {
		if (padded)
			throw new RuntimeException();
		else if (nInputBits < 0 || 64 < nInputBits)
			throw new RuntimeException();

		// logically must have space at this point
		var nRateBitsFilledMod64 = nRateBitsFilled & 0x3F;
		var c = Math.min(nInputBits, -nRateBitsFilled & 0x3F);

		states[nRateBitsFilled / 64] ^= (in0 & mask(c)) << nRateBitsFilledMod64;
		nRateBitsFilled += c;
		nInputBits -= c;

		keccakIfRequired();

		states[nRateBitsFilled / 64] ^= in0 >>> c & mask(nInputBits);
		nRateBitsFilled += nInputBits;

		keccakIfRequired();
	}

	private long mask(int bits) {
		return 0 < bits ? -1l >>> -bits : 0l;
	}

	private void squeezeSha3() {
		throw new RuntimeException();
	}

	private void squeezeKeccak() {
		keccak();
	}

	private void padSha3() {
		updateBits(0x06, 3);
		padRateBits();
	}

	private void padKeccak() {
		updateBits(0x01, 1);
		padRateBits();
	}

	private void padRateBits() {
		nRateBitsFilled = nRateBits - 1;
		updateBits(0x01, 1);
		padded = true;
	}

	private void keccakIfRequired() {
		if (nRateBits <= nRateBitsFilled) {
			keccak();
			nRateBitsFilled = 0;
		}
	}

	private void keccak() {
		for (var i = 0; i < 24; i++) {
			long a_10_;

			// theta (precalculation part)
			var c0 = states[0] ^ states[5 + 0] ^ states[10 + 0] ^ states[15 + 0] ^ states[20 + 0];
			var c1 = states[1] ^ states[5 + 1] ^ states[10 + 1] ^ states[15 + 1] ^ states[20 + 1];
			var c2 = states[2] ^ states[5 + 2] ^ states[10 + 2] ^ states[15 + 2] ^ states[20 + 2];
			var c3 = states[3] ^ states[5 + 3] ^ states[10 + 3] ^ states[15 + 3] ^ states[20 + 3];
			var c4 = states[4] ^ states[5 + 4] ^ states[10 + 4] ^ states[15 + 4] ^ states[20 + 4];

			var t0 = (c0 << 1) ^ (c0 >>> (64 - 1)) ^ c3;
			var t1 = (c1 << 1) ^ (c1 >>> (64 - 1)) ^ c4;
			var t2 = (c2 << 1) ^ (c2 >>> (64 - 1)) ^ c0;
			var t3 = (c3 << 1) ^ (c3 >>> (64 - 1)) ^ c1;
			var t4 = (c4 << 1) ^ (c4 >>> (64 - 1)) ^ c2;
			var i8 = 8;
			var i9 = 9;

			// theta (xorring part) + rho + pi
			states[0] ^= t1;
			var a01_t2 = states[01] ^ t2;
			a_10_ = a01_t2 << 01 | a01_t2 >>> 64 - 01;
			var a06_t2 = states[06] ^ t2;
			states[01] = a06_t2 << 44 | a06_t2 >>> 64 - 44;
			var a09_t0 = states[i9] ^ t0;
			states[06] = a09_t0 << 20 | a09_t0 >>> 64 - 20;
			var a22_t3 = states[22] ^ t3;
			states[i9] = a22_t3 << 61 | a22_t3 >>> 64 - 61;

			var a14_t0 = states[14] ^ t0;
			states[22] = a14_t0 << 39 | a14_t0 >>> 64 - 39;
			var a20_t1 = states[20] ^ t1;
			states[14] = a20_t1 << 18 | a20_t1 >>> 64 - 18;
			var a02_t3 = states[02] ^ t3;
			states[20] = a02_t3 << 62 | a02_t3 >>> 64 - 62;
			var a12_t3 = states[12] ^ t3;
			states[02] = a12_t3 << 43 | a12_t3 >>> 64 - 43;
			var a13_t4 = states[13] ^ t4;
			states[12] = a13_t4 << 25 | a13_t4 >>> 64 - 25;

			var a19_t0 = states[19] ^ t0;
			states[13] = a19_t0 << i8 | a19_t0 >>> 64 - i8;
			var a23_t4 = states[23] ^ t4;
			states[19] = a23_t4 << 56 | a23_t4 >>> 64 - 56;
			var a15_t1 = states[15] ^ t1;
			states[23] = a15_t1 << 41 | a15_t1 >>> 64 - 41;
			var a04_t0 = states[04] ^ t0;
			states[15] = a04_t0 << 27 | a04_t0 >>> 64 - 27;
			var a24_t0 = states[24] ^ t0;
			states[04] = a24_t0 << 14 | a24_t0 >>> 64 - 14;

			var a21_t2 = states[21] ^ t2;
			states[24] = a21_t2 << 02 | a21_t2 >>> 64 - 02;
			var a08_t4 = states[i8] ^ t4;
			states[21] = a08_t4 << 55 | a08_t4 >>> 64 - 55;
			var a16_t2 = states[16] ^ t2;
			states[i8] = a16_t2 << 45 | a16_t2 >>> 64 - 45;
			var a05_t1 = states[05] ^ t1;
			states[16] = a05_t1 << 36 | a05_t1 >>> 64 - 36;
			var a03_t4 = states[03] ^ t4;
			states[05] = a03_t4 << 28 | a03_t4 >>> 64 - 28;

			var a18_t4 = states[18] ^ t4;
			states[03] = a18_t4 << 21 | a18_t4 >>> 64 - 21;
			var a17_t3 = states[17] ^ t3;
			states[18] = a17_t3 << 15 | a17_t3 >>> 64 - 15;
			var a11_t2 = states[11] ^ t2;
			states[17] = a11_t2 << 10 | a11_t2 >>> 64 - 10;
			var a07_t3 = states[07] ^ t3;
			states[11] = a07_t3 << 06 | a07_t3 >>> 64 - 06;
			var a10_t1 = states[10] ^ t1;
			states[07] = a10_t1 << 03 | a10_t1 >>> 64 - 03;
			states[10] = a_10_;

			// chi
			for (var c = 0; c < 25; c += 5) {
				var x0 = states[c + 0];
				var x1 = states[c + 1];
				var x2 = states[c + 2];
				var x3 = states[c + 3];
				var x4 = states[c + 4];
				states[c + 0] = x0 ^ ~x1 & x2;
				states[c + 1] = x1 ^ ~x2 & x3;
				states[c + 2] = x2 ^ ~x3 & x4;
				states[c + 3] = x3 ^ ~x4 & x0;
				states[c + 4] = x4 ^ ~x0 & x1;
			}

			// iota
			states[0] ^= rc[i];
		}
	}

}
