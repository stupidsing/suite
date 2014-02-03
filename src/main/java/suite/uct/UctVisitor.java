package suite.uct;

import java.util.List;

public interface UctVisitor<Move> {

	public Iterable<Move> getAllMoves();

	public List<Move> elaborateMoves();

	public void playMove(Move move);

	public boolean evaluateRandomOutcome();

	public UctVisitor<Move> cloneVisitor();

}
