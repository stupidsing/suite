package suite.math;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.util.To;
import suite.util.Util;

public class DiscreteHaarWaveletTransformTest {

	@Test
	public void testFft() {
		int data[] = To.intArray(16, i -> i);

		for (int i = 0; i < data.length; i++) {
			int j = new Random().nextInt(data.length);
			int temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}

		int expect[] = data.clone();

		DiscreteHaarWaveletTransform dhwt = new DiscreteHaarWaveletTransform();
		data = dhwt.dhwt(data);
		data = dhwt.idhwt(data);

		Util.dump("actual", data);
		Util.dump("expect", expect);
		assertTrue(Arrays.equals(data, expect));
	}

}
