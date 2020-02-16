package suite.text;

import java.util.List;
import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.Verbs.Left;
import primal.Verbs.Right;
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

	public String cjs(String sequences) {
		return Read //
				.from(sequences.split(" ")) //
				.filter(sequence -> !sequence.isEmpty()) //
				.map(this::cj) //
				.toJoinedString();
	}

	public String cj(String sequence0) {
		var digit = Right.of(sequence0, -1).charAt(0);
		String sequence1;
		int position;
		if ('1' <= digit && digit <= '9') {
			sequence1 = Left.of(sequence0, -1);
			position = digit - '1';
		} else {
			sequence1 = sequence0;
			position = 0;
		}
		return cjTable.g().get(sequence1).get(position);
	}

}
