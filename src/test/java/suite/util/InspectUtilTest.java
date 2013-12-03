package suite.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.lp.doer.ProverConfig;

public class InspectUtilTest {

	private ProverConfig pc0 = new ProverConfig();
	private ProverConfig pc1 = new ProverConfig();

	{
		pc1.setTrace(true);
	}

	@Test
	public void testEquals() {
		assertTrue(InspectUtil.equals(pc0, pc0));
		assertFalse(InspectUtil.equals(pc0, pc1));
	}

	@Test
	public void testHashCode() {
		assertTrue(InspectUtil.hashCode(pc0) != InspectUtil.hashCode(pc1));
	}

	@Test
	public void testMapify() {
		Object map = InspectUtil.mapify(pc0);
		assertNotNull(map);
		System.out.println(map);
	}

}
