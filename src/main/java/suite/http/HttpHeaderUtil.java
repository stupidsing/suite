package suite.http;

import static primal.statics.Rethrow.ex;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Nouns.Buffer;
import primal.Nouns.Utf8;
import primal.Verbs.Build;
import primal.Verbs.Equals;
import primal.persistent.PerList;

public class HttpHeaderUtil {

	public static PerList<String> getPaths(String pathString) {
		var arr = pathString.split("/");
		var paths = PerList.<String> end();
		for (var i = arr.length - 1; i >= 0; i--) {
			String p = arr[i];
			if (!p.isEmpty() && !Equals.string(p, ".."))
				paths = PerList.cons(p, paths);
		}
		return paths;
	}

	public static Map<String, String> getCookieAttrs(String query) {
		return decodeMap(query, ";");
	}

	public static Map<String, String> getPostedAttrs(InputStream is) {
		var reader = new InputStreamReader(is, Utf8.charset);

		var query = Build.string(sb -> {
			var buffer = new char[Buffer.size];
			int nCharsRead;

			while (0 <= (nCharsRead = ex(() -> reader.read(buffer))))
				sb.append(buffer, 0, nCharsRead);
		});

		return getAttrs(query);
	}

	public static Map<String, String> getAttrs(String query) {
		return decodeMap(query, "&");
	}

	private static Map<String, String> decodeMap(String query, String sep) {
		var qs = query != null ? query.split(sep) : new String[0];
		return Read //
				.from(qs) //
				.concatMap2(q -> Read.each2(Split.strl(q, "="))) //
				.mapValue(HttpHeaderUtil::decode) //
				.toMap();
	}

	private static String decode(String s) {
		return ex(() -> URLDecoder.decode(s, Utf8.charset));
	}

}
