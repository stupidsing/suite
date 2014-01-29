package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.text.TextUtil.ConflictException;
import suite.util.To;

public class TextUtilTest {

	private TextUtil textUtil = new TextUtil();

	@Test
	public void test() throws ConflictException {
		String a = "abc12def34ghi";
		String b = "abc56def78ghi";
		String c = "abc56qwe78ghi";
		String d = "abc56qwe78ghi";

		// Test diff
		PatchData patchData = textUtil.diff(To.bytes(a), To.bytes(b));

		StringBuilder sb = new StringBuilder();
		patchData.write(sb);
		System.out.println(sb);

		String expected = "0:3|0:3|N<<abc>>" //
				+ "3:5|3:5|Y<<12|56>>" //
				+ "5:8|5:8|N<<def>>" //
				+ "10:12|8:10|Y<<34|78>>" //
				+ "10:13|10:13|N<<ghi>>";
		assertEquals(expected, sb.toString());

		// Test patch
		assertEquals(b, To.string(textUtil.patch(To.bytes(a), patchData)));

		// Test merge
		PatchData patchData1 = textUtil.diff(To.bytes(a), To.bytes(c));
		PatchData mergedPatchData = textUtil.merge(patchData, patchData1);
		assertEquals(d, To.string(textUtil.patch(To.bytes(a), mergedPatchData)));
	}

}
