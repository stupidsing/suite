package suite.file;

import java.io.Closeable;

public interface PageAllocator extends Closeable {

	public void create();

	public int allocate();

	public void deallocate(int pointer);

}
