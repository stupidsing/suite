package suite.http;

import static java.lang.Math.max;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.Header;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClients;

import primal.MoreVerbs.Decode;
import primal.MoreVerbs.Fit;
import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Read;
import primal.Verbs.Sleep;
import primal.adt.FixieArray;
import primal.adt.map.ListMultimap;
import primal.fp.Funs.Fun;
import primal.io.ReadStream;
import primal.os.Log_;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Chars;
import primal.puller.Puller;
import suite.concurrent.Backoff;
import suite.primitive.Bytes_;
import suite.streamlet.As;
import suite.util.Memoize;
import suite.util.To;

public class HttpClient {

	private static long coolDownTime;

	static {
		var s = System.getenv("HTTP_COOLDOWN");
		coolDownTime = s != null ? Long.valueOf(s) : 3000l;
	}

	public static class Request_ {
		private String method;
		private URL url;
		private Puller<Bytes> in;
		private Map<String, String> headers;

		private Request_() {
			this("GET", null, Puller.empty(), Map.ofEntries());
		}

		private Request_(String method, URL url, Puller<Bytes> in, Map<String, String> headers) {
			this.method = method;
			this.url = url;
			this.in = in;
			this.headers = headers;
		}

		public Request_ method(String method) {
			return new Request_(method, url, in, headers);
		}

		public Request_ url(URL url) {
			return new Request_(method, url, in, headers);
		}

		public Request_ in(Puller<Bytes> in) {
			return new Request_(method, url, in, headers);
		}

		public Request_ headers(Map<String, String> headers) {
			return new Request_(method, url, in, headers);
		}

		public ReadStream inputStream() {
			return out().collect(To::inputStream);
		}

		public Puller<String> lines() {
			return out().collect(As::lines);
		}

		public Puller<Chars> utf8() {
			return out().collect(Decode::utf8);
		}

		public Puller<Bytes> out() {
			return send().out;
		}

		public Response_ send() {
			return http_(method, url, in, headers);
		}
	}

	public static class Response_ {
		public final int responseCode;
		public final ListMultimap<String, String> headers;
		public final Puller<Bytes> out;

		private Response_(int responseCode, ListMultimap<String, String> headers, Puller<Bytes> out) {
			this.responseCode = responseCode;
			this.headers = headers;
			this.out = out;
		}
	}

	public static Request_ get(String url) {
		return get(To.url(url));
	}

	public static Request_ get(URL url) {
		return request().url(url);
	}

	public static Request_ request() {
		return new Request_();
	}

	public static Response_ http(String method, URL url) {
		return http(method, url, Puller.empty());
	}

	public static Response_ http(String method, String url, Puller<Bytes> in) {
		return http(method, To.url(url), in);
	}

	public static Response_ http(String method, URL url, Puller<Bytes> in) {
		return http(method, url, in, Map.ofEntries());
	}

	public static Response_ http(String method, URL url, Map<String, String> headers) {
		return http(method, url, Puller.empty(), headers);
	}

	public static Response_ http(String method, URL url, Puller<Bytes> in, Map<String, String> headers) {
		return http_(method, url, in, headers);
	}

	public static Map<String, URI> resolveLinks(URI uri) {
		var out = get(ex(uri::toURL)).utf8().toJoinedString();
		var links = new HashMap<String, URI>();
		FixieArray<String> m;
		while ((m = Fit.partsCaseInsensitive(out, "<a", "href=\"", "\"", ">", "</a>")) != null) {
			var href = m.t2;
			if (!href.startsWith("javascript:"))
				links.putIfAbsent(m.t4, uri.resolve(href));
			out = m.t5;
		}
		return links;
	}

	private static Response_ http_(String method, URL url, Puller<Bytes> in, Map<String, String> headers) {
		var al = timestampFun.apply(url.getHost());
		var backoff = new Backoff();
		long current, last, start, next;

		do
			next = coolDownTime + (start = max(last = al.get(), current = System.currentTimeMillis()));
		while (!al.compareAndSet(last, next) || backoff.exponentially());

		Sleep.quietly(start - current);

		return ex(() -> Boolean.TRUE ? httpApache(method, url, in, headers) : httpJre(method, url, in, headers));
	}

	// keep timestamps to avoid overloading servers
	private static Fun<String, AtomicLong> timestampFun = Memoize.fun(server -> new AtomicLong());

	private static Response_ httpApache(String method, URL url, Puller<Bytes> in, Map<String, String> headers0)
			throws IOException {
		Log_.info("START " + method + " " + url);
		var client = HttpClients.createDefault();

		var request = new HttpRequestBase() {
			{
				setURI(URI.create(url.toString()));
				headers0.entrySet().forEach(e -> addHeader(e.getKey(), e.getValue()));
			}

			public String getMethod() {
				return method;
			}
		};

		var response = client.execute(request);
		var statusLine = response.getStatusLine();
		var statusCode = statusLine.getStatusCode();
		var inputStream = response.getEntity().getContent();
		var headers1 = Read.from(response.getAllHeaders()).map2(Header::getName, Header::getValue).toMultimap();

		var out = Pull //
				.from(inputStream) //
				.closeAtEnd(inputStream) //
				.closeAtEnd(response) //
				.closeAtEnd(client) //
				.closeAtEnd(() -> Log_.info("END__ " + method + " " + url));

		if (statusCode == HttpURLConnection.HTTP_OK)
			return new Response_(statusCode, headers1, out);
		else
			throw new IOException("HTTP returned " + statusCode //
					+ ": " + url //
					+ ": " + statusLine.getReasonPhrase() //
					+ ": " + out.collect(As::string));
	}

	private static Response_ httpJre(String method, URL url, Puller<Bytes> in, Map<String, String> headers) throws IOException {
		var conn = (HttpURLConnection) url.openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod(method);

		headers.entrySet().forEach(e -> conn.setRequestProperty(e.getKey(), e.getValue()));

		try (var os = conn.getOutputStream()) {
			Bytes_.copy(in, os::write);
		}

		var responseCode = conn.getResponseCode();
		var out = Pull.from(conn.getInputStream());

		if (responseCode == HttpURLConnection.HTTP_MOVED_PERM //
				|| responseCode == HttpURLConnection.HTTP_MOVED_TEMP //
				|| responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
			var cookies1 = conn.getHeaderField("Set-Cookie");
			var url1 = To.url(conn.getHeaderField("Location"));

			Map<String, String> headers1 = new HashMap<>(headers);
			if (cookies1 != null)
				headers1.put("Cookie", cookies1);

			return http(method, url1, in, headers1);
		} else if (responseCode == HttpURLConnection.HTTP_OK)
			return new Response_(responseCode, new ListMultimap<>(), out);
		else
			throw new IOException("HTTP returned " + responseCode //
					+ ": " + url //
					+ ": " + conn.getResponseMessage() //
					+ ": " + out.collect(As::string));
	}

}
