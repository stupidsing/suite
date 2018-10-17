package suite.util;

import static suite.util.Friends.fail;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import suite.cfg.Defaults;
import suite.util.Rethrow.SinkEx;

/**
 * Implements an output stream using a given output stream. Extends this class
 * to provide additional functionality.
 *
 * @author ywsing
 */
public class WriteStream extends OutputStream {

	private OutputStream os;

	public static WriteStream of(OutputStream os) {
		return new WriteStream(os);
	}

	protected WriteStream(OutputStream os) {
		this.os = os;
	}

	public void writeAndClose(byte[] content) {
		doWrite(os -> os.write(content));
	}

	public void writeAndClose(String content) {
		doWriter(w -> w.write(content));
	}

	public void doPrintWriter(SinkEx<PrintWriter, IOException> sink) {
		doWriter(w -> {
			try (var pw = new PrintWriter(w)) {
				sink.f(pw);
			}
		});
	}

	public void doWriter(SinkEx<OutputStreamWriter, IOException> sink) {
		doWrite(os -> {
			try (var w = new OutputStreamWriter(os, Defaults.charset)) {
				sink.f(w);
			}
		});
	}

	public void doWrite(SinkEx<WriteStream, IOException> sink) {
		try (var os = this) {
			sink.f(os);
		} catch (IOException ex) {
			fail(ex);
		}
	}

	@Override
	public void close() throws IOException {
		os.close();
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}

	@Override
	public void write(byte[] bs, int offset, int length) throws IOException {
		os.write(bs, offset, length);
	}

	@Override
	public void write(byte[] bs) throws IOException {
		os.write(bs);
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}

}
