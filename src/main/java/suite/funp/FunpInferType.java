package suite.funp;

import suite.BindArrayUtil.Match;
import suite.Suite;
import suite.funp.FunpK.FunpAddress;
import suite.funp.FunpK.FunpApply;
import suite.funp.FunpK.FunpBoolean;
import suite.funp.FunpK.FunpFixed;
import suite.funp.FunpK.FunpIf;
import suite.funp.FunpK.FunpLambda;
import suite.funp.FunpK.FunpNumber;
import suite.funp.FunpK.FunpPolyType;
import suite.funp.FunpK.FunpReference;
import suite.funp.FunpK.FunpVariable;
import suite.funp.Funp_.Funp;
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
public class FunpInferType {

	private Atom ftBoolean = Atom.of("BOOLEAN");
	private Atom ftNumber = Atom.of("NUMBER");
	private Match defLambda = Suite.match("LAMBDA .0 .1");
	private Match defReference = Suite.match("REF .0");

	private Trail trail = new Trail();

	public Node infer(Funp funp) {
		IMap<String, Node> env = IMap.<String, Node> empty() //
				.put(TermOp.PLUS__.getName(), Suite.substitute(".0 => .1 => .2", ftNumber, ftNumber, ftNumber));

		return infer(env, funp);
	}

	public int getTypeSize(Node node) {
		if (defLambda.apply(node) != null)
			return Funp_.pointerSize;
		else if (defReference.apply(node) != null)
			return Funp_.pointerSize;
		else if (bind(ftBoolean, node))
			return 1;
		else if (bind(ftNumber, node))
			return Funp_.intSize;
		else
			throw new RuntimeException(node.toString());
	}

	private Node infer(IMap<String, Node> env, Funp funp) {
		if (funp instanceof FunpAddress)
			return defReference.substitute(infer(((FunpAddress) funp).expr));
		else if (funp instanceof FunpApply) {
			FunpApply f1 = (FunpApply) funp;
			Node[] m = defLambda.apply(infer(env, f1.lambda));
			if (!bind(m[0], infer(env, f1.value)))
				return m[1];
			else
				throw new RuntimeException();
		} else if (funp instanceof FunpBoolean)
			return ftBoolean;
		else if (funp instanceof FunpFixed) {
			FunpFixed f1 = (FunpFixed) funp;
			Node ftf = new Reference();
			if (bind(ftf, infer(env.put(f1.var, ftf), f1.expr)))
				return ftf;
			else
				throw new RuntimeException();
		} else if (funp instanceof FunpIf) {
			FunpIf f1 = (FunpIf) funp;
			Node ft;
			if (bind(ftBoolean, infer(env, f1.if_)) && bind(ft = infer(env, f1.then), infer(env, f1.else_)))
				return ft;
			else
				throw new RuntimeException();
		} else if (funp instanceof FunpLambda) {
			FunpLambda f1 = (FunpLambda) funp;
			Node ftv = new Reference();
			return defLambda.substitute(ftv, infer(env.put(f1.var, ftv), f1.expr));
		} else if (funp instanceof FunpNumber)
			return ftNumber;
		else if (funp instanceof FunpPolyType)
			return new Cloner().clone(infer(env, ((FunpPolyType) funp).expr));
		else if (funp instanceof FunpReference)
			return defReference.apply(infer(((FunpReference) funp).expr))[0];
		else if (funp instanceof FunpVariable)
			return env.get(((FunpVariable) funp).var);
		else
			throw new RuntimeException();
	}

	private boolean bind(Node ft0, Node ft1) {
		return Binder.bind(ft0, ft1, trail);
	}

}
