package org.btree;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.util.Util;

public class InMemoryPersister<Page> implements Persister<Page> {

	private Map<Integer, Page> pages = Util.createHashMap();
	private AtomicInteger counter = new AtomicInteger();

	public static <Page> InMemoryPersister<Page> create() {
		return new InMemoryPersister<Page>();
	}

	public int allocate() {
		return counter.getAndIncrement();
	}

	public void deallocate(int pageNo) {
		pages.remove(pageNo);
	}

	public Page load(int pageNo) {
		return pages.get(pageNo);
	}

	public void save(int pageNo, Page page) {
		pages.put(pageNo, page);
	}

}
