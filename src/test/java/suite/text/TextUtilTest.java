package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.util.To;

public class TextUtilTest {

	private TextUtil textUtil = new TextUtil();

	@Test
	public void test() {
		PatchData patchData = textUtil.diff(To.bytes("abc12def34ghi"), To.bytes("abc56def78ghi"));
		StringBuilder sb = new StringBuilder();
		patchData.write(sb);
		System.out.println(sb);

		String expected = "0:3|0:3|N<<abc>>" //
				+ "3:5|3:5|Y<<12|56>>" //
				+ "5:8|5:8|N<<def>>" //
				+ "10:12|8:10|Y<<34|78>>" //
				+ "10:13|10:13|N<<ghi>>";
		assertEquals(expected, sb.toString());
	}

}
