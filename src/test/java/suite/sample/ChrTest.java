package suite.sample;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class ChrTest {

	@Test
	public void test() {
		Chr chr = chr(List.of( //
				"if (LE.x .x,) then () end" //
				, "if (LE .x .y, LE .y .x,) then (.x = .y,) end" //
				, "given (LE .x .y,) if (LE .x .y,) then () end" //
				, "given (LE .x .y, LE .y .z,) if () then (LE .x .z,) end"));

		List<Node> facts = List.of(Suite.parse("LE A B"), Suite.parse("LE B C"), Suite.parse("LE C A"));
		Collection<Node> facts1 = chr.chr(facts);
		assertEquals(2, facts1.size());
		System.out.println(facts1);
	}

	private Chr chr(List<String> rules) {
		Chr chr = new Chr();
		for (var s : rules)
			chr.addRule(Suite.parse(s));
		return chr;
	}

}
