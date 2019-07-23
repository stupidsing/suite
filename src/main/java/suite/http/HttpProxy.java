package suite.http;

import static suite.util.Friends.rethrow;

import java.net.Socket;

import suite.cfg.Defaults;
import suite.os.Log_;
import suite.os.SocketUtil;
import suite.primitive.adt.pair.IntObjPair;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.Read;
import suite.util.Copy;
import suite.util.String_;
import suite.util.Thread_;
import suite.util.Util;

/**
 * A very crude HTTP proxy.
 *
 * @author ywsing
 */
public class HttpProxy {

	private int port;
	private Fun<String, IntObjPair<String>> target;

	public HttpProxy(int port, Fun<String, IntObjPair<String>> target) {
		this.port = port;
		this.target = target;
	}

	public void serve0() {
		new SocketUtil().listenIo(port, (is, os) -> {
			var line = Util.readLine(is);
			Log_.info("PROXY " + line);

			var url = line.split(" ")[1];
			var pp = String_.split2(url, "://");
			var path = pp != null ? String_.split2l(pp.v, "/").v : url;

			try (var socket1 = connect(path); //
					var is0 = is; //
					var os0 = os; //
					var is1 = socket1.getInputStream(); //
					var os1 = socket1.getOutputStream();) {
				os1.write((line + "\r\nConnection: close\r\n").getBytes(Defaults.charset));
				Read.each(Copy.streamByThread(is0, os1), Copy.streamByThread(is1, os0)).collect(Thread_::startJoin);
			}
		});
	}

	public void serve() {
		var httpIo = new HttpIo();

		new SocketUtil().listenIo(port, (is, os) -> {
			var request0 = httpIo.readRequest(is);
			var pq = request0.path() + "?" + request0.query;
			Log_.info("PROXY " + pq);

			var headers1 = request0.headers.remove("Connection").put("Connection", "close");

			var request1 = new HttpRequest( //
					request0.method, //
					request0.server, //
					request0.paths, //
					request0.query, //
					headers1, //
					request0.inputStream);

			try (var socket1 = connect(pq); //
					var is0 = is; //
					var os0 = os; //
					var is1 = socket1.getInputStream(); //
					var os1 = socket1.getOutputStream();) {
				Thread_.startJoin( //
						() -> httpIo.writeRequest(os1, request1), //
						() -> httpIo.writeResponse(os0, httpIo.readResponse(is1)));
			}
		});
	}

	private Socket connect(String path) {
		return target.apply(path).map((port1, host1) -> rethrow(() -> new Socket(host1, port1)));
	}

}
