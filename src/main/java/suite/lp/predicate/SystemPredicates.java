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
		addPredicate("generalize", evalPredicates.generalize);
		addPredicate("graph.bind", evalPredicates.graphBind);
		addPredicate("graph.generalize", evalPredicates.graphGeneralize);
		addPredicate("graph.replace", evalPredicates.graphReplace);
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
		addPredicate("suspend", findPredicates.suspend);

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
		addPredicate("file.time", ioPredicates.fileTime);
		addPredicate("file.write", ioPredicates.fileWrite);
		addPredicate("jar", ioPredicates.jar);
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
		addPredicate("import.url", ruleSetPredicates.importUrl);
		addPredicate("list", ruleSetPredicates.list);
		addPredicate("retract", ruleSetPredicates.retract);
		addPredicate("retract.all", ruleSetPredicates.retractAll);
		addPredicate("rules", ruleSetPredicates.getAllRules);
		addPredicate("with", ruleSetPredicates.with);
	}

	public Boolean call(Node query) {
		BuiltinPredicate predicate;
		Tree tree;
		Operator op;
		Node left;
		String name = null;
		Node pass = null;

		if (query instanceof Atom) {
			name = Atom.name(query);
			pass = Atom.NIL;
		} else if ((tree = Tree.decompose(query)) != null)
			if ((op = tree.getOperator()) != TermOp.TUPLE_) {
				name = op.name_();
				pass = query;
			} else if ((left = tree.getLeft()) instanceof Atom) {
				name = Atom.name(left);
				pass = tree.getRight();
			}

		predicate = name != null ? get(name) : null;
		return predicate != null ? predicate.prove(prover, pass) : null;
	}

	public BuiltinPredicate get(String name) {
		return predicates.get(name);
	}

	private void addPredicate(Operator operator, BuiltinPredicate pred) {
		predicates.put(operator.name_(), pred);
	}

	private void addPredicate(String name, BuiltinPredicate pred) {
		predicates.put(name, pred);
	}

	private BuiltinPredicate cutBegin = (prover, ps) -> prover.bind(ps, prover.getAlternative());

	private BuiltinPredicate cutEnd = (prover, ps) -> {
		prover.setAlternative(ps);
		return true;
	};

	private BuiltinPredicate ifPredicate = PredicateUtil.p3((prover, p0, p1, p2) -> {
		var b = PredicateUtil.tryProve(prover, prover1 -> prover1.prove0(p0));
		prover.setRemaining(Tree.ofAnd(b ? p1 : p2, prover.getRemaining()));
		return true;
	});

	private BuiltinPredicate not = PredicateUtil.p1((prover, p0) -> PredicateUtil.tryProve(prover, prover1 -> !prover1.prove0(p0)));

	private BuiltinPredicate once = PredicateUtil.p1((prover, p0) -> new Prover(prover).prove0(p0));

	private BuiltinPredicate systemPredicate = PredicateUtil.p1((prover, p0) -> {
		var atom = p0 instanceof Atom ? (Atom) p0 : null;
		var name = atom != null ? atom.name : null;
		return name != null && predicates.containsKey(name);
	});

}
