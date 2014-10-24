package suite.instructionexecutor;

import java.util.ArrayList;
import java.util.List;

import suite.immutable.IPointer;

public class IndexedListReader<T> {

	private List<T> list = new ArrayList<>();

	public class Pointer implements IPointer<T> {
		private int position;

		private Pointer(int position) {
			this.position = position;
		}

		public T head() {
			return list.get(position);
		}

		public Pointer tail() {
			int position1 = position + 1;
			return position1 < list.size() ? new Pointer(position1) : null;
		}
	}

	public IndexedListReader(List<T> list) {
		this.list = list;
	}

	public IPointer<T> pointer() {
		return new Pointer(0);
	}

}
