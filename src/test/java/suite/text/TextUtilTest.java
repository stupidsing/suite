package suite.text;

import org.junit.jupiter.api.Test;
import suite.util.To;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TextUtilTest {

	private TextUtil textUtil = new TextUtil();

	@Test
	public void test() {
		var orig = "abc12def34ghi";
		var version_a = "abc567def890ghi";
		var version_b = "abc12zxcvbn34ghi";
		var version_c = "abc567zxcvbn34ghi";
		var merged_ab = "abc567zxcvbn890ghi";
		var merged_ac = "abc567zxcvbn890ghi";
		var orig1 = "abc12def34xyz";
		var version1_a = "abc567def890xyz";

		// test diff
		var patch_a = textUtil.diff(To.bytes(orig), To.bytes(version_a));
		var patch_b = textUtil.diff(To.bytes(orig), To.bytes(version_b));
		var patch_c = textUtil.diff(To.bytes(orig), To.bytes(version_c));

		var expected = "abc" //
				+ "[12|567]" //
				+ "def" //
				+ "[34|890]" //
				+ "ghi";
		assertEquals(expected, textUtil.toString(patch_a));

		// test patch
		assertEquals(version_a, To.string(textUtil.patch(To.bytes(orig), patch_a)));
		assertEquals(version1_a, To.string(textUtil.merge(To.bytes(orig), To.bytes(orig1), To.bytes(version_a))));

		// test merge
		var mergedPatch_ab = textUtil.merge(patch_a, patch_b);
		assertEquals(merged_ab, To.string(textUtil.patch(To.bytes(orig), mergedPatch_ab)));

		// test merge that requires agreeing on target content
		System.out.println(textUtil.toString(patch_a));
		System.out.println(textUtil.toString(patch_c));
		var mergedPatch_ac = textUtil.merge(patch_a, patch_c, true);
		assertEquals(merged_ac, To.string(textUtil.patch(To.bytes(orig), mergedPatch_ac)));
	}

}
