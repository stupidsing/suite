package org.suite.doer;

import org.parser.Operator;
import org.parser.Parser;
import org.suite.Context;

public class TermParser extends Parser {

	public static enum TermOp implements Operator {
		NEXT__("#", Assoc.RIGHT), //
		IS____(" :- ", Assoc.RIGHT), //
		INDUCE(" => ", Assoc.RIGHT), //
		CHOICE(" | ", Assoc.RIGHT), //
		IF____(" ? ", Assoc.RIGHT), //
		LET___(" >> ", Assoc.RIGHT), //
		OR____(";", Assoc.RIGHT), //
		AND___(",", Assoc.RIGHT), //
		LE____(" <= ", Assoc.RIGHT), //
		LT____(" < ", Assoc.RIGHT), //
		GE____(" >= ", Assoc.RIGHT), //
		GT____(" > ", Assoc.RIGHT), //
		EQUAL_(" = ", Assoc.RIGHT), //
		PLUS__(" + ", Assoc.RIGHT), //
		MINUS_(" - ", Assoc.LEFT), //
		MULT__(" * ", Assoc.RIGHT), //
		DIVIDE(" / ", Assoc.LEFT), //
		BRACES("{", Assoc.LEFT), //
		SEP___(" ", Assoc.RIGHT), //
		;

		public final String name;
		public final Assoc assoc;
		public int precedence;

		private TermOp(String name, Assoc associativity) {
			this.name = name;
			this.assoc = associativity;
		}

		static {
			int precedence = 0;
			for (TermOp operator : TermOp.values())
				operator.precedence = ++precedence;
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

	public TermParser(Context context) {
		super(context, TermOp.values());
	}

}
