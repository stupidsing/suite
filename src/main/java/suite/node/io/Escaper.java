package suite.node.io;

import primal.os.Log_;
import suite.util.String_;

public class Escaper {

	public static String escape(String s, char quote) {
		return String_.build(sb -> {
			sb.append(quote);

			for (var ch : String_.chars(s))
				if (Character.isWhitespace(ch) && ch != ' ')
					if (256 <= ch)
						sb.append(encodeHex16(ch >> 8));
					else
						sb.append(encodeHex8(ch & 0xff));
				else if (ch == quote || ch == '%')
					sb.append(ch + "" + ch);
				else
					sb.append(ch);

			sb.append(quote);
		});
	}

	public static String unescape(String s, String quote) {
		s = s.replace(quote + quote, quote);

		try {
			for (var pos = 0; pos < s.length(); pos++)
				if (s.startsWith("%U", pos) && pos + 6 <= s.length()) {
					char c = (char) Integer.parseInt(s.substring(pos + 2, pos + 6), 16);
					s = s.substring(0, pos) + c + s.substring(pos + 6);
				} else if (s.startsWith("%%", pos))
					s = s.substring(0, pos) + "%" + s.substring(pos + 2);
				else if (s.startsWith("%", pos) && pos + 3 <= s.length()) {
					char c = (char) Integer.parseInt(s.substring(pos + 1, pos + 3), 16);
					s = s.substring(0, pos) + c + s.substring(pos + 3);
				}
		} catch (StringIndexOutOfBoundsException | NumberFormatException ex) {
			Log_.error(ex);
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
