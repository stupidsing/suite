package org.parser;

public interface Operator {

	public enum Assoc { // Associativity
		LEFT, RIGHT
	};

	public String getName();

	public Assoc getAssoc();

	public int getPrecedence();

}
