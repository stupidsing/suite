package suite.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.ServerSocket;

import suite.Defaults;
import suite.util.Fail;
import suite.util.Object_;
import suite.util.Thread_;

public class SocketUtil {

	public interface Io {
		public void serve(InputStream is, OutputStream os) throws IOException;
	}

	public interface Rw {
		public void serve(Reader reader, PrintWriter writer) throws IOException;
	}

	public void listenRw(int port, Rw rw) {
		listenIo(port, (is, os) -> {
			try (var reader = new BufferedReader(new InputStreamReader(is, Defaults.charset)); var writer = new PrintWriter(os)) {
				rw.serve(reader, writer);
			}
		});
	}

	public void listenIo(int port, Io io) {
		var executor = Thread_.newExecutor();

		try (var server = new ServerSocket(port)) {
			while (true) {
				var socket = server.accept();

				executor.execute(() -> {
					try (var is = socket.getInputStream(); var os = socket.getOutputStream()) {
						io.serve(is, os);
					} catch (Exception ex) {
						LogUtil.error(ex);
					} finally {
						Object_.closeQuietly(socket);
					}
				});
			}
		} catch (IOException ex) {
			Fail.t(ex);
		} finally {
			executor.shutdown();
		}
	}

}
