package suite.os;

import primal.Nouns.Utf8;
import primal.Verbs.Close;
import primal.Verbs.New;
import primal.os.Log_;

import java.io.*;
import java.net.ServerSocket;

import static primal.statics.Fail.fail;

public class Listen {

	public interface Io {
		public void serve(InputStream is, OutputStream os) throws IOException;
	}

	public interface IoAsync {
		public void serve(InputStream is, OutputStream os, Closeable close) throws IOException;
	}

	public interface Rw {
		public void serve(Reader reader, PrintWriter writer) throws IOException;
	}

	public void rw(int port, Rw rw) {
		io(port, (is, os) -> {
			try (var reader = new BufferedReader(new InputStreamReader(is, Utf8.charset)); var writer = new PrintWriter(os)) {
				rw.serve(reader, writer);
			}
		});
	}

	public void io(int port, Io io) {
		var executor = New.executor();

		try (var server = new ServerSocket(port)) {
			while (true) {
				var socket = server.accept();

				executor.execute(() -> {
					try (var is = socket.getInputStream(); var os = socket.getOutputStream()) {
						io.serve(is, os);
					} catch (Exception ex) {
						Log_.error(ex);
					} finally {
						Close.quietly(socket);
					}
				});
			}
		} catch (IOException ex) {
			fail(ex);
		} finally {
			executor.shutdown();
		}
	}

	public void ioAsync(int port, IoAsync io) {
		var executor = New.executor();

		try (var server = new ServerSocket(port)) {
			while (true) {
				var socket = server.accept(); // TODO use java.nio

				executor.execute(() -> {
					try {
						var is = socket.getInputStream();
						var os = socket.getOutputStream();
						io.serve(is, os, () -> {
							is.close();
							os.close();
							socket.close();
						});
					} catch (Exception ex) {
						Log_.error(ex);
					}
				});
			}
		} catch (IOException ex) {
			fail(ex);
		} finally {
			executor.shutdown();
		}
	}

}
