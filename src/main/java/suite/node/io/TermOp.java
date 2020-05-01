package suite.node.io;

import primal.Verbs.Equals;
import primal.parser.Operator;

import java.util.List;

public enum TermOp implements Operator {

	NEXT__("#", Assoc.RIGHT),
	IS____(" :- ", Assoc.RIGHT),
	CONTD_(" ~ ", Assoc.RIGHT),
	DEFINE(" := ", Assoc.RIGHT),
	BIGOR_(" || ", Assoc.RIGHT),
	BIGAND(" && ", Assoc.RIGHT),
	FUN___(" => ", Assoc.RIGHT),
	ARROW_(" -> ", Assoc.RIGHT),
	SEP___(" | ", Assoc.LEFT),
	JOIN__(" . ", Assoc.LEFT),
	OR____(";", Assoc.RIGHT),
	AND___(",", Assoc.RIGHT),
	EQUAL_(" = ", Assoc.RIGHT),
	NOTEQ_(" != ", Assoc.RIGHT),
	LE____(" <= ", Assoc.RIGHT),
	LT____(" < ", Assoc.RIGHT),
	PLUS__(" + ", Assoc.RIGHT),
	MINUS_(" - ", Assoc.LEFT),
	MULT__(" * ", Assoc.RIGHT),
	DIVIDE(" / ", Assoc.LEFT),
	MODULO(" % ", Assoc.LEFT),
	POWER_("^", Assoc.RIGHT),
	BRACES("_{", Assoc.LEFT),
	TUPLE_(" ", Assoc.RIGHT),
	ITEM__("/", Assoc.LEFT),
	COLON_(":", Assoc.RIGHT),
	DEREF_("*", Assoc.LEFT),
	;

	public String name;
	public Assoc assoc;
	public int precedence;

	static {
		var precedence = 0;
		for (var operator : values())
			operator.precedence = ++precedence;
	}

	private TermOp(String name, Assoc assoc) {
		this.name = name;
		this.assoc = assoc;
	}

	public static TermOp find(String name) {
		for (var operator : values())
			if (Equals.string(operator.name, name))
				return operator;
		return null;
	}

	public static boolean isSpaceBefore(Operator operator) {
		return List.of(TermOp.NEXT__).contains(operator);
	}

	public static boolean isSpaceAfter(Operator operator) {
		return List.of(TermOp.NEXT__, TermOp.AND___, TermOp.OR____).contains(operator);
	}

	public static int getLeftPrec(Operator operator) {
		return operator.precedence() - (operator.assoc() == Assoc.LEFT ? 1 : 0);
	}

	public static int getRightPrec(Operator operator) {
		return operator.precedence() - (operator.assoc() == Assoc.RIGHT ? 1 : 0);
	}

	@Override
	public String name_() {
		return name;
	}

	@Override
	public Assoc assoc() {
		return assoc;
	}

	@Override
	public int precedence() {
		return precedence;
	}

}
