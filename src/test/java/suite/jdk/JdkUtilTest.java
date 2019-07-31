package suite.jdk;

import static primal.statics.Fail.fail;

import java.io.IOException;

import org.junit.Test;

import suite.cfg.Defaults;
import suite.os.FileUtil;

public class JdkUtilTest {

	@Test
	public void test() {
		var srcDir = Defaults.tmp("src");
		var binDir = Defaults.tmp("bin");
		var className = "HelloWorld";

		FileUtil.mkdir(srcDir);
		FileUtil.mkdir(binDir);

		var src = "" //
				+ "public class " + className + " implements Runnable {" //
				+ "  public void run() {" //
				+ "    System.out.println(\"TEST\");" //
				+ "  }" //
				+ "}";

		try (var jdkLoadClassUtil = new JdkLoadClassUtil(srcDir, binDir)) {
			jdkLoadClassUtil.newInstance(Runnable.class, className, src).run();
		} catch (IOException ex) {
			fail(ex);
		}

		new JdkUnsafeLoadClassUtil(srcDir, binDir).newInstance(Runnable.class, className, src).run();
	}

}
