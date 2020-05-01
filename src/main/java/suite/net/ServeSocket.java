package suite.net;

import primal.Nouns.Buffer;
import primal.fp.Funs.Fun;
import primal.primitive.adt.Bytes;
import primal.primitive.adt.Bytes.BytesBuilder;
import suite.cfg.Defaults;
import suite.os.Listen;

import java.io.IOException;
import java.io.InputStream;

import static java.lang.Math.min;
import static primal.statics.Fail.fail;

public class ServeSocket {

	public boolean run() {
		listen(bytes -> bytes);
		return true;
	}

	private void listen(Fun<Bytes, Bytes> handle) {
		new Listen().io(5151, (is, os) -> {
			var in = read(is, Defaults.bufferLimit);
			var out = handle.apply(in);
			os.write(out.bs);
		});
	}

	private Bytes read(InputStream is, int max) throws IOException {
		var bb = new BytesBuilder();
		var buffer = new byte[Buffer.size];
		int nBytesRead;

		while ((nBytesRead = is.read(buffer, 0, min(max - bb.size(), buffer.length))) != -1) {
			bb.append(buffer, 0, nBytesRead);
			if (max < bb.size())
				fail("input too long");
		}

		return bb.toBytes();
	}

}
