package suite.cli;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import suite.Suite;
import suite.fp.FunCompilerConfig;
import suite.lp.doer.Generalizer;
import suite.lp.doer.Prover;
import suite.lp.doer.ProverConfig;
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
import suite.node.io.TermParser;
import suite.node.io.TermParser.TermOp;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

/**
 * Command line interface dispatcher.
 * 
 * @author ywsing
 */
public class CommandDispatcher {

	private CommandOption opt;

	private Builder builderL2 = null;

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
	}

	public CommandDispatcher(CommandOption opt) {
		this.opt = opt;
	}

	public boolean dispatchCommand(String input, Writer writer) throws IOException {
		if (!Util.isBlank(input))
			return dispatchCommand0(input, writer);
		else
			return true;
	}

	private boolean dispatchCommand0(String input, Writer writer) throws IOException {
		PrintWriter pw = new PrintWriter(writer);
		FunCompilerConfig fcc = opt.getFcc();
		ProverConfig pc = fcc.getProverConfig();
		RuleSet rs = pc.ruleSet();
		boolean code = true;

		InputType type = null;

		commandFound: for (int i = Math.min(3, input.length()); i >= 0; i--) {
			String starts = input.substring(0, i);

			for (InputType inputType : InputType.values())
				if (Util.equals(starts, inputType.prefix)) {
					type = inputType;
					input = input.substring(i);
					break commandFound;
				}
		}

		input = input.trim();
		if (input.endsWith("#"))
			input = Util.substr(input, 0, -1);

		final int count[] = { 0 };
		Node node = new TermParser().parse(input.trim());

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
			node = Suite.substitute(".0 as string", node);
			printEvaluatedString(writer, node);
			break;
		case EVALUATETYPE:
			fcc.setNode(node);
			pw.println(Formatter.dump(Suite.evaluateFunType(fcc)));
			break;
		case FACT:
			Suite.addRule(rs, node);
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
			code = query(new InterpretedProverBuilder(pc), rs, node);
			break;
		case QUERYCOMPILED:
			code = query(CompiledProverBuilder.level1(pc, fcc.isDumpCode()), rs, node);
			break;
		case QUERYCOMPILED2:
			if (builderL2 == null)
				builderL2 = CompiledProverBuilder.level2(pc, fcc.isDumpCode());
			code = query(builderL2, rs, node);
			break;
		case QUERYELABORATE:
			final Generalizer generalizer = new Generalizer();
			node = generalizer.generalize(node);
			Prover prover = new Prover(pc);

			Node elab = new Data<Source<Boolean>>(new Source<Boolean>() {
				public Boolean source() {
					String dump = generalizer.dumpVariables();
					if (!dump.isEmpty())
						opt.prompt().println(dump);

					count[0]++;
					return Boolean.FALSE;
				}
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
		if (files.size() == 1)
			try (FileInputStream is = new FileInputStream(files.get(0))) {
				Node node = Suite.parse(is);
				return evaluateFunctional(node) == Atom.TRUE;
			}
		else
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
		FunCompilerConfig fcc = opt.getFcc();
		fcc.setNode(node);
		return Suite.evaluateFun(fcc);
	}

	private void evaluateFunctionalToWriter(Node node, Writer writer) throws IOException {
		FunCompilerConfig fcc = opt.getFcc();
		fcc.setNode(node);
		Suite.evaluateFunToWriter(fcc, writer);
	}

	private String yesNo(boolean b) {
		return b ? "Yes\n" : "No\n";
	}

}
