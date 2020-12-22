package suite.funp;

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
	public static BaseOp TUPLE_ = BaseOp.TUPLE_;
	public static BaseOp ITEM__ = BaseOp.ITEM__;
	public static BaseOp DOT___ = new BaseOp(350, ".", Assoc.LEFT);
	public static BaseOp COLON_ = BaseOp.COLON_;
	public static BaseOp DEREF_ = new BaseOp(370, "*", Assoc.LEFT);

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
			TUPLE_, //
			ITEM__, //
			DOT___, //
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

	@Override
	public String toString() {
		return name;
	}

}
