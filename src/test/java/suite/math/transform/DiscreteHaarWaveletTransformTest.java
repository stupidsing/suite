package suite.math.transform;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.junit.jupiter.api.Test;

import primal.primitive.IntVerbs.NewInt;

public class DiscreteHaarWaveletTransformTest {

	@Test
	public void testFft() {
		var data = NewInt.array(16, i -> i);

		for (var i = 0; i < data.length; i++) {
			var j = new Random().nextInt(data.length);
			var temp = data[i];
			data[i] = data[j];
			data[j] = temp;
		}

		var expect = data.clone();

		var dhwt = new DiscreteHaarWaveletTransform();
		data = dhwt.dhwt(data);
		data = dhwt.idhwt(data);

		assertTrue(Arrays.equals(data, expect));
	}

}
