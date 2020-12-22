package suite.node.io;

import primal.parser.Operator;

public class BaseOp implements Operator {

	public static BaseOp NEXT__ = new BaseOp(100, "#", Assoc.RIGHT);
	public static BaseOp CONTD_ = new BaseOp(120, " ~ ", Assoc.RIGHT);
	public static BaseOp DEFINE = new BaseOp(130, " := ", Assoc.RIGHT);
	public static BaseOp BIGOR_ = new BaseOp(140, " || ", Assoc.RIGHT);
	public static BaseOp BIGAND = new BaseOp(150, " && ", Assoc.RIGHT);
	public static BaseOp FUN___ = new BaseOp(160, " => ", Assoc.RIGHT);
	public static BaseOp SEP___ = new BaseOp(180, " | ", Assoc.LEFT);
	public static BaseOp JOIN__ = new BaseOp(190, " . ", Assoc.LEFT);
	public static BaseOp OR____ = new BaseOp(200, ";", Assoc.RIGHT);
	public static BaseOp AND___ = new BaseOp(210, ",", Assoc.RIGHT);
	public static BaseOp EQUAL_ = new BaseOp(220, " = ", Assoc.RIGHT);
	public static BaseOp NOTEQ_ = new BaseOp(230, " != ", Assoc.RIGHT);
	public static BaseOp LE____ = new BaseOp(240, " <= ", Assoc.RIGHT);
	public static BaseOp LT____ = new BaseOp(250, " < ", Assoc.RIGHT);
	public static BaseOp PLUS__ = new BaseOp(260, " + ", Assoc.RIGHT);
	public static BaseOp MINUS_ = new BaseOp(270, " - ", Assoc.LEFT);
	public static BaseOp MULT__ = new BaseOp(280, " * ", Assoc.RIGHT);
	public static BaseOp DIVIDE = new BaseOp(290, " / ", Assoc.LEFT);
	public static BaseOp MODULO = new BaseOp(300, " % ", Assoc.LEFT);
	public static BaseOp POWER_ = new BaseOp(310, "^", Assoc.RIGHT);
	public static BaseOp TUPLE_ = new BaseOp(330, " ", Assoc.RIGHT);
	public static BaseOp ITEM__ = new BaseOp(340, "/", Assoc.LEFT);
	public static BaseOp COLON_ = new BaseOp(360, ":", Assoc.RIGHT);

	public int precedence;
	public String name;
	public Assoc assoc;

	public BaseOp(int precedence, String name, Assoc assoc) {
		this.precedence = precedence;
		this.name = name;
		this.assoc = assoc;
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
