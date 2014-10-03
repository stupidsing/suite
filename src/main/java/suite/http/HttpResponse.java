package suite.http;

import java.io.OutputStream;
import java.util.Map;

public class HttpResponse {

	private String status;
	public final Map<String, String> headers;
	public final OutputStream outputStream;

	public HttpResponse(String status, Map<String, String> headers, OutputStream outputStream) {
		this.status = status;
		this.headers = headers;
		this.outputStream = outputStream;
	}

	public String getLogString() {
		return status;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

}
