package suite.ebnf;

import suite.ebnf.Ebnf.Node;

public interface EbnfParse {

	public Node check(String entity, String in);

	public Node parse(String entity, String in);

}
