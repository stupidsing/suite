package test;

import org.apache.log4j.Level;
import org.btree.B_TreeTest;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.suite.ComparerTest;
import org.suite.FormatterTest;
import org.suite.ImportTest;
import org.suite.TermParserTest;
import org.suite.ProverTest;
import org.util.LogUtil;

@RunWith(Suite.class)
@Suite.SuiteClasses( { B_TreeTest.class, ComparerTest.class,
		FormatterTest.class, ImportTest.class, TermParserTest.class,
		ProverTest.class })
public class TestSuite {

	@Before
	public void start() {
		LogUtil.initLog4j(Level.DEBUG);
	}

}
