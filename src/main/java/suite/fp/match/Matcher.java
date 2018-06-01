package suite.fp.match;

import suite.BindMapUtil;
import suite.node.Node;
import suite.util.FunUtil.Source;
import suite.util.Rethrow;

public class Matcher<T> {

	private static BindMapUtil bindMapUtil = new BindMapUtil();

	public static Matcher<APPLY> apply = new Matcher<APPLY>(APPLY.matcher, APPLY::new);
	public static Matcher<ATOM> atom = new Matcher<ATOM>(ATOM.matcher, ATOM::new);
	public static Matcher<BOOLEAN> boolean_ = new Matcher<BOOLEAN>(BOOLEAN.matcher, BOOLEAN::new);
	public static Matcher<CHARS> chars = new Matcher<CHARS>(CHARS.matcher, CHARS::new);
	public static Matcher<CONS> cons = new Matcher<CONS>(CONS.matcher, CONS::new);
	public static Matcher<DECONS> decons = new Matcher<DECONS>(DECONS.matcher, DECONS::new);
	public static Matcher<DEFVARS> defvars = new Matcher<DEFVARS>(DEFVARS.matcher, DEFVARS::new);
	public static Matcher<ERROR> error = new Matcher<ERROR>(ERROR.matcher, ERROR::new);
	public static Matcher<FUN> fun = new Matcher<FUN>(FUN.matcher, FUN::new);
	public static Matcher<IF> if_ = new Matcher<IF>(IF.matcher, IF::new);
	public static Matcher<NIL> nil = new Matcher<NIL>(NIL.matcher, NIL::new);
	public static Matcher<NUMBER> number = new Matcher<NUMBER>(NUMBER.matcher, NUMBER::new);
	public static Matcher<PRAGMA> pragma = new Matcher<PRAGMA>(PRAGMA.matcher, PRAGMA::new);
	public static Matcher<TCO> tco = new Matcher<TCO>(TCO.matcher, TCO::new);
	public static Matcher<TREE> tree = new Matcher<TREE>(TREE.matcher, TREE::new);
	public static Matcher<UNWRAP> unwrap = new Matcher<UNWRAP>(UNWRAP.matcher, UNWRAP::new);
	public static Matcher<VAR> var = new Matcher<VAR>(VAR.matcher, VAR::new);
	public static Matcher<WRAP> wrap = new Matcher<WRAP>(WRAP.matcher, WRAP::new);

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
		public static String matcher = "CONS .type .head .tail";
		public Node type, head, tail;
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
		public static String matcher = "ERROR .m";
		public Node m;
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

	private String matcher;
	private Source<T> ctor;

	public Matcher(String matcher, Source<T> ctor) {
		this.matcher = matcher;
		this.ctor = ctor;
	}

	public T match(Node node) {
		var map = bindMapUtil.pattern(matcher).match(node);
		return Rethrow.ex(() -> {
			if (map != null) {
				var t = ctor.source();
				var clazz = t.getClass();
				for (var e : map.entrySet())
					clazz.getField(e.getKey().substring(1)).set(t, e.getValue());
				return t;
			} else
				return null;
		});
	}

}
