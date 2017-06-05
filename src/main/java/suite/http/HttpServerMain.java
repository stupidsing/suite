package suite.http;

import suite.Constants;
import suite.http.HttpSessionController.Authenticator;
import suite.immutable.IMap;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;

// java -cp target/suite-1.0-jar-with-dependencies.jar suite.http.HttpServerMain
public class HttpServerMain extends ExecutableProgram {

	private Authenticator authenticator = (username, password) -> true //
			&& String_.equals(username, "user") //
			&& String_.equals(password, "");

	public static void main(String[] args) {
		Util.run(HttpServerMain.class, args);
	}

	@Override
	protected boolean run(String[] args) {
		IMap<String, HttpHandler> empty = IMap.empty();

		HttpHandler handler0 = request -> HttpResponse.of(To.outlet("" //
				+ "<html>" //
				+ "<br/>method = " + request.method //
				+ "<br/>server = " + request.server //
				+ "<br/>path = " + request.path //
				+ "<br/>attrs = " + HttpHeaderUtil.getAttrs(request.query) //
				+ "<br/>headers = " + request.headers //
				+ "</html>" //
		));

		new HttpServer().run(HttpHandler.ofDispatch(empty //
				.put("path", HttpHandler.ofPath(Constants.tmp)) //
				.put("site", HttpHandler.ofSession(authenticator, handler0)) //
		));

		return true;
	}

}
