package suite.lp.invocable;

import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;
import suite.util.FunUtil.Fun;
import suite.util.LogUtil;

public class Invocables {

	private static final Atom ATOM = Atom.create("ATOM");
	private static final Atom NUMBER = Atom.create("NUMBER");
	private static final Atom STRING = Atom.create("STRING");
	private static final Atom TREE = Atom.create("TREE");
	private static final Atom UNKNOWN = Atom.create("UNKNOWN");

	public static abstract class InvocableNode extends Node {
		public abstract Node invoke(FunInstructionExecutor executor, List<Node> inputs);
	}

	public static class AtomString extends InvocableNode {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			String name = ((Atom) executor.getUnwrapper().apply(inputs.get(0))).getName();

			if (!name.isEmpty()) {
				Node left = executor.wrapInvocableNode(new Id(), Int.create(name.charAt(0)));
				Node right = executor.wrapInvocableNode(this, Atom.create(name.substring(1)));
				return Tree.create(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	}

	public static class GetType extends InvocableNode {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			Node node = executor.getUnwrapper().apply(inputs.get(0));
			Atom type;

			if (node instanceof Atom)
				type = ATOM;
			else if (node instanceof Int)
				type = NUMBER;
			else if (node instanceof Str)
				type = STRING;
			else if (node instanceof Tree)
				type = TREE;
			else
				type = UNKNOWN;

			return type;
		}
	}

	public static class Id extends InvocableNode {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			return inputs.get(0);
		}
	}

	public static class Log2 extends InvocableNode {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			Fun<Node, Node> unwrapper = executor.getUnwrapper();
			LogUtil.info(ExpandUtil.expandString(unwrapper, inputs.get(0)));
			return unwrapper.apply(inputs.get(1));
		}
	}

	public static class StringLength extends InvocableNode {
		public Node invoke(FunInstructionExecutor executor, List<Node> inputs) {
			return Int.create(ExpandUtil.expandString(executor.getUnwrapper(), inputs.get(0)).length());
		}
	}

}
