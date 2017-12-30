package suite.lp.compile.impl;

import java.util.HashMap;

import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.doer.ProverConstant;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.streamlet.Read;
import suite.util.FunUtil.Fun;

public class CompileGeneralizerImpl extends VariableMapper implements GeneralizerFactory {

	private static FunFactory f = new FunFactory();

	public Fun<Env, Node> compile(Node node) {
		@SuppressWarnings({ "rawtypes", "unchecked" })
		FunCreator<Fun<Env, Node>> fc = (FunCreator) FunCreator.of(Fun.class);

		return fc.create(env -> new Object() {
			private FunExpr compile_(Node node_) {
				Tree tree;

				if (node_ instanceof Atom) {
					String name = ((Atom) node_).name;
					if (ProverConstant.isCut(node_) || ProverConstant.isVariable(name)) {
						int index = findVariableIndex(node_);
						return env.index(f.int_(index));
					} else if (ProverConstant.isWildcard(name))
						return f.new_(Reference.class);
					else
						return f.object(node);
				} else if (node_ instanceof Int)
					return f.object(node_);
				else if ((tree = Tree.decompose(node)) != null) {
					FunExpr fe0 = compile_(tree.getLeft());
					FunExpr fe1 = compile_(tree.getRight());
					return f.invokeStatic(Tree.class, "of", fe0, fe1);
				} else if (node_ instanceof Tuple) {
					FunExpr[] exprs = Read.from(((Tuple) node).nodes).map(this::compile_).toArray(FunExpr.class);
					return f.invokeStatic(Tuple.class, "of", f.array(Node.class, exprs));
				} else
					throw new RuntimeException();
			}
		}.compile_(node)).apply(new HashMap<>());
	}

}
