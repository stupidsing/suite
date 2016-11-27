package suite.http;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;

import suite.Constants;
import suite.adt.Pair;
import suite.http.HttpSessionController.Authenticator;
import suite.immutable.IMap;
import suite.util.Rethrow;
import suite.util.To;
import suite.util.Util;

// java -cp target/suite-1.0-jar-with-dependencies.jar suite.http.HttpServerMain
public class HttpServerMain {

	private Authenticator authenticator = (username, password) -> true //
			&& Util.stringEquals(username, "user") //
			&& Util.stringEquals(password, "");

	public static void main(String args[]) {
		new HttpServerMain().run();
	}

	private void run() {
		IMap<String, HttpHandler> empty = IMap.empty();

		HttpHandler handler0 = request -> {
			return HttpResponse.of(To.source("" //
					+ "<html>" //
					+ "<br/>method = " + request.method //
					+ "<br/>server = " + request.server //
					+ "<br/>path = " + request.path //
					+ "<br/>attrs = " + HttpHeaderUtil.getAttrs(request.query) //
					+ "<br/>headers = " + request.headers //
					+ "</html>"));
		};

		new HttpServer().run(dispatch(empty //
				.put("path", handlePath(Constants.tmp)) //
				.put("site", new HttpSessionController(authenticator).getSessionHandler(handler0))));
	}

	private HttpHandler dispatch(IMap<String, HttpHandler> map) {
		return request -> {
			Pair<String, HttpRequest> p = request.split();
			HttpHandler handler = map.get(p.t0);
			if (handler != null)
				return handler.handle(p.t1);
			else
				throw new RuntimeException("No handler for " + p.t0);
		};
	}

	private HttpHandler handlePath(Path root) {
		return request -> Rethrow.ioException(() -> {
			Path path = root;
			long size;

			for (String p : request.path)
				path = path.resolve(p);

			try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
				size = file.getChannel().size();
			}

			return HttpResponse.of(HttpResponse.HTTP200, To.source(Files.newInputStream(path)), size);
		});
	}

}
