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

	protected void compile(String packageName, String className, String java) throws IOException {
		String pathName = srcDir + "/" + packageName.replace('.', '/');
		String filename = pathName + "/" + className + ".java";

		LogUtil.info("Writing " + filename);
		try (OutputStream os = FileUtil.out(new File(filename))) {
			os.write(java.getBytes(FileUtil.charset));
		}
		File file = new File(filename);

		// Compile the Java, load the class, return an instantiated object
		LogUtil.info("Compiling " + file);
		new File(binDir).mkdirs();

		JavaCompiler jc = ToolProvider.getSystemJavaCompiler();

		try (StandardJavaFileManager sjfm = jc.getStandardFileManager(null, null, null)) {
			if (!jc.getTask(null //
					, null //
					, null //
					, Arrays.asList("-d", binDir) //
					, null //
					, sjfm.getJavaFileObjects(file)).call())
				throw new RuntimeException("Java compilation error");
		}
	}

	public String getBinDir() {
		return binDir;
	}

}
