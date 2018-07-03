package suite.util;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import suite.Defaults;
import suite.util.Rethrow.SinkEx;

/**
 * Implements an output stream using a given output stream. Extends this class
 * to provide additional functionality.
 *
 * @author ywsing
 */
public class BasicOutputStream extends OutputStream {

	private OutputStream os;

	public BasicOutputStream(OutputStream os) {
		this.os = os;
	}

	public void writeAndClose(byte[] content) {
		doWrite(os -> os.write(content));
	}

	public void writeAndClose(String content) {
		doWriter(w -> w.write(content));
	}

	public void doWriter(SinkEx<OutputStreamWriter, IOException> sink) {
		doWrite(os -> {
			try (var w = new OutputStreamWriter(os, Defaults.charset)) {
				sink.sink(w);
			}
		});
	}

	public void doWrite(SinkEx<BasicOutputStream, IOException> sink) {
		try (var os = this) {
			sink.sink(os);
		} catch (IOException ex) {
			Fail.t(ex);
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
