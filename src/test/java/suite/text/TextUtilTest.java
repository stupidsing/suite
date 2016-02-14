package suite.text;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import suite.adt.Pair;
import suite.primitive.Bytes;
import suite.text.TextUtil.ConflictException;
import suite.util.To;

public class TextUtilTest {

	private TextUtil textUtil = new TextUtil();

	@Test
	public void test() throws ConflictException {
		String orig = "abc12def34ghi";
		String version_a = "abc567def890ghi";
		String version_b = "abc12zxcvbn34ghi";
		String version_c = "abc567zxcvbn34ghi";
		String merged_ab = "abc567zxcvbn890ghi";
		String merged_ac = "abc567zxcvbn890ghi";
		String orig1 = "abc12def34xyz";
		String version1_a = "abc567def890xyz";

		// Test diff
		List<Pair<Bytes, Bytes>> patch_a = textUtil.diff(To.bytes(orig), To.bytes(version_a));
		List<Pair<Bytes, Bytes>> patch_b = textUtil.diff(To.bytes(orig), To.bytes(version_b));
		List<Pair<Bytes, Bytes>> patch_c = textUtil.diff(To.bytes(orig), To.bytes(version_c));

		String expected = "abc" //
				+ "[12|567]" //
				+ "def" //
				+ "[34|890]" //
				+ "ghi";
		assertEquals(expected, textUtil.toString(patch_a));

		// Test patch
		assertEquals(version_a, To.string(textUtil.patch(To.bytes(orig), patch_a)));
		assertEquals(version1_a, To.string(textUtil.merge(To.bytes(orig), To.bytes(orig1), To.bytes(version_a))));

		// Test merge
		List<Pair<Bytes, Bytes>> mergedPatch_ab = textUtil.merge(patch_a, patch_b);
		assertEquals(merged_ab, To.string(textUtil.patch(To.bytes(orig), mergedPatch_ab)));

		// Test merge that requires agreeing on target content
		System.out.println(textUtil.toString(patch_a));
		System.out.println(textUtil.toString(patch_c));
		List<Pair<Bytes, Bytes>> mergedPatch_ac = textUtil.merge(patch_a, patch_c, true);
		assertEquals(merged_ac, To.string(textUtil.patch(To.bytes(orig), mergedPatch_ac)));
	}

}
