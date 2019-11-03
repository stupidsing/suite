package suite.http;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiPredicate;

import primal.MoreVerbs.Pull;
import primal.Verbs.Equals;
import primal.fp.Funs.Sink;
import primal.persistent.PerList;
import primal.persistent.PerMap;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.LngMutable;
import suite.http.Http.Handler;
import suite.http.Http.Header;
import suite.http.Http.Response;

public class HttpHandle {

	public static Handler ofData(String data) {
		return request -> Response.of(Pull.from(data));
	}

	public static Handler ofDispatch(PerMap<String, Handler> map) {
		return request0 -> request0.split().map((path, request1) -> {
			var handler = map.get(path);
			return handler != null ? handler.handle(request1) : fail("no handler for " + path);
		});
	}

	public static Handler ofPath(Path root) {
		return request -> ex(() -> {
			var path = root;
			long size;

			for (var p : request.paths)
				if (!Equals.string(p, ".."))
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
						var p0 = max(!a1.isEmpty() ? Long.valueOf(a0) : Long.MIN_VALUE, 0);
						var px = min(!a1.isEmpty() ? Long.valueOf(a1) : Long.MAX_VALUE, size);
						var p = LngMutable.of(p0);

						var empty = new Header() //
								.put("Content-Range", "bytes " + p0 + "-" + px + "/" + size) //
								.put("Content-Type", "text/html; charset=UTF-8");

						return Response.of(Http.S206, empty, Pull.from(new InputStream() {
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
									var len1 = min(len0, (int) (px - pos));
									raf.seek(pos);
									var n = raf.read(b, off, len1);
									p.update(pos + n);
									return n;
								} else
									return -1;
							}
						}));
					}

					return Response.of(Http.S200, Pull.from(Files.newInputStream(path)), size);
				}
			else
				return Response.of(Http.S404);
		});
	}

	public static Handler ofSession(BiPredicate<String, String> authenticate, Handler handler) {
		return new HttpAuthSession().getHandler(authenticate, handler);
	}

	public static Handler ofSse(Sink<Sink<Bytes>> write) {
		Header sseHeaders = new Header(PerMap //
				.<String, PerList<String>> empty() //
				.put("Cache-Control", PerList.of("no-cache")) //
				.put("Content-Type", PerList.of("text/event-stream")));

		return request -> Response.ofWriter(Http.S200, sseHeaders, write);
	}

}