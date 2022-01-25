package suite.os;

import java.util.Random;

import org.junit.jupiter.api.Test;

import suite.node.util.Ioc;
import suite.streamlet.As;

public class GenerateNameTest {

	private Random random = new Random();
	private StoreCache storeCache = Ioc.of(StoreCache.class);

	@Test
	public void testGenerateName() {
		var rg = "https://raw.githubusercontent.com";
		var repo0 = rg + "/dominictarr/random-name/master";
		var repo1 = rg + "/9b/heavy_pint/master/lists";

		var name0 = getRandom(repo0 + "/first-names.txt");
		var name1 = getRandom(repo0 + "/names.txt");
		var biz = getRandom(repo1 + "/business-names.txt");
		var street = getRandom(repo1 + "/street-addresses.txt");
		var city = getRandom(repo1 + "/city-details.txt");
		var name = name0 + " " + name1;

		System.out.println(name0.substring(0, 3).toLowerCase() + "_" + name1.substring(0, 3).toLowerCase() + "@mailnesia.com");
		System.out.println(name0 + "_" + name1);
		System.out.println(name);
		System.out.println(biz);
		System.out.println(street + ", " + city);
	}

	@Test
	public void testGetRandomQuote() {
		var url = "https://raw.githubusercontent.com/akhiltak/inspirational-quotes/master/Quotes.csv";
		var quote = getRandom(url).split(";");
		var cm = "abcdefghijklmnopqrstuvwxyz".toCharArray();

		for (var n = 0; n < 100; n++) {
			var i = random.nextInt(26);
			var j = random.nextInt(26);
			var t = cm[i];
			cm[i] = cm[j];
			cm[j] = t;
		}

		var q = quote[0].toUpperCase();

		for (var i = 0; i < 26; i++)
			q = q.replace((char) ('A' + i), cm[i]);

		System.out.println(q);
		// System.out.println(quote[0]);
		System.out.println(quote[1]);
		System.out.println(quote[2]);
	}

	private String getRandom(String url) {
		var list = storeCache.http(url).collect(As::lines).toList();
		return list.get(random.nextInt(list.size()));
	}

}
