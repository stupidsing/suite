package suite.net;

import java.io.IOException;
import java.io.InputStream;

import suite.Constants;
import suite.os.SocketUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.util.FunUtil.Fun;

public class SocketServer {

	public boolean run() throws IOException {
		listen(bytes -> bytes);
		return true;
	}

	private void listen(Fun<Bytes, Bytes> handle) throws IOException {
		new SocketUtil().listenIo(5151, (is, os) -> {
			Bytes in = read(is, 65536);
			Bytes out = handle.apply(in);
			os.write(out.bs);
		});
	}

	private Bytes read(InputStream is, int max) throws IOException {
		BytesBuilder bb = new BytesBuilder();
		byte buffer[] = new byte[Constants.bufferSize];
		int nBytesRead;

		while ((nBytesRead = is.read(buffer, 0, Math.min(max - bb.size(), buffer.length))) != -1) {
			bb.append(buffer, 0, nBytesRead);
			if (max < bb.size())
				throw new RuntimeException("Input too long");
		}

		return bb.toBytes();
	}

}
