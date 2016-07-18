package suite.http;

import java.io.IOException;
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

	private HttpHandler handler0 = request -> {
		return HttpResponse.of(To.source("" //
				+ "<html>" //
				+ "<br/>method = " + request.method //
				+ "<br/>server = " + request.server //
				+ "<br/>path = " + request.path //
				+ "<br/>attrs = " + HttpHeaderUtil.getAttrs(request.query) //
				+ "<br/>headers = " + request.headers //
				+ "</html>"));
	};

	private HttpHandler handlePath = handlePath(Constants.tmp);
	private HttpHandler handleSite = new HttpSessionController(authenticator).getSessionHandler(handler0);

	public static void main(String args[]) throws IOException {
		new HttpServerMain().run();
	}

	private void run() throws IOException {
		IMap<String, HttpHandler> empty = IMap.empty();
		new HttpServer().run(dispatch(empty //
				.put("path", handlePath) //
				.put("site", handleSite)));
	}

	private HttpHandler dispatch(IMap<String, HttpHandler> map) {
		return request -> {
			Pair<String, HttpRequest> p = request.split();
			return map.get(p.t0).handle(p.t1);
		};
	}

	private HttpHandler handlePath(Path root) {
		return request -> Rethrow.ioException(() -> {
			Path path = root.resolve(request.path);
			long size;
			try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r")) {
				size = file.getChannel().size();
			}
			return HttpResponse.of(HttpResponse.HTTP200, To.source(Files.newInputStream(path)), size);
		});
	}

}
