package suite.http;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import suite.adt.pair.Pair;
import suite.http.HttpSessionController.Authenticator;
import suite.immutable.IMap;
import suite.util.Rethrow;
import suite.util.To;

public interface HttpHandler {

	public static HttpHandler ofDispatch(IMap<String, HttpHandler> map) {
		return request -> {
			Pair<String, HttpRequest> p = request.split();
			HttpHandler handler = map.get(p.t0);
			if (handler != null)
				return handler.handle(p.t1);
			else
				throw new RuntimeException("no handler for " + p.t0);
		};
	}

	public static HttpHandler ofPath(Path root) {
		return request -> Rethrow.ex(() -> {
			Path path = root;
			long size;

			for (String p : request.path)
				path = path.resolve(p);

			try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
				size = file.getChannel().size();
			}

			return HttpResponse.of(HttpResponse.HTTP200, To.outlet(Files.newInputStream(path)), size);
		});
	}

	public static HttpHandler ofSession(Authenticator authenticator, HttpHandler handler0) {
		return new HttpSessionController(authenticator).getSessionHandler(handler0);
	}

	public HttpResponse handle(HttpRequest request);

}
