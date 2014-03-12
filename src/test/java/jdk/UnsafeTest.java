package jdk;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import sun.misc.Unsafe;

public class UnsafeTest {

	@Test
	public void test() throws Exception {
		byte bytes[] = Files.readAllBytes(Paths.get("target/classes/suite/cli/Main.class"));
		Class<?> clazz = getUnsafe().defineClass("suite.cli.Main", bytes, 0, bytes.length, getClass().getClassLoader(), null);
		((AutoCloseable) clazz.newInstance()).close();
	}

	private Unsafe getUnsafe() throws Exception {
		Field f = Unsafe.class.getDeclaredField("theUnsafe");
		f.setAccessible(true);
		return (Unsafe) f.get(null);
	}

}
