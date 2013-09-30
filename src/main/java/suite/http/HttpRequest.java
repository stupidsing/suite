package suite.http;

import java.io.InputStream;
import java.util.Map;

public class HttpRequest {

	private String method;
	private String server;
	private String path;
	private String query;
	private Map<String, String> headers;
	private InputStream inputStream;

	public HttpRequest(String method, String server, String path, String query, Map<String, String> headers, InputStream inputStream) {
		this.method = method;
		this.server = server;
		this.path = path;
		this.query = query;
		this.headers = headers;
		this.inputStream = inputStream;
	}

	public String getLogString() {
		return method + " " + server + " " + path;
	}

	public String getMethod() {
		return method;
	}

	public String getServer() {
		return server;
	}

	public String getPath() {
		return path;
	}

	public String getQuery() {
		return query;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public InputStream getInputStream() {
		return inputStream;
	}

}
