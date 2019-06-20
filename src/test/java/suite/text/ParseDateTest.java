package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ParseDateTest {

	private ParseDate pd = new ParseDate();

	@Test
	public void test() {
		assertEquals(1561027834, pd.parse("2019-06-20 10:50:34").getEpochSecond());
		assertEquals(1560909600, pd.parse("2019-06-19 02:00").getEpochSecond());
		assertEquals(1560909600, pd.parse("Wed Jun 19 2019,2:00am UTC").getEpochSecond());
		assertEquals(1560909600, pd.parse("Wednesday June 19 2019,10:00am GMT+8").getEpochSecond());
	}

}
