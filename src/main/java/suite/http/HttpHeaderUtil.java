package suite.http;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.Defaults;
import suite.immutable.IList;
import suite.util.Rethrow;
import suite.util.String_;

public class HttpHeaderUtil {

	public static IList<String> getPaths(String pathString) {
		var arr = pathString.split("/");
		var paths = IList.<String> end();
		for (var i = arr.length - 1; i >= 0; i--) {
			String p = arr[i];
			if (!p.isEmpty() && !String_.equals(p, ".."))
				paths = IList.cons(p, paths);
		}
		return paths;
	}

	public static Map<String, String> getCookieAttrs(String query) {
		return decodeMap(query, ";");
	}

	public static Map<String, String> getPostedAttrs(InputStream is) {
		var reader = new InputStreamReader(is, Defaults.charset);
		var sb = new StringBuilder();
		var buffer = new char[Defaults.bufferSize];
		int nCharsRead;

		while (0 <= (nCharsRead = Rethrow.ex(() -> reader.read(buffer))))
			sb.append(buffer, 0, nCharsRead);

		return getAttrs(sb.toString());
	}

	public static Map<String, String> getAttrs(String query) {
		return decodeMap(query, "&");
	}

	private static Map<String, String> decodeMap(String query, String sep) {
		var qs = query != null ? query.split(sep) : new String[0];
		var attrs = new HashMap<String, String>();
		for (var q : qs)
			String_.split2l(q, "=").map((k, v) -> attrs.put(k, decode(v)));
		return attrs;
	}

	private static String decode(String s) {
		return Rethrow.ex(() -> URLDecoder.decode(s, Defaults.charset));
	}

}
