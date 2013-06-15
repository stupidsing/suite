package org.instructionexecutor;

import java.io.Closeable;

import org.suite.kb.RuleSet;
import org.suite.node.Node;

public class CompiledRunUtil {

	public interface CompiledRun extends Closeable {
		public Node exec(RuleSet ruleSet);
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

}
