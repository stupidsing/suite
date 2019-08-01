package suite.jdk;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;

import primal.Verbs.New;
import suite.cli.Main;

public class UnsafeUtilTest {

	@Test
	public void test() throws Exception {
		var className = "suite.cli.Main";
		var bytes = Files.readAllBytes(Paths.get("target/classes/" + className.replace(".", "/") + ".class"));
		Class<? extends AutoCloseable> clazz = new UnsafeUtil().defineClass(Main.class, className, bytes);
		New.clazz(clazz).close();
	}

}
