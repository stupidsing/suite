package suite.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class HtmlUtil {

	private static final BiMap<String, String> escapeMap = getEscapeMap();

	public static String decode(String in) {
		StringBuilder sb = new StringBuilder();
		int index = 0;

		while (index < in.length()) {
			int start = index;
			char ch = in.charAt(index++);

			if (ch == '&') {
				while (in.charAt(index++) != ';')
					;

				String key = in.substring(start, index);

				if (Util.charAt(key, 1) == '#')
					sb.append((char) Integer.parseInt(Util.substr(key, 2, -1)));
				else {
					String entity = escapeMap.inverse().get(key);

					if (entity != null)
						sb.append(entity);
					else
						sb.append(key);
				}
			} else
				sb.append(ch);
		}

		return sb.toString();
	}

	public static String encode(String in) {
		StringBuilder sb = new StringBuilder();

		for (int index = 0; index < in.length(); index++) {
			char ch = in.charAt(index);

			if (ch < 32 || ch >= 128 || ch == '"' || ch == '<' || ch == '>') {
				String escaped = escapeMap.get("" + ch);

				if (escaped != null)
					sb.append(escaped);
				else
					sb.append("&#" + (int) ch + ";");
			} else
				sb.append(ch);
		}

		return sb.toString();
	}

	private static BiMap<String, String> getEscapeMap() {
		BiMap<String, String> escapeMap = HashBiMap.create();
		escapeMap.put("€", "&euro;");
		escapeMap.put("Space", "&nbsp;");
		escapeMap.put("\"", "&quot;");
		escapeMap.put("&", "&amp;");
		escapeMap.put("<", "&lt;");
		escapeMap.put(">", "&gt;");
		escapeMap.put("" + (char) 160, "&nbsp;");
		escapeMap.put("¡", "&iexcl;");
		escapeMap.put("¢", "&cent;");
		escapeMap.put("£", "&pound;");
		escapeMap.put("¤", "&curren;");
		escapeMap.put("¥", "&yen;");
		escapeMap.put("¦", "&brvbar;");
		escapeMap.put("§", "&sect;");
		escapeMap.put("¨", "&uml;");
		escapeMap.put("©", "&copy;");
		escapeMap.put("ª", "&ordf;");
		escapeMap.put("¬", "&not;");
		escapeMap.put("­", "&shy;");
		escapeMap.put("®", "&reg;");
		escapeMap.put("¯", "&macr;");
		escapeMap.put("°", "&deg;");
		escapeMap.put("±", "&plusmn;");
		escapeMap.put("²", "&sup2;");
		escapeMap.put("³", "&sup3;");
		escapeMap.put("´", "&acute;");
		escapeMap.put("µ", "&micro;");
		escapeMap.put("¶", "&para;");
		escapeMap.put("·", "&middot;");
		escapeMap.put("¸", "&cedil;");
		escapeMap.put("¹", "&sup1;");
		escapeMap.put("º", "&ordm;");
		escapeMap.put("»", "&raquo;");
		escapeMap.put("¼", "&frac14;");
		escapeMap.put("½", "&frac12;");
		escapeMap.put("¾", "&frac34;");
		escapeMap.put("¿", "&iquest;");
		escapeMap.put("À", "&Agrave;");
		escapeMap.put("Á", "&Aacute;");
		escapeMap.put("Â", "&Acirc;");
		escapeMap.put("Ã", "&Atilde;");
		escapeMap.put("Ä", "&Auml;");
		escapeMap.put("Å", "&Aring;");
		escapeMap.put("Æ", "&AElig;");
		escapeMap.put("Ç", "&Ccedil;");
		escapeMap.put("È", "&Egrave;");
		escapeMap.put("É", "&Eacute;");
		escapeMap.put("Ê", "&Ecirc;");
		escapeMap.put("Ë", "&Euml;");
		escapeMap.put("Ì", "&Igrave;");
		escapeMap.put("Í", "&Iacute;");
		escapeMap.put("Î", "&Icirc;");
		escapeMap.put("Ï", "&Iuml;");
		escapeMap.put("Ð", "&ETH;");
		escapeMap.put("Ñ", "&Ntilde;");
		escapeMap.put("Ò", "&Ograve;");
		escapeMap.put("Ó", "&Oacute;");
		escapeMap.put("Ô", "&Ocirc;");
		escapeMap.put("Õ", "&Otilde;");
		escapeMap.put("Ö", "&Ouml;");
		escapeMap.put("×", "&times;");
		escapeMap.put("Ø", "&Oslash;");
		escapeMap.put("Ù", "&Ugrave;");
		escapeMap.put("Ú", "&Uacute;");
		escapeMap.put("Û", "&Ucirc;");
		escapeMap.put("Ü", "&Uuml;");
		escapeMap.put("Ý", "&Yacute;");
		escapeMap.put("Þ", "&THORN;");
		escapeMap.put("ß", "&szlig;");
		escapeMap.put("à", "&agrave;");
		escapeMap.put("á", "&aacute;");
		escapeMap.put("â", "&acirc;");
		escapeMap.put("ã", "&atilde;");
		escapeMap.put("ä", "&auml;");
		escapeMap.put("å", "&aring;");
		escapeMap.put("æ", "&aelig;");
		escapeMap.put("ç", "&ccedil;");
		escapeMap.put("è", "&egrave;");
		escapeMap.put("é", "&eacute;");
		escapeMap.put("ê", "&ecirc;");
		escapeMap.put("ë", "&euml;");
		escapeMap.put("ì", "&igrave;");
		escapeMap.put("í", "&iacute;");
		escapeMap.put("î", "&icirc;");
		escapeMap.put("ï", "&iuml;");
		escapeMap.put("ð", "&eth;");
		escapeMap.put("ñ", "&ntilde;");
		escapeMap.put("ò", "&ograve;");
		escapeMap.put("ó", "&oacute;");
		escapeMap.put("ô", "&ocirc;");
		escapeMap.put("õ", "&otilde;");
		escapeMap.put("ö", "&ouml;");
		escapeMap.put("÷", "&divide;");
		escapeMap.put("ø", "&oslash;");
		escapeMap.put("ù", "&ugrave;");
		escapeMap.put("ú", "&uacute;");
		escapeMap.put("û", "&ucirc;");
		escapeMap.put("ü", "&uuml;");
		escapeMap.put("ý", "&yacute;");
		escapeMap.put("þ", "&thorn;");
		return escapeMap;
	}

}
