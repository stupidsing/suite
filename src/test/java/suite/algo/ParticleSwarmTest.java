package suite.algo;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static org.junit.Assert.assertTrue;
import static suite.util.Streamlet_.forInt;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.Math_;
import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.Read;

// https://medium.com/@deepulse/a-practical-guide-to-particle-swarm-optimization-c6a615113a71
public class ParticleSwarmTest {

	private Random random = new Random();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var particles = forInt(500).map(i -> new Particle()).toList();
		var globalBest = DblObjPair.of(Double.MIN_VALUE, vec.of());
		var delta = 1d;

		for (var i = 0; i < 1024; i++)
			for (var particle : particles) {
				var xs = particle.xs;
				var fitness = 1d / schwefel(xs);

				if (globalBest.k < fitness)
					globalBest.update(fitness, vec.copyOf(xs));

				particle.updateLocal(fitness);
				particle.influence(globalBest);
				particle.move(delta *= .9999d);
			}

		System.out.println(globalBest.k);
		System.out.println(Arrays.toString(globalBest.v));

		assertTrue(globalBest.k < .01d);
	}

	private class Particle {
		private float[] xs;
		private float[] velocity;
		private DblObjPair<float[]> best = DblObjPair.of(Double.MIN_VALUE, vec.of());

		public Particle() {
			xs = vec.of(-512f + 1024f * random.nextFloat(), -512f + 1024f * random.nextFloat());

			double vx, vy, d;
			do {
				vx = -.5d + random.nextDouble();
				vy = -.5d + random.nextDouble();
				d = vx * vx + vy * vy;
			} while (d <= Math_.epsilon || 1d < d);

			velocity = vec.scale(vec.of(vx, vy), 64d / d);
		}

		private void updateLocal(double fitness) {
			if (best.k < fitness)
				best.update(fitness, vec.copyOf(xs));
		}

		private void influence(DblObjPair<float[]> globalBest) {
			var memoryInfluence = .1d;
			var socialInfluence = .1d;
			vec.scaleOn(velocity, 1d - memoryInfluence - socialInfluence);
			vec.addOn(velocity, vec.scaleOn(vec.sub(best.v, xs), memoryInfluence));
			vec.addOn(velocity, vec.scaleOn(vec.sub(globalBest.v, xs), socialInfluence));
			vec.normalizeOn(velocity);
		}

		private void move(double scale) {
			for (var index = 0; index < velocity.length; index++) {
				var x = xs[index];
				var v = velocity[index];

				// -512d < x + scale * velocity < 512d
				// -512d - x < scale * velocity < 512d - x
				// if velocity +ve:
				// (-512d - x) / velocity < scale < (512d - x) / velocity
				// if velocity -ve:
				// (512d - x) / velocity < scale < (-512d - x) / velocity
				if (Math_.epsilon < v) {
					var invf = 1d / v;
					scale = max(scale, invf * (-512d - x));
					scale = min(scale, invf * (+512d - x));
				} else if (v < Math_.epsilon) {
					var invf = 1d / v;
					scale = max(scale, invf * (+512d - x));
					scale = min(scale, invf * (-512d - x));
				}
			}

			vec.addOn(xs, vec.scale(velocity, scale));
		}
	}

	private double schwefel(float[] xs) {
		var x = xs[0];
		var y = xs[1];
		var alpha = 418.982887d;
		return Read.each(x, y).toDouble(Obj_Dbl.sum(x_ -> alpha - Math.sin(sqrt(abs(x_)))));
	}

}
