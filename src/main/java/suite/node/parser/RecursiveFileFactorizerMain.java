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
				Pair.of("ic-compile .0 .1", "ic-compile .1") //
				, Pair.of("ic-compile0 .0 .1", "ic-compile0 .1") //
				, Pair.of("ic-compile-operand .0 .1", "ic-compile-operand .1") //
				, Pair.of("ic-compile-operand0 .0 .1", "ic-compile-operand0 .1") //
				, Pair.of("ic-compile-better-option .0 .1", "ic-compile-better-option .1") //
				, Pair.of("ic-let .0 .1", "ic-let .1") //
				, Pair.of("ic-push-pop-parameters .0 .1", "ic-push-pop-parameters .1") //
		);

		FileUtil.findPaths(Paths.get("src/")) //
				.filter(path -> WildcardUtil.isMatch("ic*.sl", path.getFileName().toString())) //
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
