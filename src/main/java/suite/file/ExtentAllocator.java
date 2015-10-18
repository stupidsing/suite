package suite.file;

import java.io.Closeable;

public interface ExtentAllocator extends Closeable {

	public class ExtentPointer {
		public final int pageNo;
		public final int count;

		public ExtentPointer(int pageNo, int count) {
			this.pageNo = pageNo;
			this.count = count;
		}
	}

	public void create();

	public ExtentPointer allocate(int count);

	public void deallocate(ExtentPointer pointer);

}