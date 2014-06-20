package suite.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class JdkUtil {

	private String srcDir;
	private String binDir;

	public JdkUtil(String srcDir, String binDir) {
		this.srcDir = srcDir;
		this.binDir = binDir;
	}

	protected String compile(String canonicalName, String java) throws IOException {
		String srcFilename = srcDir + "/" + canonicalName.replace('.', '/') + ".java";
		String binFilename = binDir + "/" + canonicalName.replace('.', '/') + ".class";

		LogUtil.info("Writing " + srcFilename);
		try (OutputStream os = FileUtil.out(srcFilename)) {
			os.write(java.getBytes(FileUtil.charset));
		}

		// Compile the Java, load the class, return an instantiated object
		LogUtil.info("Compiling " + srcFilename);
		new File(binDir).mkdirs();

		JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

		try (StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null)) {
			if (!jc.getTask(null //
					, null //
					, null //
					, Arrays.asList("-d", binDir) //
					, null //
					, sjfm.getJavaFileObjects(new File(srcFilename))).call())
				throw new RuntimeException("Java compilation error");
		}

		return binFilename;
	}

	public String getBinDir() {
		return binDir;
	}

}
