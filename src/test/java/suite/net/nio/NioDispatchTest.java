package suite.net.nio;

import static org.junit.Assert.assertArrayEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import org.junit.Test;

import suite.cfg.Defaults;

public class NioDispatchTest {

	@Test
	public void testTextExchange() throws IOException {
		var hello = "HELLO";
		var charset = Defaults.charset;

		try (var dispatch = new NioDispatch()) {
			dispatch.asyncListen(5151, sc -> {
				dispatch.asyncReadLine(sc, (byte) 10, bytes -> {
					assertArrayEquals(hello.getBytes(Defaults.charset), bytes.toArray());
					dispatch.stop();
				});
			});

			// dispatch.asyncConnect(new InetSocketAddress("localhost", 5151), null);

			try (var socket = new Socket("localhost", 5151);
					var is = socket.getInputStream();
					var os = socket.getOutputStream();
					var isr = new InputStreamReader(is, charset);
					var reader = new BufferedReader(isr);
					var writer = new PrintWriter(os)) {
				var m = hello;
				writer.println(m);
				writer.flush();
				dispatch.run();
			}
		}
	}

}
