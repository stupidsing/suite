package suite.http;

import java.io.IOException;
import java.net.Socket;

import suite.Constants;
import suite.os.SocketUtil;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.Fail;
import suite.util.FunUtil.Fun;
import suite.util.Rethrow;
import suite.util.String_;
import suite.util.Thread_;
import suite.util.Util;

/**
 * A very crude HTTP proxy.
 *
 * @author yw.sing
 */
public class HttpProxy {

	private int port;
	private Fun<String, IntObjPair<String>> target;

	public HttpProxy() {
		this(8051, path -> IntObjPair.of(9051, "127.0.0.1"));
	}

	public HttpProxy(int port, Fun<String, IntObjPair<String>> target) {
		this.port = port;
		this.target = target;
	}

	public void serve() {
		try {
			serve_();
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	private void serve_() throws IOException {
		new SocketUtil().listenIo(port, (is0, os0) -> {
			var line = Util.readLine(is0);
			var url = line.split(" ")[1];
			var pp = String_.split2(url, "://");
			var path = pp != null ? String_.split2l(pp.t1, "/").t1 : url;
			var s1 = target.apply(path).map((port1, host1) -> Rethrow.ex(() -> new Socket(host1, port1)));

			try (var socket1 = s1; var is1 = socket1.getInputStream(); var os1 = socket1.getOutputStream();) {
				os1.write(line.getBytes(Constants.charset));
				os1.write(13);
				os1.write(10);
				var threads = Read.each(Copy.streamByThread(is0, os1), Copy.streamByThread(is1, os0));
				Thread_.startJoin(threads);
			}
		});
	}

}
