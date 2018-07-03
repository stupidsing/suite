package suite.parser;

import static org.junit.Assert.assertNotNull;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import suite.cfg.Defaults;
import suite.util.FunUtil;

public class LexerTest {

	@Test
	public void test() throws IOException {
		var buffer = new char[Defaults.bufferSize];
		var sb = new StringBuilder();
		int nCharsRead;

		try (var reader = new FileReader("src/main/java/suite/parser/Lexer.java")) {
			while (0 <= (nCharsRead = reader.read(buffer)))
				sb.append(buffer, 0, nCharsRead);
		}

		var nTokens = 0;

		for (var token : FunUtil.iter(new Lexer(sb.toString()).tokens())) {
			assertNotNull(token);
			nTokens++;
		}

		System.out.println("Number of tokens = " + nTokens);
	}

}
