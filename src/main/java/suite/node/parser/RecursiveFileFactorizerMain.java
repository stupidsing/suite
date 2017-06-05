package suite.node.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import suite.Constants;
import suite.adt.pair.Pair;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.util.To;
import suite.util.Util;
import suite.util.Util.ExecutableProgram;
import suite.wildcard.WildcardUtil;

// mAIN=suite.node.parser.RecursiveFileFactorizerMain ./run.sh
public class RecursiveFileFactorizerMain extends ExecutableProgram {

	public static void main(String[] args) {
		Util.run(RecursiveFileFactorizerMain.class, args);
	}

	protected boolean run(String[] args) throws IOException {
		List<Pair<String, String>> fts = Arrays.asList( //
				Pair.of("fc-define-var-types .0 .1 .2 .3", "fc-define-var-types .1 .2 .3 .0") //
		);

		FileUtil.findPaths(Paths.get("src/main/ll/fc")) //
				.filter(path -> WildcardUtil.isMatch("*.sl", path.getFileName().toString())) //
				.forEach(path -> {
					RecursiveFactorizer recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
					String s = To.string(path);
					for (Pair<String, String> ft : fts)
						s = recursiveFactorizer.rewrite(ft.t0, ft.t1, s);
					try {
						Files.write(path, s.getBytes(Constants.charset));
					} catch (IOException ex) {
						throw new RuntimeException(ex);
					}
				});

		return true;
	}

}
