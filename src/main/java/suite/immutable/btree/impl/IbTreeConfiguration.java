package suite.immutable.btree.impl;

import java.util.Comparator;

import suite.util.SerializeUtil.Serializer;

public class IbTreeConfiguration<Key> {

	private String filenamePrefix;
	private int pageSize;
	private int maxBranchFactor;
	private Comparator<Key> comparator;
	private Serializer<Key> serializer;
	private long capacity;

	public String getFilenamePrefix() {
		return filenamePrefix;
	}

	public void setFilenamePrefix(String filenamePrefix) {
		this.filenamePrefix = filenamePrefix;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getMaxBranchFactor() {
		return maxBranchFactor;
	}

	public void setMaxBranchFactor(int maxBranchFactor) {
		this.maxBranchFactor = maxBranchFactor;
	}

	public Comparator<Key> getComparator() {
		return comparator;
	}

	public void setComparator(Comparator<Key> comparator) {
		this.comparator = comparator;
	}

	public Serializer<Key> getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer<Key> serializer) {
		this.serializer = serializer;
	}

	public long getCapacity() {
		return capacity;
	}

	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}

}
