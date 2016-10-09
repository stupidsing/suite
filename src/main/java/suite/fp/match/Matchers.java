package suite.fp.match;

import suite.node.Node;

public class Matchers {

	public static class APPLY {
		public static String matcher = "APPLY .param .fun";
		public Node param, fun;
	}

	public static class ATOM {
		public static String matcher = "ATOM .value";
		public Node value;
	}

	public static class BOOLEAN {
		public static String matcher = "BOOLEAN .value";
		public Node value;
	}

	public static class CHARS {
		public static String matcher = "CHARS .value";
		public Node value;
	}

	public static class CONS {
		public static String matcher = "CONS _ .head .tail";
		public Node head, tail;
	}

	public static class DECONS {
		public static String matcher = "DECONS .type .value .left .right .then .else_";
		public Node type, value, left, right, then, else_;
	}

	public static class DEFVARS {
		public static String matcher = "DEF-VARS .list .do_";
		public Node list, do_;
	}

	public static class ERROR {
		public static String matcher = "ERROR";
	}

	public static class FUN {
		public static String matcher = "FUN .param .do_";
		public Node param, do_;
	}

	public static class IF {
		public static String matcher = "IF .if_ .then_ .else_";
		public Node if_, then_, else_;
	}

	public static class NIL {
		public static String matcher = "NIL";
	}

	public static class NUMBER {
		public static String matcher = "NUMBER .value";
		public Node value;
	}

	public static class PRAGMA {
		public static String matcher = "PRAGMA _ .do_";
		public Node do_;
	}

	public static class TCO {
		public static String matcher = "TCO .iter .in_";
		public Node iter, in_;
	}

	public static class TREE {
		public static String matcher = "TREE .op .left .right";
		public Node op, left, right;
	}

	public static class UNWRAP {
		public static String matcher = "UNWRAP .do_";
		public Node do_;
	}

	public static class VAR {
		public static String matcher = "VAR .name";
		public Node name;
	}

	public static class WRAP {
		public static String matcher = "WRAP .do_";
		public Node do_;
	}

}
