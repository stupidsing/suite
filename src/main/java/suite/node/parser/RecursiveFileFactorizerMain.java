package suite.node.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import suite.adt.Pair;
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
		Pair<String, String> ft0 = Pair.of("PS .0 .1", "PS .1 .0");
		Pair<String, String> ft1 = Pair.of("PARAM .0 .1", "PARAM .1 .0");

		FileUtil.findPaths(Paths.get("src/")) //
				.filter(path -> WildcardUtil.isMatch("ic*.sl", path.getFileName().toString())) //
				.forEach(path -> {
					try {
						RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
						String s = FileUtil.read(path);
						for (Pair<String, String> ft : Arrays.asList(ft0, ft1))
							s = recursiveFactorizer.rewrite(ft.t0, ft.t1, s);
						Files.write(path, s.getBytes(FileUtil.charset));
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				});

		return true;
	}

}
