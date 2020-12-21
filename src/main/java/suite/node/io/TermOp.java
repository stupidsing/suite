package suite.node.io;

import static primal.statics.Rethrow.ex;

import java.util.List;

import primal.Verbs.Equals;
import primal.parser.Operator;

public class TermOp implements Operator {

	public static TermOp NEXT__ = new TermOp("#", Assoc.RIGHT);
	public static TermOp IS____ = new TermOp(" :- ", Assoc.RIGHT);
	public static TermOp CONTD_ = new TermOp(" ~ ", Assoc.RIGHT);
	public static TermOp DEFINE = new TermOp(" := ", Assoc.RIGHT);
	public static TermOp BIGOR_ = new TermOp(" || ", Assoc.RIGHT);
	public static TermOp BIGAND = new TermOp(" && ", Assoc.RIGHT);
	public static TermOp FUN___ = new TermOp(" => ", Assoc.RIGHT);
	public static TermOp ARROW_ = new TermOp(" -> ", Assoc.RIGHT);
	public static TermOp SEP___ = new TermOp(" | ", Assoc.LEFT);
	public static TermOp JOIN__ = new TermOp(" . ", Assoc.LEFT);
	public static TermOp OR____ = new TermOp(";", Assoc.RIGHT);
	public static TermOp AND___ = new TermOp(",", Assoc.RIGHT);
	public static TermOp EQUAL_ = new TermOp(" = ", Assoc.RIGHT);
	public static TermOp NOTEQ_ = new TermOp(" != ", Assoc.RIGHT);
	public static TermOp LE____ = new TermOp(" <= ", Assoc.RIGHT);
	public static TermOp LT____ = new TermOp(" < ", Assoc.RIGHT);
	public static TermOp PLUS__ = new TermOp(" + ", Assoc.RIGHT);
	public static TermOp MINUS_ = new TermOp(" - ", Assoc.LEFT);
	public static TermOp MULT__ = new TermOp(" * ", Assoc.RIGHT);
	public static TermOp DIVIDE = new TermOp(" / ", Assoc.LEFT);
	public static TermOp MODULO = new TermOp(" % ", Assoc.LEFT);
	public static TermOp POWER_ = new TermOp("^", Assoc.RIGHT);
	public static TermOp BRACES = new TermOp("_{", Assoc.LEFT);
	public static TermOp TUPLE_ = new TermOp(" ", Assoc.RIGHT);
	public static TermOp ITEM__ = new TermOp("/", Assoc.LEFT);
	public static TermOp COLON_ = new TermOp(":", Assoc.RIGHT);
	public static TermOp DEREF_ = new TermOp("*", Assoc.LEFT);

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

	public String name;
	public Assoc assoc;
	public int precedence;

	static {
		var precedence = 0;
		for (var operator : values)
			operator.precedence = ++precedence;
	}

	private TermOp(String name, Assoc assoc) {
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

}
