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
		String version_a = "abc567def890ghi";
		String version_b = "abc12qwerty34ghi";
		String version_c = "xyz567qwerty34ghi";
		String merged_ab = "abc567qwerty890ghi";
		String merged_ac = "xyz567qwerty890ghi";
		String orig1 = "abc12def34xyz";
		String version1_a = "abc567def890xyz";

		// Test diff
		PatchData patch_a = textUtil.diff(To.bytes(orig), To.bytes(version_a));
		System.out.println(patch_a);

		String expected = "0-3|0-3|=[abc]" //
				+ "3-5|3-6|C[12|567]" //
				+ "5-8|6-9|=[def]" //
				+ "8-10|9-12|C[34|890]" //
				+ "10-13|12-15|=[ghi]";
		assertEquals(expected, patch_a.toString());

		// Test patch
		assertEquals(version_a, To.string(textUtil.patch(To.bytes(orig), patch_a)));
		assertEquals(version1_a, To.string(textUtil.patch(To.bytes(orig1), patch_a)));

		// Test merge
		PatchData patch_b = textUtil.diff(To.bytes(orig), To.bytes(version_b));
		PatchData mergedPatch_ab = textUtil.merge(patch_a, patch_b);
		assertEquals(merged_ab, To.string(textUtil.patch(To.bytes(orig), mergedPatch_ab)));

		// Test merge that requires agreeing on target content
		PatchData patch_c = textUtil.diff(To.bytes(orig), To.bytes(version_c));
		PatchData mergedPatch_ac = textUtil.merge(patch_a, patch_c);
		assertEquals(merged_ac, To.string(textUtil.patch(To.bytes(orig), mergedPatch_ac)));
	}

}
