package suite.file;

import java.io.Closeable;
import java.util.Objects;

import suite.util.Util;

public interface ExtentAllocator extends Closeable {

	public class Extent {
		public final int start;
		public final int end;

		public Extent(int start, int end) {
			this.start = start;
			this.end = end;
		}

		public boolean equals(Object object) {
			if (Util.clazz(object) == Extent.class) {
				Extent other = (Extent) object;
				return Objects.equals(start, other.start) && Objects.equals(end, other.end);
			} else
				return false;
		}

		public int hashCode() {
			return Objects.hashCode(start) ^ Objects.hashCode(end);
		}
	}

	public void create();

	public Extent allocate(int count);

	public void deallocate(Extent extent);

}
