package suite.fp.eval;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.Test;

import suite.Suite;

public class MonadTest {

	@Test
	public void testMonad() throws IOException {
		StringWriter sw = new StringWriter();
		Suite.evaluateFunToWriter(Suite.fcc(Suite.applyDo(Suite.parse("" //
				+ "using MONAD >> ( \n" //
				+ "    define v := (mutable^string) of (skip-type-check 1) >> ( \n" //
				+ "        putm {atom:scope} {v} {\"abc\"} # getm {atom:scope} {v} \n" //
				+ "    ) \n" //
				+ ") \n" //
				+ ""), Suite.parse("string"))), sw);
		assertEquals("abc", sw.toString());
	}

}
