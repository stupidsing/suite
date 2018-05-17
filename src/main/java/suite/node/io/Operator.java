package suite.node.io;

public interface Operator {

	public enum Assoc { // associativity
		LEFT, RIGHT
	}

	public String name_();

	public Assoc assoc();

	public int precedence();

}
