package suite.ebnf;

import suite.ebnf.Ebnf.Node;

public interface EbnfParse {

	public Node check(EbnfGrammar eg, String in);

	public Node parse(EbnfGrammar eg, String in);

}
