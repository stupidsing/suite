package suite.algo;

import static suite.util.Friends.fail;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import suite.primitive.adt.pair.IntDblPair;
import suite.primitive.adt.pair.IntIntPair;

// https://towardsdatascience.com/reinforcement-learning-with-python-8ef0242a2fa2
public class Q_LearningTest {

	private Random random = new Random();

	private List<IntIntPair> locations = List.of( //
			IntIntPair.of(0, 0), //
			IntIntPair.of(0, 4), //
			IntIntPair.of(4, 0), //
			IntIntPair.of(4, 3));

	private int nStates = 5 * 5 * 5 * 4;
	private int nActions = 6;
	private int onTaxi = locations.size();

	private Set<IntIntPair> noLeft_ = Set.of( //
			IntIntPair.of(0, 2), //
			IntIntPair.of(3, 1), //
			IntIntPair.of(3, 3), //
			IntIntPair.of(4, 1), //
			IntIntPair.of(4, 3));

	private Set<IntIntPair> noRight = Set.of( //
			IntIntPair.of(0, 1), //
			IntIntPair.of(3, 0), //
			IntIntPair.of(3, 2), //
			IntIntPair.of(4, 0), //
			IntIntPair.of(4, 2));

	private class Result {
		private State state;
		private int reward;
		private boolean done;

		private Result(State state, int reward) {
			this(state, reward, false);
		}

		private Result(State state, int reward, boolean done) {
			this.state = state;
			this.reward = reward;
			this.done = done;
		}
	}

	private class State {
		private IntIntPair taxi;
		private int passenger;
		private int destination;

		private State(IntIntPair taxi, int passenger, int destination) {
			this.taxi = taxi;
			this.passenger = passenger;
			this.destination = destination;
		}

		private Result move(int action) {
			if (action == 0) // up
				if (taxi.t0 != 0)
					return new Result(new State(IntIntPair.of(taxi.t0 - 1, taxi.t1), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 1) // down
				if (taxi.t0 != 4)
					return new Result(new State(IntIntPair.of(taxi.t0 + 1, taxi.t1), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 2) // left
				if (taxi.t1 != 0 && !noLeft_.contains(taxi))
					return new Result(new State(IntIntPair.of(taxi.t0, taxi.t1 - 1), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 3) // right
				if (taxi.t1 != 4 && !noRight.contains(taxi))
					return new Result(new State(IntIntPair.of(taxi.t0, taxi.t1 + 1), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 4) // pickup
				if (passenger != onTaxi && Objects.equals(taxi, locations.get(passenger)))
					return new Result(new State(taxi, onTaxi, destination), -1);
				else
					return new Result(this, -10);
			else if (action == 5) // drop-off
				if (passenger == onTaxi && Objects.equals(taxi, locations.get(destination)))
					return new Result(this, 20, true);
				else
					return new Result(this, -10);
			else
				return fail();
		}

		private int encode() {
			var c = passenger;
			c = taxi.t0 + 5 * c;
			c = taxi.t1 + 5 * c;
			c = destination + 4 * c;
			return c;
		}
	}

	private State decode(int c) {
		var destination = c % 4;
		c /= 4;
		var taxiy = c % 5;
		c /= 5;
		var taxix = c % 5;
		c /= 5;
		var passenger = c % 5;
		return new State(IntIntPair.of(taxix, taxiy), passenger, destination);
	}

	@Test
	public void test() {
		var initial = new State(IntIntPair.of(3, 1), 0, 3);
		var alpha = .1d;
		var gamma = .6d;
		var epsilon = .1d;

		var nalpha = 1d - alpha;
		var q = new float[nStates][nActions];
		Result result;

		for (var iter = 0; iter < 524288; iter++) {
			State state;
			var st = (state = initial).encode();

			do {
				var qst = q[st];
				int action;

				if (random.nextDouble() < epsilon)
					action = random.nextInt(nActions);
				else
					action = getMaxActionValue(qst).t0;

				result = state.move(action);
				st = (state = result.state).encode();

				var adj = result.reward + gamma * getMaxActionValue(q[st]).t1;
				qst[action] = (float) (nalpha * qst[action] + alpha * adj);
			} while (!result.done);
		}

		{
			var state = initial;

			do {
				System.out.println("TAXI = " + state.taxi);
				result = state.move(getMaxActionValue(q[state.encode()]).t0);
				state = result.state;
			} while (!result.done);
		}

		decode(0);
	}

	private IntDblPair getMaxActionValue(float[] qst) {
		var actionValue = IntDblPair.of(-1, Double.NEGATIVE_INFINITY);
		float value;
		for (var action = 0; action < nActions; action++)
			if (actionValue.t1 < (value = qst[action]))
				actionValue.update(action, value);
		return actionValue;
	}

}
