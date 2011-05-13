package org.weiqi.uct;

import java.util.List;

public interface UctVisitor<Move> {

	public UctVisitor<Move> cloneVisitor();

	public List<Move> elaborateMoves();

	public void playMove(Move move);

	public boolean evaluateRandomOutcome();

}
