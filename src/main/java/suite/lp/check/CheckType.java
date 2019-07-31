package suite.lp.check;

import static primal.statics.Fail.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.adt.Pair;
import suite.Suite;
import primal.adt.IdentityKey;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Generalizer;
import suite.lp.kb.Prototype;
import suite.lp.kb.Rule;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.SwitchNode;
import suite.node.io.TermOp;
import suite.node.tree.TreeTuple;
import suite.node.util.TreeUtil;
import suite.streamlet.Read;

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
						var type0 = types.computeIfAbsent(key, k -> new Reference());
						var type1 = getType(ps[i]);
						bind(type0, type1);
					}
			} catch (Exception ex) {
				fail("in predicate " + prototype, ex);
			}
		});

		trail.unwindAll();
	}

	private Node getType(Node data) {
		return new SwitchNode<Node>(data //
		).match(Atom.NIL, () -> {
			return Suite.substitute("_;");
		}).applyIf(Atom.class, n -> {
			return getEnumType(n, Atom.NIL);
		}).applyIf(Reference.class, n -> {
			return variableTypes.computeIfAbsent(IdentityKey.of(n), k -> new Reference()).finalNode();
		}).applyTree((op, l, r) -> {
			if (op == TermOp.AND___) {
				var type = Suite.substitute(".0;", getType(l));
				bind(type, getType(r));
				return type;
			} else if (op == TermOp.TUPLE_)
				if (l instanceof Atom) {
					var ps = TreeUtil.elements(r, TreeUtil.nElements(r));
					return getEnumType(l, TreeUtil.buildUp(TermOp.TUPLE_, Read.from(ps).map(this::getType).toList()));
				} else
					return new Reference(); // free type
			else {
				var name = Atom.of(op.name_());
				var tl = getType(l);
				var tr = getType(r);
				return getEnumType(name, TreeTuple.of(tl, tr));
			}
		}).applyIf(Node.class, n -> {
			return Atom.of(data.getClass().getSimpleName());
		}).result();
	}

	private Node getEnumType(Node name, Node type1) {
		var map = new HashMap<Node, Reference>();
		map.put(name, Reference.of(type1));
		return Dict.of(map);
	}

	private void bind(Node type0, Node type1) {
		if (!Binder.bind(type0, type1, trail))
			fail("type mismatch between " + type0 + " and " + type1);
	}

}
