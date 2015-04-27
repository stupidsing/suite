package suite.ebnf;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EbnfGrammar {

	public enum EbnfGrammarType {
		AND___, //
		ENTITY, //
		EXCEPT, //
		NAMED_, //
		NIL___, //
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

}
