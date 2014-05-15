package suite.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import suite.Suite;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.kb.RuleSet;
import suite.lp.search.CompiledProverBuilder;
import suite.lp.search.InterpretedProverBuilder;
import suite.lp.search.ProverBuilder.Builder;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.Formatter;
import suite.node.io.PrettyPrinter;
import suite.node.io.TermOp;
import suite.util.CommandUtil;
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

	private CommandOption opt;
	private RuleSet ruleSet = Suite.createRuleSet();
	private Builder builderLevel2 = null;

	private enum InputType {
		EVALUATE("\\"), //
		EVALUATEDO("\\d"), //
		EVALUATEDOSTR("\\ds"), //
		EVALUATESTR("\\s"), //
		EVALUATETYPE("\\t"), //
		FACT(""), //
		OPTION("-"), //
		PRETTYPRINT("\\p"), //
		QUERY("?"), //
		QUERYCOMPILED("/l"), //
		QUERYCOMPILED2("/ll"), //
		QUERYELABORATE("/"), //
		;

		private String prefix;

		private InputType(String prefix) {
			this.prefix = prefix;
		}

		public String toString() {
			return prefix;
		}
	}

	public CommandDispatcher(CommandOption opt) {
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

		int count[] = { 0 };
		Node node = Suite.parse(input.trim());

		switch (type) {
		case EVALUATE:
			pw.println(Formatter.dump(evaluateFunctional(node)));
			break;
		case EVALUATEDO:
			node = Suite.applyDo(node, Atom.create("any"));
			pw.println(Formatter.dump(evaluateFunctional(node)));
			break;
		case EVALUATEDOSTR:
			node = Suite.applyDo(node, Atom.create("string"));
			printEvaluatedString(writer, node);
			break;
		case EVALUATESTR:
			node = Suite.substitute("string of .0", node);
			printEvaluatedString(writer, node);
			break;
		case EVALUATETYPE:
			pw.println(Formatter.dump(Suite.evaluateFunType(opt.fcc(node))));
			break;
		case FACT:
			Suite.addRule(ruleSet, node);
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
			code = query(CompiledProverBuilder.level1(opt.pc(ruleSet), opt.isDumpCode()), ruleSet, node);
			break;
		case QUERYCOMPILED2:
			if (builderLevel2 == null)
				builderLevel2 = CompiledProverBuilder.level2(opt.pc(ruleSet), opt.isDumpCode());
			code = query(builderLevel2, ruleSet, node);
			break;
		case QUERYELABORATE:
			Generalizer generalizer = new Generalizer();
			node = generalizer.generalize(node);
			Prover prover = new Prover(opt.pc(ruleSet));

			Node elab = new Data<Source<Boolean>>(() -> {
				String dump = generalizer.dumpVariables();
				if (!dump.isEmpty())
					opt.prompt().println(dump);

				count[0]++;
				return Boolean.FALSE;
			});

			prover.prove(Tree.create(TermOp.AND___, node, elab));

			if (count[0] == 1)
				opt.prompt().println(count[0] + " solution\n");
			else
				opt.prompt().println(count[0] + " solutions\n");
		}

		pw.flush();

		return code;
	}

	public boolean dispatchLogical(List<String> files) throws IOException {
		boolean result = true;

		RuleSet ruleSet = Suite.createRuleSet();
		result &= Suite.importResource(ruleSet, "auto.sl");

		for (String file : files)
			result &= Suite.importFile(ruleSet, file);

		return result;
	}

	public boolean dispatchFilter(List<String> inputs, Reader reader, Writer writer) throws IOException {
		StringBuilder sb = new StringBuilder();
		for (String input : inputs)
			sb.append(input + " ");

		Node node = Suite.applyReader(reader, Suite.parse(sb.toString()));
		evaluateFunctionalToWriter(node, writer);
		return true;
	}

	public boolean dispatchFunctional(List<String> files) throws IOException {
		if (files.size() == 1) {
			Node node = Suite.parse(To.string(new File(files.get(0))));
			return evaluateFunctional(node) == Atom.TRUE;
		} else
			throw new RuntimeException("Only one evaluation is allowed");
	}

	private void printEvaluatedString(Writer writer, Node node) throws IOException {
		evaluateFunctionalToWriter(node, writer);
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

	private void evaluateFunctionalToWriter(Node node, Writer writer) throws IOException {
		Suite.evaluateFunToWriter(opt.fcc(node), writer);
	}

	private String yesNo(boolean b) {
		return b ? "Yes\n" : "No\n";
	}

}
