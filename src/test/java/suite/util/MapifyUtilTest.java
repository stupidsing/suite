package suite.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.lp.doer.Configuration.ProverConfig;

public class MapifyUtilTest {

	private MapifyUtil mapifyUtil = new MapifyUtil(new InspectUtil());

	@Test
	public void testMapify() {
		ProverConfig pc0 = new ProverConfig();
		pc0.setRuleSet(null);

		Object map = mapifyUtil.mapify(ProverConfig.class, pc0);
		assertNotNull(map);
		System.out.println(map);

		ProverConfig pc1 = (ProverConfig) mapifyUtil.unmapify(ProverConfig.class, map);
		System.out.println(pc1);

		assertEquals(pc0, pc1);
		assertTrue(pc0.hashCode() == pc1.hashCode());
	}

}
