package suite.node.parser;

import primal.MoreVerbs.Read;
import primal.Nouns.Utf8;
import primal.Verbs.ReadString;
import primal.adt.Pair;
import primal.parser.Wildcard;
import suite.node.io.TermOp;
import suite.os.FileUtil;
import suite.util.RunUtil;

import java.nio.file.Files;
import java.nio.file.Paths;

import static primal.statics.Rethrow.ex;

// mAIN=suite.node.parser.RecursiveFileFactorizerMain ./run.sh
public class RecursiveFileFactorizerMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var fts = Read.each(
					Pair.of("fc-define-var-types .0 .1 .2 .3", "fc-define-var-types .1 .2 .3 .0")
			);

			FileUtil.findPaths(Paths.get("src/main/ll/fc"))
					.filter(path -> Wildcard.isMatch("*.sl", path.getFileName().toString()))
					.forEach(path -> {
						var recursiveFactorizer = new RecursiveFactorizer(TermOp.values());

						var bs = fts
								.fold(ReadString.from(path), (s_, ft) -> recursiveFactorizer.rewrite(ft.k, ft.v, s_))
								.getBytes(Utf8.charset);

						ex(() -> Files.write(path, bs));
					});

			return true;
		});
	}

}
