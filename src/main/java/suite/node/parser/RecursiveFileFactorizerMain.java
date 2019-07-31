package suite.node.parser;

import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Paths;

import suite.adt.pair.Pair;
import suite.cfg.Defaults;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.parser.Wildcard;
import suite.streamlet.Read;
import suite.util.RunUtil;

// mAIN=suite.node.parser.RecursiveFileFactorizerMain ./run.sh
public class RecursiveFileFactorizerMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var fts = Read.each( //
					Pair.of("fc-define-var-types .0 .1 .2 .3", "fc-define-var-types .1 .2 .3 .0") //
			);

			FileUtil.findPaths(Paths.get("src/main/ll/fc")) //
					.filter(path -> Wildcard.isMatch("*.sl", path.getFileName().toString())) //
					.forEach(path -> {
						var recursiveFactorizer = new RecursiveFactorizer(TermOp.values());

						var bs = fts //
								.fold(FileUtil.read(path), (s_, ft) -> recursiveFactorizer.rewrite(ft.k, ft.v, s_)) //
								.getBytes(Defaults.charset);

						ex(() -> Files.write(path, bs));
					});

			return true;
		});
	}

}
