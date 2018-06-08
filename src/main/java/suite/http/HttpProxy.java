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

	public void serve(HttpHandler handler) {
		try {
			serve_(handler);
		} catch (IOException ex) {
			Fail.t(ex);
		}
	}

	private void serve_(HttpHandler handler) throws IOException {
		new SocketUtil().listenIo(8051, (is, os) -> {
			var line = Util.readLine(is);
			var ls = line.split(" ");
			var url = ls[1];

			var pp = String_.split2(url, "://");
			var path = pp != null ? String_.split2l(pp.t1, "/").t1 : url;
			var host = path != null ? "127.0.0.1" : null;

			try (var socket1 = new Socket(host, 9051); var is1 = socket1.getInputStream(); var os1 = socket1.getOutputStream();) {
				os1.write(line.getBytes(Constants.charset));
				var threads = Read.each(Copy.streamByThread(is, os1), Copy.streamByThread(is1, os));
				Thread_.startJoin(threads);
			}
		});
	}

}
