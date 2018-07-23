package suite.http;

import static suite.util.Friends.fail;
import static suite.util.Friends.rethrow;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BiPredicate;

import suite.immutable.IMap;
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
				}
			else
				return HttpResponse.of(HttpResponse.HTTP404);

			return HttpResponse.of(HttpResponse.HTTP200, To.outlet(Files.newInputStream(path)), size);
		});
	}

	public static HttpHandler ofSession(BiPredicate<String, String> authenticate, HttpHandler handler0) {
		return new HttpSessionControl(authenticate).getSessionHandler(handler0);
	}

	public HttpResponse handle(HttpRequest request);

}
