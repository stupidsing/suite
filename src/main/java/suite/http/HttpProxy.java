package suite.http;

import static primal.statics.Rethrow.ex;

import java.net.Socket;

import primal.MoreVerbs.Read;
import primal.MoreVerbs.Split;
import primal.Nouns.Utf8;
import primal.Verbs.ReadLine;
import primal.Verbs.Start;
import primal.fp.Funs.Fun;
import primal.os.Log_;
import primal.primitive.adt.pair.IntObjPair;
import suite.http.Http.Request;
import suite.os.Listen;
import suite.util.Copy;

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
		new Listen().io(port, (is, os) -> {
			var line = ReadLine.from(is);
			Log_.info("PROXY " + line);

			var url = line.split(" ")[1];
			var pp = Split.string(url, "://");
			var path = pp != null ? Split.strl(pp.v, "/").v : url;

			try (var socket1 = connect(path); //
					var is0 = is; //
					var os0 = os; //
					var is1 = socket1.getInputStream(); //
					var os1 = socket1.getOutputStream();) {
				os1.write((line + "\r\nConnection: close\r\n").getBytes(Utf8.charset));
				Read.each(Copy.streamByThread(is0, os1), Copy.streamByThread(is1, os0)).collect(Start::thenJoin);
			}
		});
	}

	public void serve() {
		var httpIo = new HttpIo();

		new Listen().io(port, (is, os) -> {
			var request0 = httpIo.readRequest(is);
			var pq = request0.path() + "?" + request0.query;
			Log_.info("PROXY " + pq);

			var headers1 = request0.headers.remove("Connection").put("Connection", "close");

			var request1 = new Request( //
					request0.method, //
					request0.server, //
					request0.paths, //
					request0.query, //
					headers1, //
					request0.in);

			try (var socket1 = connect(pq); //
					var is0 = is; //
					var os0 = os; //
					var is1 = socket1.getInputStream(); //
					var os1 = socket1.getOutputStream();) {
				Start.thenJoin( //
						() -> httpIo.writeRequest(os1, request1), //
						() -> httpIo.writeResponse(os0, httpIo.readResponse(is1)));
			}
		});
	}

	private Socket connect(String path) {
		return target.apply(path).map((port1, host1) -> ex(() -> new Socket(host1, port1)));
	}

}
