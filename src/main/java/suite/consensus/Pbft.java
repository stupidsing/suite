package suite.consensus;

import primal.MoreVerbs.Read;
import primal.Verbs;
import primal.fp.Funs.Source;
import primal.os.Log_;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.entry;
import static primal.statics.Fail.fail;

/**
 * https://www.geeksforgeeks.org/practical-byzantine-fault-tolerancepbft/
 *
 * @author ywsing
 */
public class Pbft {

	private int maxTraitors = 1;
	private int nGenerals = 4; // must be <= 3 * maxTraitors + 1
	private int maxPrepareDuration = 5; // T

	private Source<String> decisionf;
	private List<General> generals;

	public interface General {
		public Map<General, Message> round(int epoch, Map<General, Message> recvs);
	}

	public class Message {
		public final Mt type;
		public final Object decision;

		public Message(Mt type, Object decision) {
			this.type = type;
			this.decision = decision;
		}

		public boolean isDecision(Object decision) {
			return Verbs.Equals.ab(this.decision, decision);
		}
	}

	public enum Mt {
		PREP, PREPARE, COMMIT, REPLY, VIEWCHANGE, NEWVIEW,
	}

	public void play() {
		var messages = new HashMap<General, Map<General, Message>>();

		for (var round = 0; true; round++) {
			var messages1 = new HashMap<General, Map<General, Message>>();

			for (var general : generals) {
				var recvs = messages.get(general);
				var sends = general.round(round, recvs != null ? recvs : Map.ofEntries());
				sends.forEach((to, m) -> messages1.computeIfAbsent(to, g -> new HashMap<>()).put(general, m));
			}

			messages = messages1;
		}
	}

	private enum State { //
		PRE____, //
		PREPARE, //
		COMMIT_, //
		VIEWCHG, //
	}

	public class GoodGeneral implements General { // a good one
		private int leaderIndex;
		private General leader;
		private Object decision;
		private int prepareTime;
		private State state = State.PRE____;

		@Override
		public Map<General, Message> round(int epoch, Map<General, Message> recvs) {
			var fromLeader = recvs.get(leader);

			if (state == State.PRE____)
				if (this == leader)
					return Read //
							.from(generals) //
							.filter(r -> r != this) //
							.map2(r -> r, r -> new Message(Mt.PREP, decisionf.g())) //
							.toMap();
				else if (fromLeader.type == Mt.PREP) {
					state = State.PREPARE;
					decision = fromLeader.decision;
					prepareTime = epoch;
					return Read //
							.from(generals) //
							.filter(r -> r != this) //
							.map2(r -> r, r -> new Message(Mt.PREPARE, decision)) //
							.toMap();
				} else
					return Map.ofEntries();
			else if (state == State.PREPARE)
				if (nGenerals - maxTraitors <= Read.from2(recvs).values().filter(m -> m.type == Mt.PREPARE && m.isDecision(decision)).size()) {
					state = State.COMMIT_;
					return Read //
							.from(generals) //
							.filter(r -> r != this) //
							.map2(r -> r, r -> new Message(Mt.COMMIT, decisionf.g())) //
							.toMap();
				} else if (prepareTime + maxPrepareDuration < epoch)
					return viewChange();
				else
					return Map.ofEntries();
			else if (state == State.COMMIT_)
				if (nGenerals - maxTraitors <= Read.from2(recvs).values().filter(m -> m.type == Mt.COMMIT && m.isDecision(decision)).size()) {
					Log_.info("EXECUTE " + decision);
					state = State.PRE____;
					return Map.ofEntries(entry(null, new Message(Mt.REPLY, decision)));
				} else if (prepareTime + maxPrepareDuration < epoch)
					return viewChange();
				else
					return Map.ofEntries();
			else if (state == State.VIEWCHG)
				if (this == leader)
					if (nGenerals - maxTraitors <= Read.from2(recvs).values().filter(m -> m.type == Mt.VIEWCHANGE && m.isDecision(leader)).size())
						return Read //
								.from(generals) // //
								.filter(r -> r != this) //
								.map2(r -> r, r -> new Message(Mt.NEWVIEW, decision)) //
								.toMap();
					else
						return Map.ofEntries();
				else if (fromLeader.type == Mt.NEWVIEW) {
					state = State.PREPARE;
					decision = fromLeader.decision;
					prepareTime = epoch;
					return Read //
							.from(generals) //
							.filter(r -> r != this) //
							.map2(r -> r, r -> new Message(Mt.PREPARE, decision)) //
							.toMap();
				} else
					return Map.ofEntries();
			else
				return fail();
		}

		private Map<General, Message> viewChange() {
			state = State.VIEWCHG;
			leaderIndex = (leaderIndex + 1) % nGenerals;
			leader = generals.get(leaderIndex);
			return Read //
					.from(generals) // //
					.filter(r -> r != this) //
					.map2(r -> r, r -> new Message(Mt.VIEWCHANGE, Integer.toString(leaderIndex))) //
					.toMap();
		}
	}

}
