package suite;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import suite.lp.doer.ProverTest;
import suite.os.LogUtil;

@RunWith(Suite.class)
@Suite.SuiteClasses({ ProverTest.class, })
public class TestSuite {

	@Before
	public void start() {
		LogUtil.initLog4j(Level.DEBUG);
	}

}
