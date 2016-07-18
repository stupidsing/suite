package suite.http;

import suite.immutable.IMap;
import suite.primitive.Bytes;
import suite.util.FunUtil.Source;

public class HttpResponse {

	public static final String HTTP200 = "200 OK";
	public static final String HTTP403 = "403 forbidden";
	public static final String HTTP500 = "500 internal server error";

	public final String status;
	public final IMap<String, String> headers;
	public final Source<Bytes> out;

	public static HttpResponse of(String status) {
		return of(() -> null);
	}

	public static HttpResponse of(Source<Bytes> out) {
		return of(HTTP200, IMap.empty(), out);
	}

	public static HttpResponse of(String status, IMap<String, String> headers, Source<Bytes> out, long length) {
		return of(status, headers.put("Content-Length", Long.toString(length)), out);
	}

	public static HttpResponse of(String status, IMap<String, String> headers, Source<Bytes> out) {
		return new HttpResponse(status, headers.put("Content-Type", "text/html; charset=UTF-8"), out);
	}

	public HttpResponse(String status, IMap<String, String> headers, Source<Bytes> out) {
		this.status = status;
		this.headers = headers;
		this.out = out;
	}

	public String getLogString() {
		return status;
	}

}
