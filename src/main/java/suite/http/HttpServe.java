package suite.http;

import primal.os.Log_;
import suite.os.Listen;
import suite.primitive.IoSink;

/**
 * A very crude HTTP server.
 *
 * Possible improvements:
 *
 * TODO persistent connection
 *
 * TODO direct output without buffering
 *
 * @author ywsing
 */
public class HttpServe {

	private HttpIo httpIo = new HttpIo();
	private Listen listen = new Listen();
	private int port;

	public HttpServe(int port) {
		this.port = port;
	}

	public void serve(HttpHandler handler) {
		listen.io(port, (is, os) -> {
			var request = httpIo.readRequest(is);
			HttpResponse response = null;

			try {
				response = handler.handle(request);
			} catch (Exception ex) {
				Log_.error(ex);
				response = HttpResponse.of(HttpResponse.HTTP500);
			} finally {
				Log_.info(request.getLogString() + " " + response.getLogString());
			}

			httpIo.writeResponse(os, response);
		});
	}

	public void serveAsync(HttpHandlerAsync handler) {
		listen.ioAsync(port, (is, os, close) -> {
			var request = httpIo.readRequest(is);

			IoSink<HttpResponse> sink = response -> {
				Log_.info(request.getLogString() + " " + response.getLogString());
				httpIo.writeResponse(os, response);
				close.close();
			};

			try {
				handler.handle(request, sink);
			} catch (Exception ex) {
				Log_.error(ex);
				sink.f(HttpResponse.of(HttpResponse.HTTP500));
			}
		});
	}

}
