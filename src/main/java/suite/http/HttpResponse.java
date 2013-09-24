package suite.http;

import java.io.OutputStream;
import java.util.Map;

public class HttpResponse {

	private String status;
	private Map<String, String> headers;
	private OutputStream outputStream;

	public HttpResponse(String status, Map<String, String> headers, OutputStream outputStream) {
		this.status = status;
		this.headers = headers;
		this.outputStream = outputStream;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public OutputStream getOutputStream() {
		return outputStream;
	}

}
