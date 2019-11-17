package suite.http;

import static primal.statics.Rethrow.ex;

import java.io.IOException;

import primal.Nouns.Buffer;
import primal.os.Log_;
import primal.primitive.adt.Bytes;
import primal.puller.Puller;
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
		serveAsync(async(handler));
	}

	public void serveAsync(HandlerAsync handler) {
		listen.ioAsync(port, (is, os, close) -> {
			var in = Puller.of(() -> ex(() -> {
				var bs = new byte[Buffer.size];
				var n = is.read(bs);
				return 0 <= n ? Bytes.of(bs, 0, n) : null;
			}));

			var request = httpIo.readRequest(in);

			IoSink<Response> sink = response -> {
				Log_.info(request.getLogString() + " " + response.getLogString());
				httpIo.writeResponse(os, response);
				close.close();
			};

			try {
				handler.handle(request, sink);
			} catch (Exception ex) {
				Log_.error(ex);
				sink.f(Http.R500);
			}
		});
	}

	private HandlerAsync async(Handler handler) {
		return (request, sink) -> {
			var response = handler.handle(request);
			try {
				sink.f(response);
			} catch (IOException ex) {
				Log_.error(ex);
			}
		};
	}

}
