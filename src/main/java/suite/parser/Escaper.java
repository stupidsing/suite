package suite.parser;

import suite.util.LogUtil;

public class Escaper {

	public static String escape(String s, char quote) {
		StringBuilder sb = new StringBuilder();
		sb.append(quote);

		for (char ch : s.toCharArray())
			if (Character.isWhitespace(ch) && ch != ' ') {
				if (ch >= 256)
					sb.append(encodeHex(ch >> 8));
				sb.append(encodeHex(ch & 0xff));
			} else if (ch == quote || ch == '%')
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

				if (pos1 < s.length() && s.charAt(pos1) != '%') {
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

	private static String encodeHex(int i) {
		return "%" + String.format("%02x", i).toUpperCase();
	}

}
