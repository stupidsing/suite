package jdk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.junit.Test;

import suite.util.IoUtil;

public class JdkCompilerTest {

	@Test
	public void test() throws IOException, ReflectiveOperationException {
		String srcDir = "/tmp/src";
		String binDir = "/tmp/bin";
		String className = "HelloWorld";

		new File(srcDir).mkdirs();
		new File(binDir).mkdirs();

		String srcFile = srcDir + "/" + className + ".java";
		String src = "" //
				+ "public class " + className + " implements Runnable {" //
				+ "  public void run() {" //
				+ "    System.out.println(\"TEST\");" //
				+ "  }" //
				+ "}";

		try (OutputStream os = new FileOutputStream(srcFile)) {
			os.write(src.getBytes(IoUtil.charset));
		}

		JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

		try (StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null)) {
			File file = new File(srcFile);
			jc.getTask(null //
					, null //
					, null //
					, Arrays.asList("-d", binDir) //
					, null //
					, sjfm.getJavaFileObjects(file)).call();

			URL urls[] = { new URL("file://" + binDir + "/") };

			try (URLClassLoader ucl = new URLClassLoader(urls)) {
				Class<?> clazz = ucl.loadClass(className);
				System.out.println("Class has been successfully loaded");
				Runnable object = (Runnable) clazz.newInstance();
				object.run();
			}
		}
	}

}
