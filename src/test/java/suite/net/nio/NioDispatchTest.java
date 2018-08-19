package suite.net.nio;

import static org.junit.Assert.assertArrayEquals;

import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;

import org.junit.Test;

import suite.cfg.Defaults;
import suite.primitive.Bytes;
import suite.util.Rethrow;

public class NioDispatchTest {

	private InetAddress localHost = Rethrow.ex(() -> InetAddress.getLocalHost());
	private int port = 5151;
	private String hello = "HELLO";
	private Charset charset = Defaults.charset;

	@Test
	public void testTextExchange0() throws IOException {
		try (var dispatch = new NioDispatch();
				var listen = listen(dispatch);
				var socket = new Socket(localHost, port);
				var os = socket.getOutputStream();
				var writer = new PrintWriter(os)) {
			writer.println(hello);
			writer.flush();
			dispatch.run();
		}
	}

	@Test
	public void testTextExchange() throws IOException {
		try (var dispatch = new NioDispatch(); var listen = listen(dispatch);) {
			dispatch.asyncConnect(new InetSocketAddress(localHost, port), sc -> {
				dispatch.asyncWriteAll(sc, Bytes.of((hello + "\n").getBytes(charset)), () -> {
				});
			});
			dispatch.run();
		}
	}

	private Closeable listen(NioDispatch dispatch) throws IOException {
		return dispatch.asyncListen(port, sc -> {
			dispatch.asyncReadLine(sc, (byte) 10, bytes -> {
				assertArrayEquals(hello.getBytes(charset), bytes.toArray());
				dispatch.close(sc);
				dispatch.stop();
			});
		});
	}

}
