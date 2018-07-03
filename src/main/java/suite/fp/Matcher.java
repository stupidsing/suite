package suite.fp;

import static suite.util.Friends.rethrow;

import suite.BindMapUtil;
import suite.node.Node;
import suite.util.FunUtil.Source;

public class Matcher<T> {

	private static BindMapUtil bindMapUtil = new BindMapUtil();

	public static Matcher<APPLY> apply = new Matcher<>(APPLY::new, "APPLY .param .fun");
	public static Matcher<ATOM> atom = new Matcher<>(ATOM::new, "ATOM .value");
	public static Matcher<BOOLEAN> boolean_ = new Matcher<>(BOOLEAN::new, "BOOLEAN .value");
	public static Matcher<CHARS> chars = new Matcher<>(CHARS::new, "CHARS .value");
	public static Matcher<CONS> cons = new Matcher<>(CONS::new, "CONS .type .head .tail");
	public static Matcher<DECONS> decons = new Matcher<>(DECONS::new, "DECONS .type .value .left .right .then .else_");
	public static Matcher<DEFVARS> defvars = new Matcher<>(DEFVARS::new, "DEF-VARS .list .do_");
	public static Matcher<ERROR> error = new Matcher<>(ERROR::new, "ERROR .m");
	public static Matcher<FUN> fun = new Matcher<>(FUN::new, "FUN .param .do_");
	public static Matcher<IF> if_ = new Matcher<>(IF::new, "IF .if_ .then_ .else_");
	public static Matcher<NIL> nil = new Matcher<>(NIL::new, "NIL");
	public static Matcher<NUMBER> number = new Matcher<>(NUMBER::new, "NUMBER .value");
	public static Matcher<PRAGMA> pragma = new Matcher<>(PRAGMA::new, "PRAGMA _ .do_");
	public static Matcher<TCO> tco = new Matcher<>(TCO::new, "TCO .iter .in_");
	public static Matcher<TREE> tree = new Matcher<>(TREE::new, "TREE .op .left .right");
	public static Matcher<UNWRAP> unwrap = new Matcher<>(UNWRAP::new, "UNWRAP .do_");
	public static Matcher<VAR> var = new Matcher<>(VAR::new, "VAR .name");
	public static Matcher<WRAP> wrap = new Matcher<>(WRAP::new, "WRAP .do_");

	public static class APPLY {
		public Node param, fun;
	}

	public static class ATOM {
		public Node value;
	}

	public static class BOOLEAN {
		public Node value;
	}

	public static class CHARS {
		public Node value;
	}

	public static class CONS {
		public Node type, head, tail;
	}

	public static class DECONS {
		public Node type, value, left, right, then, else_;
	}

	public static class DEFVARS {
		public Node list, do_;
	}

	public static class ERROR {
		public Node m;
	}

	public static class FUN {
		public Node param, do_;
	}

	public static class IF {
		public Node if_, then_, else_;
	}

	public static class NIL {
	}

	public static class NUMBER {
		public Node value;
	}

	public static class PRAGMA {
		public Node do_;
	}

	public static class TCO {
		public Node iter, in_;
	}

	public static class TREE {
		public Node op, left, right;
	}

	public static class UNWRAP {
		public Node do_;
	}

	public static class VAR {
		public Node name;
	}

	public static class WRAP {
		public Node do_;
	}

	private Source<T> ctor;
	private String p;

	public Matcher(Source<T> ctor, String p) {
		this.ctor = ctor;
		this.p = p;
	}

	public T match(Node node) {
		var map = bindMapUtil.pattern(p).match(node);
		return rethrow(() -> {
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
