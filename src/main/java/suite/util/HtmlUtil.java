package suite.util;

import suite.adt.BiMap;
import suite.adt.HashBiMap;

public class HtmlUtil {

	private BiMap<String, String> escapeTokenByChar = new HashBiMap<>();

	public HtmlUtil() {
		initialize();
	}

	public String decode(String in) {
		BiMap<String, String> charByEscapeToken = escapeTokenByChar.inverse();
		String decoded;

		if (in != null) {
			StringBuilder sb = new StringBuilder();
			int index = 0;

			while (index < in.length()) {
				int start = index;
				char ch = in.charAt(index++);

				if (ch == '&') {
					while (in.charAt(index++) != ';')
						;

					String key = in.substring(start, index);

					if (String_.charAt(key, 1) == '#')
						sb.append((char) Integer.parseInt(String_.range(key, 2, -1)));
					else {
						String entity = charByEscapeToken.get(key);

						if (entity != null)
							sb.append(entity);
						else
							sb.append(key);
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
			StringBuilder sb = new StringBuilder();

			for (int index = 0; index < in.length(); index++) {
				char ch = in.charAt(index);

				if (ch < 32 || 128 <= ch || ch == '"' || ch == '<' || ch == '>') {
					String escaped = escapeTokenByChar.get("" + ch);

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
