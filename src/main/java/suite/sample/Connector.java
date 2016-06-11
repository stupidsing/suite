package suite.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.Socket;
import java.nio.charset.Charset;

import suite.Constants;
import suite.os.SocketUtil;

public class Connector {

	public void connect() throws IOException {
		Charset charset = Constants.charset;

		try (Socket socket = new Socket("wwww.google.com", 80);
				InputStream is = socket.getInputStream();
				OutputStream os = socket.getOutputStream();
				Reader reader = new BufferedReader(new InputStreamReader(is, charset));
				PrintWriter writer = new PrintWriter(os)) {
			writer.print("GET /\r\n\r\n");
			while (reader.ready())
				System.out.println((char) reader.read());
		}
	}

	public void listen() throws IOException {
		new SocketUtil().listenRw(5151, (reader, writer) -> writer.println("Hello World"));
	}

}
