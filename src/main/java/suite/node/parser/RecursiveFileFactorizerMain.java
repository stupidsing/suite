package suite.node.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

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
		List<Pair<String, String>> fts = Arrays.asList( //
				Pair.of("declare .0 as .1", "declare .1 .0") //
		);

		FileUtil.findPaths(Paths.get("src/")) //
				.filter(path -> WildcardUtil.isMatch("*.il", path.getFileName().toString())) //
				.forEach(path -> {
					try {
						RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
						String s = FileUtil.read(path);
						for (Pair<String, String> ft : fts)
							s = recursiveFactorizer.rewrite(ft.t0, ft.t1, s);
						Files.write(path, s.getBytes(FileUtil.charset));
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				});

		return true;
	}

}
