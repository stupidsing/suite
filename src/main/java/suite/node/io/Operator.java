package suite.node.io;

public interface Operator {

	public enum Assoc { // associativity
		LEFT, RIGHT
	}

	public String getName();

	public Assoc getAssoc();

	public int getPrecedence();

}
