package org.btree;


public interface Persister<P> {

	public int allocate();

	public void deallocate(int pageNo);

	public P load(int pageNo);

	public void save(int pageNo, P page);

}
