package suite.streamlet;

import static primal.statics.Rethrow.ex;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Path;

import primal.Nouns.Utf8;
import primal.Verbs.ReadLine;
import primal.puller.Puller;
import primal.streamlet.Streamlet;

public class ReadLines {

	public static Streamlet<String> from(Path path) {
		return from(path.toFile());
	}

	public static Streamlet<String> from(File file) {
		return from(ex(() -> new FileInputStream(file)));
	}

	public static Streamlet<String> from(InputStream is) {
		return from(new InputStreamReader(is, Utf8.charset)).closeAtEnd(is);
	}

	public static Streamlet<String> from(Reader reader) {
		var br = new BufferedReader(reader);
		return new Streamlet<>(() -> Puller.of(() -> ex(() -> ReadLine.from(br))).closeAtEnd(br).closeAtEnd(reader));
	}

}
