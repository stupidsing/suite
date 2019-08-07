package suite.http;

import java.io.InputStream;

import primal.adt.Pair;
import suite.persistent.PerList;

public class HttpRequest {

	public final String method;
	public final String server;
	public final PerList<String> paths;
	public final String query;
	public final HttpHeader headers;
	public final InputStream inputStream;

	public HttpRequest( //
			String method, //
			String server, //
			String path, //
			String query, //
			HttpHeader headers, //
			InputStream inputStream) {
		this(method, server, HttpHeaderUtil.getPaths(path), query, headers, inputStream);
	}

	public HttpRequest( //
			String method, //
			String server, //
			PerList<String> paths, //
			String query, //
			HttpHeader headers, //
			InputStream inputStream) {
		this.method = method;
		this.server = server;
		this.paths = paths;
		this.query = query;
		this.headers = headers;
		this.inputStream = inputStream;
	}

	public String path() {
		return paths.streamlet().fold("", (s0, s1) -> s0 + "/" + s1);
	}

	public Pair<String, HttpRequest> split() {
		if (!paths.isEmpty())
			return Pair.of(paths.head, new HttpRequest(method, server, paths.tail, query, headers, inputStream));
		else
			return Pair.of("", new HttpRequest(method, server, PerList.end(), query, headers, inputStream));
	}

	public String getLogString() {
		return method + " " + paths.streamlet().toJoinedString("/");
	}

}
