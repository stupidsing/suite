package suite.lp.predicate;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import suite.lp.doer.Prover;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;

public class SystemPredicates {

	public interface SystemPredicate {
		public boolean prove(Prover prover, Node ps);
	}

	private Map<String, SystemPredicate> predicates = new HashMap<>();

	private Prover prover;

	public SystemPredicates(Prover prover) {
		this.prover = prover;

		addPredicate("cut.begin", cutBegin);
		addPredicate("cut.end", cutEnd);
		addPredicate("not", not);
		addPredicate("once", once);
		addPredicate("system.predicate", systemPredicate);

		addPredicate("bound", EvalPredicates.bound);
		addPredicate("clone", EvalPredicates.clone);
		addPredicate("complexity", EvalPredicates.complexity);
		addPredicate("contains", EvalPredicates.contains);
		addPredicate("eval.fun", EvalPredicates.evalFun);
		addPredicate("eval.js", EvalPredicates.evalJs);
		addPredicate(TermOp.LE____, EvalPredicates.compare);
		addPredicate(TermOp.LT____, EvalPredicates.compare);
		addPredicate(TermOp.NOTEQ_, EvalPredicates.notEquals);
		addPredicate(TermOp.GE____, EvalPredicates.compare);
		addPredicate(TermOp.GT____, EvalPredicates.compare);
		addPredicate("generalize", EvalPredicates.generalize);
		addPredicate("hash", EvalPredicates.hash);
		addPredicate("hash.id", EvalPredicates.hashId);
		addPredicate("is.cyclic", EvalPredicates.isCyclic);
		addPredicate("let", EvalPredicates.let);
		addPredicate("random", EvalPredicates.randomPredicate);
		addPredicate("replace", EvalPredicates.replace);
		addPredicate("rewrite", EvalPredicates.rewrite);
		addPredicate("same", EvalPredicates.same);
		addPredicate("specialize", EvalPredicates.specialize);
		addPredicate("temp", EvalPredicates.temp);
		addPredicate("tree", EvalPredicates.tree);
		addPredicate("tree.intern", EvalPredicates.treeIntern);

		addPredicate("find.all", FindPredicates.findAll);
		addPredicate("find.all.memoized", FindPredicates.findAllMemoized);
		addPredicate("find.all.memoized.clear", FindPredicates.findAllMemoizedClear);

		addPredicate("char.ascii", FormatPredicates.charAscii);
		addPredicate("concat", FormatPredicates.concat);
		addPredicate("graphize", FormatPredicates.graphize);
		addPredicate("is.atom", FormatPredicates.isAtom);
		addPredicate("is.int", FormatPredicates.isInt);
		addPredicate("is.string", FormatPredicates.isString);
		addPredicate("is.tree", FormatPredicates.isTree);
		addPredicate("parse", FormatPredicates.parse);
		addPredicate("persist.load", FormatPredicates.persistLoad);
		addPredicate("persist.save", FormatPredicates.persistSave);
		addPredicate("pretty.print", FormatPredicates.prettyPrint);
		addPredicate("rpn", FormatPredicates.rpnPredicate);
		addPredicate("starts.with", FormatPredicates.startsWith);
		addPredicate("string.length", FormatPredicates.stringLength);
		addPredicate("substring", FormatPredicates.substring);
		addPredicate("to.atom", FormatPredicates.toAtom);
		addPredicate("to.dump.string", FormatPredicates.toDumpString);
		addPredicate("to.int", FormatPredicates.toInt);
		addPredicate("to.string", FormatPredicates.toString);
		addPredicate("treeize", FormatPredicates.treeize);
		addPredicate("trim", FormatPredicates.trim);

		addPredicate("intern.map.clear", InternMapPredicates.internMapClear);
		addPredicate("intern.map.contains", InternMapPredicates.internMapContains);
		addPredicate("intern.map.put", InternMapPredicates.internMapPut);

		addPredicate("dump", IoPredicates.dump);
		addPredicate("dump.stack", IoPredicates.dumpStack);
		addPredicate("exec", IoPredicates.exec);
		addPredicate("exit", IoPredicates.exit);
		addPredicate("file.exists", IoPredicates.fileExists);
		addPredicate("file.read", IoPredicates.fileRead);
		addPredicate("file.write", IoPredicates.fileWrite);
		addPredicate("home.dir", IoPredicates.homeDir);
		addPredicate("log", IoPredicates.log);
		addPredicate("nl", IoPredicates.nl);
		addPredicate("sink", IoPredicates.sink);
		addPredicate("source", IoPredicates.source);
		addPredicate("throw", IoPredicates.throwPredicate);
		addPredicate("write", IoPredicates.write(System.out));
		addPredicate("write.error", IoPredicates.write(System.err));

		addPredicate("assert", RuleSetPredicates.assertz);
		addPredicate("asserta", RuleSetPredicates.asserta);
		addPredicate("assertz", RuleSetPredicates.assertz);
		addPredicate("import", RuleSetPredicates.importPredicate);
		addPredicate("import.path", RuleSetPredicates.importPath);
		addPredicate("list", RuleSetPredicates.list);
		addPredicate("retract", RuleSetPredicates.retract);
		addPredicate("retract.all", RuleSetPredicates.retractAll);
		addPredicate("rules", RuleSetPredicates.getAllRules);
		addPredicate("with", RuleSetPredicates.with);
	}

