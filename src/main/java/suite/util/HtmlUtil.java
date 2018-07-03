package suite.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import suite.adt.map.BiMap;
import suite.adt.map.HashBiMap;
import suite.primitive.adt.pair.IntIntPair;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.FunUtil.Fun;

public class HtmlUtil {

	private BiMap<String, String> escapeTokenByChar = new HashBiMap<>();

	public HtmlUtil() {
		initialize();
	}

	public String decode(String in) {
		var charByEscapeToken = escapeTokenByChar.inverse();
		String decoded;

		if (in != null) {
			var sb = new StringBuilder();
			var index = 0;

			while (index < in.length()) {
				var start = index;
				var ch = in.charAt(index++);

				if (ch == '&') {
					while (in.charAt(index++) != ';')
						;

					var key = in.substring(start, index);

					if (String_.charAt(key, 1) == '#')
						sb.append((char) Integer.parseInt(String_.range(key, 2, -1)));
					else {
						var entity = charByEscapeToken.get(key);
						sb.append(entity != null ? entity : key);
					}
				} else
					sb.append(ch);
			}

			decoded = sb.toString();
		} else
			decoded = null;

		return decoded;
	}

	public String encode(String in) {
		String encoded;

		if (in != null) {
			var sb = new StringBuilder();

			for (var index = 0; index < in.length(); index++) {
				var ch = in.charAt(index);

				if (ch < 32 || 128 <= ch || ch == '"' || ch == '<' || ch == '>') {
					var escaped = escapeTokenByChar.get("" + ch);

					if (escaped != null)
						sb.append(escaped);
					else
						sb.append("&#" + (int) ch + ";");
				} else
					sb.append(ch);
			}

			encoded = sb.toString();
		} else
			encoded = null;

		return encoded;
	}

	public class HtmlNode {
		public final String tag;
		public final List<HtmlNode> children = new ArrayList<>();

		private HtmlNode(String tag) {
			this.tag = tag;
		}
	}

	public HtmlNode parse(String in) {
		var pairs = new ArrayList<IntIntPair>();
		int pos0, pos1 = 0;

		while (0 <= (pos0 = in.indexOf("<", pos1)) && 0 <= (pos1 = in.indexOf(">", pos0 + 1)))
			pairs.add(IntIntPair.of(pos0, ++pos1));

		Fun<String, IntObjPair<String>> getNameFun = tag -> {
			int p0 = 1, p1 = p0 + 1, px = tag.length() - 1;
			var first = tag.charAt(p1);
			var last = tag.charAt(px - 1);
			int d;

			if (first == '!')
				return IntObjPair.of(0, tag);
			else {
				if (first == '/') {
					p1++;
					d = -1;
				} else if (last == '/') {
					px--;
					d = 0;
				} else
					d = 1;

				var ps0 = tag.indexOf(' ');
				var ps1 = 0 <= ps0 ? ps0 : px;
				return IntObjPair.of(d, tag.substring(p1, ps1));
			}
		};

		var deque = new ArrayDeque<>(List.of(new HtmlNode(null)));
		var prevp = 0;

		for (var pair : pairs) {
			HtmlNode htmlNode = deque.element(), htmlNode1;
			var p0 = pair.t0;
			var px = pair.t1;

			htmlNode.children.add(new HtmlNode(in.substring(prevp, p0)));

			var tag = in.substring(p0, px);
			var dn = getNameFun.apply(tag);
			var d = dn.t0;
			var name = dn.t1;

			if (d == -1)
				while (!deque.isEmpty() && !String_.equals(getNameFun.apply(deque.pop().tag).t1, name))
					;
			else {
				htmlNode.children.add(htmlNode1 = new HtmlNode(tag));
				if (d == 1)
					deque.push(htmlNode1);
			}

			prevp = px;
		}

		return deque.pop();

	}

	private void initialize() {
		putEscapeMap("€", "&euro;");
		putEscapeMap(" ", "&nbsp;");
		putEscapeMap("\"", "&quot;");
		putEscapeMap("&", "&amp;");
		putEscapeMap("<", "&lt;");
		putEscapeMap(">", "&gt;");
		putEscapeMap("" + (char) 160, "&nbsp;");
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
