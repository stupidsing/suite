package suite.sample;

import primal.Nouns.Utf8;
import suite.os.Listen;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Connector {

	public void connect() throws IOException {
		var charset = Utf8.charset;

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
		new Listen().rw(5151, (reader, writer) -> writer.println("Hello World"));
	}

}
