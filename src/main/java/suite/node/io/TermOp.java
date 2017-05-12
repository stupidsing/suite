package suite.node.io;

import java.util.Arrays;

import suite.util.String_;

public enum TermOp implements Operator {

	NEXT__("#", Assoc.RIGHT), //
	IS____(" :- ", Assoc.RIGHT), //
	CONTD_(" >> ", Assoc.RIGHT), //
	DEFINE(" := ", Assoc.RIGHT), //
	BIGOR_(" || ", Assoc.RIGHT), //
	BIGAND(" && ", Assoc.RIGHT), //
	FUN___(" => ", Assoc.RIGHT), //
	ARROW_(" -> ", Assoc.RIGHT), //
	SEP___(" | ", Assoc.LEFT), //
	JOIN__(" . ", Assoc.LEFT), //
	OR____(";", Assoc.RIGHT), //
	AND___(",", Assoc.RIGHT), //
	EQUAL_(" = ", Assoc.RIGHT), //
	NOTEQ_(" != ", Assoc.RIGHT), //
	LE____(" <= ", Assoc.RIGHT), //
	LT____(" < ", Assoc.RIGHT), //
	PLUS__(" + ", Assoc.RIGHT), //
	MINUS_(" - ", Assoc.LEFT), //
	MULT__(" * ", Assoc.RIGHT), //
	DIVIDE(" / ", Assoc.LEFT), //
	MODULO(" % ", Assoc.LEFT), //
	POWER_("^", Assoc.RIGHT), //
	BRACES("{", Assoc.LEFT), //
	TUPLE_(" ", Assoc.RIGHT), //
	ITEM__("/", Assoc.LEFT), //
	COLON_(":", Assoc.RIGHT), //
	;

	public String name;
	public Assoc assoc;
	public int precedence;

	static {
		int precedence = 0;
		for (TermOp operator : values())
			operator.precedence = ++precedence;
	}

	private TermOp(String name, Assoc associativity) {
		this.name = name;
		assoc = associativity;
	}

	public static TermOp find(String name) {
		for (TermOp operator : values())
			if (String_.equals(operator.name, name))
				return operator;
		return null;
	}

	public static boolean isSpaceBefore(Operator operator) {
		return Arrays.asList(TermOp.NEXT__).contains(operator);
	}

	public static boolean isSpaceAfter(Operator operator) {
		return Arrays.asList(TermOp.NEXT__, TermOp.AND___, TermOp.OR____).contains(operator);
	}

	public static int getLeftPrec(Operator operator) {
		return operator.getPrecedence() - (operator.getAssoc() == Assoc.LEFT ? 1 : 0);
	}

	public static int getRightPrec(Operator operator) {
		return operator.getPrecedence() - (operator.getAssoc() == Assoc.RIGHT ? 1 : 0);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Assoc getAssoc() {
		return assoc;
	}

	@Override
	public int getPrecedence() {
		return precedence;
	}

}
