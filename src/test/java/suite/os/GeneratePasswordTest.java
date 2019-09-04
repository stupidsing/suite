package suite.os;

import java.security.SecureRandom;

import org.junit.Test;

public class GeneratePasswordTest {

	private SecureRandom sr = new SecureRandom();

	private String alphabets = "abcdefghijklmnopqrstuvwxyz";
	private String caps = alphabets.toUpperCase();
	private String digits = "0123456789";
	private String symbols = "~!@#$%^&*()_+`-={}|[]\\:\";'<>?,./'";

	@Test
	public void test() {
		var length = 10;
		var nCaps = 1;
		var nDigits = 2;
		var nSymbols = 1;
		var nAlphabets = length - nCaps - nDigits - nSymbols;
		var pw = "";

		pw = insert(pw, nAlphabets, alphabets);
		pw = insert(pw, nCaps, caps);
		pw = insert(pw, nDigits, digits);
		pw = insert(pw, nSymbols, symbols);

		System.out.println(pw);
	}

	private String insert(String pw, int n, String dict) {
		for (var i = 0; i < n; i++) {
			var p = sr.nextInt(pw.length() + 1);
			pw = pw.substring(0, p) + dict.charAt(sr.nextInt(dict.length())) + pw.substring(p);
		}
		return pw;
	}

}
