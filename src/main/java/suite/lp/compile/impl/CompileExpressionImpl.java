package suite.lp.compile.impl;

import java.util.Map;

import primal.fp.Funs.Iterate;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.ClonerFactory;
import suite.lp.doer.EvaluatorFactory;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.node.util.TreeUtil;

public class CompileExpressionImpl implements EvaluatorFactory {

	private static FunFactory f = new FunFactory();

	private ClonerFactory clonerFactory;

	public CompileExpressionImpl(ClonerFactory clonerFactory) {
		this.clonerFactory = clonerFactory;
	}

	public Evaluate_ evaluator(Node node) {
		var fc = FunCreator.of(Evaluate_.class, false);

		return fc.create(new Iterate<>() {
			private FunExpr env;

			public FunExpr apply(FunExpr env) {
				this.env = env;
				return compile_(node);
			}

			private FunExpr compile_(Node node) {
				return new SwitchNode<FunExpr>(node
				).match(".0 + .1", (a, b) -> {
					return compileOperator(a, b, "+");
				}).match(".0 - .1", (a, b) -> {
					return compileOperator(a, b, "-");
				}).match(".0 * .1", (a, b) -> {
					return compileOperator(a, b, "*");
				}).match(".0 / .1", (a, b) -> {
					return compileOperator(a, b, "/");
				}).match(".0 and .1", (a, b) -> {
					return compileOperator(a, b, "&&");
				}).match(".0 or .1", (a, b) -> {
					return compileOperator(a, b, "||");
				}).match(".0 shl .1", (a, b) -> {
					return compileOperator(a, b, "<<");
				}).match(".0 shr .1", (a, b) -> {
					return compileOperator(a, b, ">>");
				}).applyIf(Int.class, i -> {
					return f.int_(i.number);
				}).applyIf(Node.class, i -> {
					var n_ = clonerFactory.cloner(node);
					Evaluate_ evaluate = env -> TreeUtil.evaluate(n_.apply(env));
					return f.object(evaluate).invoke("evaluate", env);
				}).nonNullResult();
			}

			private FunExpr compileOperator(Node a, Node b, String op) {
				var fe0 = compile_(a);
				var fe1 = compile_(b);
				return f.bi(op, fe0, fe1);
			}
		}).apply(Map.ofEntries());
	}

}
