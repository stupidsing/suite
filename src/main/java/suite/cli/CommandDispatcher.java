package suite.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import primal.MoreVerbs.Read;
import primal.Verbs.Is;
import primal.Verbs.Substring;
import primal.Verbs.Take;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import suite.Suite;
import suite.fp.InterpretFunEager;
import suite.fp.InterpretFunLazy;
import suite.fp.InterpretFunLazy0;
import suite.lp.Configuration.ProverCfg;
import suite.lp.compile.impl.CompileGeneralizerImpl;
import suite.lp.doer.Prover;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.SewingProverBuilder;
import suite.lp.sewing.impl.SewingProverImpl;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.pp.PrettyPrinter;
import suite.util.CommandUtil;

/**
 * Command line interface dispatcher.
 *
 * @author ywsing
 */
public class CommandDispatcher {

	private CommandOptions opt;
	private RuleSet ruleSet;
	private Builder builderLevel2 = null;

	private enum InputType {
		EVALUATE("\\"), //
		EVALUATEDO("\\d"), //
		EVALUATEDOCHARS("\\dc"), //
		EVALUATEDOSTR("\\ds"), //
		EVALUATEEFI("\\i"), //
		EVALUATELFI0("\\j"), //
		EVALUATELFI("\\k"), //
		EVALUATESTR("\\s"), //
		EVALUATETYPE("\\t"), //
		FACT(""), //
		OPTION("-"), //
		PRETTYPRINT("\\p"), //
		QUERY("?"), //
		QUERYCOMPILED("/l"), //
		QUERYCOMPILED2("/ll"), //
		QUERYELABORATE("/"), //
		QUERYSEWING("?s"), //
		QUERYSEWINGELAB("/s"), //
		RESET("\\reset"), //
		;

		private String prefix;

		private InputType(String prefix) {
			this.prefix = prefix;
		}

		public String toString() {
			return prefix;
		}
	}

	public CommandDispatcher(CommandOptions opt) {
		this.opt = opt;
		ruleSet = Suite.newRuleSet(opt.getImports());
	}

	public boolean importFiles(List<String> importFilenames) throws IOException {
		var code = true;
		code &= ruleSet.importPath("auto.sl");
		for (var importFilename : importFilenames)
			code &= ruleSet.importFile(importFilename);
		return code;
	}

	public boolean dispatchCommand(String input, Writer writer) throws IOException {
		return Is.blank(input) || dispatchCommand_(input, writer);
	}

	private boolean dispatchCommand_(String input, Writer writer) throws IOException {
		var pw = new PrintWriter(writer);
		var code = true;

		var pair = new CommandUtil<>(InputType.values()).recognize(input);
		var type = pair.k;
		input = pair.v.trim();

		if (input.endsWith("#"))
			input = Substring.of(input, 0, -1);

		var node = Suite.parse(input.trim());

		switch (type) {
		case EVALUATE -> pw.println(Formatter.dump(evaluateFunctional(node)));
		case EVALUATEDO -> pw.println(Formatter.dump(evaluateFunctional(Suite.applyPerform(node, Atom.of("any")))));
		case EVALUATEDOCHARS -> printEvaluated(writer, Suite.applyPerform(node, Suite.parse("[n^Chars]")));
		case EVALUATEDOSTR -> printEvaluated(writer, Suite.applyWriter(Suite.applyPerform(node, Atom.of("string"))));
		case EVALUATEEFI -> {
			var efi = new InterpretFunEager();
			efi.setLazyify(opt.isLazy());
			pw.println(efi.eager(node));
		}
		case EVALUATELFI0 -> pw.println(new InterpretFunLazy0().lazy(node).get());
		case EVALUATELFI -> pw.println(new InterpretFunLazy().lazy(node).get());
		case EVALUATESTR -> printEvaluated(writer, Suite.applyWriter(Suite.substitute("string of .0", node)));
		case EVALUATETYPE -> pw.println(Formatter.dump(Suite.evaluateFunType(opt.fcc(node))));
		case FACT -> ruleSet.addRule(Rule.of(node));
		case OPTION -> {
			var source = Take.from(("-" + input).split(" "));
			String option;
			while ((option = source.g()) != null)
				opt.processOption(option, source);
		}
		case PRETTYPRINT -> pw.println(new PrettyPrinter().prettyPrint(node));
		case QUERY -> code = query(new InterpretedProverBuilder(opt.pc(ruleSet)), ruleSet, node);
		case QUERYCOMPILED -> code = query(CompiledProverBuilder.level1(opt.pc(ruleSet)), ruleSet, node);
		case QUERYCOMPILED2 -> {
			if (builderLevel2 == null)
				builderLevel2 = CompiledProverBuilder.level2(opt.pc(ruleSet));
			code = query(builderLevel2, ruleSet, node);
		}
		case QUERYELABORATE -> elaborate(node, new Prover(opt.pc(ruleSet))::prove);
		case QUERYSEWING -> code = query(new SewingProverBuilder(opt.pc(ruleSet)), ruleSet, node);
		case QUERYSEWINGELAB -> elaborate(node,
				n -> new SewingProverImpl(ruleSet).prover(n).test(new ProverCfg(ruleSet)));
		case RESET -> {
			ruleSet = Suite.newRuleSet();
			importFiles(List.of());
		}
		}

		pw.flush();

		return code;
	}

