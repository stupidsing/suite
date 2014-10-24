package suite.instructionexecutor;

import java.io.IOException;
import java.io.Reader;

import suite.immutable.IPointer;
import suite.primitive.Chars;
import suite.util.Util;

public class IndexedCharsReader {

	private static int bufferSize = 4096;

	public static IPointer<Chars> read(Reader reader) {
		return new IndexedSourceReader<Chars>(() -> {
			try {
				char buffer[] = new char[bufferSize];
				int nCharsRead = reader.read(buffer);
				if (nCharsRead >= 0)
					return Chars.of(buffer, 0, nCharsRead);
				else {
					Util.closeQuietly(reader);
					return null;
				}
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}).pointer();
	}

}
