package suite.fp;

import org.junit.jupiter.api.Test;
import suite.Suite;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SieveTest {

	@Test
	public void testSieve() {
		var sieve = "define sieve := `$p; $qs` => p; (qs | filter_{q => q % p != 0} | sieve) ~ ";
		assertEquals(Suite.parse("2; 3; 5; 7; 11; 13; 17; 19; 23; 29;"), //
				Suite.evaluateFun(sieve + "2 | iterate_{`+ 1`} | sieve | take_{10}", true));
	}

}
