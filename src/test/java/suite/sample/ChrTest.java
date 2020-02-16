package suite.sample;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import suite.Suite;

public class ChrTest {

	@Test
	public void test() {
		var chr = chr(List.of( //
				"if (LE.x .x,) then () end" //
				, "if (LE .x .y, LE .y .x,) then (.x = .y,) end" //
				, "given (LE .x .y,) if (LE .x .y,) then () end" //
				, "given (LE .x .y, LE .y .z,) if () then (LE .x .z,) end"));

		var facts0 = List.of(Suite.parse("LE A B"), Suite.parse("LE B C"), Suite.parse("LE C A"));
		var facts1 = chr.chr(facts0);
		assertEquals(2, facts1.size());
		System.out.println(facts1);
	}

	private Chr chr(List<String> rules) {
		var chr = new Chr();
		for (var s : rules)
			chr.addRule(Suite.parse(s));
		return chr;
	}

}
