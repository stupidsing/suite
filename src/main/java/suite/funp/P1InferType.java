package suite.funp;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.funp.Funp_.PN0;
import suite.funp.P0.FunpAddress;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpFixed;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPolyType;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpVariable;
import suite.immutable.IMap;
import suite.lp.Trail;
import suite.lp.doer.Binder;
import suite.lp.doer.Cloner;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.node.io.TermOp;

/**
 * Hindley-Milner type inference.
 *
 * @author ywsing
 */
public class P1InferType {

	private Atom ftBoolean = Atom.of("BOOLEAN");
	private Atom ftNumber = Atom.of("NUMBER");
	private Match defLambda = Suite.match("LAMBDA .0 .1");
	private Match defReference = Suite.match("REF .0");

	private Trail trail = new Trail();

	public Node infer(PN0 funp) {
		IMap<String, Node> env = IMap.<String, Node> empty() //
				.put(TermOp.PLUS__.getName(), Suite.substitute(".0 => .1 => .2", ftNumber, ftNumber, ftNumber));

		return infer(env, funp);
	}

	public int getTypeSize(Node node) {
		if (defLambda.apply(node) != null)
			return Funp_.pointerSize + Funp_.pointerSize;
		else if (defReference.apply(node) != null)
			return Funp_.pointerSize;
		else if (bind(ftBoolean, node))
			return Funp_.booleanSize;
		else if (bind(ftNumber, node))
			return Funp_.integerSize;
		else
			throw new RuntimeException(node.toString());
	}

	private Node infer(IMap<String, Node> env, PN0 np0) {
		if (np0 instanceof FunpAddress)
			return defReference.substitute(infer(((FunpAddress) np0).expr));
		else if (np0 instanceof FunpApply) {
			FunpApply f1 = (FunpApply) np0;
			Node[] m = defLambda.apply(infer(env, f1.lambda));
			if (!bind(m[0], infer(env, f1.value)))
				return m[1];
			else
				throw new RuntimeException();
		} else if (np0 instanceof FunpBoolean)
			return ftBoolean;
		else if (np0 instanceof FunpFixed) {
			FunpFixed f1 = (FunpFixed) np0;
			Node ftf = new Reference();
			if (bind(ftf, infer(env.put(f1.var, ftf), f1.expr)))
				return ftf;
			else
				throw new RuntimeException();
		} else if (np0 instanceof FunpIf) {
			FunpIf f1 = (FunpIf) np0;
			Node ft;
			if (bind(ftBoolean, infer(env, f1.if_)) && bind(ft = infer(env, f1.then), infer(env, f1.else_)))
				return ft;
			else
				throw new RuntimeException();
		} else if (np0 instanceof FunpLambda) {
			FunpLambda f1 = (FunpLambda) np0;
			Node ftv = new Reference();
			return defLambda.substitute(ftv, infer(env.put(f1.var, ftv), f1.expr));
		} else if (np0 instanceof FunpNumber)
			return ftNumber;
		else if (np0 instanceof FunpPolyType)
			return new Cloner().clone(infer(env, ((FunpPolyType) np0).expr));
		else if (np0 instanceof FunpReference)
			return defReference.apply(infer(((FunpReference) np0).expr))[0];
		else if (np0 instanceof FunpVariable)
			return env.get(((FunpVariable) np0).var);
		else
			throw new RuntimeException();
	}

	private boolean bind(Node ft0, Node ft1) {
		return Binder.bind(ft0, ft1, trail);
	}

}
