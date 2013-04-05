package org.btree;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class InMemoryAllocPersister<Page> implements Allocator, Persister<Page> {

	private Map<Integer, Page> pages = new HashMap<>();
	private AtomicInteger counter = new AtomicInteger();

	@Override
	public int allocate() {
		return counter.getAndIncrement();
	}

	@Override
	public void deallocate(int pageNo) {
		pages.remove(pageNo);
	}

	@Override
	public Page load(int pageNo) {
		return pages.get(pageNo);
	}

	@Override
	public void save(int pageNo, Page page) {
		pages.put(pageNo, page);
	}

}
