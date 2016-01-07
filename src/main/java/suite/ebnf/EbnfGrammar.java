package suite.ebnf;

import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import suite.adt.Pair;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Util;

public class EbnfGrammar {

	public enum EbnfGrammarType {
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

	public final EbnfGrammarType type;
	public final String content;
	public final List<EbnfGrammar> children;

	public static Map<String, EbnfGrammar> parse(Reader reader) {
		EbnfBreakdown breakdown = new EbnfBreakdown();

		List<Pair<String, String>> pairs = Read.lines(reader) //
				.filter(line -> !line.isEmpty() && !line.startsWith("#")) //
				.map(line -> line.replace('\t', ' ')) //
				.split(line -> !line.startsWith(" ")) //
				.map(o -> o.fold("", String::concat)) //
				.map(line -> Util.split2(line, " ::= ")) //
				.filter(lr -> lr != null) //
				.toList();

		Map<String, EbnfGrammar> grammarByEntity = Read.from(pairs) //
				.map(lr -> Pair.of(lr.t0, breakdown.breakdown(lr.t0, lr.t1))) //
				.collect(As::map);

		return grammarByEntity;
	}

	public EbnfGrammar(EbnfGrammarType type) {
		this(type, null, Collections.emptyList());
	}

	public EbnfGrammar(EbnfGrammarType type, String content) {
		this(type, content, Collections.emptyList());
	}

	public EbnfGrammar(EbnfGrammarType type, EbnfGrammar child) {
		this(type, null, child);
	}

	public EbnfGrammar(EbnfGrammarType type, String content, EbnfGrammar child) {
		this(type, content, Arrays.asList(child));
	}

	public EbnfGrammar(EbnfGrammarType type, List<EbnfGrammar> children) {
		this(type, null, children);
	}

	public EbnfGrammar(EbnfGrammarType type, String content, List<EbnfGrammar> children) {
		this.type = type;
		this.content = content;
		this.children = children;
	}

	public String describe() {
		return type + (content != null ? "." + content : "") //
				+ (type != EbnfGrammarType.NAMED_
						? "(" + Read.from(children).map(EbnfGrammar::describe).collect(As.joined(",")) + ")" //
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
		for (EbnfGrammar child : children)
			child.toString(indent + "| ", sb);
	}

}
