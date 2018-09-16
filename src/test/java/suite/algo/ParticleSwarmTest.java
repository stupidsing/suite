package suite.algo;

import static org.junit.Assert.assertTrue;
import static suite.util.Friends.abs;
import static suite.util.Friends.max;
import static suite.util.Friends.min;
import static suite.util.Friends.sqrt;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import suite.math.Math_;
import suite.math.linalg.Vector;
import suite.primitive.DblPrimitives.Obj_Dbl;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.DblObjPair;
import suite.streamlet.Read;

// https://medium.com/@deepulse/a-practical-guide-to-particle-swarm-optimization-c6a615113a71
public class ParticleSwarmTest {

	private Random random = new Random();
	private Vector vec = new Vector();

	@Test
	public void test() {
		var particles = Ints_.range(500).map(i -> new Particle()).toList();
		DblObjPair<float[]> globalBest = DblObjPair.of(Double.MIN_VALUE, null);
		var delta = 1d;

		for (var i = 0; i < 1024; i++)
			for (var particle : particles) {
				var fitness = 1d / schwefel(particle.xs);

				if (particle.best.t0 < fitness)
					particle.best.update(fitness, vec.of(particle.xs));

				if (globalBest.t0 < fitness)
					globalBest.update(fitness, vec.of(particle.xs));

				particle.influence(globalBest);
				particle.move(delta *= .9999d);

				// TODO clip particle xs against wall
			}

		System.out.println(globalBest.t0);
		System.out.println(Arrays.toString(globalBest.t1));

		assertTrue(globalBest.t0 < .01d);
	}

	private class Particle {
		private float[] xs;
		private float[] velocity;
		private DblObjPair<float[]> best = DblObjPair.of(Double.MIN_VALUE, null);

		public Particle() {
			xs = new float[] { -512f + 1024f * random.nextFloat(), -512f + 1024f * random.nextFloat(), };

			double vx, vy, d;
			do {
				vx = -.5d + random.nextDouble();
				vy = -.5d + random.nextDouble();
				d = vx * vx + vy * vy;
			} while (d <= Math_.epsilon || 1d < d);

			velocity = vec.scale(new float[] { (float) vx, (float) vy, }, 64d / d);
		}

		private void influence(DblObjPair<float[]> globalBest) {
			var memoryInfluence = .1d;
			var socialInfluence = .1d;
			vec.scaleOn(velocity, 1d - memoryInfluence - socialInfluence);
			vec.addOn(velocity, vec.scale(vec.sub(best.t1, xs), memoryInfluence));
			vec.addOn(velocity, vec.scale(vec.sub(globalBest.t1, xs), socialInfluence));
			velocity = vec.normalize(velocity);
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
