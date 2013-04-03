package org.btree;

public interface Allocator {

	public int allocate();

	public void deallocate(int pageNo);

}
