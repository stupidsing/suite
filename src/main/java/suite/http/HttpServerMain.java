package suite.http;

import java.io.IOException;

import suite.http.HttpSessionController.Authenticator;
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

	private HttpHandler handler1 = new HttpSessionController(authenticator).getSessionHandler(handler0);

	public static void main(String args[]) throws IOException {
		new HttpServerMain().run();
	}

	private void run() throws IOException {
		new HttpServer().run(handler1);
	}

}
