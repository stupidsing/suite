package suite.http;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.adt.Opt;
import primal.adt.Pair;
import primal.fp.Funs.Sink;
import primal.persistent.PerList;
import primal.persistent.PerMap;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.LngMutable;
import primal.puller.Puller;
import primal.streamlet.Streamlet2;
import suite.primitive.IoSink;

public class Http {

	public static final Status S200 = new Status("200 OK");
	public static final Status S206 = new Status("206 Partial Content");
	public static final Status S403 = new Status("403 forbidden");
	public static final Status S404 = new Status("404 file not found");
	public static final Status S405 = new Status("404 method not allowed");
	public static final Status S500 = new Status("500 internal server error");

	public static final Response R403 = Response.of(S403);
	public static final Response R404 = Response.of(S404);
	public static final Response R405 = Response.of(S405);
	public static final Response R500 = Response.of(S500);

	public interface Handler {
		public Response handle(Request request);
	}

	public interface HandlerAsync {
		public void handle(Request request, IoSink<Response> sink);
	}

	public static class Header {
		private PerMap<String, PerList<String>> map;

		public Header() {
			this(PerMap.empty());
		}

		public Header(PerMap<String, PerList<String>> map) {
			this.map = map;
		}

		public Opt<String> getOpt(String key) {
			return map.getOpt(key).map(opt -> opt.streamlet().uniqueResult());
		}

		public String getOrFail(String key) {
			return getOpt(key).g();
		}

		public Header put(String key, String value) {
			var list = map.getOpt(key).ifNone(PerList::end).g();
			return new Header(map.replace(key, PerList.cons(value, list)));
		}

		public Header remove(String key) {
			return new Header(map.remove(key));
		}

		public Streamlet2<String, String> streamlet() {
			return Read.from2(map).concatMapValue(Read::from);
		}
	}

	public static class Request {
		public final String method;
		public final String server;
		public final PerList<String> paths;
		public final String query;
		public final Header headers;
		public final Puller<Bytes> in;

		public Request(
				String method,
				String server,
				String path,
				String query,
				Header headers,
				Puller<Bytes> in) {
			this(method, server, HttpHeaderUtil.getPaths(path), query, headers, in);
		}

		public Request(
				String method,
				String server,
				PerList<String> paths,
				String query,
				Header headers,
				Puller<Bytes> in) {
			this.method = method;
			this.server = server;
			this.paths = paths;
			this.query = query;
			this.headers = headers;
			this.in = in;
		}

		public Pair<String, Request> split() {
			if (!paths.isEmpty())
				return Pair.of(paths.head, new Request(method, server, paths.tail, query, headers, in));
			else
				return Pair.of("", new Request(method, server, PerList.end(), query, headers, in));
		}

		public String getLogString() {
			return method + " " + path();
		}

		public String path() {
			return paths.streamlet().map(path -> "/" + path).toJoinedString();
		}
	}

	public static class Response {
		public final String status;
		public final Header headers;
		public final Puller<Bytes> body;
		public final Sink<Sink<Bytes>> write;

		private static Response of(Status status) {
			return of(status, new Header(), Puller.empty());
		}

		public static Response of(Puller<Bytes> body) {
			return of(S200, new Header(), body);
		}

		public static Response of(Status status, String body) {
			var bs = body.getBytes(Utf8.charset);
			return of(status, Pull.from(bs), bs.length);
		}

		public static Response of(Status status, Puller<Bytes> body, long length) {
			var empty = new Header();
			return of(status, empty.put("Content-Length", Long.toString(length)), body);
		}

		public static Response of(Status status, Header headers, Puller<Bytes> body) {
			return new Response(status, headers.put("Content-Type", "text/html; charset=UTF-8"), body);
		}

		public static Response ofWriter(Status status, Header headers, Sink<Sink<Bytes>> write) {
			return new Response(status, headers, null, write);
		}

		public Response(Status status, Header headers, Puller<Bytes> body) {
			this(status, headers, body, null);
		}

		public Response(String status, Header headers, Puller<Bytes> body) {
			this(status, headers, body, null);
		}

		private Response(Status status, Header headers, Puller<Bytes> body, Sink<Sink<Bytes>> write) {
			this(status.line, headers, body, write);
		}

		private Response(String status, Header headers, Puller<Bytes> body, Sink<Sink<Bytes>> write) {
			this.status = status;
			this.headers = headers;
			this.body = body;
			this.write = write;
		}

		public String getLogString() {
			return status;
		}
	}

	public static class Session {
		public final String username;
		public final LngMutable lastRequestDt;

		public Session(String username, long current) {
			this.username = username;
			lastRequestDt = LngMutable.of(current);
		}
	}

	public interface SessionManager {
		public Session get(String id);

		public void put(String id, Session session);

		public void remove(String id);
	}

	public static class Status {
		public final String line;

		private Status(String line) {
			this.line = line;
		}
	}

}
