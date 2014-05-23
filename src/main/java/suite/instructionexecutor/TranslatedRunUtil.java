package suite.instructionexecutor;

import java.util.Arrays;

import suite.lp.intrinsic.Intrinsic;
import suite.lp.intrinsic.IntrinsicBridge;
import suite.lp.kb.RuleSet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

public class TranslatedRunUtil {

	public interface TranslatedRun {
		public Node exec(TranslatedRunConfig config, Closure closure);
	}

	public static class TranslatedRunConfig {
		public RuleSet ruleSet;
		public boolean isLazy;

		public TranslatedRunConfig(RuleSet ruleSet, boolean isLazy) {
			this.ruleSet = ruleSet;
			this.isLazy = isLazy;
		}
	}

	public interface Frame {
	}

	public static class Closure extends Node {
		public Frame frame;
		public int ip;
		public Node result;

		public Closure(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}
	}

	public static class IntrinsicFrame implements Frame {
		public Intrinsic intrinsic;
		public Node node;
	}

	public static IntrinsicBridge getIntrinsicBridge(TranslatedRunConfig config, TranslatedRun translatedRun) {
		if (config.isLazy)
			return new IntrinsicBridge() {
				public Node unwrap(Node node) {
					node = node.finalNode();
					if (node instanceof Closure) {
						Closure closure = (Closure) node;
						if (closure.result == null)
							closure.result = translatedRun.exec(config, closure);
						node = closure.result;
					}
					return node;
				}

				public Node wrapIntrinsic(Intrinsic intrinsic, Node node) {
					IntrinsicFrame frame = new IntrinsicFrame();
					frame.intrinsic = intrinsic;
					frame.node = node;
					return new Closure(frame, InstructionTranslator.invokeJavaEntryPoint);
				}
			};
		else
			return new IntrinsicBridge() {
				public Node unwrap(Node node) {
					return node;
				}

				public Node wrapIntrinsic(Intrinsic intrinsic, Node node) {
					return intrinsic.invoke(this, Arrays.asList(node));
				}
			};
	}

	public static Node toNode(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	public static Node toNode(int i) {
		return Int.of(i);
	}

	// Generic type signature allows passing in Closure returning Closure
	public static <T extends Node> T toNode(T t) {
		return t;
	}

}
