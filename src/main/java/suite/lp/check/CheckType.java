package suite.lp.check;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Suite;
import suite.adt.IdentityKey;
import suite.adt.pair.Pair;
import suite.lp.Trail;
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
import suite.node.tree.TreeTuple;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;
import suite.util.Fail;

public class CheckType {

	private CheckLogicUtil clu = new CheckLogicUtil();
	private Trail trail = new Trail();
	private Map<IdentityKey<Node>, Reference> variableTypes = new HashMap<>();

	public void check(List<Rule> rules) {
		var nElementsByPrototype = clu.getNumberOfElements(rules);
		var types = new HashMap<Pair<Prototype, Integer>, Reference>();

		Read.from(rules).concatMap(rule -> {
			var generalizer = new Generalizer();
			var head = generalizer.generalize(rule.head);
			var tail = generalizer.generalize(rule.tail);
			return clu.scan(tail).cons(head);
		}).forEach(pred -> {
			var prototype = Prototype.of(pred);
			var nElements = prototype != null ? nElementsByPrototype.get(prototype) : null;
			var ps = nElements != null ? TreeUtil.elements(pred, nElements) : new Node[0];

			try {
				if (nElements != null)
					for (var i = 1; i < nElements; i++) {
						var key = Pair.of(prototype, i);
						var p = ps[i];
						var type0 = types.computeIfAbsent(key, k -> new Reference());
						var type1 = getType(p);
						bind(type0, type1);
					}
			} catch (Exception ex) {
				Fail.t("in predicate " + prototype, ex);
			}
		});

		trail.unwindAll();
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
				var name = tree.getLeft();
				if (name instanceof Atom) {
					var node = tree.getRight();
					var ps = TreeUtil.elements(node, TreeUtil.nElements(node));
					type = getEnumType(name, Tree.of(TermOp.TUPLE_, Read.from(ps).map(this::getType).toList()));
				} else
					return new Reference(); // free type
			} else {
				var name = Atom.of(tree.getOperator().name_());
				var lt = getType(tree.getLeft());
				var rt = getType(tree.getRight());
				type = getEnumType(name, TreeTuple.of(lt, rt));
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
		var dict = Dict.of();
		dict.map.put(name, Reference.of(type1));
		return dict;
	}

	private void bind(Node type0, Node type1) {
		if (!Binder.bind(type0, type1, trail))
			Fail.t("type mismatch between " + type0 + " and " + type1);
	}

}
