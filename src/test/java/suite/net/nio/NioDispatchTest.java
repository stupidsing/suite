package suite.net.nio;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;

import org.junit.Test;

import suite.cfg.Defaults;
import suite.os.LogUtil;
import suite.primitive.Bytes;
import suite.streamlet.FunUtil.Sink;
import suite.util.Rethrow;
import suite.util.Thread_;

public class NioDispatchTest {

	private InetAddress localHost = Rethrow.ex(() -> InetAddress.getLocalHost());
	private int port = 5151;
	private Charset charset = Defaults.charset;
	private String hello = "HELLO";
	private Bytes helloBytes = Bytes.of(hello.getBytes(charset));

	private byte lf = 10;
	private char lineFeed = (char) lf;
	private Bytes lfs = Bytes.of(new byte[] { lf, });
	private Sink<IOException> fail = LogUtil::error;

	@Test
	public void testTextExchange0() throws IOException {
		try (var dispatch = new NioDispatch();
				var listen = listen(dispatch);
				var socket = new Socket(localHost, port);
				var is = socket.getInputStream();
				var os = socket.getOutputStream();
				var isr = new InputStreamReader(is);
				var br = new BufferedReader(isr);
				var pw = new PrintWriter(os)) {
			Thread_.startThread(() -> {
				pw.print(hello + lineFeed);
				pw.flush();
				assertEquals(hello, br.readLine());
				System.out.println("OK");
				dispatch.stop();
			});

			dispatch.run();
		}
	}

	@Test
	public void testTextExchange() throws IOException {
		try (var dispatch = new NioDispatch(); var listen = listen(dispatch);) {
			var buffer = dispatch.new Buffer();

			dispatch.asyncConnect( //
					new InetSocketAddress(localHost, port), //
					sc -> buffer.writeAll(sc, Bytes.concat(helloBytes, lfs), v -> buffer.readLine(sc, lf, bytes -> {
						assertEquals(helloBytes, bytes);
						System.out.println("OK");
						dispatch.close(sc);
						dispatch.stop();
					}, fail), fail), //
					fail);

			dispatch.run();
		}
	}

	private Closeable listen(NioDispatch dispatch) throws IOException {
		var buffer = dispatch.new Buffer();

		return dispatch.asyncListen(port, new Sink<SocketChannel>() {
			public void sink(SocketChannel sc) {
				buffer.readLine(sc, lf, bytes -> buffer.writeAll(sc, Bytes.concat(bytes, lfs), v -> sink(sc), fail), fail);
			}
		});
	}

}
