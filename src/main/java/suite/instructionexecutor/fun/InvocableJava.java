package suite.instructionexecutor.fun;

import suite.instructionexecutor.ExpandUtil;
import suite.instructionexecutor.FunInstructionExecutor;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.node.Str;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;

public class InvocableJava {

	private static final Atom ATOM = Atom.create("ATOM");
	private static final Atom NUMBER = Atom.create("NUMBER");
	private static final Atom STRING = Atom.create("STRING");
	private static final Atom TREE = Atom.create("TREE");
	private static final Atom UNKNOWN = Atom.create("UNKNOWN");

	public interface InvocableJavaFun {
		public Node invoke(FunInstructionExecutor executor, Node input);
	}

	public static class AtomString implements InvocableJavaFun {
		public Node invoke(FunInstructionExecutor executor, Node input) {
			String name = ((Atom) executor.getUnwrapper().apply(input)).getName();

			if (!name.isEmpty()) {
				Node left = executor.createInvocableJavaFunClosure(Id.class, Int.create(name.charAt(0)));
				Node right = executor.createInvocableJavaFunClosure(AtomString.class, Atom.create(name.substring(1)));
				return Tree.create(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	}

	public static class GetType implements InvocableJavaFun {
		public Node invoke(FunInstructionExecutor executor, Node input) {
			Node node = executor.getUnwrapper().apply(input);
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

	public static class Id implements InvocableJavaFun {
		public Node invoke(FunInstructionExecutor executor, Node input) {
			return input;
		}
	}

	public static class StringLength implements InvocableJavaFun {
		public Node invoke(FunInstructionExecutor executor, Node input) {
			return Int.create(ExpandUtil.expandString(input, executor.getUnwrapper()).length());
		}
	}

}
