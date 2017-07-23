package suite.math.transform;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.primitive.Ints_;

public class DiscreteHaarWaveletTransformTest {

	@Test
	public void testFft() {
		int[] data = Ints_.toArray(16, i -> i);

		for (int i = 0; i < data.length; i++) {
			int j = new Random().nextInt(data.length);
			int temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}

		int[] expect = data.clone();

		DiscreteHaarWaveletTransform dhwt = new DiscreteHaarWaveletTransform();
		data = dhwt.dhwt(data);
		data = dhwt.idhwt(data);

		assertTrue(Arrays.equals(data, expect));
	}

}
