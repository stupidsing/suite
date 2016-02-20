package suite.file;

import java.io.Closeable;

public interface ExtentAllocator extends Closeable {

	public class Extent {
		public final int start;
		public final int end;

		public Extent(int start, int end) {
			this.start = start;
			this.end = end;
		}
	}

	public void create();

	public Extent allocate(int count);

	public void deallocate(Extent extent);

}
