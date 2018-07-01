package suite.node.parser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import suite.Defaults;
import suite.adt.pair.Pair;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.parser.Wildcard;
import suite.util.Fail;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.To;

// mAIN=suite.node.parser.RecursiveFileFactorizerMain ./run.sh
public class RecursiveFileFactorizerMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(RecursiveFileFactorizerMain.class, args);
	}

	protected boolean run(String[] args) throws IOException {
		var fts = List.of( //
				Pair.of("fc-define-var-types .0 .1 .2 .3", "fc-define-var-types .1 .2 .3 .0") //
		);

		FileUtil.findPaths(Paths.get("src/main/ll/fc")) //
				.filter(path -> Wildcard.isMatch("*.sl", path.getFileName().toString())) //
				.forEach(path -> {
					var recursiveFactorizer = new RecursiveFactorizer(TermOp.values());
					var s = To.string(path);
					for (var ft : fts)
						s = recursiveFactorizer.rewrite(ft.t0, ft.t1, s);
					try {
						Files.write(path, s.getBytes(Defaults.charset));
					} catch (IOException ex) {
						Fail.t(ex);
					}
				});

		return true;
	}

}
