package suite.jdk;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

public class UnsafeTest {

	@Test
	public void test() throws Exception {
		String className = "suite.cli.Main";
		byte bytes[] = Files.readAllBytes(Paths.get("target/classes/" + className.replace(".", "/") + ".class"));
		Class<? extends AutoCloseable> clazz = new UnsafeUtil().defineClass(AutoCloseable.class, className, bytes);
		clazz.newInstance().close();
	}

}
