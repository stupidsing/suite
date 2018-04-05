package suite.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import suite.Constants;
import suite.os.SocketUtil;
import suite.util.Fail;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.To;

public class SimpleCgiServerMain extends ExecutableProgram {

	public interface Handler {
		public void handle(Map<String, String> headers, OutputStream os) throws IOException;
	}

	public static void main(String[] args) {
		RunUtil.run(SimpleCgiServerMain.class, args);
	}

	@Override
	protected boolean run(String[] args) throws IOException {
		run((headers, os) -> {
			OutputStreamWriter writer = new OutputStreamWriter(os, Constants.charset);
			writer.write("<html>" + headers + "</html>");
		});
		return true;
	}

	private void run(Handler handler) throws IOException {
		new SocketUtil().listenIo(4000, (is, os) -> {
			var headers = readHeaders(is);

			os.write(("Status: 200 OK\r\n" //
					+ "Content-Type: text/html\r\n" //
					+ "\r\n").getBytes(Constants.charset));

			handler.handle(headers, os);
		});
	}

	private Map<String, String> readHeaders(InputStream sis) throws IOException {
		var header = readNetstring(sis);

		var zeroPositions = IntBuffer.allocate(256);

		for (var i = 0; i < header.length(); i++)
			if (header.charAt(i) == 0)
				zeroPositions.put(i);

		zeroPositions.flip();

		Map<String, String> headers = new HashMap<>();
		var start = 0;
		var i = 0;

		while (i < zeroPositions.limit() - 1) {
			var z0 = zeroPositions.get(i++);
			var z1 = zeroPositions.get(i++);
			String key = header.substring(start, z0);
			String value = header.substring(z0 + 1, z1);
			start = z1 + 1;
			headers.put(key, value);
		}

		return headers;
	}

	private String readNetstring(InputStream sis) throws IOException {
		var length = 0;
		int c;

		while (0 <= (c = sis.read()) && c != ':' && length < 65536)
			if ('0' <= c && c <= '9')
				length = length * 10 + c - '0';
			else
				Fail.t("invalid netstring length");

		if (c != ':')
			Fail.t("netstring length not ended with ':'");

		var nBytesRead = 0;
		var bytes = new byte[length];
		while (nBytesRead < length)
			nBytesRead += sis.read(bytes);

		return To.string(bytes);
	}

}
