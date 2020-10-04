package suite.sample;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.junit.jupiter.api.Test;

// https://docs.oracle.com/javase/8/docs/technotes/guides/security/jsse/samples/sockets/client/SSLSocketClient.java
public class SslTest {

	@Test
	public void test() throws IOException {
		var factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
		var socket = (SSLSocket) factory.createSocket("pointless.online", 443);

		/*
		 * Send HTTP Request
		 *
		 * Before any application data is sent or received, the SSL socket will do SSL
		 * handshaking first to set up the security attributes.
		 *
		 * SSL handshaking can be initiated by either flushing data down the pipe, or by
		 * starting the handshaking by hand.
		 *
		 * Handshaking is started manually in this example because PrintWriter catches
		 * all IOExceptions (including SSLExceptions), sets an internal error flag, and
		 * then returns without rethrowing the exception.
		 *
		 * Unfortunately, this means any error messages are lost, which caused lots of
		 * confusion for others using this code. The only way to tell there was an error
		 * is to call PrintWriter.checkError().
		 */
		socket.startHandshake();

		try (var os = socket.getOutputStream();
				var osw = new OutputStreamWriter(os);
				var bw = new BufferedWriter(osw);
				var pw = new PrintWriter(bw);) {
			pw.print("GET / HTTP/1.1\r\nHost: pointless.online\r\n\r\n");
			pw.flush();

			if (!pw.checkError())
				try (var is = socket.getInputStream(); var isr = new InputStreamReader(is); var br = new BufferedReader(isr);) {
					String line;

					while ((line = br.readLine()) != null)
						System.out.println(line);
				}
			else
				throw new IOException("PrintWriter error");
		}
	}

}
