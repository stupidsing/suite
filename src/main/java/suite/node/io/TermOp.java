package suite.node.io;

import static primal.statics.Rethrow.ex;

import java.util.List;

import primal.Verbs.Equals;
import primal.parser.Operator;

public class TermOp implements Operator {

	public static TermOp NEXT__ = new TermOp(100, "#", Assoc.RIGHT);
	public static TermOp IS____ = new TermOp(110, " :- ", Assoc.RIGHT);
	public static TermOp CONTD_ = new TermOp(120, " ~ ", Assoc.RIGHT);
	public static TermOp DEFINE = new TermOp(130, " := ", Assoc.RIGHT);
	public static TermOp BIGOR_ = new TermOp(140, " || ", Assoc.RIGHT);
	public static TermOp BIGAND = new TermOp(150, " && ", Assoc.RIGHT);
	public static TermOp FUN___ = new TermOp(160, " => ", Assoc.RIGHT);
	public static TermOp ARROW_ = new TermOp(170, " -> ", Assoc.RIGHT);
	public static TermOp SEP___ = new TermOp(180, " | ", Assoc.LEFT);
	public static TermOp JOIN__ = new TermOp(190, " . ", Assoc.LEFT);
	public static TermOp OR____ = new TermOp(200, ";", Assoc.RIGHT);
	public static TermOp AND___ = new TermOp(210, ",", Assoc.RIGHT);
	public static TermOp EQUAL_ = new TermOp(220, " = ", Assoc.RIGHT);
	public static TermOp NOTEQ_ = new TermOp(230, " != ", Assoc.RIGHT);
	public static TermOp LE____ = new TermOp(240, " <= ", Assoc.RIGHT);
	public static TermOp LT____ = new TermOp(250, " < ", Assoc.RIGHT);
	public static TermOp PLUS__ = new TermOp(260, " + ", Assoc.RIGHT);
	public static TermOp MINUS_ = new TermOp(270, " - ", Assoc.LEFT);
	public static TermOp MULT__ = new TermOp(280, " * ", Assoc.RIGHT);
	public static TermOp DIVIDE = new TermOp(290, " / ", Assoc.LEFT);
	public static TermOp MODULO = new TermOp(300, " % ", Assoc.LEFT);
	public static TermOp POWER_ = new TermOp(310, "^", Assoc.RIGHT);
	public static TermOp BRACES = new TermOp(320, "_{", Assoc.LEFT);
	public static TermOp TUPLE_ = new TermOp(330, " ", Assoc.RIGHT);
	public static TermOp ITEM__ = new TermOp(340, "/", Assoc.LEFT);
	public static TermOp COLON_ = new TermOp(350, ":", Assoc.RIGHT);
	public static TermOp DEREF_ = new TermOp(360, "*", Assoc.LEFT);

	public static TermOp[] values = new TermOp[] { //
			NEXT__, //
			IS____, //
			CONTD_, //
			DEFINE, //
			BIGOR_, //
			BIGAND, //
			FUN___, //
			ARROW_, //
			SEP___, //
			JOIN__, //
			OR____, //
			AND___, //
			EQUAL_, //
			NOTEQ_, //
			LE____, //
			LT____, //
			PLUS__, //
			MINUS_, //
			MULT__, //
			DIVIDE, //
			MODULO, //
			POWER_, //
			BRACES, //
			TUPLE_, //
			ITEM__, //
			COLON_, //
			DEREF_, //
	};

	public int precedence;
	public String name;
	public Assoc assoc;

	private TermOp(int precedence, String name, Assoc assoc) {
		this.precedence = precedence;
		this.name = name;
		this.assoc = assoc;
	}

	public static TermOp find(String name) {
		for (var operator : values)
			if (Equals.string(operator.name, name))
				return operator;
		return null;
	}

	public static TermOp valueOf(String field) {
		return (TermOp) ex(() -> TermOp.class.getField(field).get(null));
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

	@Override
	public String toString() {
		return name;
	}

}
