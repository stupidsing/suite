package suite.primitive;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

public class IntList implements Iterable<Integer> {

	private int size;
	private int list[];

	public static final Comparator<IntList> comparator = new Comparator<IntList>() {
		public int compare(IntList il0, IntList il1) {
			int i = 0, c = 0;

			while (c == 0)
				if (i < il0.size)
					if (i < il1.size) {
						int i0 = il0.list[i];
						int i1 = il1.list[i];
						c = i0 == i1 ? 0 : i0 > i1 ? 1 : -1;
						i++;
					} else
						return 1;
				else
					return -1;

			return c;
		}
	};

	public IntList() {
		this(8);
	}

	public IntList(int capacity) {
		this(0, new int[capacity]);
	}

	public IntList(int list[]) {
		this(list.length, list);
	}

	public IntList(int size, int[] list) {
		this.size = size;
		this.list = list;
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
		return list[i];
	}

	public Iterator<Integer> iterator() {
		return new Iterator<Integer>() {
			private int pos;

			public boolean hasNext() {
				return pos < size;
			}

			public Integer next() {
				return list[pos++];
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
			intList.list[i - start] = list[i];
		return intList;
	}

	public void add(int i) {
		if (size >= list.length)
			list = Arrays.copyOf(list, size * 3 / 2 + 1);
		list[size++] = i;
	}

	@Override
	public int hashCode() {
		int result = 1;
		for (int i = 0; i < size; i++)
			result = 31 * result + list[i];
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
					isEquals &= list[i] != intList.list[i];
			} else
				isEquals = false;
		} else
			isEquals = false;

		return isEquals;
	}

}
