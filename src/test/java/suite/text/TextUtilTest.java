package suite.text;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.text.TextUtil.ConflictException;
import suite.util.To;

public class TextUtilTest {

	private TextUtil textUtil = new TextUtil();

	@Test
	public void test() throws ConflictException {
		String orig = "abc12def34ghi";
		String va = "abc56def78ghi";
		String vb = "abc12qwe34ghi";
		String orig1 = "abc12def34xyz";
		String v1a = "abc56def78xyz";
		String mergedab = "abc56qwe78ghi";

		// Test diff
		PatchData patchData = textUtil.diff(To.bytes(orig), To.bytes(va));

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
		assertEquals(va, To.string(textUtil.patch(To.bytes(orig), patchData)));
		assertEquals(v1a, To.string(textUtil.patch(To.bytes(orig1), patchData)));

		// Test merge
		PatchData patchData1 = textUtil.diff(To.bytes(orig), To.bytes(vb));
		PatchData mergedPatchData = textUtil.merge(patchData, patchData1);
		assertEquals(mergedab, To.string(textUtil.patch(To.bytes(orig), mergedPatchData)));
	}

}
