package suite.ebnf;

import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.String_;

public class Grammar {

	public enum GrammarType {
		AND___, //
		ENTITY, //
		EXCEPT, //
		NAMED_, //
		OPTION, //
		OR____, //
		REPT0_, //
		REPT0H, //
		REPT1_, //
		STRING, //
	}

	public final GrammarType type;
	public final String content;
	public final List<Grammar> children;

	public static Map<String, Grammar> parse(Reader reader) {
		Breakdown breakdown = new Breakdown();

		List<Pair<String, String>> pairs = Read //
				.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(o -> o.fold("", String::concat)) //
				.map(line -> String_.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.toList();

		return Read.from(pairs) //
				.map2(lr -> lr.t0, lr -> breakdown.breakdown(lr.t0, lr.t1)) //
				.collect(As::map);
	}

	public Grammar(GrammarType type) {
		this(type, null, Collections.emptyList());
	}

	public Grammar(GrammarType type, String content) {
		this(type, content, Collections.emptyList());
	}

	public Grammar(GrammarType type, Grammar child) {
		this(type, null, child);
	}

	public Grammar(GrammarType type, String content, Grammar child) {
		this(type, content, List.of(child));
	}

	public Grammar(GrammarType type, List<Grammar> children) {
		this(type, null, children);
	}

	public Grammar(GrammarType type, String content, List<Grammar> children) {
		this.type = type;
		this.content = content;
		this.children = children;
	}

	public String describe() {
		return type + (content != null ? "." + content : "") //
				+ (type != GrammarType.NAMED_ //
						? Read.from(children).map(Grammar::describe).collect(As.joinedBy("(", ",", ")")) //
						: "");
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		toString("", sb);
		return sb.toString();
	}

	private void toString(String indent, StringBuilder sb) {
		sb.append(indent + type + " (" + content + ")\n");
		for (Grammar child : children)
			child.toString(indent + "| ", sb);
	}

}
