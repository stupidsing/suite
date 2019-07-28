package suite.jdk;

import static suite.util.Fail.fail;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import javax.tools.ToolProvider;

import suite.cfg.Defaults;
import suite.os.FileUtil;
import suite.os.Log_;

public class JdkUtil {

	private Path srcDir;
	private Path binDir;

	public JdkUtil(Path srcDir, Path binDir) {
		this.srcDir = srcDir;
		this.binDir = binDir;
	}

	protected Path compile(String canonicalName, String java) {
		Path srcFilePath = srcDir.resolve(canonicalName.replace('.', '/') + ".java");
		Path binFilePath = binDir.resolve(canonicalName.replace('.', '/') + ".class");

		Log_.info("Writing " + srcFilePath);
		FileUtil.out(srcFilePath).doWrite(os -> os.write(java.getBytes(Defaults.charset)));

		// compile the Java, load the class, return an instantiated object
		Log_.info("Compiling " + srcFilePath);
		FileUtil.mkdir(binDir);

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

}
