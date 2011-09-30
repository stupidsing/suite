package org.net;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import org.junit.Test;
import org.net.ChannelListeners.BufferedChannel;
import org.net.ChannelListeners.RequestResponseChannel;
import org.net.ChannelListeners.RequestResponseMatcher;
import org.net.NioServer.ChannelListenerFactory;
import org.util.Util;
import org.util.Util.Event;

public class NioServerTest {

	@Test
	public void testTextExchange() throws IOException {
		final String hello = "HELLO";

		NioServer<BufferedChannel> server = new NioServer<BufferedChannel>(
				new ChannelListenerFactory<BufferedChannel>() {
					public BufferedChannel create() {
						return new BufferedChannel() {
							public void onConnected() {
								send(hello + "\n");
							}

							public void onReceive(String request) {
								send(request);
							}
						};
					}
				});

		server.spawn();
		Event closeServer = server.listen(5151);

		Socket socket = new Socket("localhost", 5151);
		InputStream is = socket.getInputStream();
		OutputStream os = socket.getOutputStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		PrintWriter writer = new PrintWriter(os);

		try {
			String m = "testing nio";
			writer.println(m);
			writer.flush();

			assertEquals(hello, reader.readLine());
			assertEquals(m, reader.readLine());
		} finally {
			Util.closeQuietly(reader);
			Util.closeQuietly(writer);
		}

		closeServer.perform(null);
		server.unspawn();
	}

	@Test
	public void testRequestResponse() throws IOException {
		final RequestResponseMatcher matcher = new RequestResponseMatcher();

		NioServer<RequestResponseChannel> server = new NioServer<RequestResponseChannel>(
				new ChannelListenerFactory<RequestResponseChannel>() {
					public RequestResponseChannel create() {
						return new RequestResponseChannel(matcher) {
							public String respondForRequest(String request) {
								return request;
							}
						};
					}
				});

		server.spawn();
		Event closeServer = server.listen(5151);

		RequestResponseChannel client = server.connect(
				InetAddress.getLocalHost(), 5151);

		for (String s : new String[] { "ABC", "WXYZ", "" }) {
			assertEquals(s, matcher.requestForResponse(client, s));
			System.out.println("Request '" + s + "' is okay");
		}

		closeServer.perform(null);
		server.unspawn();
	}

}
