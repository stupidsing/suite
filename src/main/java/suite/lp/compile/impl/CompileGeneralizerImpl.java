package suite.lp.compile.impl;

import suite.lp.doer.Generalizer;
import suite.lp.doer.GeneralizerFactory;
import suite.node.Node;
import suite.util.FunUtil.Source;

public class CompileGeneralizerImpl extends CompileClonerImpl implements GeneralizerFactory {

	public Source<NodeEnv> g(Node node) {
		Generalize_ fun = generalizer(node);
		return () -> g(fun::apply);
	}

	public Generalize_ generalizer(Node node) {
		return cloner(new Generalizer().generalize(node))::apply;
	}

}
