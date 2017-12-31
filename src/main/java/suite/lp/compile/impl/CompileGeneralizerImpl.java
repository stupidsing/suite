package suite.lp.compile.impl;

import java.util.HashMap;

import suite.adt.pair.Pair;
import suite.jdk.gen.FunCreator;
import suite.jdk.gen.FunExpression.FunExpr;
import suite.jdk.gen.FunFactory;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.doer.ProverConstant;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Dict;
import suite.node.Int;
import suite.node.Node;
import suite.node.Reference;
import suite.node.Tree;
import suite.node.Tuple;
import suite.streamlet.Read;

public class CompileGeneralizerImpl extends VariableMapper implements GeneralizerFactory {

	private static FunFactory f = new FunFactory();

	public Generalize_ generalizer(Node node) {
		FunCreator<Generalize_> fc = FunCreator.of(Generalize_.class);

		return fc.create(env -> new Object() {
			private FunExpr compile_(Node node_) {
				Tree tree;

				if (node_ instanceof Atom) {
					String name = ((Atom) node_).name;
					if (ProverConstant.isCut(node_) || ProverConstant.isVariable(name)) {
						int index = computeIndex(node_);
						return env.field("refs").index(f.int_(index));
					} else if (ProverConstant.isWildcard(name))
						return f.new_(Reference.class);
					else
						return f.object(node_);
				} else if (node_ instanceof Dict) {
					FunExpr[] exprs = Read //
							.from2(((Dict) node_).map) //
							.map((key, value) -> f.invokeStatic(Pair.class, "of", compile_(key), compile_(value))) //
							.toArray(FunExpr.class);
					return f.invokeStatic(Dict.class, "of", f.array(Pair.class, exprs));
				} else if (node_ instanceof Int)
					return f.object(node_);
				else if ((tree = Tree.decompose(node_)) != null) {
					FunExpr fe0 = compile_(tree.getLeft()).cast(Node.class);
					FunExpr fe1 = compile_(tree.getRight()).cast(Node.class);
					return f.invokeStatic(Tree.class, "of", f.object(tree.getOperator()), fe0, fe1);
				} else if (node_ instanceof Tuple) {
					FunExpr[] exprs = Read.from(((Tuple) node_).nodes).map(this::compile_).toArray(FunExpr.class);
					return f.invokeStatic(Tuple.class, "of", f.array(Node.class, exprs));
				} else
					throw new RuntimeException();
			}
		}.compile_(node)).apply(new HashMap<>());
	}

}
