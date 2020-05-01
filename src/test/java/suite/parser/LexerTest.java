package suite.parser;

import org.junit.jupiter.api.Test;
import primal.Nouns.Buffer;

import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class LexerTest {

	@Test
	public void test() throws IOException {
		var buffer = new char[Buffer.size];
		var sb = new StringBuilder();
		int nCharsRead;

		try (var reader = new FileReader("src/main/java/suite/parser/Lexer.java")) {
			while (0 <= (nCharsRead = reader.read(buffer)))
				sb.append(buffer, 0, nCharsRead);
		}

		var nTokens = 0;

		for (var token : new Lexer(sb.toString()).tokens()) {
			assertNotNull(token);
			nTokens++;
		}

		System.out.println("Number of tokens = " + nTokens);
	}

}
