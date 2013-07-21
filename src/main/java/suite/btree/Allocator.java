package suite.btree;

public interface Allocator {

	public int allocate();

	public void deallocate(int pageNo);

}
