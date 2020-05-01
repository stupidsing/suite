package suite.util;

import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.Verbs.Is;
import primal.Verbs.Substring;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.persistent.PerMap;
import primal.primitive.IntIntSink;
import primal.primitive.adt.IntRange;
import primal.primitive.adt.pair.IntObjPair;
import primal.streamlet.Streamlet;
import suite.node.io.Operator.Assoc;

import java.util.*;
import java.util.function.Predicate;

public class ScrapeHtml {

	private HtmlUtil htmlUtil = new HtmlUtil();

	private SmartSplit ss = new SmartSplit(
			c -> false,
			c -> false,
			c -> c == '\'' || c == '"' || c == '`');

	private Set<String> voidElementTagNames = Set.of(
			"area",
			"base",
			"br",
			"col",
			"embed",
			"hr",
			"img",
			"input",
			"link",
			"meta",
			"param",
			"source",
			"track",
			"wbr");

	public class HtmlNode {
		public final String name;
		public final String tag;
		public final List<Pair<String, String>> attrs;
		public final PerMap<String, String> attrByName;
		public final List<HtmlNode> children = new ArrayList<>();
		public int p0, p1, p2, px; // start tag range, end tag range

		private HtmlNode(String name, String tag, int p0, int p1) {
			this.name = name;
			this.tag = tag;
			if (name != null)
				attrs = ss
						.splitn(Substring.of(tag, 1, -1), " ", Assoc.RIGHT)
						.skip(1)
						.map(kv -> Split.strl(kv, "="))
						.filter(kv -> !kv.v.isEmpty())
						.map(Pair.mapSnd(v -> {
							var isQuoted = v.startsWith("'") && v.endsWith("'")
									|| v.startsWith("\"") && v.endsWith("\"");
							return !isQuoted ? v : Substring.of(v, 1, -1);
						}))
						.toList();
			else
				attrs = Collections.emptyList();
			attrByName = Read.from(attrs).fold(PerMap.empty(), (m, p) -> m.put(p.k, p.v));
			this.p0 = p0;
			this.p1 = p1;
		}

		public String attr(String name) {
			return attr(name, "");
		}

		public String attr(String name, String value_) {
			return attrByName.getOpt(name).or(value_);
		}

		public Streamlet<HtmlNode> findBy(String name, String id) {
			return findBy(hn -> hn.attrByName.getOpt(name).map(id::equals).or(false));
		}

		public Streamlet<HtmlNode> findBy(Predicate<HtmlNode> pred) {
			return new Object() {
				private Streamlet<HtmlNode> find(HtmlNode n) {
					var children = n.children().concatMap(this::find);
					return pred.test(n) ? children.cons(n) : children;
				}
			}.find(this);
		}

		public boolean isClass(String c) {
			return attrByName
					.getOpt("class")
					.map(cs -> Read.from(cs.split(" ")).filter(c_ -> Equals.string(c, c_)).first() != null)
					.or(false);
		}

		public String text() {
			var sb = new StringBuilder();
			new Object() {
				private void r(HtmlNode h) {
					if (h.name == null)
						sb.append(h.tag);
					h.children().forEach(this::r);
				}
			}.r(this);
			return sb.toString();
		}

		public Streamlet<HtmlNode> children() {
			return Read.from(children);
		}

		public String toString() {
			return tag;
		}
	}

	public HtmlNode parse(String in) {
		var pairs = new ArrayList<IntRange>();
		int pos0, posx = 0;

		nextTag: while (0 <= (pos0 = in.indexOf("<", posx)))
			if ((posx = pos0 + 1) < in.length() && !Is.whitespace(in.charAt(posx)))
				if (0 <= (posx = in.indexOf(">", posx))) {
					pairs.add(IntRange.of(pos0, ++posx));

					if (in.startsWith("<![CDATA[", pos0)) {
						posx = in.indexOf("]]>", pos0 + 9);
						continue nextTag;
					}

					for (var rawTextTag : List.of("script", "style", "textarea", "title"))
						if (in.startsWith(rawTextTag, pos0 + 1)) {
							posx = in.indexOf("</" + rawTextTag, posx);
							continue nextTag;
						}
				} else
					break;

		Fun<String, IntObjPair<String>> getNameFun = tag -> {
			int p1 = 1, px = tag.length() - 1;
			var first = tag.charAt(p1);
			var last = tag.charAt(px - 1);
			int d;

			if (first == '!')
				return IntObjPair.of(0, null);
			else {
				if (first == '/') {
					p1++;
					d = -1;
				} else if (last == '/') {
					px--;
					d = 0;
				} else
					d = 1;

				var ps = 0;
				while (ps < px && !Is.whitespace(tag.charAt(ps)))
					ps++;
				var name = tag.substring(p1, ps);
				return IntObjPair.of(d, name);
			}
		};

		var deque = new ArrayDeque<>(List.of(new HtmlNode(null, "", 0, 0)));

		IntIntSink addTextFun = (prevp, p0) -> {
			if (prevp != p0) {
				var s = htmlUtil.decode(in.substring(prevp, p0)).trim();
				if (!s.isEmpty())
					deque.element().children.add(new HtmlNode(null, s, prevp, p0));
			}
		};

		var prevp = 0;

		for (var pair : pairs) {
			var htmlNode = deque.element();
			var p0 = pair.s;
			var px = pair.e;

			addTextFun.sink2(prevp, p0);

			var tag = in.substring(p0, px);

			prevp = getNameFun.apply(tag).map((d, name) -> {
				if (d == -1) { // closing tag
					HtmlNode hn;
					while (!deque.isEmpty())
						if (Equals.string(getNameFun.apply((hn = deque.pop()).tag).v, name)) {
							hn.p2 = p0;
							hn.px = px;
							break;
						}
				} else { // opening tag
					var htmlNode1 = new HtmlNode(name, tag, p0, px);
					htmlNode.children.add(htmlNode1);
					if (d == 1)
						deque.push(htmlNode1);
				}
				return px;
			});
		}

		addTextFun.sink2(prevp, in.length());

		return deque.pop();
	}

	public String format(HtmlNode node) {
		return Build.string(sb -> new Object() {
			private void f(HtmlNode node_) {
				if (node_.name != null) {
					sb.append("<" + node_.name);
					for (var attr : node_.attrs)
						sb.append(" " + attr.k + "='" + attr.v + "'");
					sb.append(">");
					for (var child : node_.children)
						f(child);
					if (!voidElementTagNames.contains(node_.name))
						sb.append("</" + node_.name + ">");
				} else
					sb.append(node_.tag);
			}
		}.f(node));
	}

}
