package suite.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import primal.Nouns.Utf8;
import suite.http.Crypts.Crypt;

public class CryptsTest {

	@Test
	public void test() {
		var in = "Secret Message";

		var crypts = new Crypts();
		var aes = crypts.aes("ssshhhhhhhhhhh!!!!");
		var rsa = crypts.rsa("");

		test("aes", aes, in);
		test("rsa", rsa, in);
	}

	private void test(String algo, Crypt<String> aes, String in) {
		var inbs = in.getBytes(Utf8.charset);
		var outBs = aes.decrypt(aes.encrypt(inbs));
		var out = new String(outBs, Utf8.charset);
		System.out.println(algo + " = " + out);
		assertEquals(in, out);
	}

}
