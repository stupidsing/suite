package suite.instructionexecutor;

import java.io.Closeable;

import suite.lp.invocable.Invocable;
import suite.lp.invocable.InvocableBridge;
import suite.lp.kb.RuleSet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;
import suite.util.FunUtil.Fun;

public class TranslatedRunUtil {

	public interface TranslatedRun extends Closeable {
		public Node exec(TranslatedRunConfig config, Closure closure);
	}

	public static class TranslatedRunConfig {
		public RuleSet ruleSet;
	}

	public interface Frame {
	}

	public static class Closure extends Node {
		public Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}

		public Frame frame;
		public int ip;
		public Node result;
	}

	public static InvocableBridge getInvocableBridge(final TranslatedRunConfig config, final TranslatedRun translatedRun) {
		final Fun<Node, Node> unwrapper = new Fun<Node, Node>() {
			public Node apply(Node node) {
				node = node.finalNode();
				if (node instanceof Closure) {
					Closure closure = (Closure) node;
					if (closure.result == null)
						closure.result = translatedRun.exec(config, closure);
					node = closure.result;
				}
				return node;
			}
		};

		return new InvocableBridge() {
			public Fun<Node, Node> getUnwrapper() {
				return unwrapper;
			}

			public Node wrapInvocable(Invocable invocable, Node node) {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static Node toNode(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	public static Node toNode(int i) {
		return Int.create(i);
	}

	// Generic type signature allows passing in Closure returning Closure
	public static <T extends Node> T toNode(T t) {
		return t;
	}

}
