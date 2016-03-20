package suite.instructionexecutor;

import suite.fp.intrinsic.Intrinsics;
import suite.fp.intrinsic.Intrinsics.Intrinsic;
import suite.fp.intrinsic.Intrinsics.IntrinsicCallback;
import suite.lp.kb.RuleSet;
import suite.node.Atom;
import suite.node.Int;
import suite.node.Node;

public class TranslatedRunUtil {

	public interface TranslatedRun {
		public Node exec(TranslatedRunConfig config, Thunk thunk);
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

	public static class Thunk extends Node {
		public Frame frame;
		public int ip;
		public Node result;

		public Thunk(Frame frame, int ip) {
			this.frame = frame;
			this.ip = ip;
		}
	}

	public static class IntrinsicFrame implements Frame {
		public Intrinsic intrinsic;
		public Node node;
	}

	public static IntrinsicCallback getIntrinsicCallback(TranslatedRunConfig config, TranslatedRun translatedRun) {
		if (config.isLazy)
			return new IntrinsicCallback() {
				public Node enclose(Intrinsic intrinsic, Node node) {
					IntrinsicFrame frame = new IntrinsicFrame();
					frame.intrinsic = intrinsic;
					frame.node = node;
					return new Thunk(frame, InstructionTranslator.invokeJavaEntryPoint);
				}

				public Node yawn(Node node) {
					if (node instanceof Thunk) {
						Thunk thunk = (Thunk) node;
						if (thunk.result == null)
							thunk.result = translatedRun.exec(config, thunk);
						node = thunk.result;
					}
					return node;
				}
			};
		else
			return Intrinsics.eagerIntrinsicCallback;
	}

	public static Node toNode(boolean b) {
		return b ? Atom.TRUE : Atom.FALSE;
	}

	public static Node toNode(int i) {
		return Int.of(i);
	}

	// Generic type signature allows passing in Thunk returning Thunk
	public static <T extends Node> T toNode(T t) {
		return t;
	}

}
