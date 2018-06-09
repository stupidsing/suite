package suite.http;

import suite.os.LogUtil;
import suite.os.SocketUtil;

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

}
