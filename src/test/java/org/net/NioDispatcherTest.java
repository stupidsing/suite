package org.net;

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
import org.net.Channels.BufferedChannel;
import org.net.Channels.RequestResponseChannel;
import org.net.Channels.Sender;
import org.util.FunUtil.Fun;
import org.util.FunUtil.Source;
import org.util.IoUtil;
import org.util.Util;

public class NioDispatcherTest {

	@Test
	public void testTextExchange() throws IOException {
		final String hello = "HELLO";
		final Charset charset = IoUtil.charset;

		final BufferedChannel channel = new BufferedChannel() {
			public void onConnected(Sender sender) {
				super.onConnected(sender);
				String s = hello + "\n";
				send(new Bytes(s.getBytes(charset)));
			}

			public void onReceive(Bytes request) {
				send(request);
			}
		};
		Source<BufferedChannel> source = new Source<BufferedChannel>() {
			public BufferedChannel apply() {
				return channel;
			}
		};
		NioDispatcher<BufferedChannel> dispatcher = new NioDispatcher<>(source);

		dispatcher.start();

		try (Closeable closeServer = dispatcher.listen(5151);
				Socket socket = new Socket("localhost", 5151);
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
				InputStreamReader isr = new InputStreamReader(is, charset);
				BufferedReader reader = new BufferedReader(isr);
				PrintWriter writer = new PrintWriter(os);) {
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
		final RequestResponseMatcher matcher = new RequestResponseMatcher();
		final ThreadPoolExecutor executor = Util.createExecutor();

		final Fun<Bytes, Bytes> handler = new Fun<Bytes, Bytes>() {
			public Bytes apply(Bytes request) {
				return request;
			}
		};
		Source<RequestResponseChannel> source = new Source<RequestResponseChannel>() {
			public RequestResponseChannel apply() {
				return new RequestResponseChannel(matcher, executor, handler);
			}
		};
		NioDispatcher<RequestResponseChannel> dispatcher = new NioDispatcher<>(
				source);

		dispatcher.start();
		try (Closeable closeServer = dispatcher.listen(5151)) {
			InetAddress localHost = InetAddress.getLocalHost();
			InetSocketAddress address = new InetSocketAddress(localHost, 5151);
			RequestResponseChannel client = dispatcher.connect(address);

			for (String s : new String[] { "ABC", "WXYZ", "" }) {
				byte bs[] = s.getBytes(IoUtil.charset);
				Bytes request = new Bytes(bs);
				Bytes response = matcher.requestForResponse(client, request);
				assertEquals(request, response);
				System.out.println("Request '" + s + "' is okay");
			}
		}

		executor.awaitTermination(0, TimeUnit.SECONDS);
		dispatcher.stop();
	}

}
