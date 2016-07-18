package suite.http;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.Constants;
import suite.adt.Pair;
import suite.util.Rethrow;
import suite.util.Util;

public class HttpHeaderUtil {

	public static Map<String, String> getCookieAttrs(String query) {
		String qs[] = query != null ? query.split(";") : new String[0];
		Map<String, String> attrs = new HashMap<>();

		for (String q : qs) {
			Pair<String, String> pair = Util.split2(q, "=");
			attrs.put(pair.t0, decode(pair.t1));
		}

		return attrs;
	}

	public static Map<String, String> getPostedAttrs(InputStream is) {
		int size = 4096;
		BufferedReader br = new BufferedReader(new InputStreamReader(is, Constants.charset));
		StringBuilder sb = new StringBuilder();
		char buffer[] = new char[size];
		int nCharsRead;

		while (0 <= (nCharsRead = Rethrow.ioException(() -> br.read(buffer))))
			sb.append(buffer, 0, nCharsRead);

		return HttpHeaderUtil.getAttrs(sb.toString());
	}

	public static Map<String, String> getAttrs(String query) {
		String qs[] = query != null ? query.split("&") : new String[0];
		Map<String, String> attrs = new HashMap<>();

		for (String q : qs) {
			Pair<String, String> pair = Util.split2(q, "=");
			attrs.put(pair.t0, decode(pair.t1));
		}

		return attrs;
	}

	private static String decode(String s) {
		return Rethrow.ioException(() -> URLDecoder.decode(s, "UTF-8"));
	}

}
