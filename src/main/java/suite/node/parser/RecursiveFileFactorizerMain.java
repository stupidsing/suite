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
		Pair<String, String> ft0 = Pair.of("ic-compile .0 .1 .2", "ic-compile .f/.0/.vs .1 .2");
		Pair<String, String> ft1 = Pair.of("ic-compile0 .0 .1 .2", "ic-compile0 .f/.0/.vs .1 .2");
		Pair<String, String> ft2 = Pair.of("ic-compile-operand .0 .1 .2 .3", "ic-compile-operand .f/.0/.vs .1 .2 .3");
		Pair<String, String> ft3 = Pair.of("ic-compile-operand0 .0 .1 .2 .3", "ic-compile-operand0 .f/.0/.vs .1 .2 .3");
		Pair<String, String> ft4 = Pair.of("ic-compile-better-option .0 .1 .2", "ic-compile-better-option .f/.0/.vs .1 .2");
		Pair<String, String> ft5 = Pair.of("ic-let .0 .1 .2 .3", "ic-let .f/.0/.vs .1 .2 .3");
		Pair<String, String> ft6 = Pair.of("ic-push-pop-parameters .0 .1 .2 .3", "ic-push-pop-parameters .f/.0/.vs .1 .2 .3");

		for (Pair<String, String> ft : Arrays.asList(ft0, ft1, ft2, ft3, ft4, ft5, ft6))
			FileUtil.findPaths(Paths.get("src/")) //
					.filter(path -> WildcardUtil.isMatch("ic*.sl", path.getFileName().toString())) //
					.forEach(path -> {
						try {
							RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
							String s0 = FileUtil.read(path);
							String sx = recursiveFactorizer.rewrite(ft.t0, ft.t1, s0);
							Files.write(path, sx.getBytes(FileUtil.charset));
						} catch (IOException ex) {
							throw new RuntimeException(ex);
						}
					});

		return true;
	}

}
