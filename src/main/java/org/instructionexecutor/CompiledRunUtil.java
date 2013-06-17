package org.instructionexecutor;

import java.io.Closeable;

import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.util.FunUtil.Fun;

public class CompiledRunUtil {

	public interface CompiledRun extends Closeable {
		public Node exec(CompiledRunConfig config, Closure closure);
	}

	public static class CompiledRunConfig {
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

	public static class CutPoint {
		public Frame frame;
		public int ip;
		public int bsp;
		public int csp;
		public int jp;
	}

	public static Fun<Node, Node> getUnwrapper(final CompiledRunConfig config, final CompiledRun compiledRun) {
		return new Fun<Node, Node>() {
			public Node apply(Node node) {
				node = node.finalNode();
				if (node instanceof Closure)
					node = compiledRun.exec(config, (Closure) node);
				return node;
			}
		};
	}

}
