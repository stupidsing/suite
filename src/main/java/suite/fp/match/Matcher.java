package suite.fp.match;

import suite.BindMapUtil;
import suite.fp.match.Matchers.APPLY;
import suite.fp.match.Matchers.ATOM;
import suite.fp.match.Matchers.BOOLEAN;
import suite.fp.match.Matchers.CHARS;
import suite.fp.match.Matchers.CONS;
import suite.fp.match.Matchers.DECONS;
import suite.fp.match.Matchers.DEFVARS;
import suite.fp.match.Matchers.ERROR;
import suite.fp.match.Matchers.FUN;
import suite.fp.match.Matchers.IF;
import suite.fp.match.Matchers.NIL;
import suite.fp.match.Matchers.NUMBER;
import suite.fp.match.Matchers.PRAGMA;
import suite.fp.match.Matchers.TCO;
import suite.fp.match.Matchers.TREE;
import suite.fp.match.Matchers.UNWRAP;
import suite.fp.match.Matchers.VAR;
import suite.fp.match.Matchers.WRAP;
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

	public String matcher;
	public Source<T> ctor;

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
