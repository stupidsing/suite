package suite.funp;

import static primal.statics.Rethrow.ex;

import java.util.List;

import primal.Verbs.Equals;
import primal.parser.Operator;
import primal.parser.Operator.Assoc;
import suite.node.io.BaseOp;

public class FunpOp {

	public static BaseOp NEXT__ = BaseOp.NEXT__;
	public static BaseOp CONTD_ = BaseOp.CONTD_;
	public static BaseOp DEFINE = BaseOp.DEFINE;
	public static BaseOp BIGOR_ = BaseOp.BIGOR_;
	public static BaseOp BIGAND = BaseOp.BIGAND;
	public static BaseOp FUN___ = BaseOp.FUN___;
	public static BaseOp SEP___ = BaseOp.SEP___;
	public static BaseOp JOIN__ = BaseOp.JOIN__;
	public static BaseOp OR____ = BaseOp.OR____;
	public static BaseOp AND___ = BaseOp.AND___;
	public static BaseOp EQUAL_ = BaseOp.EQUAL_;
	public static BaseOp NOTEQ_ = BaseOp.NOTEQ_;
	public static BaseOp LE____ = BaseOp.LE____;
	public static BaseOp LT____ = BaseOp.LT____;
	public static BaseOp PLUS__ = BaseOp.PLUS__;
	public static BaseOp MINUS_ = BaseOp.MINUS_;
	public static BaseOp MULT__ = BaseOp.MULT__;
	public static BaseOp DIVIDE = BaseOp.DIVIDE;
	public static BaseOp MODULO = BaseOp.MODULO;
	public static BaseOp POWER_ = BaseOp.POWER_;
	public static BaseOp BRACES = BaseOp.BRACES;
	public static BaseOp TUPLE_ = BaseOp.TUPLE_;
	public static BaseOp ITEM__ = new BaseOp(340, "/", Assoc.LEFT);
	public static BaseOp COLON_ = BaseOp.COLON_;
	public static BaseOp DEREF_ = BaseOp.DEREF_;

	public static Operator[] values = new Operator[] { //
			NEXT__, //
			CONTD_, //
			DEFINE, //
			BIGOR_, //
			BIGAND, //
			FUN___, //
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

	private FunpOp(int precedence, String name, Assoc assoc) {
		this.precedence = precedence;
		this.name = name;
		this.assoc = assoc;
	}

	public static Operator find(String name) {
		for (var operator : values)
			if (Equals.string(operator.name_(), name))
				return operator;
		return null;
	}

	public static FunpOp valueOf(String field) {
		return (FunpOp) ex(() -> FunpOp.class.getField(field).get(null));
	}

	public static boolean isSpaceBefore(Operator operator) {
		return List.of(BaseOp.NEXT__).contains(operator);
	}

	public static boolean isSpaceAfter(Operator operator) {
		return List.of(BaseOp.NEXT__, BaseOp.AND___, BaseOp.OR____).contains(operator);
	}

	public static int getLeftPrec(Operator operator) {
		return operator.precedence() - (operator.assoc() == Assoc.LEFT ? 1 : 0);
	}

	public static int getRightPrec(Operator operator) {
		return operator.precedence() - (operator.assoc() == Assoc.RIGHT ? 1 : 0);
	}

	@Override
	public String toString() {
		return name;
	}

}
