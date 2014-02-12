package suite.primitive;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class IntList implements Iterable<Integer> {

	private int ints[];
	private int size;

	public static final Comparator<IntList> comparator = new Comparator<IntList>() {
		public int compare(IntList il0, IntList il1) {
			int size0 = il0.size(), size1 = il1.size(), minSize = Math.min(size0, size1);
			int index = 0, c = 0;

			while (c == 0 && index < minSize) {
				int i0 = il0.ints[index];
				int i1 = il1.ints[index];
				c = i0 == i1 ? 0 : i0 > i1 ? 1 : -1;
				index++;
			}

			return c != 0 ? c : size0 - size1;
		}
	};

	public IntList() {
		this(8);
	}

	public IntList(int capacity) {
		this(0, new int[capacity]);
	}

	public IntList(int ints[]) {
		this(ints.length, ints);
	}

	public IntList(int size, int list[]) {
		this.size = size;
		this.ints = list;
	}

	public static IntList asList(int... in) {
		return new IntList(in);
	}

	public int size() {
		return size;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public int get(int i) {
		return ints[i];
	}

	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			private int pos;

			public boolean hasNext() {
				return pos < size;
			}

			public Integer next() {
				return ints[pos++];
			}

			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public IntList subList(int start, int end) {
		if (start < 0)
			start += size;
		if (end < start)
			end += size;
		end = Math.min(size, end);

		IntList intList = new IntList(end - start);
		for (int i = start; i < end; i++)
			intList.ints[i - start] = ints[i];
		return intList;
	}

	public void add(int i) {
		if (size >= ints.length)
			ints = Arrays.copyOf(ints, size * 3 / 2 + 1);
		ints[size++] = i;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = 0; i < size; i++)
			result = 31 * result + ints[i];
		return result;
	}

	@Override
	public boolean equals(Object object) {
		boolean isEquals;

		if (object instanceof IntList) {
			IntList intList = (IntList) object;

			if (size == intList.size) {
				isEquals = true;

				for (int i = 0; i < size; i++)
					isEquals &= ints[i] != intList.ints[i];
			} else
				isEquals = false;
		} else
			isEquals = false;

		return isEquals;
	}

}
