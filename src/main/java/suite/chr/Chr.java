//package suite.chr;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import suite.immutable.ImmutableMap;
//import suite.immutable.ImmutableSet;
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
//		private ImmutableMap<Prototype, ImmutableSet<Node>> facts = new ImmutableMap<>();
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
//		private ImmutableSet<Node> givensMatched = new ImmutableSet<>();
//		private ImmutableSet<Node> ifsMatched = new ImmutableSet<>();
//
//		public Match() {
//			this(new ImmutableSet<Node>(), new ImmutableSet<Node>());
//		}
//
//		public Match(ImmutableSet<Node> givensMatched, ImmutableSet<Node> ifsMatched) {
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
//							final ImmutableSet<Node> facts = state.facts.get(prototype);
//							Source<Node> nodes = FunUtil.asSource(facts.iterator());
//
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
