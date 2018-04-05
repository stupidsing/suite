package suite.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.Constants;
import suite.immutable.IList;
import suite.util.Rethrow;
import suite.util.String_;

public class HttpHeaderUtil {

	public static IList<String> getPath(String pathString) {
		var arr = pathString.split("/");
		IList<String> path = IList.end();
		for (var i = arr.length - 1; i >= 0; i--)
			if (!arr[i].isEmpty())
				path = IList.cons(arr[i], path);
		return path;
	}

	public static Map<String, String> getCookieAttrs(String query) {
		var qs = query != null ? query.split(";") : new String[0];
		Map<String, String> attrs = new HashMap<>();

		for (var q : qs) {
			var pair = String_.split2(q, "=");
			attrs.put(pair.t0, decode(pair.t1));
		}

		return attrs;
	}

	public static Map<String, String> getPostedAttrs(InputStream is) {
		var br = new BufferedReader(new InputStreamReader(is, Constants.charset));
		var sb = new StringBuilder();
		var buffer = new char[Constants.bufferSize];
		int nCharsRead;

		while (0 <= (nCharsRead = Rethrow.ex(() -> br.read(buffer))))
			sb.append(buffer, 0, nCharsRead);

		return getAttrs(sb.toString());
	}

	public static Map<String, String> getAttrs(String query) {
		var qs = query != null ? query.split("&") : new String[0];
		Map<String, String> attrs = new HashMap<>();

		for (var q : qs) {
			var pair = String_.split2(q, "=");
			attrs.put(pair.t0, decode(pair.t1));
		}

		return attrs;
	}

	private static String decode(String s) {
		return Rethrow.ex(() -> URLDecoder.decode(s, "UTF-8"));
	}

}
