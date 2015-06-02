package suite.lp.predicate;

import java.util.HashMap;
import java.util.Map;

import suite.lp.doer.Prover;
import suite.lp.predicate.PredicateUtil.BuiltinPredicate;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Operator;
import suite.node.io.TermOp;

public class SystemPredicates {

	private Map<String, BuiltinPredicate> predicates = new HashMap<>();

	private Prover prover;

	private static EvalPredicates evalPredicates = new EvalPredicates();
	private static FindPredicates findPredicates = new FindPredicates();
	private static FormatPredicates formatPredicates = new FormatPredicates();
	private static InternPredicates internPredicates = new InternPredicates();
	private static IoPredicates ioPredicates = new IoPredicates();
	private static RuleSetPredicates ruleSetPredicates = new RuleSetPredicates();

	public SystemPredicates(Prover prover) {
		this.prover = prover;

		addPredicate("cut.begin", cutBegin);
		addPredicate("cut.end", cutEnd);
		addPredicate("if", ifPredicate);
		addPredicate("not", not);
		addPredicate("once", once);
		addPredicate("system.predicate", systemPredicate);

		addPredicate("bound", evalPredicates.bound);
		addPredicate("clone", evalPredicates.clone);
		addPredicate("complexity", evalPredicates.complexity);
		addPredicate("contains", evalPredicates.contains);
		addPredicate("dkv", evalPredicates.dictKeyValue);
		addPredicate("eval.fun", evalPredicates.evalFun);
		addPredicate("eval.js", evalPredicates.evalJs);
		addPredicate(TermOp.LE____, evalPredicates.compare);
		addPredicate(TermOp.LT____, evalPredicates.compare);
		addPredicate(TermOp.NOTEQ_, evalPredicates.notEquals);
		addPredicate(TermOp.GE____, evalPredicates.compare);
		addPredicate(TermOp.GT____, evalPredicates.compare);
		addPredicate("generalize", evalPredicates.generalize);
		addPredicate("graph.generalize", evalPredicates.graphGeneralize);
		addPredicate("graph.specialize", evalPredicates.graphSpecialize);
		addPredicate("hash", evalPredicates.hash);
		addPredicate("hash.id", evalPredicates.hashId);
		addPredicate("is.cyclic", evalPredicates.isCyclic);
		addPredicate("length", evalPredicates.length);
		addPredicate("let", evalPredicates.let);
		addPredicate("random", evalPredicates.randomPredicate);
		addPredicate("replace", evalPredicates.replace);
		addPredicate("rewrite", evalPredicates.rewrite);
		addPredicate("same", evalPredicates.same);
		addPredicate("specialize", evalPredicates.specialize);
		addPredicate("temp", evalPredicates.temp);
		addPredicate("tree", evalPredicates.tree);

		addPredicate("find.all", findPredicates.findAll);
		addPredicate("find.all.memoized", findPredicates.findAllMemoized);
		addPredicate("find.all.memoized.clear", findPredicates.findAllMemoizedClear);

		addPredicate("char.ascii", formatPredicates.charAscii);
		addPredicate("concat", formatPredicates.concat);
		addPredicate("graphize", formatPredicates.graphize);
		addPredicate("is.atom", formatPredicates.isAtom);
		addPredicate("is.int", formatPredicates.isInt);
		addPredicate("is.string", formatPredicates.isString);
		addPredicate("is.tree", formatPredicates.isTree);
		addPredicate("parse", formatPredicates.parse);
		addPredicate("persist.load", formatPredicates.persistLoad);
		addPredicate("persist.save", formatPredicates.persistSave);
		addPredicate("pretty.print", formatPredicates.prettyPrint);
		addPredicate("pretty.print.new", formatPredicates.prettyPrintNew);
		addPredicate("rpn", formatPredicates.rpnPredicate);
		addPredicate("starts.with", formatPredicates.startsWith);
		addPredicate("string.length", formatPredicates.stringLength);
		addPredicate("substring", formatPredicates.substring);
		addPredicate("to.atom", formatPredicates.toAtom);
		addPredicate("to.dump.string", formatPredicates.toDumpString);
		addPredicate("to.int", formatPredicates.toInt);
		addPredicate("to.string", formatPredicates.toString);
		addPredicate("treeize", formatPredicates.treeize);
		addPredicate("trim", formatPredicates.trim);

		addPredicate("intern.map.clear", internPredicates.internMapClear);
		addPredicate("intern.map.contains", internPredicates.internMapContains);
		addPredicate("intern.map.put", internPredicates.internMapPut);
		addPredicate("intern.tree", internPredicates.internTree);

		addPredicate("dump", ioPredicates.dump);
		addPredicate("dump.stack", ioPredicates.dumpStack);
		addPredicate("exec", ioPredicates.exec);
		addPredicate("exit", ioPredicates.exit);
		addPredicate("file.exists", ioPredicates.fileExists);
		addPredicate("file.read", ioPredicates.fileRead);
		addPredicate("file.write", ioPredicates.fileWrite);
		addPredicate("home.dir", ioPredicates.homeDir);
		addPredicate("log", ioPredicates.log);
		addPredicate("nl", ioPredicates.nl);
		addPredicate("read.line", ioPredicates.readLine);
		addPredicate("sink", ioPredicates.sink);
		addPredicate("source", ioPredicates.source);
		addPredicate("throw", ioPredicates.throwPredicate);
		addPredicate("try", ioPredicates.tryPredicate);
		addPredicate("write", ioPredicates.write(System.out));
		addPredicate("write.error", ioPredicates.write(System.err));

		addPredicate("assert", ruleSetPredicates.assertz);
		addPredicate("asserta", ruleSetPredicates.asserta);
		addPredicate("assertz", ruleSetPredicates.assertz);
		addPredicate("import", ruleSetPredicates.importPredicate);
		addPredicate("import.path", ruleSetPredicates.importPath);
		addPredicate("list", ruleSetPredicates.list);
		addPredicate("retract", ruleSetPredicates.retract);
		addPredicate("retract.all", ruleSetPredicates.retractAll);
		addPredicate("rules", ruleSetPredicates.getAllRules);
		addPredicate("with", ruleSetPredicates.with);
	}

