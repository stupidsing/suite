package suite.net;

import static java.lang.Math.min;
import static suite.util.Friends.fail;

import java.io.IOException;
import java.io.InputStream;

import suite.cfg.Defaults;
import suite.os.SocketUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes.BytesBuilder;
import suite.streamlet.FunUtil.Fun;

public class ServeSocket {

	public boolean run() {
		listen(bytes -> bytes);
		return true;
	}

	private void listen(Fun<Bytes, Bytes> handle) {
		new SocketUtil().listenIo(5151, (is, os) -> {
			var in = read(is, Defaults.bufferLimit);
			var out = handle.apply(in);
			os.write(out.bs);
		});
	}

	private Bytes read(InputStream is, int max) throws IOException {
		var bb = new BytesBuilder();
		var buffer = new byte[Defaults.bufferSize];
		int nBytesRead;

		while ((nBytesRead = is.read(buffer, 0, min(max - bb.size(), buffer.length))) != -1) {
			bb.append(buffer, 0, nBytesRead);
			if (max < bb.size())
				fail("input too long");
		}

		return bb.toBytes();
	}

}
