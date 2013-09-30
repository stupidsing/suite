package suite.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import suite.http.HttpServer.Handler;

public class HttpServerMain {

	private Handler handler0 = new HttpHandler() {
		protected void handle(Reader reader, Writer writer) throws IOException {
			writer.write("<html>" //
					+ "<br/>method = " + request.getMethod() + "<br/>server = " + request.getServer()
					+ "<br/>path = "
					+ request.getPath() + "<br/>attrs = " + HttpUtil.getAttrs(request.getQuery()) //
					+ "<br/>headers = " + request.getHeaders() + "</html>");
		}
	};

	public static void main(String args[]) throws IOException {
		new HttpServerMain().run();
	}

	private void run() throws IOException {
		handler0.getClass();
		new HttpServer().run(handler0);
	}

}
