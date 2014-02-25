package suite.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Objects;

import suite.http.HttpServer.Handler;
import suite.http.HttpSessionController.Authenticator;

public class HttpServerMain {

	private Authenticator authenticator = new Authenticator() {
		public boolean authenticate(String username, String password) {
			return Objects.equals(username, "user") && Objects.equals(password, "");
		}
	};

	private Handler handler0 = new HttpHandler() {
		protected void handle(Reader reader, Writer writer) throws IOException {
			writer.write("<html>" //
					+ "<br/>method = " + request.getMethod() //
					+ "<br/>server = " + request.getServer() //
					+ "<br/>path = " + request.getPath() //
					+ "<br/>attrs = " + HttpUtil.getAttrs(request.getQuery()) //
					+ "<br/>headers = " + request.getHeaders() //
					+ "</html>");
		}
	};

	private Handler handler1 = new HttpSessionController(authenticator).getSessionHandler(handler0);

	public static void main(String args[]) throws IOException {
		new HttpServerMain().run();
	}

	private void run() throws IOException {
		handler0.getClass();
		handler1.getClass();
		new HttpServer().run(handler1);
	}

}
