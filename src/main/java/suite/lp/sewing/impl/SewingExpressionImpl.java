package suite.lp.sewing.impl;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.objectweb.asm.Opcodes;

import suite.Suite;
import suite.jdk.FunCreator;
import suite.jdk.FunExpression.FunExpr;
import suite.lp.predicate.EvalPredicates;
import suite.lp.sewing.SewingCloner;
import suite.lp.sewing.SewingExpression;
import suite.lp.sewing.VariableMapper.Env;
import suite.node.Int;
import suite.node.Node;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class SewingExpressionImpl implements SewingExpression {

	private static String key = "key";
	private static Fun<Map<String, Object>, Evaluate> compiledNumber = compileNumber(key);
	private static Map<Integer, Fun<Map<String, Object>, Evaluate>> compiledByOpcode = new ConcurrentHashMap<>();
	private SewingCloner sc;

	public interface Evaluate {
		public int evaluate(Env env);
	}

	public SewingExpressionImpl(SewingCloner sc) {
		this.sc = sc;
	}

	public Evaluate compile(Node node) {
		Node m[];

		if ((m = Suite.matcher(".0 + .1").apply(node)) != null)
			return compileOperator(m, Opcodes.IADD);
		else if ((m = Suite.matcher(".0 - .1").apply(node)) != null)
			return compileOperator(m, Opcodes.ISUB);
		else if ((m = Suite.matcher(".0 * .1").apply(node)) != null)
			return compileOperator(m, Opcodes.IMUL);
		else if ((m = Suite.matcher(".0 / .1").apply(node)) != null)
			return compileOperator(m, Opcodes.IDIV);
		else if ((m = Suite.matcher(".0 and .1").apply(node)) != null)
			return compileOperator(m, Opcodes.IAND);
		else if ((m = Suite.matcher(".0 or .1").apply(node)) != null)
			return compileOperator(m, Opcodes.IOR);
		else if ((m = Suite.matcher(".0 shl .1").apply(node)) != null)
			return compileOperator(m, Opcodes.ISHL);
		else if ((m = Suite.matcher(".0 shr .1").apply(node)) != null)
			return compileOperator(m, Opcodes.ISHR);
		else if (node instanceof Int)
			return compiledNumber.apply(Collections.singletonMap(key, ((Int) node).number));
		else {
			Fun<Env, Node> f = sc.compile(node);
			return env -> new EvalPredicates().evaluate(f.apply(env));
		}
	}

	private static Fun<Map<String, Object>, Evaluate> compileNumber(String key) {
		FunCreator<Evaluate> fc = FunCreator.of(Evaluate.class, Collections.singletonMap(key, int.class));
		return fc.create(fc.field(key));
	}

	private Evaluate compileOperator(Node m[], int opcode) {
		String e0 = "e0", e1 = "e1";

		Fun<Map<String, Object>, Evaluate> fc1 = compiledByOpcode //
				.computeIfAbsent(opcode, opcode_ -> {
					FunCreator<Evaluate> fc0 = FunCreator.of(Evaluate.class,
							Read.<String, Class<?>> empty2() //
									.cons(e0, Evaluate.class) //
									.cons(e1, Evaluate.class) //
									.toMap());
					FunExpr env_ = fc0.parameter(1);
					FunExpr v0 = fc0.field(e0).invoke("evaluate", env_);
					FunExpr v1 = fc0.field(e1).invoke("evaluate", env_);
					return fc0.create(fc0.bi(v0, v1, opcode));
				});

		return fc1.apply(Read.<String, Object> empty2() //
				.cons(e0, compile(m[0])) //
				.cons(e1, compile(m[1])) //
				.toMap());
	}

}
