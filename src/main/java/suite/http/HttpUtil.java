package suite.http;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import suite.http.HttpServer.Handler;
import suite.util.Pair;
import suite.util.Util;

public class HttpUtil {

	public interface QueryAttrHandler {
		public void handle(String method //
				, String server //
				, String path //
				, Map<String, String> attrs //
				, Map<String, String> headers //
				, InputStream is //
				, OutputStream os) throws IOException;
	}

	public static Handler queryAttrHandler(final QueryAttrHandler queryAttrHandler) {
		return new Handler() {
			public void handle(String method //
					, String server //
					, String path //
					, String query //
					, Map<String, String> headers //
					, InputStream is //
					, OutputStream os) throws IOException {
				queryAttrHandler.handle(method, server, path, getAttrs(query), headers, is, os);
			}
		};
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
