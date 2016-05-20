package jdk;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Test;

import suite.jdk.JdkLoadClassUtil;
import suite.jdk.JdkUnsafeLoadClassUtil;
import suite.os.FileUtil;

public class JdkUtilTest {

	@Test
	public void test() throws IOException, ReflectiveOperationException {
		Path srcDir = FileUtil.tmp.resolve("src");
		Path binDir = FileUtil.tmp.resolve("bin");
		String className = "HelloWorld";

		FileUtil.mkdir(srcDir);
		FileUtil.mkdir(binDir);

		String src = "" //
				+ "public class " + className + " implements Runnable {" //
				+ "  public void run() {" //
				+ "    System.out.println(\"TEST\");" //
				+ "  }" //
				+ "}";

		try (JdkLoadClassUtil jdkLoadClassUtil = new JdkLoadClassUtil(srcDir, binDir)) {
			jdkLoadClassUtil.newInstance(Runnable.class, className, src).run();
		}

		new JdkUnsafeLoadClassUtil(srcDir, binDir).newInstance(Runnable.class, className, src).run();
	}

}
