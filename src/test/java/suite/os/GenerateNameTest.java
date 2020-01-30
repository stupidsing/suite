package suite.os;

import java.util.Random;

import org.junit.Test;

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

		System.out.println(name);
		System.out.println(biz);
		System.out.println(street + ", " + city);
	}

	@Test
	public void testGetRandomQuote() {
		var quote = getRandom("https://raw.githubusercontent.com/akhiltak/inspirational-quotes/master/Quotes.csv")
				.split(";");

		System.out.println(quote[0]);
		System.out.println(quote[1]);
		System.out.println(quote[2]);
	}

	private String getRandom(String url) {
		var list = storeCache.http(url).collect(As::lines).toList();
		return list.get(random.nextInt(list.size()));
	}

}
