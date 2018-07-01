package suite.net.nio;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import suite.Defaults;
import suite.net.nio.NioChannelFactory.BufferedNioChannel;
import suite.net.nio.NioChannelFactory.NioChannel;
import suite.net.nio.NioChannelFactory.RequestResponseNioChannel;
import suite.primitive.Bytes;
import suite.util.FunUtil.Iterate;
import suite.util.FunUtil.Source;
import suite.util.Thread_;
import suite.util.To;

public class NioDispatcherTest {

	@Test
	public void testTextExchange() throws IOException {
		var hello = "HELLO";
		var charset = Defaults.charset;

		Source<NioChannel> source = () -> {
			var channel = new BufferedNioChannel();
			channel.onConnected.wire(sender -> {
				var s = hello + "\n";
				channel.send(To.bytes(s));

			});
			channel.onReceive.wire(channel::send);
			return NioChannelFactory.buffered(channel);
		};
		var dispatcher = new NioDispatcherImpl<>(source);
		dispatcher.start();

		try (var closeServer = dispatcher.listen(5151);
				var socket = new Socket("localhost", 5151);
				var is = socket.getInputStream();
				var os = socket.getOutputStream();
				var isr = new InputStreamReader(is, charset);
				var reader = new BufferedReader(isr);
				var writer = new PrintWriter(os)) {
			var m = "testing nio";
			writer.println(m);
			writer.flush();

			assertEquals(hello, reader.readLine());
			assertEquals(m, reader.readLine());
		} finally {
			dispatcher.stop();
		}
	}

	@Test
	public void testRequestResponse() throws IOException, InterruptedException {
		var matcher = new RequestResponseMatcher();
		var executor = Thread_.newExecutor();
		Iterate<Bytes> handler = request -> request;

		var dispatcher = new NioDispatcherImpl<>(
				() -> NioChannelFactory.requestResponse(new RequestResponseNioChannel(), matcher, executor, handler));
		dispatcher.start();

		try (var closeServer = dispatcher.listen(5151)) {
			var localHost = InetAddress.getLocalHost();
			var address = new InetSocketAddress(localHost, 5151);
			var client = dispatcher.connect(address);

			for (var s : new String[] { "ABC", "WXYZ", "", }) {
				var bs = s.getBytes(Defaults.charset);
				var request = Bytes.of(bs);
				var response = matcher.requestForResponse(client, request);
				assertEquals(request, response);
				System.out.println("Request '" + s + "' is okay");
			}
		} finally {
			dispatcher.stop();
		}

		executor.awaitTermination(0, TimeUnit.SECONDS);
	}

}
