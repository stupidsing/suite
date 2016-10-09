package suite.inspect;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.lp.Configuration.ProverConfig;
import suite.node.util.Singleton;

public class InspectTest {

	private Inspect inspect = Singleton.get().getInspect();
	private ProverConfig pc0 = new ProverConfig();
	private ProverConfig pc1 = new ProverConfig();

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
