package suite.http;

import suite.os.LogUtil;
import suite.os.SocketUtil;
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
	private int port;

	public HttpServe(int port) {
		this.port = port;
	}

	public void serve(HttpHandler handler) {
		new SocketUtil().listenIo(port, (is, os) -> {
			var request = httpIo.readRequest(is);
			HttpResponse response = null;

			try {
				response = handler.handle(request);
			} catch (Exception ex) {
				LogUtil.error(ex);
				response = HttpResponse.of(HttpResponse.HTTP500);
			} finally {
				LogUtil.info(request.getLogString() + " " + response.getLogString());
			}

			httpIo.writeResponse(os, response);
		});
	}

	public void serveAsync(HttpHandlerAsync handler) {
		new SocketUtil().listenIoAsync(port, (is, os, close) -> {
			var request = httpIo.readRequest(is);

			IoSink<HttpResponse> sink = response -> {
				LogUtil.info(request.getLogString() + " " + response.getLogString());
				httpIo.writeResponse(os, response);
				close.close();
			};

			try {
				handler.handle(request, sink);
			} catch (Exception ex) {
				LogUtil.error(ex);
				sink.sink(HttpResponse.of(HttpResponse.HTTP500));
			}
		});
	}

}
