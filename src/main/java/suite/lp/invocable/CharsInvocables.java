package suite.lp.invocable;

import java.util.List;

import suite.instructionexecutor.ExpandUtil;
import suite.lp.invocable.Invocables.Id;
import suite.node.Atom;
import suite.node.Data;
import suite.node.Int;
import suite.node.Node;
import suite.node.Tree;
import suite.node.io.TermParser.TermOp;
import suite.primitive.Chars;

public class CharsInvocables {

	public static class Append implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			Chars chars0 = (Chars) ((Data<?>) inputs.get(0)).getData();
			Chars chars1 = (Chars) ((Data<?>) inputs.get(1)).getData();
			return new Data<Chars>(chars0.append(chars1));
		}
	}

	public static class CharsString implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			Chars chars = (Chars) ((Data<?>) inputs.get(0)).getData();

			if (!chars.isEmpty()) {
				Node left = bridge.wrapInvocable(new Id(), Int.create(chars.get(0)));
				Node right = bridge.wrapInvocable(this, new Data<Chars>(chars.subchars(1)));
				return Tree.create(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	}

	public static class StringChars implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			String value = ExpandUtil.expandString(bridge.getUnwrapper(), inputs.get(0));
			return new Data<Chars>(new Chars(value.toCharArray()));
		}
	}

	public static class Subchars implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			int start = ((Int) inputs.get(0)).getNumber();
			int end = ((Int) inputs.get(1)).getNumber();
			Chars chars = (Chars) ((Data<?>) inputs.get(2)).getData();
			return new Data<Chars>(chars.subchars(start, end));
		}
	}

}
