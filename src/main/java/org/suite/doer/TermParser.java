package org.suite.doer;

import org.parser.Operator;
import org.parser.Parser;
import org.suite.Context;

public class TermParser extends Parser {

	public static enum TermOp implements Operator {
		NEXT__("#", Assoc.LEFT), //
		IS____(" :- ", Assoc.LEFT), //
		INDUCE(" => ", Assoc.RIGHT), //
		OR____(";", Assoc.LEFT), //
		AND___(",", Assoc.LEFT), //
		LE____(" <= ", Assoc.LEFT), //
		LT____(" < ", Assoc.LEFT), //
		GE____(" >= ", Assoc.LEFT), //
		GT____(" > ", Assoc.LEFT), //
		EQUAL_(" = ", Assoc.LEFT), //
		PLUS__(" + ", Assoc.LEFT), //
		MINUS_(" - ", Assoc.RIGHT), //
		MULT__(" * ", Assoc.LEFT), //
		DIVIDE(" / ", Assoc.RIGHT), //
		SEP___(" ", Assoc.LEFT);

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
