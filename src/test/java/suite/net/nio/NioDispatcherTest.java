package suite.net.nio;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import suite.Constants;
import suite.net.nio.NioChannelFactory.BufferedNioChannel;
import suite.net.nio.NioChannelFactory.NioChannel;
import suite.net.nio.NioChannelFactory.RequestResponseNioChannel;
import suite.primitive.Bytes;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;
import suite.util.To;
import suite.util.Util;

public class NioDispatcherTest {

	@Test
	public void testTextExchange() throws IOException {
		String hello = "HELLO";
		Charset charset = Constants.charset;

		Source<NioChannel> source = () -> NioChannelFactory.buffered(() -> {
			BufferedNioChannel channel = new BufferedNioChannel();
			channel.onConnected.register(sender -> {
				String s = hello + "\n";
				channel.send(To.bytes(s));

			});
			channel.onReceive.register(channel::send);
			return channel;
		});
		NioDispatcher<NioChannel> dispatcher = new NioDispatcherImpl<>(source);
		dispatcher.start();

		try (Closeable closeServer = dispatcher.listen(5151);
				Socket socket = new Socket("localhost", 5151);
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
				InputStreamReader isr = new InputStreamReader(is, charset);
				BufferedReader reader = new BufferedReader(isr);
				PrintWriter writer = new PrintWriter(os)) {
			String m = "testing nio";
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
		RequestResponseMatcher matcher = new RequestResponseMatcher();
		ThreadPoolExecutor executor = Util.createExecutor();
		Fun<Bytes, Bytes> handler = request -> request;

		NioDispatcher<RequestResponseNioChannel> dispatcher = new NioDispatcherImpl<>( //
				() -> NioChannelFactory.requestResponse( //
						RequestResponseNioChannel::new, //
						matcher, //
						executor, //
						handler));
		dispatcher.start();

		try (Closeable closeServer = dispatcher.listen(5151)) {
			InetAddress localHost = InetAddress.getLocalHost();
			InetSocketAddress address = new InetSocketAddress(localHost, 5151);
			RequestResponseNioChannel client = dispatcher.connect(address);

			for (String s : new String[] { "ABC", "WXYZ", "", }) {
				byte bs[] = s.getBytes(Constants.charset);
				Bytes request = Bytes.of(bs);
				Bytes response = matcher.requestForResponse(client, request);
				assertEquals(request, response);
				System.out.println("Request '" + s + "' is okay");
			}
		} finally {
			dispatcher.stop();
		}

		executor.awaitTermination(0, TimeUnit.SECONDS);
	}

}