	public Boolean call(Node query) {
		SystemPredicate predicate;
		Tree tree;
		String name = null;
		Node pass = query;

		if (query instanceof Atom) {
			name = ((Atom) query).getName();
			pass = Atom.NIL;
		} else if ((tree = Tree.decompose(query)) != null)
			if (tree.getOperator() != TermOp.TUPLE_)
				name = tree.getOperator().getName();
			else {
				Node left = tree.getLeft();

				if (left instanceof Atom) {
					name = ((Atom) left).getName();
					pass = tree.getRight();
				}
			}

		predicate = name != null ? predicates.get(name) : null;
		return predicate != null ? predicate.prove(prover, pass) : null;
	}

	public static SystemPredicate predicate(Sink<Node> fun) {
		return (prover, ps) -> {
			fun.sink(ps.finalNode());
			return true;
		};
	}

	public static SystemPredicate boolPredicate(Predicate<Node> fun) {
		return (prover, ps) -> fun.test(ps.finalNode());
	}

	public static SystemPredicate funPredicate(Fun<Node, Node> fun) {
		return (prover, ps) -> {
			Node params[] = Tree.getParameters(ps, 2);
			Node p0 = params[0].finalNode(), p1 = params[1].finalNode();
			return prover.bind(p1, fun.apply(p0));
		};
	}

	private void addPredicate(Operator operator, SystemPredicate pred) {
		predicates.put(operator.getName(), pred);
	}

	private void addPredicate(String name, SystemPredicate pred) {
		predicates.put(name, pred);
	}

	private SystemPredicate cutBegin = (prover, ps) -> {
		return prover.bind(ps, prover.getAlternative());
	};

	private SystemPredicate cutEnd = (prover, ps) -> {
		prover.setAlternative(ps.finalNode());
		return true;
	};

	private SystemPredicate not = (prover, ps) -> {
		Prover prover1 = new Prover(prover);
		boolean result = !prover1.prove0(ps);
		if (!result) // Roll back bindings if overall goal is failed
			prover1.undoAllBinds();
		return result;
	};

	private SystemPredicate once = (prover, ps) -> {
		return new Prover(prover).prove0(ps);
	};

	private SystemPredicate systemPredicate = (prover, ps) -> {
		ps = ps.finalNode();
		Atom atom = ps instanceof Atom ? (Atom) ps : null;
		String name = atom != null ? atom.getName() : null;
		return name != null && predicates.containsKey(name);
	};

}
