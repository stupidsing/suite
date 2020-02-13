package suite.text;

import java.util.List;
import java.util.Map;

import primal.Verbs.Equals;
import primal.fp.Funs.Source;
import suite.node.util.Singleton;
import suite.streamlet.As;
import suite.util.Memoize;

public class Chinese {

	private Source<Map<String, List<String>>> cjTable = Memoize.source(() -> Singleton.me.storeCache //
			.http("https://pointless.online/storey/cangjie5.txt") //
			.collect(As::lines) //
			.dropWhile(line -> !Equals.string(line, "BEGIN_TABLE")) //
			.drop(1) //
			.takeWhile(line -> !Equals.string(line, "END_TABLE")) //
			.map(line -> line.split("\t")) //
			.map2(array -> array[0], array -> array[1]) //
			.toListMap());

	public String cj(String sequence) {
		return cjTable.g().get(sequence).iterator().next();
	}

}
