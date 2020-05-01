package suite.lp.compile.impl;

import suite.lp.doer.Generalizer;
import suite.lp.doer.GeneralizerFactory;
import suite.lp.sewing.VariableMapper;
import suite.node.Atom;
import suite.node.Node;
import suite.node.Reference;

import java.util.IdentityHashMap;

public class CompileGeneralizerImpl implements GeneralizerFactory {

	private CompileClonerImpl cc = new CompileClonerImpl();
	private VariableMapper<Atom> vm;

	@Override
	public VariableMapper<Atom> mapper() {
		return vm;
	}

	@Override
	public Generalize_ generalizer(Node node) {
		var generalizer = new Generalizer();
		var generalize = cc.cloner(generalizer.generalize(node));
		var indices = new IdentityHashMap<Reference, Atom>();
		for (var variableName : generalizer.getVariableNames())
			indices.put(generalizer.getVariable(variableName), variableName);
		vm = cc.mapper().mapKeys(indices::get);
		return generalize::apply;
	}

}
