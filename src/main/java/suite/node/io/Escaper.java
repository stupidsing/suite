package suite.node.io;

import suite.util.LogUtil;
import suite.util.Util;

public class Escaper {

	public static String escape(String s, char quote) {
		StringBuilder sb = new StringBuilder();
		sb.append(quote);

		for (char ch : Util.chars(s))
			if (Character.isWhitespace(ch) && ch != ' ')
				if (ch >= 256)
					sb.append(encodeHex16(ch >> 8));
				else
					sb.append(encodeHex8(ch & 0xff));
			else if (ch == quote || ch == '%')
				sb.append(ch + "" + ch);
			else
				sb.append(ch);

		sb.append(quote);
		return sb.toString();
	}

	public static String unescape(String s, String quote) {
		s = s.replace(quote + quote, quote);

		try {
			int pos = 0;
			while ((pos = s.indexOf('%', pos)) != -1) {
				int pos1 = pos + 1;

				if (pos1 < s.length() && s.charAt(pos1) == 'U') {
					String hex = s.substring(pos1 + 1, pos + 6);
					char c = (char) Integer.parseInt(hex, 16);
					s = s.substring(0, pos) + c + s.substring(pos + 6);
				} else if (pos1 < s.length() && s.charAt(pos1) != '%') {
					String hex = s.substring(pos1, pos + 3);
					char c = (char) Integer.parseInt(hex, 16);
					s = s.substring(0, pos) + c + s.substring(pos + 3);
				} else
					s = s.substring(0, pos) + s.substring(pos1);

				pos++;
			}
		} catch (StringIndexOutOfBoundsException | NumberFormatException ex) {
			LogUtil.error(ex);
		}

		return s;
	}

	private static String encodeHex16(int i) {
		return "%U" + String.format("%04x", i).toUpperCase();
	}

	private static String encodeHex8(int i) {
		return "%" + String.format("%02x", i).toUpperCase();
	}

}
