package suite.http;

import java.io.InputStream;
import java.util.Map;

public class HttpRequest {

	public final String method;
	public final String server;
	public final String path;
	public final String query;
	public final Map<String, String> headers;
	public final InputStream inputStream;

	public HttpRequest(String method, String server, String path, String query, Map<String, String> headers, InputStream inputStream) {
		this.method = method;
		this.server = server;
		this.path = path;
		this.query = query;
		this.headers = headers;
		this.inputStream = inputStream;
	}

	public String getLogString() {
		return method + " " + path;
	}

}
