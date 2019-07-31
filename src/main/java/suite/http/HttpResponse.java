package suite.http;

import primal.fp.Funs.Sink;
import suite.primitive.Bytes;
import suite.streamlet.Puller;

public class HttpResponse {

	public static final String HTTP200 = "200 OK";
	public static final String HTTP206 = "206 Partial Content";
	public static final String HTTP403 = "403 forbidden";
	public static final String HTTP404 = "404 file not found";
	public static final String HTTP500 = "500 internal server error";

	public final String status;
	public final HttpHeader headers;
	public final Puller<Bytes> out;
	public final Sink<Sink<Bytes>> write;

	public static HttpResponse of(String status) {
		return of(status, new HttpHeader(), Puller.empty());
	}

	public static HttpResponse of(Puller<Bytes> out) {
		return of(HTTP200, new HttpHeader(), out);
	}

	public static HttpResponse of(String status, Puller<Bytes> out, long length) {
		var empty = new HttpHeader();
		return of(status, empty.put("Content-Length", Long.toString(length)), out);
	}

	public static HttpResponse of(String status, HttpHeader headers, Puller<Bytes> out) {
		return new HttpResponse(status, headers.put("Content-Type", "text/html; charset=UTF-8"), out);
	}

	public static HttpResponse ofWriter(String status, HttpHeader headers, Sink<Sink<Bytes>> write) {
		return new HttpResponse(status, headers, null, write);
	}

	public HttpResponse(String status, HttpHeader headers, Puller<Bytes> out) {
		this(status, headers, out, null);
	}

	private HttpResponse(String status, HttpHeader headers, Puller<Bytes> out, Sink<Sink<Bytes>> write) {
		this.status = status;
		this.headers = headers;
		this.out = out;
		this.write = write;
	}

	public String getLogString() {
		return status;
	}

}
