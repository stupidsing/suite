package suite.http;

import static suite.util.Friends.fail;
import static suite.util.Friends.rethrow;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiPredicate;

import suite.immutable.IMap;
import suite.primitive.LngMutable;
import suite.util.String_;
import suite.util.To;

public interface HttpHandler {

	public static HttpHandler ofData(String data) {
		return request -> HttpResponse.of(To.outlet(data));
	}

	public static HttpHandler ofDispatch(IMap<String, HttpHandler> map) {
		return request -> {
			var p = request.split();
			var handler = map.get(p.t0);
			return handler != null ? handler.handle(p.t1) : fail("no handler for " + p.t0);
		};
	}

	public static HttpHandler ofPath(Path root) {
		return request -> rethrow(() -> {
			var path = root;
			long size;

			for (var p : request.paths)
				if (!String_.equals(p, ".."))
					path = path.resolve(p);

			var file = path.toFile();

			if (file.exists())
				try (var raf = new RandomAccessFile(file, "r")) {
					size = raf.getChannel().size();

					var range = request.headers.get("Range");
					var ranges = range != null ? range.split(",") : new String[0];
					var array = ranges.length == 1 ? ranges[0].split("-") : new String[0];

					if (array.length == 2) {
						var a0 = array[0];
						var a1 = array[1];
						var p0 = Math.max(!a1.isEmpty() ? Long.valueOf(a0) : Long.MIN_VALUE, 0);
						var px = Math.min(!a1.isEmpty() ? Long.valueOf(a1) : Long.MAX_VALUE, size);
						var p = LngMutable.of(p0);

						var empty = new HttpHeader() //
								.put("Content-Range", "bytes " + p0 + "-" + px + "/" + size) //
								.put("Content-Type", "text/html; charset=UTF-8");

						return HttpResponse.of(HttpResponse.HTTP206, empty, To.outlet(new InputStream() {
							public int read() throws IOException {
								var pos = p.value();
								if (pos != px) {
									raf.seek(p.increment());
									p.update(pos + 1);
									return raf.read();
								} else
									return -1;
							}

							public int read(byte b[], int off, int len0) throws IOException {
								var pos = p.value();
								if (pos != px) {
									var len1 = Math.min(len0, (int) (px - pos));
									raf.seek(pos);
									var n = raf.read(b, off, len1);
									p.update(pos + n);
									return n;
								} else
									return -1;
							}
						}));
					}

					return HttpResponse.of(HttpResponse.HTTP200, To.outlet(Files.newInputStream(path)), size);
				}
			else
				return HttpResponse.of(HttpResponse.HTTP404);
		});
	}

	public static HttpHandler ofSession(BiPredicate<String, String> authenticate, HttpHandler handler0) {
		return new HttpSessionControl(authenticate).getSessionHandler(handler0);
	}

	public HttpResponse handle(HttpRequest request);

}
