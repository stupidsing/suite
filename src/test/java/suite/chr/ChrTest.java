package suite.chr;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.node.Node;

public class ChrTest {

	@Test
	public void test() {
		Chr chr = new Chr();
		chr.addRule(Suite.parse("given () if (LE.x .x,) then () when () end"));
		chr.addRule(Suite.parse("given () if (LE .x .y, LE .y .x,) then (.x = .y,) when () end"));
		chr.addRule(Suite.parse("given (LE .x .y,) if (LE .x .y,) then () when () end"));
		chr.addRule(Suite.parse("given (LE .x .y, LE .y .z,) if () then (LE .x .z,) when () end"));

		List<Node> facts = Arrays.asList(Suite.parse("LE A B"), Suite.parse("LE B C"), Suite.parse("LE C A"));
		System.out.println(chr.chr(facts));
	}
}
