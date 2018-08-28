package suite.net.nio;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
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
	private String hello = "HELLO";
	private Charset charset = Defaults.charset;
	private byte lf = 10;
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
				pw.print(hello + "\n");
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
					sc -> buffer.writeAll(sc, Bytes.of((hello + "\n").getBytes(charset)), v -> buffer.readLine(sc, lf, bytes -> {
						assertArrayEquals(hello.getBytes(charset), bytes.toArray());
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

		return dispatch.asyncListen(port, sc -> {
			new Object() {
				public void run() {
					buffer.readLine(sc, lf, bytes -> {
						buffer.writeAll(sc, bytes, v0 -> {
							buffer.writeAll(sc, Bytes.of(new byte[] { lf, }), v1 -> run(), fail);
						}, fail);
					}, fail);
				}
			}.run();
		});
	}

}
