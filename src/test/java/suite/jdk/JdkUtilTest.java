package suite.jdk;

import static primal.statics.Fail.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.List;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import primal.Nouns.Tmp;
import primal.Verbs.Mk;
import primal.Verbs.New;
import primal.jdk.UnsafeUtil;

public class JdkUtilTest {

	private String className = "HelloWorld";

	private String source = "" //
			+ "package primal.jdk;" //
			+ "public class " + className + " implements Runnable {" //
			+ "	public void run() {" //
			+ "		System.out.println(\"TEST\");" //
			+ "	}" //
			+ "}";

	@Test
	public void test() {
		var srcDir = Tmp.path("src");
		var binDir = Tmp.path("bin");

		Mk.dir(srcDir);
		Mk.dir(binDir);

		try (var jdkLoadClassUtil = new JdkLoadClassUtil(srcDir, binDir)) {
			jdkLoadClassUtil.newInstance(Runnable.class, className, source).run();
		}
	}

	@Test
	public void test1() throws IOException {
		var pkgName = UnsafeUtil.class.getPackageName();
		var clazz = compile(Runnable.class, pkgName + "." + className, source);
		New.clazz(clazz).run();
	}

	private <I> Class<? extends I> compile(Class<I> clazz, String className, String source) {
		var filename = className.replace('.', '/') + ".java";
		var baos_ = new ByteArrayOutputStream();

		try (var baos = baos_) {
			var sjfo = new SimpleJavaFileObject(URI.create(filename), Kind.SOURCE) {
				public CharSequence getCharContent(boolean ignoreEncodingErrors) {
					return source;
				}

				public OutputStream openOutputStream() {
					return baos;
				}
			};

			var sfm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null);

			var jfm = new ForwardingJavaFileManager<>(sfm) {
				public JavaFileObject getJavaFileForOutput(Location loc, String clazz, Kind kind, FileObject sibling) {
					return sjfo;
				}
			};

			ToolProvider.getSystemJavaCompiler().getTask(null, jfm, null, null, null, List.of(sjfo)).call();
		} catch (IOException ex) {
			fail(ex);
		}

		return new UnsafeUtil().defineClass(clazz, filename, baos_.toByteArray());
	}

}
