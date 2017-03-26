package suite.parser;

import static org.junit.Assert.assertNotNull;

import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;

import suite.Constants;
import suite.util.FunUtil;

public class LexerTest {

	@Test
	public void test() throws IOException {
		char buffer[] = new char[Constants.bufferSize];
		StringBuilder sb = new StringBuilder();
		int nCharsRead;

		try (FileReader reader = new FileReader("src/main/java/suite/parser/Lexer.java")) {
			while (0 <= (nCharsRead = reader.read(buffer)))
				sb.append(buffer, 0, nCharsRead);
		}

		int nTokens = 0;

		for (String token : FunUtil.iter(new Lexer(sb.toString()).tokens())) {
			assertNotNull(token);
			nTokens++;
		}

		System.out.println("Number of tokens = " + nTokens);
	}

}
