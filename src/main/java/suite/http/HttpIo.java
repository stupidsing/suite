package suite.http;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.Verbs.ReadLine;
import primal.Verbs.Split;
import primal.adt.FixieArray;
import primal.adt.Opt;
import primal.fp.Funs2.Fun2;
import primal.io.ReadStream;
import suite.util.Copy;
import suite.util.To;

public class HttpIo {

	public HttpRequest readRequest(InputStream is0) {
		var ls = ReadLine.from(is0).split(" ");
		var headers = readHeaders(is0);

		return FixieArray.of(ls).map((method, url, protocol) -> {
			Fun2<String, String, HttpRequest> requestFun = (host, pqs) -> Split.strl(pqs, "?")
					.map((path0, query) -> {
						var is1 = getContentStream(is0, headers);
						var path1 = path0.startsWith("/") ? path0 : "/" + path0;
						var path2 = ex(() -> URLDecoder.decode(path1, Utf8.charset));

						return Equals.string(protocol, "HTTP/1.1") //
								? new HttpRequest(method, host, path2, query, headers, is1) //
								: fail("only HTTP/1.1 is supported");
					});

			var pp = Split.string(url, "://");
			return pp != null ? Split.strl(pp.v, "/").map(requestFun) : requestFun.apply("", url);
		});
	}

	public HttpResponse readResponse(InputStream is0) {
		var ls = ReadLine.from(is0).split(" ");
		var headers = readHeaders(is0);

		return FixieArray.of(ls).map((protocol, status) -> {
			var cl = Opt.of(headers.get("Content-Length")).map(Integer::parseInt);
			var is1 = !cl.isEmpty() ? sizeLimitedInputStream(is0, cl.get()) : is0;

			return Equals.string(protocol, "HTTP/1.1") //
					? new HttpResponse(status, headers, To.puller(is1)) //
					: fail("only HTTP/1.1 is supported");
		});
	}

	public void writeRequest(OutputStream os, HttpRequest request) throws IOException {
		var server = request.server;
		var path = request.path();
		var query = request.query;
		var url = (!server.isEmpty() ? "http://" + server + "/" : "") + path + (!query.isEmpty() ? "?" + query : "");

		var s = request.method + " " + url + " HTTP/1.1\r\n" //
				+ request.headers.streamlet().map((k, v) -> k + ": " + v + "\r\n").toJoinedString() //
				+ "\r\n";

		os.write(s.getBytes(Utf8.charset));
		Copy.stream(request.inputStream, os);
	}

	public void writeResponse(OutputStream os, HttpResponse response) throws IOException {
		var s = "HTTP/1.1 " + response.status + "\r\n" //
				+ response.headers.streamlet().map((k, v) -> k + ": " + v + "\r\n").toJoinedString() //
				+ "\r\n";

		os.write(s.getBytes(Utf8.charset));
		var out = response.out;

		if (out != null)
			Copy.stream(out.collect(To::inputStream), os);
		else {
			response.write.f(bytes -> {
				try {
					if (bytes != null) {
						os.write(bytes.toArray());
						os.flush();
					} else
						os.close();
				} catch (Exception ex) {
					throw new RuntimeException(ex);
				}
			});
		}
	}

	private HttpHeader readHeaders(InputStream is) {
		var headers = new HttpHeader();
		String line;
		while (!(line = ReadLine.from(is)).isEmpty()) {
			var headers0 = headers;
			headers = Split.strl(line, ":").map((k, v) -> headers0.put(k, v));
		}
		return headers;
	}

	private InputStream getContentStream(InputStream is, HttpHeader headers) {
		var cl = Opt.of(headers.get("Content-Length")).map(Integer::parseInt);
		return !cl.isEmpty() ? sizeLimitedInputStream(is, cl.get()) : is;
	}

	private InputStream sizeLimitedInputStream(InputStream is, int size) {
		return new ReadStream(is) {
			private int remaining = size;

			public int read() throws IOException {
				return 0 < remaining-- ? is.read() : -1;
			}

			public int read(byte[] bytes, int offset, int length) throws IOException {
				if (0 < remaining) {
					var result = is.read(bytes, offset, min(length, remaining));
					remaining -= max(0, result);
					return result;
				} else
					return -1;
			}
		};
	}

}
