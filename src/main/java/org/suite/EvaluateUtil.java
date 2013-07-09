package org.suite;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.instructionexecutor.ExpandUtil;
import org.instructionexecutor.FunInstructionExecutor;
import org.suite.doer.Cloner;
import org.suite.doer.ProverConfig;
import org.suite.kb.RuleSet;
import org.suite.kb.RuleSetUtil;
import org.suite.node.Atom;
import org.suite.node.Node;
import org.suite.search.CompiledProverBuilder.CompiledProverBuilderLevel1;
import org.suite.search.InterpretedProverBuilder;
import org.suite.search.ProverBuilder.Builder;
import org.suite.search.ProverBuilder.Finder;
import org.util.FunUtil;
import org.util.FunUtil.Sink;
import org.util.FunUtil.Source;
import org.util.Util;

public class EvaluateUtil {

	public boolean proveLogic(Node lp) {
		Builder builder = new CompiledProverBuilderLevel1(new ProverConfig(), false);
		return proveLogic(builder, RuleSetUtil.create(), lp);
	}

	public boolean proveLogic(RuleSet rs, Node lp) {
		return proveLogic(new InterpretedProverBuilder(), rs, lp);
	}

	public boolean proveLogic(Builder builder, RuleSet rs, Node lp) {
		Node goal = Suite.substitute(".0, sink ()", lp);
		return !evaluateLogic(builder, rs, goal).isEmpty();
	}

	public List<Node> evaluateLogic(Builder builder, RuleSet rs, Node lp) {
		return collect(builder.build(rs, lp), Atom.NIL);
	}

	public Node evaluateFun(FunCompilerConfig fcc) {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			Node result = executor.execute();
			return fcc.isLazy() ? ExpandUtil.expand(result, executor.getUnwrapper()) : result;
		}
	}

	public void evaluateFunIo(FunCompilerConfig fcc, Reader reader, Writer writer) throws IOException {
		try (FunInstructionExecutor executor = configureFunExecutor(fcc)) {
			executor.executeIo(reader, writer);
		}
	}

	private FunInstructionExecutor configureFunExecutor(FunCompilerConfig fcc) {
		RuleSet rs = Suite.funCompilerRuleSet(fcc.isLazy());
		Atom mode = Atom.create(fcc.isLazy() ? "LAZY" : "EAGER");

		Node node = Suite.substitute("" //
				+ "source .in" //
				+ ", compile-function .0 .in .out" //
				+ (fcc.isDumpCode() ? ", pretty.print .out" : "") //
				+ ", sink .out", mode);

		Node code = doFcc(rs, node, fcc);

		if (code != null) {
			FunInstructionExecutor e = new FunInstructionExecutor(code);
			e.setProverConfig(fcc.getProverConfig());
			return e;
		} else
			throw new RuntimeException("Function compilation error");
	}

	public Node evaluateFunType(FunCompilerConfig fcc) {
		RuleSet rs = Suite.funCompilerRuleSet();

		Node node = Suite.parse("" //
				+ "source .in" //
				+ ", fc-parse .in .p" //
				+ ", infer-type-rule .p ()/()/() .tr/() .t" //
				+ ", resolve-type-rules .tr" //
				+ ", fc-parse-type .out .t" //
				+ ", sink .out");

		Node type = doFcc(rs, node, fcc);

		if (type != null)
			return type;
		else
			throw new RuntimeException("Type inference error");
	}

	private Node doFcc(RuleSet rs, Node compileNode, FunCompilerConfig fcc) {
		Builder builder = new InterpretedProverBuilder(fcc.getProverConfig());
		Finder finder = builder.build(rs, compileNode);
		List<Node> nodes = collect(finder, appendLibraries(fcc));
		return nodes.size() == 1 ? nodes.get(0).finalNode() : null;
	}

	private Node appendLibraries(FunCompilerConfig fcc) {
		Node node = fcc.getNode();

		for (String library : fcc.getLibraries())
			if (!Util.isBlank(library))
				node = Suite.substitute("using .0 >> .1", Atom.create(library), node);

		return node;
	}

	private List<Node> collect(Finder finder, Node in) {
		final List<Node> nodes = new ArrayList<>();

		Source<Node> source = FunUtil.source(in);
		Sink<Node> sink = new Sink<Node>() {
			public void sink(Node node) {
				nodes.add(new Cloner().clone(node));
			}
		};

		finder.find(source, sink);
		return nodes;
	}

}
