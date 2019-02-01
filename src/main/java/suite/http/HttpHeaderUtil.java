package suite.http;

import static suite.util.Friends.rethrow;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Map;

import suite.cfg.Defaults;
import suite.persistent.PerList;
import suite.streamlet.Read;
import suite.util.String_;

public class HttpHeaderUtil {

	public static PerList<String> getPaths(String pathString) {
		var arr = pathString.split("/");
		var paths = PerList.<String> end();
		for (var i = arr.length - 1; i >= 0; i--) {
			String p = arr[i];
			if (!p.isEmpty() && !String_.equals(p, ".."))
				paths = PerList.cons(p, paths);
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

		while (0 <= (nCharsRead = rethrow(() -> reader.read(buffer))))
			sb.append(buffer, 0, nCharsRead);

		return getAttrs(sb.toString());
	}

	public static Map<String, String> getAttrs(String query) {
		return decodeMap(query, "&");
	}

	private static Map<String, String> decodeMap(String query, String sep) {
		var qs = query != null ? query.split(sep) : new String[0];
		return Read //
				.from(qs) //
				.concatMap2(q -> Read.each2(String_.split2l(q, "="))) //
				.mapValue(HttpHeaderUtil::decode) //
				.toMap();
	}

	private static String decode(String s) {
		return rethrow(() -> URLDecoder.decode(s, Defaults.charset));
	}

}
