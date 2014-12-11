package suite.ebnf;

import java.util.Arrays;
import java.util.List;

public class EbnfNode {

	public enum EbnfType {
		AND___, //
		ENTITY, //
		EXCEPT, //
		OPTION, //
		OR____, //
		REPT0_, //
		REPT1_, //
		STRING, //
	}

	public final EbnfType type;
	public final String content;
	public final List<EbnfNode> children;

	public EbnfNode(EbnfType type, String content) {
		this(type, content, null);
	}

	public EbnfNode(EbnfType type, EbnfNode child) {
		this(type, null, Arrays.asList(child));
	}

	public EbnfNode(EbnfType type, List<EbnfNode> children) {
		this(type, null, children);
	}

	private EbnfNode(EbnfType type, String content, List<EbnfNode> children) {
		this.type = type;
		this.content = content;
		this.children = children;
	}

}
