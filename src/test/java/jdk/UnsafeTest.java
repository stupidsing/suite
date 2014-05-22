package jdk;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import suite.util.UnsafeUtil;

public class UnsafeTest {

	@Test
	public void test() throws Exception {
		String className = "suite.cli.Main";
		byte bytes[] = Files.readAllBytes(Paths.get("target/classes/" + className.replace(".", "/") + ".class"));
		Class<?> clazz = new UnsafeUtil().defineClass(className, bytes);
		((AutoCloseable) clazz.newInstance()).close();
	}

}
