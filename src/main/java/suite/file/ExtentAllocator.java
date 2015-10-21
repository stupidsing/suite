package suite.file;

import java.io.Closeable;

public interface ExtentAllocator extends Closeable {

	public class Extent {
		public final int pointer;
		public final int count;

		public Extent(int pointer, int count) {
			this.pointer = pointer;
			this.count = count;
		}
	}

	public void create();

	public Extent allocate(int count);

	public void deallocate(Extent pointer);

}