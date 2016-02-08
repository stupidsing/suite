package suite.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.adt.Pair;
import suite.os.FileUtil;
import suite.util.Util;

public class HttpHeaderUtil {

	public static Map<String, String> getCookieAttrs(String query) throws UnsupportedEncodingException {
		String qs[] = query != null ? query.split(";") : new String[0];
		Map<String, String> attrs = new HashMap<>();

		for (String q : qs) {
			Pair<String, String> pair = Util.split2(q, "=");
			attrs.put(pair.t0, URLDecoder.decode(pair.t1, "UTF-8"));
		}

		return attrs;
	}

	public static Map<String, String> getPostedAttrs(InputStream is) throws IOException {
		int size = 4096;
		BufferedReader br = new BufferedReader(new InputStreamReader(is, FileUtil.charset));
		StringBuilder sb = new StringBuilder();
		char buffer[] = new char[size];
		int nCharsRead;

		while (0 <= (nCharsRead = br.read(buffer)))
			sb.append(buffer, 0, nCharsRead);

		return HttpHeaderUtil.getAttrs(sb.toString());
	}

	public static Map<String, String> getAttrs(String query) throws UnsupportedEncodingException {
		String qs[] = query != null ? query.split("&") : new String[0];
		Map<String, String> attrs = new HashMap<>();

		for (String q : qs) {
			Pair<String, String> pair = Util.split2(q, "=");
			attrs.put(pair.t0, URLDecoder.decode(pair.t1, "UTF-8"));
		}

		return attrs;
	}

}
