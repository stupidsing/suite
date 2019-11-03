package suite.util;

import primal.Verbs.Build;
import primal.Verbs.Get;
import primal.Verbs.Substring;
import primal.adt.map.BiHashMap;
import primal.adt.map.BiMap;

public class HtmlUtil {

	private BiMap<String, String> escapeTokenByChar = new BiHashMap<>();

	public HtmlUtil() {
		initialize();
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
							if (Character.toLowerCase(Get.ch(key, 2)) == 'x')
								sb.append((char) Integer.parseInt(Substring.of(key, 3, -1), 16));
							else
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
