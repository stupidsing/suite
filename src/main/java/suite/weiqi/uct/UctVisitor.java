package suite.weiqi.uct;

import java.util.List;

public interface UctVisitor<Move> {

	public UctVisitor<Move> cloneVisitor();

	public Iterable<Move> getAllMovesOnBoard();

	public List<Move> elaborateMoves();

	public void playMove(Move move);

	public boolean evaluateRandomOutcome();

}
