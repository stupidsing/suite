package suite.lp.compile.impl;

import suite.lp.doer.Generalizer;
import suite.lp.doer.GeneralizerFactory;
import suite.node.Node;

public class CompileGeneralizerImpl extends CompileClonerImpl implements GeneralizerFactory {

	public Generalize_ generalizer(Node node) {
		return cloner(new Generalizer().generalize(node))::apply;
	}

}
