package suite.http;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import suite.http.HttpServer.Handler;
import suite.util.FileUtil;

public class HttpServerMain {

	private Handler handler0 = new Handler() {
		public void handle(HttpRequest request, HttpResponse response) throws IOException {
			try (Reader reader = new InputStreamReader(request.getInputStream(), FileUtil.charset);
					Writer writer = new OutputStreamWriter(response.getOutputStream(), FileUtil.charset)) {
				writer.write("<html>" //
						+ "<br/>method = " + request.getMethod() + "<br/>server = " + request.getServer()
						+ "<br/>path = "
						+ request.getPath() + "<br/>attrs = " + HttpUtil.getAttrs(request.getQuery()) //
						+ "<br/>headers = " + request.getHeaders() + "</html>");
			}
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
