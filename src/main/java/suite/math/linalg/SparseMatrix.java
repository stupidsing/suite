package suite.math.linalg;

import java.util.ArrayList;
import java.util.List;

import suite.adt.PriorityQueue;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.Ints_;
import suite.primitive.adt.pair.IntIntPair;

public class SparseMatrix {

	private int height;
	private int width_;
	private List<List<Span>> matrix;

	private static class Span {
		public final int column;
		public final float value;

		private Span(int column, float value) {
			this.column = column;
			this.value = value;
		}
	}

	public static SparseMatrix identity(int size) {
		return new SparseMatrix(size, size, Ints_.range(size).map(i -> List.of(new Span(i, 1f))).toList());
	}

	private SparseMatrix(int height, int width_, List<List<Span>> matrix) {
		this.height = height;
		this.width_ = width_;
		this.matrix = matrix;
	}

	public SparseMatrix transpose() {
		PriorityQueue<IntIntPair> pq = new PriorityQueue<>(IntIntPair.class, height, (p0, p1) -> Integer.compare(p0.t1, p1.t1));
		List<List<Span>> matrix1 = Ints_.range(width_).map(i -> (List<Span>) new ArrayList<Span>()).toList();
		int[] js = new int[height];

		IntSink enqRow = r -> {
			int j = js[r];
			if (j < matrix.get(r).size())
				pq.insert(IntIntPair.of(r, j));
		};

		for (int r = 0; r < height; r++)
			enqRow.sink(r);

		while (!pq.isEmpty()) {
			IntIntPair pair = pq.extractMin();
			int r = pair.t0;
			Span span = matrix.get(r).get(js[r]++);
			matrix1.get(span.column).add(new Span(r, span.value));
			enqRow.sink(r);
		}

		return new SparseMatrix(width_, height, matrix1);
	}

}
