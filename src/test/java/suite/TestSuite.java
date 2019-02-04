package suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

// (cd src/test/java/; find * -name \*.java) | sed 's#/#.#g' | sed 's#\.java$#\.class, //#g'

@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		suite.trade.PairTest.class, //
})
public class TestSuite {

}
