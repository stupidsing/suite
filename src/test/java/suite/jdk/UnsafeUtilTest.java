package suite.jdk;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import suite.cli.Main;
import suite.object.Object_;

public class UnsafeUtilTest {

	@Test
	public void test() throws Exception {
		var className = "suite.cli.Main";
		var bytes = Files.readAllBytes(Paths.get("target/classes/" + className.replace(".", "/") + ".class"));
		Class<? extends AutoCloseable> clazz = new UnsafeUtil().defineClass(Main.class, className, bytes);
		Object_.new_(clazz).close();
	}

}
