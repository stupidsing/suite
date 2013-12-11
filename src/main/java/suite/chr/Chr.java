//package suite.chr;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import suite.fp.RbTreeMap;
//import suite.fp.RbTreeSet;
//import suite.lp.kb.Prototype;
//import suite.node.Node;
//import suite.util.FunUtil;
//import suite.util.FunUtil.Fun;
//import suite.util.FunUtil.Source;
//
//import com.google.common.collect.ArrayListMultimap;
//import com.google.common.collect.ListMultimap;
//
///**
// * Constraint handling rules implementation.
// * 
// * @author ywsing
// */
//public class Chr {
//
//	private ListMultimap<Prototype, Node> facts = ArrayListMultimap.create();
//	private List<Rule> rules = new ArrayList<>();
//
//	private class State {
//		private RbTreeMap<Prototype, RbTreeSet<Node>> facts = new RbTreeMap<>();
//	}
//
//	private class Rule {
//		private List<Node> givens = new ArrayList<>();
//		private List<Node> ifs = new ArrayList<>();
//		private List<Node> thens = new ArrayList<>();
//		private Node when;
//	}
//
//	private class Match {
//		private RbTreeSet<Node> givensMatched = new RbTreeSet<>();
//		private RbTreeSet<Node> ifsMatched = new RbTreeSet<>();
//
//		public Match() {
//			this(new RbTreeSet<Node>(), new RbTreeSet<Node>());
//		}
//
//		public Match(RbTreeSet<Node> givensMatched, RbTreeSet<Node> ifsMatched) {
//			this.givensMatched = givensMatched;
//			this.ifsMatched = ifsMatched;
//		}
//	}
//
//	public void add(Node fact) {
//		facts.put(getPrototype(fact), fact);
//	}
//
//	public void remove(Node fact) {
//		facts.remove(getPrototype(fact), fact);
//	}
//
//	public void chr(final State state) {
//		Source<Rule> source = FunUtil.asSource(rules);
//
//		FunUtil.filter(new Fun<Rule, Boolean>() {
//			public Boolean apply(Rule rule) {
//				Source<State> states = FunUtil.asSource(state);
//
//				for (final Node if_ : rule.ifs)
//					states = FunUtil.concat(FunUtil.map(new Fun<State, Source<State>>() {
//						public Source<State> apply(final State state) {
//							final Prototype prototype = getPrototype(if_);
//							final RbTreeSet<Node> facts = state.facts.get(prototype);
//							Source<Node> nodes = FunUtil.asSource(facts.iterator());
//							return FunUtil.map(new Fun<Node, State>() {
//								public State apply(Node node) {
//									State state1 = new State();
//									state1.facts = state.facts.replace(prototype, facts.remove(node));
//									return state1;
//								}
//							}, nodes);
//						}
//					}, states));
//
//				return null;
//			}
//		}, source);
//	}
//
//	private Prototype getPrototype(Node node) {
//		return Prototype.get(node);
//	}
//
//}
