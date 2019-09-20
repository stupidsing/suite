package suite.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.Verbs.Get;
import primal.Verbs.Is;
import primal.Verbs.Substring;
import primal.adt.Pair;
import primal.adt.map.BiHashMap;
import primal.adt.map.BiMap;
import primal.fp.Funs.Fun;
import primal.persistent.PerMap;
import primal.primitive.IntIntSink;
import primal.primitive.adt.IntRange;
import primal.primitive.adt.pair.IntObjPair;
import primal.streamlet.Streamlet;
import suite.node.io.Operator.Assoc;

public class ScrapeHtml {

	private SmartSplit ss = new SmartSplit( //
			c -> false, //
			c -> false, //
			c -> c == '\'' || c == '"' || c == '`');

	private Set<String> voidElementTagNames = Set.of( //
			"area", //
			"base", //
			"br", //
			"col", //
			"embed", //
			"hr", //
			"img", //
			"input", //
			"link", //
			"meta", //
			"param", //
			"source", //
			"track", //
			"wbr");

	private BiMap<String, String> escapeTokenByChar = new BiHashMap<>();

	public ScrapeHtml() {
		initialize();
	}

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
				attrs = ss //
						.splitn(Substring.of(tag, 1, -1), " ", Assoc.RIGHT) //
						.skip(1) //
						.map(kv -> Split.strl(kv, "=")) //
						.filter(kv -> !kv.v.isEmpty()) //
						.map(Pair.mapSnd(v -> {
							var isQuoted = v.startsWith("'") && v.endsWith("'")
									|| v.startsWith("\"") && v.endsWith("\"");
							return !isQuoted ? v : Substring.of(v, 1, -1);
						})) //
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
			var value = attrByName.get(name);
			return value != null ? value : value_;
		}

		public Streamlet<HtmlNode> findBy(String name, String id) {
			return findBy(hn -> id.equals(hn.attrByName.get(name)));
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
			var cs = attrByName.get("class");
			return cs != null && Read.from(cs.split(" ")).filter(c_ -> Equals.string(c, c_)).first() != null;
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

		while (0 <= (pos0 = in.indexOf("<", posx)))
			if ((posx = pos0 + 1) < in.length() && !Is.whitespace(in.charAt(posx)))
				if (0 <= (posx = in.indexOf(">", posx))) {
					pairs.add(IntRange.of(pos0, ++posx));

					for (var rawTextTag : List.of("script", "style", "textarea", "title"))
						if (in.startsWith(rawTextTag, pos0 + 1)) {
							posx = in.indexOf("</" + rawTextTag, posx);
							break;
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
				var s = decode(in.substring(prevp, p0)).trim();
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

	public String decode(String in) {
		var charByEscapeToken = escapeTokenByChar.inverse();
		String decoded;

		if (in != null)
			decoded = Build.string(sb -> {
				var index = 0;

				while (index < in.length()) {
					var start = index;
					var ch = in.charAt(index++);

					if (ch == '&') {
						while (index - start <= 8 && index < in.length() && in.charAt(index++) != ';')
							;

						var key = in.substring(start, index);
						String entity;

						if (Get.ch(key, 1) == '#')
							sb.append((char) Integer.parseInt(Substring.of(key, 2, -1)));
						else if ((entity = charByEscapeToken.get(key)) != null)
							sb.append(entity);
						else
							sb.append(key);
					} else
						sb.append(ch);
				}
			});
		else
			decoded = null;

		return decoded;
	}

	public String encode(String in) {
		String encoded;

		if (in != null)
			encoded = Build.string(sb -> {
				for (var index = 0; index < in.length(); index++) {
					var ch = in.charAt(index);
					var escaped = escapeTokenByChar.get(Character.toString(ch));
					var isAscii = 32 <= ch && ch < 128 && ch != '"' && ch != '<' && ch != '>';

					if (escaped != null)
						sb.append(escaped);
					else if (!isAscii)
						sb.append("&#" + (int) ch);
					else
						sb.append(ch);
				}
			});
		else
			encoded = null;

		return encoded;
	}

	private void initialize() {
		putEscapeMap("" + (char) 160, "&nbsp;");
		putEscapeMap("€", "&euro;");
		putEscapeMap(" ", "&nbsp;");
		putEscapeMap("\"", "&quot;");
		putEscapeMap("&", "&amp;");
		putEscapeMap("<", "&lt;");
		putEscapeMap(">", "&gt;");
		putEscapeMap("¡", "&iexcl;");
		putEscapeMap("¢", "&cent;");
		putEscapeMap("£", "&pound;");
		putEscapeMap("¤", "&curren;");
		putEscapeMap("¥", "&yen;");
		putEscapeMap("¦", "&brvbar;");
		putEscapeMap("§", "&sect;");
		putEscapeMap("¨", "&uml;");
		putEscapeMap("©", "&copy;");
		putEscapeMap("ª", "&ordf;");
		putEscapeMap("¬", "&not;");
		putEscapeMap("­", "&shy;");
		putEscapeMap("®", "&reg;");
		putEscapeMap("¯", "&macr;");
		putEscapeMap("°", "&deg;");
		putEscapeMap("±", "&plusmn;");
		putEscapeMap("²", "&sup2;");
		putEscapeMap("³", "&sup3;");
		putEscapeMap("´", "&acute;");
		putEscapeMap("µ", "&micro;");
		putEscapeMap("¶", "&para;");
		putEscapeMap("·", "&middot;");
		putEscapeMap("¸", "&cedil;");
		putEscapeMap("¹", "&sup1;");
		putEscapeMap("º", "&ordm;");
		putEscapeMap("»", "&raquo;");
		putEscapeMap("¼", "&frac14;");
		putEscapeMap("½", "&frac12;");
		putEscapeMap("¾", "&frac34;");
		putEscapeMap("¿", "&iquest;");
		putEscapeMap("À", "&Agrave;");
		putEscapeMap("Á", "&Aacute;");
		putEscapeMap("Â", "&Acirc;");
		putEscapeMap("Ã", "&Atilde;");
		putEscapeMap("Ä", "&Auml;");
		putEscapeMap("Å", "&Aring;");
		putEscapeMap("Æ", "&AElig;");
		putEscapeMap("Ç", "&Ccedil;");
		putEscapeMap("È", "&Egrave;");
		putEscapeMap("É", "&Eacute;");
		putEscapeMap("Ê", "&Ecirc;");
		putEscapeMap("Ë", "&Euml;");
		putEscapeMap("Ì", "&Igrave;");
		putEscapeMap("Í", "&Iacute;");
		putEscapeMap("Î", "&Icirc;");
		putEscapeMap("Ï", "&Iuml;");
		putEscapeMap("Ð", "&ETH;");
		putEscapeMap("Ñ", "&Ntilde;");
		putEscapeMap("Ò", "&Ograve;");
		putEscapeMap("Ó", "&Oacute;");
		putEscapeMap("Ô", "&Ocirc;");
		putEscapeMap("Õ", "&Otilde;");
		putEscapeMap("Ö", "&Ouml;");
		putEscapeMap("×", "&times;");
		putEscapeMap("Ø", "&Oslash;");
		putEscapeMap("Ù", "&Ugrave;");
		putEscapeMap("Ú", "&Uacute;");
		putEscapeMap("Û", "&Ucirc;");
		putEscapeMap("Ü", "&Uuml;");
		putEscapeMap("Ý", "&Yacute;");
		putEscapeMap("Þ", "&THORN;");
		putEscapeMap("ß", "&szlig;");
		putEscapeMap("à", "&agrave;");
		putEscapeMap("á", "&aacute;");
		putEscapeMap("â", "&acirc;");
		putEscapeMap("ã", "&atilde;");
		putEscapeMap("ä", "&auml;");
		putEscapeMap("å", "&aring;");
		putEscapeMap("æ", "&aelig;");
		putEscapeMap("ç", "&ccedil;");
		putEscapeMap("è", "&egrave;");
		putEscapeMap("é", "&eacute;");
		putEscapeMap("ê", "&ecirc;");
		putEscapeMap("ë", "&euml;");
		putEscapeMap("ì", "&igrave;");
		putEscapeMap("í", "&iacute;");
		putEscapeMap("î", "&icirc;");
		putEscapeMap("ï", "&iuml;");
		putEscapeMap("ð", "&eth;");
		putEscapeMap("ñ", "&ntilde;");
		putEscapeMap("ò", "&ograve;");
		putEscapeMap("ó", "&oacute;");
		putEscapeMap("ô", "&ocirc;");
		putEscapeMap("õ", "&otilde;");
		putEscapeMap("ö", "&ouml;");
		putEscapeMap("÷", "&divide;");
		putEscapeMap("ø", "&oslash;");
		putEscapeMap("ù", "&ugrave;");
		putEscapeMap("ú", "&uacute;");
		putEscapeMap("û", "&ucirc;");
		putEscapeMap("ü", "&uuml;");
		putEscapeMap("ý", "&yacute;");
		putEscapeMap("þ", "&thorn;");
	}

	private void putEscapeMap(String ch, String escapeToken) {
		escapeTokenByChar.put(ch, escapeToken);
	}

}
