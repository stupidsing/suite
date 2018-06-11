package suite.http;

import static suite.util.Friends.min;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;

import suite.Constants;
import suite.adt.Opt;
import suite.adt.pair.FixieArray;
import suite.immutable.IMap;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.Fail;
import suite.util.FunUtil2.Fun2;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;

public class HttpIo {

	public HttpRequest readRequest(InputStream is0) {
		var ls = Util.readLine(is0).split(" ");
		var headers = readHeaders(is0);

		return FixieArray.of(ls).map((method, url, protocol) -> {
			Fun2<String, String, HttpRequest> requestFun = (host, pqs) -> String_.split2l(pqs, "?").map((path0, query) -> {
				var is1 = getContentStream(is0, headers);
				var path1 = path0.startsWith("/") ? path0 : "/" + path0;
				var path2 = Rethrow.ex(() -> URLDecoder.decode(path1, Constants.charset));

				return String_.equals(protocol, "HTTP/1.1") //
						? new HttpRequest(method, host, path2, query, headers, is1) //
						: Fail.t("only HTTP/1.1 is supported");
			});

			var pp = String_.split2(url, "://");
			return pp != null ? String_.split2l(pp.t1, "/").map(requestFun) : requestFun.apply("", url);
		});
	}

	public HttpResponse readResponse(InputStream is0) {
		var ls = Util.readLine(is0).split(" ");
		var headers = readHeaders(is0);

		return FixieArray.of(ls).map((protocol, status) -> {
			var cl = Opt.of(headers.get("Content-Length")).map(Integer::parseInt);
			var is1 = !cl.isEmpty() ? sizeLimitedInputStream(is0, cl.get()) : is0;

			return String_.equals(protocol, "HTTP/1.1") //
					? new HttpResponse(status, headers, To.outlet(is1)) //
					: Fail.t("only HTTP/1.1 is supported");
		});
	}

	public void writeRequest(OutputStream os, HttpRequest request) throws IOException {
		var server = request.server;
		var path = request.path();
		var url = !server.isEmpty() ? "http://" + server + "/" + path : path;

		var sb = new StringBuilder();
		sb.append(request.method + " " + url + " HTTP/1.1\r\n");
		Read.from2(request.headers).sink((k, v) -> sb.append(k + ": " + v + "\r\n"));
		sb.append("\r\n");

		os.write(sb.toString().getBytes(Constants.charset));
		Copy.stream(request.inputStream, os);
	}

	public void writeResponse(OutputStream os, HttpResponse response) throws IOException {
		var sb = new StringBuilder();
		sb.append("HTTP/1.1 " + response.status + "\r\n");
		Read.from2(response.headers).sink((k, v) -> sb.append(k + ": " + v + "\r\n"));
		sb.append("\r\n");

		os.write(sb.toString().getBytes(Constants.charset));
		Copy.stream(response.out.collect(To::inputStream), os);
	}

	private IMap<String, String> readHeaders(InputStream is) {
		var headers = IMap.<String, String> empty();
		String line;
		while (!(line = Util.readLine(is)).isEmpty()) {
			var headers0 = headers;
			headers = String_.split2l(line, ":").map((k, v) -> headers0.put(k, v));
		}
		return headers;
	}

	private InputStream getContentStream(InputStream is, IMap<String, String> headers) {
		var cl = Opt.of(headers.get("Content-Length")).map(Integer::parseInt);
		return !cl.isEmpty() ? sizeLimitedInputStream(is, cl.get()) : is;
	}

	private InputStream sizeLimitedInputStream(InputStream is, int size) {
		return new BasicInputStream(is) {
			private int remaining = size;

			public int read() throws IOException {
				return 0 < remaining-- ? is.read() : -1;
			}

			public int read(byte[] bytes, int offset, int length) throws IOException {
				int result;

				if (0 < remaining) {
					result = is.read(bytes, offset, min(length, remaining));

					if (0 <= result)
						remaining -= result;
				} else
					result = -1;

				return result;
			}
		};
	}

}
