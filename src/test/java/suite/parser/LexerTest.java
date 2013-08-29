package suite.parser;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

public class LexerTest {

	private int bufferSize = 4096;

	@Test
	public void test() throws IOException {
		char buffer[] = new char[bufferSize];
		StringBuilder sb = new StringBuilder();
		int nCharsRead;

		try (FileReader reader = new FileReader("src/main/java/suite/parser/Lexer.java")) {
			while ((nCharsRead = reader.read(buffer)) >= 0)
				sb.append(buffer, 0, nCharsRead);
		}

		for (String token : new Lexer(sb.toString()).tokens())
			System.out.println(token);
	}

}
