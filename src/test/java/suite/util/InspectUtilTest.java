package suite.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.lp.doer.Configuration.ProverConfig;

public class InspectUtilTest {

	private InspectUtil inspectUtil = new InspectUtil();
	private ProverConfig pc0 = new ProverConfig();
	private ProverConfig pc1 = new ProverConfig();

	{
		pc1.setTrace(true);
	}

	@Test
	public void testEquals() {
		assertTrue(inspectUtil.equals(pc0, pc0));
		assertFalse(inspectUtil.equals(pc0, pc1));
	}

	@Test
	public void testHashCode() {
		assertTrue(inspectUtil.hashCode(pc0) != inspectUtil.hashCode(pc1));
	}

}
