package suite.jdk;

import static primal.statics.Fail.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

import primal.Nouns.Utf8;
import primal.Verbs.Mk;
import primal.Verbs.New;
import primal.Verbs.ReadFile;
import primal.Verbs.WriteFile;
import primal.jdk.UnsafeUtil;
import primal.os.Log_;

public class JdkUtil {

	private Path srcDir;
	private Path binDir;

	public JdkUtil(Path srcDir, Path binDir) {
		this.srcDir = srcDir;
		this.binDir = binDir;
	}

	public <T> T newInstance(Class<T> interfaceClazz, String canonicalName, String java) {
		return New.clazz(load(interfaceClazz, canonicalName, compile(canonicalName, java)));
	}

	protected Path compile(String canonicalName, String java) {
		Path srcFilePath = srcDir.resolve(canonicalName.replace('.', '/') + ".java");
		Path binFilePath = binDir.resolve(canonicalName.replace('.', '/') + ".class");

		Log_.info("Writing " + srcFilePath);
		WriteFile.to(srcFilePath).doWrite(os -> os.write(java.getBytes(Utf8.charset)));

		// compile the Java, load the class, return an instantiated object
		Log_.info("Compiling " + srcFilePath);
		Mk.dir(binDir);

		var jc = ToolProvider.getSystemJavaCompiler();

		try (var sjfm = jc.getStandardFileManager(null, null, null)) {
			if (jc.getTask( //
					null, //
					null, //
					null, //
					List.of("-d", binDir.toString()), //
					null, //
					sjfm.getJavaFileObjects(srcFilePath.toFile())).call())
				return binFilePath;
			else
				return fail("Java compilation error");
		} catch (IOException ex) {
			return fail(ex);
		}
	}

	private <T> Class<? extends T> load(Class<T> interfaceClazz, String canonicalName, Path path) {
		Log_.info("Loading class " + canonicalName);
		var bytes = ReadFile.from(path).readBytes();
		return new UnsafeUtil().defineClass(interfaceClazz, canonicalName, bytes);
	}

}
