package suite.node.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;
import suite.wildcard.WildcardUtil;

// mvn assembly:single && java -cp target/suite-1.0-jar-with-dependencies.jar suite.node.parser.RecursiveFileFactorizerMain
public class RecursiveFileFactorizerMain extends ExecutableProgram {

	public static void main(String args[]) {
		Util.run(RecursiveFileFactorizerMain.class, args);
	}

	protected boolean run(String args[]) throws IOException {
		// String from = "ic-parse .0 .1";
		// String to = "ic-parse .f .0 .1";
		String from = "ic-compile .0 .1 .2";
		String to = "ic-compile .f/.0/.ve .0 .1 .2";

		FileUtil.findPaths(Paths.get("src/")) //
				.filter(path -> WildcardUtil.isMatch("ic*.sl", path.getFileName().toString())) //
				.forEach(path -> {
					try {
						RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
						String s0 = FileUtil.read(path);
						String sx = recursiveFactorizer.rewrite(from, to, s0);
						Files.write(path, sx.getBytes(FileUtil.charset));
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				});

		return true;
	}

}
