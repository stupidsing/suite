package suite.jdk;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import suite.Constants;
import suite.os.FileUtil;
import suite.os.LogUtil;

public class JdkUtil {

	private Path srcDir;
	private Path binDir;

	public JdkUtil(Path srcDir, Path binDir) {
		this.srcDir = srcDir;
		this.binDir = binDir;
	}

	protected Path compile(String canonicalName, String java) throws IOException {
		Path srcFilePath = srcDir.resolve(canonicalName.replace('.', '/') + ".java");
		Path binFilePath = binDir.resolve(canonicalName.replace('.', '/') + ".class");

		LogUtil.info("Writing " + srcFilePath);
		try (OutputStream os = FileUtil.out(srcFilePath)) {
			os.write(java.getBytes(Constants.charset));
		}

		// compile the Java, load the class, return an instantiated object
		LogUtil.info("Compiling " + srcFilePath);
		FileUtil.mkdir(binDir);

		JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

		try (StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null)) {
			if (!jc.getTask( //
					null, //
					null, //
					null, //
					Arrays.asList("-d", binDir.toString()), //
					null, //
					sjfm.getJavaFileObjects(srcFilePath.toFile())).call())
				throw new RuntimeException("Java compilation error");
		}

		return binFilePath;
	}

}
