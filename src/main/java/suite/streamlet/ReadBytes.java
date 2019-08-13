package suite.streamlet;

import static primal.statics.Rethrow.ex;

import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;

import primal.MoreVerbs.Pull;
import primal.primitive.adt.Bytes;
import primal.streamlet.Streamlet;

public class ReadBytes {

	public static Streamlet<Bytes> from(Path path) {
		var file = path.toFile();

		return new Streamlet<>(() -> {
			InputStream is = ex(() -> new FileInputStream(file));
			return Pull.from(is).closeAtEnd(is);
		});
	}

	public static Streamlet<Bytes> from(String data) {
		return new Streamlet<>(() -> Pull.from(data));
	}

	public static Streamlet<Bytes> from(InputStream is) {
		return new Streamlet<>(() -> Pull.from(is));
	}

}
