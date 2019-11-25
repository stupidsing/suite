package suite.math.linalg;

import static suite.util.Streamlet_.forInt;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import primal.primitive.IntPrim.IntSink;
import primal.primitive.adt.pair.IntIntPair;
import suite.adt.PriorityQueue;

public class SparseMatrix {

	private int height;
	private int width_;
	private List<Spans> matrix;

	private static class Spans {
		private int size;
		private int[] columns = new int[4];
		private float[] values = new float[4];

		private Spans(Span... spans) {
			for (var span : spans)
				add(span.column, span.value);
		}

		private void add(int column, float value) {
			while (size + 1 < columns.length) {
				columns = Arrays.copyOf(columns, columns.length * 2);
				values = Arrays.copyOf(values, values.length * 2);
			}
			columns[size] = column;
			values[size] = value;
			size++;
		}
	}

	private static class Span {
		public final int column;
		public final float value;

		private Span(int column, float value) {
			this.column = column;
			this.value = value;
		}
	}

	public static SparseMatrix identity(int size) {
		return new SparseMatrix(size, size, forInt(size).map(i -> new Spans(new Span(i, 1f))).toList());
	}

	private SparseMatrix(int height, int width_, List<Spans> matrix) {
		this.height = height;
		this.width_ = width_;
		this.matrix = matrix;
	}

	public SparseMatrix transpose() {
		var pq = new PriorityQueue<>(IntIntPair.class, height, Comparator.comparingInt(IntIntPair::snd));
		var matrix1 = forInt(width_).map(i -> new Spans()).toList();
		var js = new int[height];

		IntSink enqRow = r -> {
			var j = js[r];
			if (j < matrix.get(r).size)
				pq.add(IntIntPair.of(r, j));
		};

		for (var r = 0; r < height; r++)
			enqRow.f(r);

		while (!pq.isEmpty()) {
			var pair = pq.extractMin();
			var r = pair.t0;
			var j = js[r]++;
			var spans = matrix.get(r);
			matrix1.get(spans.columns[j]).add(r, spans.values[j]);
			enqRow.f(r);
		}

		return new SparseMatrix(width_, height, matrix1);
	}

}
