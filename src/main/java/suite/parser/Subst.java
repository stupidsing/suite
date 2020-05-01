package suite.parser;

import primal.Verbs.Build;
import primal.fp.Funs.Iterate;

import java.util.Map;

public class Subst {

	private String openSubst;
	private String closeSubst;

	public Subst() {
		this("${", "}");
	}

	public Subst(String openSubst, String closeSubst) {
		this.openSubst = openSubst;
		this.closeSubst = closeSubst;
	}

	public String subst(String s, Map<String, String> map) {
		return subst(s, map::get);
	}

	public String subst(String s, Iterate<String> fun) {
		return Build.string(sb -> subst(s, fun, sb));
	}

	public void subst(String s, Iterate<String> fun, StringBuilder sb) {
		var pos0 = 0;
		int pos1, pos2;
		String value;

		while (0 <= (pos1 = s.indexOf(openSubst, pos0))
				&& 0 <= (pos2 = s.indexOf(closeSubst, pos1))
				&& (value = fun.apply(s.substring(pos1 + 2, pos2))) != null) {
			sb.append(s.substring(pos0, pos1));
			subst(value, fun, sb);
			pos0 = pos2 + 1;
		}

		sb.append(s.substring(pos0));
	}

}
