package suite;

import org.apache.log4j.Level;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import suite.os.Log_;

// (cd src/test/java/; find * -name \*.java) | sed 's#/#.#g' | sed 's#\.java$#\.class, //#g'

@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		suite.trade.PairTest.class, //
})
public class TestSuite {

	@Before
	public void start() {
		Log_.initLogging(Level.DEBUG);
	}

}
