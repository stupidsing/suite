package org.suite.search;

import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.util.FunUtil.Sink;

public class ProveSearch {

	public static final Atom in = Atom.create(".in");
	public static final Atom out = Atom.create(".out");

	public interface Builder {
		public Finder build(RuleSet ruleSet, Node goal);
	}

	public interface Finder {
		public void find(Node in, Sink<Node> sink);
	}

}
