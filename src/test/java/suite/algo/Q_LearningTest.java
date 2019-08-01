package suite.algo;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import primal.Verbs.Equals;
import primal.primitive.adt.pair.IntDblPair;
import suite.primitive.Coord;

// https://towardsdatascience.com/reinforcement-learning-with-python-8ef0242a2fa2
public class Q_LearningTest {

	private Random random = new Random();

	private List<Coord> locations = List.of( //
			Coord.of(0, 0), //
			Coord.of(0, 4), //
			Coord.of(4, 0), //
			Coord.of(4, 3));

	private int nStates = 5 * 5 * 5 * 4;
	private int nActions = 6;
	private int onTaxi = locations.size();

	private Set<Coord> noLeft_ = Set.of( //
			Coord.of(0, 2), //
			Coord.of(3, 1), //
			Coord.of(3, 3), //
			Coord.of(4, 1), //
			Coord.of(4, 3));

	private Set<Coord> noRight = Set.of( //
			Coord.of(0, 1), //
			Coord.of(3, 0), //
			Coord.of(3, 2), //
			Coord.of(4, 0), //
			Coord.of(4, 2));

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
		private Coord taxi;
		private int passenger;
		private int destination;

		private State(Coord taxi, int passenger, int destination) {
			this.taxi = taxi;
			this.passenger = passenger;
			this.destination = destination;
		}

		private Result move(int action) {
			if (action == 0) // up
				if (taxi.x != 0)
					return new Result(new State(Coord.of(taxi.x - 1, taxi.y), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 1) // down
				if (taxi.x != 4)
					return new Result(new State(Coord.of(taxi.x + 1, taxi.y), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 2) // left
				if (taxi.y != 0 && !noLeft_.contains(taxi))
					return new Result(new State(Coord.of(taxi.x, taxi.y - 1), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 3) // right
				if (taxi.y != 4 && !noRight.contains(taxi))
					return new Result(new State(Coord.of(taxi.x, taxi.y + 1), passenger, destination), -1);
				else
					return new Result(this, -1);
			else if (action == 4) // pickup
				if (passenger != onTaxi && Equals.ab(taxi, locations.get(passenger)))
					return new Result(new State(taxi, onTaxi, destination), -1);
				else
					return new Result(this, -10);
			else if (action == 5) // drop-off
				if (passenger == onTaxi && Equals.ab(taxi, locations.get(destination)))
					return new Result(this, 20, true);
				else
					return new Result(this, -10);
			else
				return fail();
		}

		private int encode() {
			var c = passenger;
			c = taxi.x + 5 * c;
			c = taxi.y + 5 * c;
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
		var passenger = c;
		return new State(Coord.of(taxix, taxiy), passenger, destination);
	}

	@Test
	public void test() {
		var initial = new State(Coord.of(3, 1), 0, 3);
		var alpha = .1d;
		var gamma = .6d;
		var epsilon = .1d;

		var q = new float[nStates][nActions];
		var nalpha = 1d - alpha;
		Result result;
		State state;
		float[] qst;

		for (var iter = 0; iter < 524288; iter++) {
			qst = q[(state = initial).encode()];

			do {
				var qst0 = qst;
				int action;

				if (random.nextDouble() < epsilon)
					action = random.nextInt(nActions);
				else
					action = getMaxActionValue(qst).t0;

				qst = q[(state = (result = state.move(action)).state).encode()];

				var adj = result.reward + gamma * getMaxActionValue(qst).t1;
				qst0[action] = (float) (nalpha * qst0[action] + alpha * adj);
			} while (!result.done);
		}

		var path = new ArrayList<String>();
		qst = q[(state = initial).encode()];

		do {
			path.add(state.taxi.toString());

			var action = getMaxActionValue(qst).t0;
			qst = q[(state = (result = state.move(action)).state).encode()];
		} while (!result.done);

		System.out.println("TAXI = " + path);
		assertTrue(path.size() == 13);
		assertNotNull(decode(0));
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
