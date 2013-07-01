package org.instructionexecutor;

import java.io.Closeable;

import org.suite.kb.RuleSet;
import org.suite.node.Atom;
import org.suite.node.Int;
import org.suite.node.Node;
import org.util.FunUtil.Fun;

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

	public static Fun<Node, Node> getUnwrapper(final TranslatedRunConfig config, final TranslatedRun translatedRun) {
		return new Fun<Node, Node>() {
			public Node apply(Node node) {
				node = node.finalNode();
				if (node instanceof Closure)
					node = translatedRun.exec(config, (Closure) node);
				return node;
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
	public static <T extends Node> T toNode(T n) {
		return n;
	}

}
