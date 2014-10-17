package suite.cli;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.stream.Collectors;

import suite.Suite;
import suite.fp.LazyFunInterpreter;
import suite.fp.PrecompileMain;
import suite.lp.Configuration.ProverConfig;
import suite.lp.doer.Prover;
import suite.lp.kb.Rule;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.lp.search.SewingProverBuilder;
import suite.lp.sewing.SewingGeneralizer;
import suite.lp.sewing.SewingGeneralizer.Generalization;
import suite.lp.sewing.SewingProver;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.PrettyPrinter;
import suite.node.io.TermOp;
import suite.util.CommandUtil;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.Pair;
import suite.util.To;
import suite.util.Util;

/**
 * Command line interface dispatcher.
 *
 * @author ywsing
 */
public class CommandDispatcher {

	private CommandOptions opt;
	private RuleSet ruleSet = Suite.createRuleSet();
	private Builder builderLevel2 = null;

	private enum InputType {
		EVALUATE("\\"), //
		EVALUATEDO("\\d"), //
		EVALUATEDOCHARS("\\dc"), //
		EVALUATEDOSTR("\\ds"), //
		EVALUATEINTERPRET("\\i"), //
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
	}

	public boolean importFiles(List<String> importFilenames) throws IOException {
		boolean code = true;
		code &= Suite.importPath(ruleSet, "auto.sl");
		for (String importFilename : importFilenames)
			code &= Suite.importFile(ruleSet, importFilename);
		return code;
	}

	public boolean dispatchCommand(String input, Writer writer) throws IOException {
		return Util.isBlank(input) || dispatchCommand0(input, writer);
	}

	private boolean dispatchCommand0(String input, Writer writer) throws IOException {
		PrintWriter pw = new PrintWriter(writer);
		boolean code = true;

		Pair<InputType, String> pair = new CommandUtil<>(InputType.values()).recognize(input);
		InputType type = pair.t0;
		input = pair.t1.trim();

		if (input.endsWith("#"))
			input = Util.substr(input, 0, -1);

		Node node = Suite.parse(input.trim());

		switch (type) {
		case EVALUATE:
			pw.println(Formatter.dump(evaluateFunctional(node)));
			break;
		case EVALUATEDO:
			node = Suite.applyPerform(node, Atom.of("any"));
			pw.println(Formatter.dump(evaluateFunctional(node)));
			break;
		case EVALUATEDOCHARS:
			node = Suite.applyPerform(node, Suite.parse("[data^Chars]"));
			printEvaluated(writer, node);
			break;
		case EVALUATEDOSTR:
			node = Suite.applyPerform(node, Atom.of("string"));
			printEvaluated(writer, Suite.applyWriter(node));
			break;
		case EVALUATEINTERPRET:
			System.out.println(new LazyFunInterpreter().lazy(node).get());
			break;
		case EVALUATESTR:
			node = Suite.substitute("string of .0", node);
			printEvaluated(writer, Suite.applyWriter(node));
			break;
		case EVALUATETYPE:
			pw.println(Formatter.dump(Suite.evaluateFunType(opt.fcc(node))));
			break;
		case FACT:
			ruleSet.addRule(Rule.formRule(node));
			break;
		case OPTION:
			Source<String> source = To.source(("-" + input).split(" "));
			String option;
			while ((option = source.source()) != null)
				opt.processOption(option, source);
			break;
		case PRETTYPRINT:
			pw.println(new PrettyPrinter().prettyPrint(node));
			break;
		case QUERY:
			code = query(new InterpretedProverBuilder(opt.pc(ruleSet)), ruleSet, node);
			break;
		case QUERYCOMPILED:
			code = query(CompiledProverBuilder.level1(opt.pc(ruleSet)), ruleSet, node);
			break;
		case QUERYCOMPILED2:
			if (builderLevel2 == null)
				builderLevel2 = CompiledProverBuilder.level2(opt.pc(ruleSet));
			code = query(builderLevel2, ruleSet, node);
			break;
		case QUERYELABORATE:
			elaborate(node, new Prover(opt.pc(ruleSet))::prove);
			break;
		case QUERYSEWING:
			code = query(new SewingProverBuilder(opt.pc(ruleSet)), ruleSet, node);
			break;
		case QUERYSEWINGELAB:
			elaborate(node, n -> new SewingProver(ruleSet).compile(n).apply(new ProverConfig(ruleSet)));
		}

		pw.flush();

		return code;
	}

	private void elaborate(Node node, Sink<Node> sink) {
		int count[] = { 0 };
		Generalization generalization = SewingGeneralizer.process(node);
		node = generalization.node;

		Node elab = new Data<Source<Boolean>>(() -> {
			String dump = generalization.dumpVariables();
			if (!dump.isEmpty())
				opt.prompt().println(dump);

			count[0]++;
			return Boolean.FALSE;
		});

		sink.sink(Tree.of(TermOp.AND___, node, elab));

		if (count[0] == 1)
			opt.prompt().println(count[0] + " solution\n");
		else
			opt.prompt().println(count[0] + " solutions\n");
	}

	public boolean dispatchEvaluate(List<String> inputs) {
		return evaluateFunctional(parseNode(inputs)) == Atom.TRUE;
	}

	public boolean dispatchFilter(List<String> inputs, Reader reader, Writer writer) throws IOException {
		boolean isChars = opt.isChars();
		Node node = parseNode(inputs);
		node = isChars ? Suite.applyCharsReader(node, reader) : Suite.applyStringReader(node, reader);
		if (opt.isDo())
			node = Suite.applyPerform(node, isChars ? Suite.parse("[data^Chars]") : Atom.of("string"));
		if (isChars)
			printEvaluated(writer, node);
		else
			printEvaluated(writer, Suite.applyWriter(node));
		return true;
	}

	public boolean dispatchPrecompile(List<String> filenames) {
		boolean result = true;
		for (String filename : filenames)
			result &= Suite.precompile(filename, opt.pc(null));
		return result;
	}

	public boolean dispatchPrecompileAll(List<String> filenames) {
		if (filenames.size() == 0)
			try (PrecompileMain precompileMain = new PrecompileMain()) {
				return precompileMain.precompile();
			}
		else
			throw new RuntimeException("No parameter is allowed");
	}

	public boolean dispatchProve(List<String> inputs) throws IOException {
		String in = parseInput(inputs);
		RuleSet ruleSet = Suite.createRuleSet();
		return Suite.importPath(ruleSet, "auto.sl") && Suite.proveLogic(ruleSet, in);
	}

	public boolean dispatchType(List<String> inputs) throws IOException {
		Node node = parseNode(inputs);
		System.out.println(Formatter.dump(Suite.evaluateFunType(opt.fcc(node))));
		return true;
	}

	private Node parseNode(List<String> inputs) {
		return Suite.parse(parseInput(inputs));
	}

	private String parseInput(List<String> inputs) {
		return inputs.stream().collect(Collectors.joining(" "));
	}

	private void printEvaluated(Writer writer, Node node) throws IOException {
		Suite.evaluateFunToWriter(opt.fcc(node), writer);
		writer.flush();
	}

	private boolean query(Builder builder, RuleSet ruleSet, Node node) {
		boolean result = Suite.proveLogic(builder, ruleSet, node);
		opt.prompt().println(yesNo(result));
		return result;
	}

	private Node evaluateFunctional(Node node) {
		return Suite.evaluateFun(opt.fcc(node));
	}

	private String yesNo(boolean b) {
		return b ? "Yes\n" : "No\n";
	}

}
