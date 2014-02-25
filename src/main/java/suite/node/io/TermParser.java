package suite.node.io;

import java.util.Objects;

public class TermParser extends Parser {

	public static enum TermOp implements Operator {
		NEXT__("#", Assoc.RIGHT), //
		IS____(" :- ", Assoc.RIGHT), //
		CONTD_(" >> ", Assoc.RIGHT), //
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
		GE____(" >= ", Assoc.RIGHT), //
		GT____(" > ", Assoc.RIGHT), //
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

		public final String name;
		public final Assoc assoc;
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
				if (Objects.equals(operator.name, name))
					return operator;
			return null;
		}

		public String getName() {
			return name;
		}

		public Assoc getAssoc() {
			return assoc;
		}

		public int getPrecedence() {
			return precedence;
		}
	}

	public TermParser() {
		super(TermOp.values());
	}

}
