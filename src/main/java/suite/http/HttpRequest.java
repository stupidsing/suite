package suite.http;

import java.io.InputStream;

import suite.adt.pair.Pair;
import suite.immutable.IList;
import suite.immutable.IMap;
import suite.streamlet.As;
import suite.streamlet.Read;

public class HttpRequest {

	public final String method;
	public final String server;
	public final IList<String> paths;
	public final String query;
	public final IMap<String, String> headers;
	public final InputStream inputStream;

	public HttpRequest( //
			String method, //
			String server, //
			String path, //
			String query, //
			IMap<String, String> headers, //
			InputStream inputStream) {
		this(method, server, HttpHeaderUtil.getPaths(path), query, headers, inputStream);
	}

	public HttpRequest( //
			String method, //
			String server, //
			IList<String> paths, //
			String query, //
			IMap<String, String> headers, //
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
			return Pair.of("", new HttpRequest(method, server, IList.end(), query, headers, inputStream));
	}

	public String getLogString() {
		return method + " " + Read.from(paths).collect(As.joinedBy("/"));
	}

}
