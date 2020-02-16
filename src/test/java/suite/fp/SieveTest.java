package suite.fp;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import suite.Suite;

public class SieveTest {

	@Test
	public void testSieve() {
		var sieve = "define sieve := `$p; $qs` => p; (qs | filter_{q => q % p != 0} | sieve) ~ ";
		assertEquals(Suite.parse("2; 3; 5; 7; 11; 13; 17; 19; 23; 29;"), //
				Suite.evaluateFun(sieve + "2 | iterate_{`+ 1`} | sieve | take_{10}", true));
	}

}
