package suite.btree;

public interface Persister<P> {

	public P load(int pageNo);

	public void save(int pageNo, P page);

}
