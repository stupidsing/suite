package suite.jdk;

import org.junit.Test;

import primal.Verbs.Mk;
import suite.cfg.Defaults;

public class JdkUtilTest {

	@Test
	public void test() {
		var srcDir = Defaults.tmp("src");
		var binDir = Defaults.tmp("bin");
		var className = "HelloWorld";

		Mk.dir(srcDir);
		Mk.dir(binDir);

		var src = "" //
				+ "public class " + className + " implements Runnable {" //
				+ "  public void run() {" //
				+ "    System.out.println(\"TEST\");" //
				+ "  }" //
				+ "}";

		try (var jdkLoadClassUtil = new JdkLoadClassUtil(srcDir, binDir)) {
			jdkLoadClassUtil.newInstance(Runnable.class, className, src).run();
		}

		new JdkUnsafeLoadClassUtil(srcDir, binDir).newInstance(Runnable.class, className, src).run();
	}

}
