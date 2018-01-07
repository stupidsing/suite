package suite.lp.compile.impl;

import java.util.IdentityHashMap;
import java.util.Map;

import suite.lp.doer.Generalizer;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.sewing.Env;
import suite.lp.sewing.VariableMapper.NodeEnv;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;
import suite.util.FunUtil.Source;

public class CompileGeneralizerImpl implements GeneralizerFactory {

	private CompileClonerImpl cc = new CompileClonerImpl();

	public Source<NodeEnv<Reference>> g(Node node) {
		return cc.vm.g(generalizer(node)::apply);
	}

	@Override
	public Env env() {
		return cc.vm.env();
	}

	@Override
	public Generalize_ generalizer(Node node) {
		Generalizer generalizer = new Generalizer();
		Map<String, Integer> indices = new IdentityHashMap<>();
		Generalize_ generalize = cc.cloner(generalizer.generalize(node))::apply;

		for (Atom variableName : generalizer.getVariableNames())
			indices.put(variableName.name, cc.vm.computeIndex(generalizer.getVariable(variableName)));

		return generalize;
	}

}
