package suite;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import suite.funp.FunpTest;
import suite.os.ElfTest;
import suite.os.ElfTest0;

// (cd src/test/java/; find * -name \*.java) | sed 's#/#.#g' | sed 's#\.java$#\.class, //#g'
// USE_32BIT=1 mvn -Dtest=TestSuite test
@RunWith(Suite.class)
@Suite.SuiteClasses({ //
		ElfTest.class, //
		ElfTest0.class, //
		FunpTest.class, //
})
public class TestSuite {

}
