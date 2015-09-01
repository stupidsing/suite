package suite.lp.checker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.IdentityKey;
import suite.lp.Journal;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;

public class TypeChecker {

	private CheckerUtil checkerUtil = new CheckerUtil();
	private Journal journal = new Journal();
	private Map<IdentityKey<Node>, Reference> variableTypes = new HashMap<>();

	private class PredicateParameter {
		private Prototype prototype;
		private int index;

		private PredicateParameter(Prototype prototype, int index) {
			this.prototype = prototype;
			this.index = index;
		}

		public int hashCode() {
			return prototype.hashCode() + index;
		}

		public boolean equals(Object object) {
			if (object.getClass() == PredicateParameter.class) {
				PredicateParameter other = (PredicateParameter) object;
				return prototype.equals(other.prototype) && index == other.index;
			} else
				return false;
		}
	}

	public void check(List<Rule> rules) {
		Map<Prototype, Integer> nElementsByPrototype = checkerUtil.getNumberOfElements(rules);

		Map<PredicateParameter, Reference> types = new HashMap<>();

		Read.from(rules).concatMap(rule -> {
			Generalizer generalizer = new Generalizer();
			Node head = generalizer.generalize(rule.head);
			Node tail = generalizer.generalize(rule.tail);
			return checkerUtil.scan(tail).cons(head);
		}).forEach(pred -> {
			Prototype prototype = Prototype.of(pred);
			Integer nElements = prototype != null ? nElementsByPrototype.get(prototype) : null;
			Node ps[] = nElements != null ? TreeUtil.getElements(pred, nElements) : new Node[0];

			try {
				if (nElements != null)
					for (int i = 1; i < nElements; i++) {
						PredicateParameter key = new PredicateParameter(prototype, i);
						Node p = ps[i];
						Node type0 = types.computeIfAbsent(key, k -> new Reference());
						Node type1 = getType(p);
						bind(type0, type1);
					}
			} catch (Exception ex) {
				throw new RuntimeException("In predicate " + prototype, ex);
			}
		});

		journal.undoAllBinds();
	}

	private Node getType(Node data) {
		Node type;
		Tree tree;

		if (data instanceof Reference)
			type = variableTypes.computeIfAbsent(IdentityKey.of(data), k -> new Reference()).finalNode();
		else if ((tree = Tree.decompose(data)) != null)
			if (tree.getOperator() == TermOp.AND___) {
				type = Suite.substitute(".0;", getType(tree.getLeft()));
				bind(type, getType(tree.getRight()));
			} else if (tree.getOperator() == TermOp.TUPLE_) {
				Node name = tree.getLeft();
				if (name instanceof Atom) {
					Node node = tree.getRight();
					Node ps[] = TreeUtil.getElements(node, TreeUtil.getNumberOfElements(node));
					type = getEnumType(name, Tree.of(TermOp.TUPLE_, Read.from(ps).map(this::getType).toList()));
				} else
					return new Reference(); // Free type
			} else {
				Atom name = Atom.of(tree.getOperator().getName());
				Node lt = getType(tree.getLeft());
				Node rt = getType(tree.getRight());
				type = getEnumType(name, Tree.of(TermOp.TUPLE_, lt, rt));
			}
		else if (data == Atom.NIL)
			type = Suite.substitute("_;");
		else if (data instanceof Atom)
			type = getEnumType(data, Atom.NIL);
		else
			type = Atom.of(data.getClass().getSimpleName());

		return type;
	}

	private Node getEnumType(Node name, Node type1) {
		HashMap<Node, Reference> map = new HashMap<>();
		map.put(name, Reference.of(type1));
		return new Dict(map);
	}

	private void bind(Node type0, Node type1) {
		if (!Binder.bind(type0, type1, journal))
			throw new RuntimeException("Type mismatch between " + type0 + " and " + type1);
	}

}
