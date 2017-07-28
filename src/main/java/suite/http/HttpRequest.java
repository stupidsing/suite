package suite.http;

import java.io.InputStream;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.immutable.IList;
import suite.streamlet.As;
import suite.streamlet.Read;

public class HttpRequest {

	public final String method;
	public final String server;
	public final IList<String> path;
	public final String query;
	public final Map<String, String> headers;
	public final InputStream inputStream;

	public HttpRequest( //
			String method, //
			String server, //
			String path, //
			String query, //
			Map<String, String> headers, //
			InputStream inputStream) {
		this(method, server, HttpHeaderUtil.getPath(path), query, headers, inputStream);
	}

	public HttpRequest( //
			String method, //
			String server, //
			IList<String> path, //
			String query, //
			Map<String, String> headers, //
			InputStream inputStream) {
		this.method = method;
		this.server = server;
		this.path = path;
		this.query = query;
		this.headers = headers;
		this.inputStream = inputStream;
	}

	public Pair<String, HttpRequest> split() {
		if (!path.isEmpty())
			return Pair.of(path.head, new HttpRequest(method, server, path.tail, query, headers, inputStream));
		else
			return Pair.of("", new HttpRequest(method, server, IList.end(), query, headers, inputStream));
	}

	public String getLogString() {
		return method + " " + Read.from(path).collect(As.joinedBy("/"));
	}

}
