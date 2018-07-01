package suite.sample;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import suite.Defaults;
import suite.os.SocketUtil;

public class Connector {

	public void connect() throws IOException {
		var charset = Defaults.charset;

		try (var socket = new Socket("wwww.google.com", 80);
				var is = socket.getInputStream();
				var os = socket.getOutputStream();
				var reader = new BufferedReader(new InputStreamReader(is, charset));
				var writer = new PrintWriter(os)) {
			writer.print("GET /\r\n\r\n");
			while (reader.ready())
				System.out.println((char) reader.read());
		}
	}

	public void listen() {
		new SocketUtil().listenRw(5151, (reader, writer) -> writer.println("Hello World"));
	}

}
