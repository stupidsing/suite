package suite.http;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;

import suite.primitive.Chars;

public abstract class HttpSourceHandler extends HttpIoHandler {

	protected abstract List<Chars> handle(Chars in) throws IOException;

	@Override
	public void handle(Reader reader, Writer writer) throws IOException {
		char buffer[] = new char[4096];
		int nCharsRead;

		while ((nCharsRead = reader.read(buffer)) >= 0)
			for (Chars chars : handle(Chars.of(buffer, 0, nCharsRead)))
				writer.write(chars.cs, chars.start, chars.end - chars.start);
	}

}
