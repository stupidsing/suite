package suite.file;

import java.io.Closeable;

public interface ExtentAllocator extends Closeable {

	public class Extent {
		public final int start;
		public final int count;

		public Extent(int start, int count) {
			this.start = start;
			this.count = count;
		}
	}

	public void create();

	public Extent allocate(int count);

	public void deallocate(Extent extent);

}
