package suite.jdk;

import java.io.IOException;

import org.junit.Test;

import suite.Defaults;
import suite.os.FileUtil;
import suite.util.Fail;

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
			Fail.t(ex);
		}

		new JdkUnsafeLoadClassUtil(srcDir, binDir).newInstance(Runnable.class, className, src).run();
	}

}
