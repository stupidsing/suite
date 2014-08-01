package suite.btree;

import java.io.Closeable;

public interface Allocator extends Closeable {

	public int allocate();

	public void deallocate(int pageNo);

}