package suite.lp.compile.impl;

import java.util.HashMap;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.ClonerFactory;
import suite.lp.doer.ClonerFactory.Clone_;
import suite.lp.doer.EvaluatorFactory;
import suite.node.Int;
import suite.node.Node;
import suite.node.io.SwitchNode;
import suite.node.util.TreeUtil;
import suite.util.FunUtil.Iterate;

public class CompileExpressionImpl implements EvaluatorFactory {

	private static FunFactory f = new FunFactory();

	private ClonerFactory clonerFactory;

	public CompileExpressionImpl(ClonerFactory clonerFactory) {
		this.clonerFactory = clonerFactory;
	}

	public Evaluate_ evaluator(Node node) {
		FunCreator<Evaluate_> fc = FunCreator.of(Evaluate_.class);

		return fc.create(new Iterate<>() {
			private FunExpr env;

			public FunExpr apply(FunExpr env) {
				this.env = env;
				return compile_(node);
			}

			private FunExpr compile_(Node node) {
				return new SwitchNode<FunExpr>(node //
				).match(".0 + .1", m -> {
					return compileOperator(m, "+");
				}).match(".0 - .1", m -> {
					return compileOperator(m, "-");
				}).match(".0 * .1", m -> {
					return compileOperator(m, "*");
				}).match(".0 / .1", m -> {
					return compileOperator(m, "/");
				}).match(".0 and .1", m -> {
					return compileOperator(m, "&&");
				}).match(".0 or .1", m -> {
					return compileOperator(m, "||");
				}).match(".0 shl .1", m -> {
					return compileOperator(m, "<<");
				}).match(".0 shr .1", m -> {
					return compileOperator(m, ">>");
				}).applyIf(Int.class, i -> {
					return f.int_(i.number);
				}).applyIf(Node.class, i -> {
					Clone_ n_ = clonerFactory.cloner(node);
					Evaluate_ evaluate = env -> TreeUtil.evaluate(n_.apply(env));
					return f.object(evaluate).invoke("evaluate", env);
				}).nonNullResult();
			}

			private FunExpr compileOperator(Node[] m, String op) {
				FunExpr fe0 = compile_(m[0]);
				FunExpr fe1 = compile_(m[1]);
				return f.bi(op, fe0, fe1);
			}
		}).apply(new HashMap<>());
	}

}
