package suite.http;

import primal.os.Log_;
import suite.http.Http.Handler;
import suite.http.Http.HandlerAsync;
import suite.http.Http.Response;
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

	public void serve(Handler handler) {
		listen.io(port, (is, os) -> {
			var request = httpIo.readRequest(is);
			Response response = null;

			try {
				response = handler.handle(request);
			} catch (Exception ex) {
				Log_.error(ex);
				response = Response.of(Http.S500);
			} finally {
				Log_.info(request.getLogString() + " " + response.getLogString());
			}

			httpIo.writeResponse(os, response);
		});
	}

	public void serveAsync(HandlerAsync handler) {
		listen.ioAsync(port, (is, os, close) -> {
			var request = httpIo.readRequest(is);

			IoSink<Response> sink = response -> {
				Log_.info(request.getLogString() + " " + response.getLogString());
				httpIo.writeResponse(os, response);
				close.close();
			};

			try {
				handler.handle(request, sink);
			} catch (Exception ex) {
				Log_.error(ex);
				sink.f(Response.of(Http.S500));
			}
		});
	}

}
