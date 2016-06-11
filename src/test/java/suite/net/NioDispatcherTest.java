package suite.net;

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
import suite.net.channels.BufferedChannel;
import suite.net.channels.RequestResponseChannel;
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

		BufferedChannel channel = new BufferedChannel() {
			public void onConnected(Sender sender) {
				super.onConnected(sender);
				String s = hello + "\n";
				send(To.bytes(s));
			}

			public void onReceive(Bytes request) {
				send(request);
			}
		};
		Source<BufferedChannel> source = () -> channel;
		NioDispatcher<BufferedChannel> dispatcher = new NioDispatcherImpl<>(source);

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
		}

		dispatcher.stop();
	}

	@Test
	public void testRequestResponse() throws IOException, InterruptedException {
		RequestResponseMatcher matcher = new RequestResponseMatcher();
		ThreadPoolExecutor executor = Util.createExecutor();
		Fun<Bytes, Bytes> handler = request -> request;

		Source<RequestResponseChannel> source = () -> new RequestResponseChannel(matcher, executor, handler);
		NioDispatcher<RequestResponseChannel> dispatcher = new NioDispatcherImpl<>(source);
		dispatcher.start();

		try (Closeable closeServer = dispatcher.listen(5151)) {
			InetAddress localHost = InetAddress.getLocalHost();
			InetSocketAddress address = new InetSocketAddress(localHost, 5151);
			RequestResponseChannel client = dispatcher.connect(address);

			for (String s : new String[] { "ABC", "WXYZ", "", }) {
				byte bs[] = s.getBytes(Constants.charset);
				Bytes request = Bytes.of(bs);
				Bytes response = matcher.requestForResponse(client, request);
				assertEquals(request, response);
				System.out.println("Request '" + s + "' is okay");
			}
		}

		dispatcher.stop();
		executor.awaitTermination(0, TimeUnit.SECONDS);
	}

}
