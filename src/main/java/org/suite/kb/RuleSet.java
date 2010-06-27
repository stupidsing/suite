package org.suite.kb;

import java.util.ArrayList;
import java.util.List;

import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class RuleSet {

	public static class Rule {
		private Node head, tail;

		public Rule(Node head, Node tail) {
			this.head = head;
			this.tail = tail;
		}

		public Node getHead() {
			return head;
		}

		public Node getTail() {
			return tail;
		}
	}

	private List<Rule> rules = new ArrayList<Rule>();

	// Index rules by prototype.
	// Have to use a multi-map implementation that allow null keys.
	private ListMultimap<Prototype, Rule> index = ArrayListMultimap.create();

	public boolean importFrom(Node node) {
		Prover prover = new Prover(this);
		boolean result = true;
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.NEXT__)) != null) {
			Rule rule = formRule(tree.getLeft());

			if (rule.getHead() != Atom.nil)
				addRule(rule);
			else
				result &= prover.prove(rule.getTail());

			node = tree.getRight();
		}

		return result;
	}

	public void addRule(Node node) {
		addRule(formRule(node));
	}

	public void addRule(Rule rule) {
		rules.add(rule);
		index.get(Prototype.get(rule)).add(rule);
	}

	public static Rule formRule(Node node) {
		Tree tree = Tree.decompose(node, TermOp.IS____);
		if (tree != null)
			return new Rule(tree.getLeft(), tree.getRight());
		else
			return new Rule(node, Atom.nil);
	}

	public static Node formClause(Rule rule) {
		Node head = rule.getHead(), tail = rule.getTail();
		if (tail != Atom.nil)
			return new Tree(TermOp.IS____, head, tail);
		else
			return head;
	}

	/**
	 * Get list of rules for a certain query. Use prototype objects as the
	 * mapping key to do the matching behind.
	 * 
	 * @param head
	 * @return
	 */
	public List<Rule> getRules(Node head) {
		return getRules(Prototype.get(head));
	}

	public List<Rule> getRules(Prototype proto) {

		// If the query is "un-prototype-able," or the rule set contains
		// "un-prototype-able" entries, full traversal is required
		if (proto != null && !index.containsKey(null))
			return index.get(proto);
		else
			return getRules();
	}

	public List<Rule> getRules() {
		return rules;
	}

}
