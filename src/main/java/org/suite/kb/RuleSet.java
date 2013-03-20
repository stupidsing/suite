package org.suite.kb;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.suite.doer.Cloner;
import org.suite.doer.Comparer;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.TermOp;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.node.Tree;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;

public class RuleSet implements RuleSearcher {

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

	private List<Rule> rules = new ArrayList<>();

	// Index rules by prototype.
	// Have to use a multi-map implementation that allow null keys.
	private ListMultimap<Prototype, Rule> index = ArrayListMultimap.create();

	public boolean importFrom(Node node) {
		Prover prover = new Prover(this);
		boolean result = true;
		Tree tree;

		while ((tree = Tree.decompose(node, TermOp.NEXT__)) != null) {
			Rule rule = formRule(tree.getLeft());

			if (rule.getHead() != Atom.NIL)
				addRule(rule);
			else {
				Node goal = new Generalizer().generalize(rule.getTail());
				result &= prover.prove(goal);
			}

			node = tree.getRight();
		}

		return result;
	}

	public void clear() {
		rules.clear();
		index.clear();
	}

	public void addRule(Node node) {
		addRule(formRule(node));
	}

	public void addRule(Rule rule) {
		rule = new Cloner().clone(rule);
		rules.add(rule);
		index.get(Prototype.get(rule)).add(rule);
	}

	public void addRuleToFront(Node node) {
		addRuleToFront(formRule(node));
	}

	public void addRuleToFront(Rule rule) {
		rule = new Cloner().clone(rule);
		rules.add(0, rule);
		index.get(Prototype.get(rule)).add(0, rule);
	}

	public void removeRule(Node node) {
		removeRule(formRule(node));
	}

	public void removeRule(Rule rule) {
		removeRule(rules, rule);
		removeRule(index.get(Prototype.get(rule)), rule);
	}

	private static void removeRule(List<Rule> rules, Rule rule) {
		Iterator<Rule> iter = rules.iterator();
		Comparer comparer = Comparer.comparer;

		while (iter.hasNext()) {
			Rule rule1 = iter.next();
			if (comparer.compare(rule.getHead(), rule1.getHead()) == 0
					&& comparer.compare(rule.getTail(), rule1.getTail()) == 0)
				iter.remove();
		}
	}

	public static Rule formRule(Node node) {
		Tree tree = Tree.decompose(node, TermOp.IS____);
		if (tree != null)
			return new Rule(tree.getLeft(), tree.getRight());
		else
			return new Rule(node, Atom.NIL);
	}

	public static Node formClause(Rule rule) {
		Node head = rule.getHead(), tail = rule.getTail();
		if (tail != Atom.NIL)
			return Tree.create(TermOp.IS____, head, tail);
		else
			return head;
	}

	/**
	 * Get list of rules for a certain query. Use prototype objects as the
	 * mapping key to do the matching behind.
	 */
	@Override
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

	@Override
	public List<Rule> getRules() {
		return rules;
	}

}
