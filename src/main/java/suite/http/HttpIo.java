package suite.http;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import primal.MoreVerbs.Pull;
import primal.MoreVerbs.Split;
import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.Verbs.ReadLine;
import primal.adt.FixieArray;
import primal.fp.Funs2.Fun2;
import primal.io.ReadStream;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
import suite.http.Http.Header;
import suite.http.Http.Request;
import suite.http.Http.Response;
import suite.util.Copy;
import suite.util.To;

public class HttpIo {

	public Request readRequest(Puller<Bytes> in0) {
		return readRequest(To.inputStream(in0));
	}

	public Request readRequest(InputStream is0) {
		var ls = ReadLine.from(is0).split(" ");
		var headers = readHeaders(is0);

		return FixieArray.of(ls).map((method, url, protocol) -> {
			Fun2<String, String, Request> requestFun = (host, pqs) -> Split.strl(pqs, "?").map((path0, query) -> {
				var is1 = getContentStream(is0, headers);
				var path1 = path0.startsWith("/") ? path0 : "/" + path0;
				var path2 = ex(() -> URLDecoder.decode(path1, Utf8.charset));

				return Equals.string(protocol, "HTTP/1.1") //
						? new Request(method, host, path2, query, headers, Pull.from_(is1)) //
						: fail("only HTTP/1.1 is supported");
			});

			var pp = Split.string(url, "://");
			return pp != null ? Split.strl(pp.v, "/").map(requestFun) : requestFun.apply("", url);
		});
	}

	public Response readResponse(InputStream is0) {
		var ls = ReadLine.from(is0).split(" ");
		var headers = readHeaders(is0);

		return FixieArray.of(ls).map((protocol, status) -> {
			var is1 = headers //
					.getOpt("Content-Length") //
					.map(Integer::parseInt) //
					.map(cl -> sizeLimitedInputStream(is0, cl)) //
					.or(is0);

			return Equals.string(protocol, "HTTP/1.1") //
					? new Response(status, headers, Pull.from(is1)) //
					: fail("only HTTP/1.1 is supported");
		});
	}

	public void writeRequest(OutputStream os, Request request) throws IOException {
		var server = request.server;
		var path = request.path();
		var query = request.query;
		var url = (!server.isEmpty() ? "http://" + server + "/" : "") + path + (!query.isEmpty() ? "?" + query : "");

		var s = request.method + " " + url + " HTTP/1.1\r\n" //
				+ request.headers.streamlet().map((k, v) -> k + ": " + v + "\r\n").toJoinedString() //
				+ "\r\n";

		os.write(s.getBytes(Utf8.charset));
		Copy.stream(To.inputStream(request.in), os);
	}

	public void writeResponse(OutputStream os, Response response) throws IOException {
		var s = "HTTP/1.1 " + response.status + "\r\n" //
				+ response.headers.streamlet().map((k, v) -> k + ": " + v + "\r\n").toJoinedString() //
				+ "\r\n";

		os.write(s.getBytes(Utf8.charset));
		var body = response.body;

		if (body != null)
			Copy.stream(body.collect(To::inputStream), os);
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

	private Header readHeaders(InputStream is) {
		var headers = new Header();
		String line;
		while (!(line = ReadLine.from(is)).isEmpty()) {
			var headers0 = headers;
			headers = Split.strl(line, ":").map((k, v) -> headers0.put(k, v));
		}
		return headers;
	}

	private InputStream getContentStream(InputStream is, Header headers) {
		return headers.getOpt("Content-Length").map(Integer::parseInt).map(cl -> sizeLimitedInputStream(is, cl)).or(is);
	}

	private InputStream sizeLimitedInputStream(InputStream is, int size) {
		return new ReadStream(is) {
			private int remaining = size;

			public int read() throws IOException {
				return 0 < remaining-- ? is.read() : -1;
			}

			public int read(byte[] bs) throws IOException {
				return read(bs, 0, bs.length);
			}

			public int read(byte[] bs, int offset, int length) throws IOException {
				if (0 < remaining) {
					var result = is.read(bs, offset, min(length, remaining));
					remaining -= max(0, result);
					return result;
				} else
					return -1;
			}
		};
	}

}
