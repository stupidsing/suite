package org.weiqi.uct;

public interface UctVisitor<Move> {

	public UctVisitor<Move> cloneVisitor();

	public Iterable<Move> elaborateMoves();

	public void playMove(Move move);

	public boolean evaluateRandomOutcome();

}
