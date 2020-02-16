package suite.inspect;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import suite.lp.Configuration.ProverCfg;
import suite.node.util.Singleton;

public class InspectTest {

	private Inspect inspect = Singleton.me.inspect;
	private ProverCfg pc0 = new ProverCfg();
	private ProverCfg pc1 = new ProverCfg();

	{
		pc1.setTrace(true);
	}

	@Test
	public void testEquals() {
		assertTrue(inspect.equals(pc0, pc0));
		assertFalse(inspect.equals(pc0, pc1));
	}

	@Test
	public void testHashCode() {
		assertTrue(inspect.hashCode(pc0) != inspect.hashCode(pc1));
	}

}