	public Boolean call(Node query) {
		BuiltinPredicate predicate;
		Tree tree;
		String name = null;
		Node pass = query;

		if (query instanceof Atom) {
			name = ((Atom) query).name;
			pass = Atom.NIL;
		} else if ((tree = Tree.decompose(query)) != null)
			if (tree.getOperator() != TermOp.TUPLE_)
				name = tree.getOperator().getName();
			else {
				Node left = tree.getLeft();

				if (left instanceof Atom) {
					name = ((Atom) left).name;
					pass = tree.getRight();
				}
			}

		predicate = name != null ? get(name) : null;
		return predicate != null ? predicate.prove(prover, pass) : null;
	}

	public BuiltinPredicate get(String name) {
		return predicates.get(name);
	}

	private void addPredicate(Operator operator, BuiltinPredicate pred) {
		predicates.put(operator.getName(), pred);
	}

	private void addPredicate(String name, BuiltinPredicate pred) {
		predicates.put(name, pred);
	}

	private BuiltinPredicate cutBegin = (prover, ps) -> prover.bind(ps, prover.getAlternative());

	private BuiltinPredicate cutEnd = (prover, ps) -> {
		prover.setAlternative(ps.finalNode());
		return true;
	};

	private BuiltinPredicate ifPredicate = (prover, ps) -> {
		Node params[] = Tree.getParameters(ps, 3);
		boolean result = PredicateUtil.tryProve(prover, prover1 -> prover1.prove0(params[0]));
		Node n = result ? params[1] : params[2];
		prover.setRemaining(Tree.of(TermOp.AND___, n, prover.getRemaining()));
		return true;
	};

	private BuiltinPredicate not = (prover, ps) -> PredicateUtil.tryProve(prover, prover1 -> !prover1.prove0(ps));

	private BuiltinPredicate once = (prover, ps) -> new Prover(prover).prove0(ps);

	private BuiltinPredicate systemPredicate = (prover, ps) -> {
		ps = ps.finalNode();
		Atom atom = ps instanceof Atom ? (Atom) ps : null;
		String name = atom != null ? atom.name : null;
		return name != null && predicates.containsKey(name);
	};

}
