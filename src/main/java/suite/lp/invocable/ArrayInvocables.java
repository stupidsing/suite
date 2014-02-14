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
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class ArrayInvocables {

	public static class Append implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			List<Node> array0 = Data.get(inputs.get(0));
			List<Node> array1 = Data.get(inputs.get(1));
			return new Data<>(Util.add(array0, array1));
		}
	}

	public static class ArrayList implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			List<Node> array = Data.get(inputs.get(0));

			if (!array.isEmpty()) {
				Node left = bridge.wrapInvocable(new Id(), array.get(0));
				Node right = bridge.wrapInvocable(this, new Data<>(array.subList(1, array.size())));
				return Tree.create(TermOp.OR____, left, right);
			} else
				return Atom.NIL;
		}
	}

	public static class Left implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			int position = ((Int) inputs.get(0)).getNumber();
			List<Node> array = Data.get(inputs.get(1));
			return new Data<>(Util.left(array, position));
		}
	}

	public static class ListArray implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			Source<Node> value = ExpandUtil.expandList(bridge.getUnwrapper(), inputs.get(0));
			return new Data<>(To.list(value));
		}
	}

	public static class Right implements Invocable {
		public Node invoke(InvocableBridge bridge, List<Node> inputs) {
			int position = ((Int) inputs.get(0)).getNumber();
			List<Node> array = Data.get(inputs.get(1));
			return new Data<>(Util.right(array, position));
		}
	}

}
