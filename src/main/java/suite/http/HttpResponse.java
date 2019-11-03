package suite.http;

import primal.MoreVerbs.Pull;
import primal.Nouns.Utf8;
import primal.fp.Funs.Sink;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;

public class HttpResponse {

	public static class Status {
		public String line;

		private Status(String line) {
			this.line = line;
		}
	}

	public static final Status HTTP200 = new Status("200 OK");
	public static final Status HTTP206 = new Status("206 Partial Content");
	public static final Status HTTP403 = new Status("403 forbidden");
	public static final Status HTTP404 = new Status("404 file not found");
	public static final Status HTTP500 = new Status("500 internal server error");

	public final String status;
	public final HttpHeader headers;
	public final Puller<Bytes> out;
	public final Sink<Sink<Bytes>> write;

	public static HttpResponse of(Status status) {
		return of(status, new HttpHeader(), Puller.empty());
	}

	public static HttpResponse of(Puller<Bytes> out) {
		return of(HTTP200, new HttpHeader(), out);
	}

	public static HttpResponse of(Status status, String out) {
		var bs = out.getBytes(Utf8.charset);
		return of(status, Pull.from(bs), bs.length);
	}

	public static HttpResponse of(Status status, Puller<Bytes> out, long length) {
		var empty = new HttpHeader();
		return of(status, empty.put("Content-Length", Long.toString(length)), out);
	}

	public static HttpResponse of(Status status, HttpHeader headers, Puller<Bytes> out) {
		return new HttpResponse(status, headers.put("Content-Type", "text/html; charset=UTF-8"), out);
	}

	public static HttpResponse ofWriter(Status status, HttpHeader headers, Sink<Sink<Bytes>> write) {
		return new HttpResponse(status, headers, null, write);
	}

	public HttpResponse(Status status, HttpHeader headers, Puller<Bytes> out) {
		this(status, headers, out, null);
	}

	public HttpResponse(String status, HttpHeader headers, Puller<Bytes> out) {
		this(status, headers, out, null);
	}

	private HttpResponse(Status status, HttpHeader headers, Puller<Bytes> out, Sink<Sink<Bytes>> write) {
		this(status.line, headers, out, write);
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
