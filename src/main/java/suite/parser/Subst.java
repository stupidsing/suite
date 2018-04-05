package suite.parser;

import java.util.Map;

import suite.util.FunUtil.Iterate;

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
		var sb = new StringBuilder();
		subst(s, fun, sb);
		return sb.toString();
	}

	public void subst(String s, Iterate<String> fun, StringBuilder sb) {
		while (true) {
			var pos0 = s.indexOf(openSubst);
			int pos1 = s.indexOf(closeSubst, pos0);

			if (0 <= pos0 && 0 <= pos1) {
				String left = s.substring(0, pos0);
				String key = s.substring(pos0 + 2, pos1);
				var right = s.substring(pos1 + 1);
				var value = fun.apply(key);

				if (value != null) {
					sb.append(left);
					subst(value, fun, sb);
					s = right;
					continue;
				}
			}

			sb.append(s);
			break;
		}
	}

}
