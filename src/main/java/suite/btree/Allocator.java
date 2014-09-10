package suite.btree;

import java.io.Closeable;

public interface Allocator extends Closeable {

	public void create();

	public int allocate();

	public void deallocate(int pageNo);

}