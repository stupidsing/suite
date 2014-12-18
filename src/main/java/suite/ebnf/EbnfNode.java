package suite.ebnf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EbnfNode {

	public enum EbnfType {
		AND___, //
		ENTITY, //
		EXCEPT, //
		NAMED_, //
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
		this(type, content, new ArrayList<>());
	}

	public EbnfNode(EbnfType type, EbnfNode child) {
		this(type, null, child);
	}

	public EbnfNode(EbnfType type, String content, EbnfNode child) {
		this(type, content, Arrays.asList(child));
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