	private void elaborate(Node node0, Sink<Node> sink) {
		var count = new int[] { 0 };
		var ne = new CompileGeneralizerImpl().g(node0).g();
		var node1 = ne.node;

		var elab = new Data<Source<Boolean>>(() -> {
			var dump = ne.dumpVariables();
			if (!dump.isEmpty())
				opt.prompt().println(dump);

			count[0]++;
			return Boolean.FALSE;
		});

		sink.f(Tree.ofAnd(node1, elab));

		opt.prompt().println(count[0] + " solution" + (count[0] == 1 ? "" : "s") + "\n");
	}

	public boolean dispatchEvaluate(List<String> inputs) {
		return evaluateFunctional(parseNode(inputs)) == Atom.TRUE;
	}

	public boolean dispatchFilter(List<String> inputs, Reader reader, Writer writer) throws IOException {
		var isChars = opt.isChars();
		var node = parseNode(inputs);
		node = isChars ? Suite.applyCharsReader(node, reader) : Suite.applyStringReader(node, reader);
		if (opt.isDo())
			node = Suite.applyPerform(node, isChars ? Suite.parse("[n^Chars]") : Atom.of("string"));
		printEvaluated(writer, isChars ? node : Suite.applyWriter(node));
		return true;
	}

	public boolean dispatchPrecompile(List<String> filenames) {
		return Read.from(filenames).map(filename -> Suite.precompile(filename, opt.pc(null))).isAll(b -> b);
	}

	public boolean dispatchProve(List<String> inputs) throws IOException {
		var in = parseInput(inputs);
		var ruleSet = Suite.newRuleSet();
		return ruleSet.importPath("auto.sl") && Suite.proveLogic(ruleSet, in);
	}

	public boolean dispatchType(List<String> inputs) throws IOException {
		var node = parseNode(inputs);
		System.out.println(Formatter.dump(Suite.evaluateFunType(opt.fcc(node))));
		return true;
	}

	private Node parseNode(List<String> inputs) {
		return Suite.parse(parseInput(inputs));
	}

	private String parseInput(List<String> inputs) {
		return Read.from(inputs).toJoinedString(" ");
	}

	private void printEvaluated(Writer writer, Node node) throws IOException {
		Suite.evaluateFunToWriter(opt.fcc(node), writer);
		writer.flush();
	}

	private boolean query(Builder builder, RuleSet ruleSet, Node node) {
		var b = Suite.proveLogic(builder, ruleSet, node);
		opt.prompt().println(yesNo(b));
		return b;
	}

	private Node evaluateFunctional(Node node) {
		return Suite.evaluateFun(opt.fcc(node));
	}

	private String yesNo(boolean b) {
		return b ? "Yes\n" : "No\n";
	}

}
