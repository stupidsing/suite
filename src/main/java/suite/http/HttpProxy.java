package suite.http;

import java.io.IOException;
import java.net.Socket;

import suite.Constants;
import suite.os.SocketUtil;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.Fail;
import suite.util.String_;
import suite.util.Thread_;
import suite.util.Util;

/**
 * A very crude HTTP proxy.
 *
 * @author yw.sing
 */
public class HttpProxy {

	private String host1;
	private int port0, port1;

	public HttpProxy() {
		this("127.0.0.1", 8051, 9051);
	}

	public HttpProxy(String host1, int port0, int port1) {
		this.host1 = host1;
		this.port0 = port0;
		this.port1 = port1;
	}

	public void serve(HttpHandler handler) {
		try {
			serve_(handler);
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	private void serve_(HttpHandler handler) throws IOException {
		new SocketUtil().listenIo(port0, (is0, os0) -> {
			var line = Util.readLine(is0);
			var url = line.split(" ")[1];

			var pp = String_.split2(url, "://");
			var path = pp != null ? String_.split2l(pp.t1, "/").t1 : url;
			var host = path != null ? host1 : null;

			try (var socket1 = new Socket(host, port1); var is1 = socket1.getInputStream(); var os1 = socket1.getOutputStream();) {
				os1.write(line.getBytes(Constants.charset));
				var threads = Read.each(Copy.streamByThread(is0, os1), Copy.streamByThread(is1, os0));
				Thread_.startJoin(threads);
			}
		});
	}

}
