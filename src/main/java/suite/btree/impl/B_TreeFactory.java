package suite.btree.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import suite.file.JournalledPageFileImpl;
import suite.file.PageFile;
import suite.file.PageFileImpl;
import suite.file.SerializedPageFile;
import suite.file.SubPageFileImpl;
import suite.primitive.Bytes;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class B_TreeFactory<Key, Value> {

	private int pageSize = PageFile.defaultPageSize;

	private Serializer<Key> keySerializer;
	private Serializer<Value> valueSerializer;

	private class B_TreeSuperblockSerializer implements Serializer<B_TreeImpl<Key, Value>.Superblock> {
		private B_TreeImpl<Key, Value> b_tree;

		public B_TreeSuperblockSerializer(B_TreeImpl<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_TreeImpl<Key, Value>.Superblock read(DataInput dataInput) throws IOException {
			B_TreeImpl<Key, Value>.Superblock superblock = b_tree.new Superblock();
			superblock.root = SerializeUtil.intSerializer.read(dataInput);
			return superblock;
		}

		public void write(DataOutput dataOutput, B_TreeImpl<Key, Value>.Superblock value) throws IOException {
			SerializeUtil.intSerializer.write(dataOutput, value.root);
		}
	}

	private class B_TreePageSerializer implements Serializer<B_TreeImpl<Key, Value>.Page> {
		private B_TreeImpl<Key, Value> b_tree;

		private static final char LEAF = 'L';
		private static final char BRANCH = 'I';

		public B_TreePageSerializer(B_TreeImpl<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_TreeImpl<Key, Value>.Page read(DataInput dataInput) throws IOException {
			int pageNo = dataInput.readInt();
			int size = dataInput.readInt();

			B_TreeImpl<Key, Value>.Page page = b_tree.new Page(pageNo);

			for (int i = 0; i < size; i++) {
				char nodeType = dataInput.readChar();
				Key key = keySerializer.read(dataInput);

				if (nodeType == BRANCH) {
					int branch = dataInput.readInt();
					addBranch(page, key, branch);
				} else if (nodeType == LEAF) {
					Value value = valueSerializer.read(dataInput);
					addLeaf(page, key, value);
				}
			}

			return page;
		}

		public void write(DataOutput dataOutput, B_TreeImpl<Key, Value>.Page page) throws IOException {
			dataOutput.writeInt(page.pageNo);
			dataOutput.writeInt(page.size());

			for (B_TreeImpl<Key, Value>.KeyPointer kp : page)
				if (kp.pointer instanceof B_TreeImpl.Branch) {
					dataOutput.writeChar(BRANCH);
					keySerializer.write(dataOutput, kp.key);
					dataOutput.writeInt(kp.getBranchPageNo());
				} else if (kp.pointer instanceof B_TreeImpl.Leaf) {
					dataOutput.writeChar(LEAF);
					keySerializer.write(dataOutput, kp.key);
					valueSerializer.write(dataOutput, kp.getLeafValue());
				}
		}

		private void addLeaf(List<B_TreeImpl<Key, Value>.KeyPointer> kps, Key k, Value v) {
			kps.add(b_tree.new KeyPointer(k, b_tree.new Leaf(v)));
		}

		private void addBranch(List<B_TreeImpl<Key, Value>.KeyPointer> kps, Key k, int branch) {
			kps.add(b_tree.new KeyPointer(k, b_tree.new Branch(branch)));
		}

	}

	public B_TreeFactory(Serializer<Key> keySerializer, Serializer<Value> valueSerializer) {
		this.keySerializer = keySerializer;
		this.valueSerializer = valueSerializer;
	}

	public B_TreeImpl<Key, Value> produce(String filename, boolean isNew, Comparator<Key> cmp, int nPages) throws IOException {
		if (isNew)
			Files.deleteIfExists(Paths.get(filename));

		PageFile f = new JournalledPageFileImpl(filename, pageSize);
		return produce(f, isNew, cmp, nPages);
	}

	public B_TreeImpl<Key, Value> produce(PageFile f, boolean isNew, Comparator<Key> cmp, int nPages) throws IOException {
		int nSuperblockPages = 1;
		int nAllocatorPages = nPages / pageSize;
		int p0 = 0, p1 = p0 + nAllocatorPages, p2 = p1 + nSuperblockPages, p3 = p2 + nPages;

		return produce(isNew, cmp //
				, new SubPageFileImpl(f, p0, p1) //
				, new SubPageFileImpl(f, p1, p2) //
				, new SubPageFileImpl(f, p2, p3));
	}

	public B_TreeImpl<Key, Value> produce(String pathName, boolean isNew, Comparator<Key> cmp) throws IOException {
		String sbf = pathName + ".superblock";
		String alf = pathName + ".alloc";
		String f = pathName + ".pages";

		if (isNew)
			for (String filename : new String[] { sbf, alf, f })
				Files.deleteIfExists(Paths.get(filename));

		return produce(isNew, cmp //
				, new PageFileImpl(alf, pageSize) //
				, new PageFileImpl(sbf, pageSize) //
				, new PageFileImpl(f, pageSize));
	}

	private B_TreeImpl<Key, Value> produce(boolean isNew, Comparator<Key> comparator //
			, PageFile alpf0, PageFile sbpf0, PageFile pf0) throws IOException {
		B_TreeImpl<Key, Value> b_tree = new B_TreeImpl<>(comparator);

		Serializer<Bytes> als = SerializeUtil.bytes(pageSize);
		B_TreeSuperblockSerializer sbs = new B_TreeSuperblockSerializer(b_tree);
		B_TreePageSerializer ps = new B_TreePageSerializer(b_tree);

		SerializedPageFile<Bytes> alpf = new SerializedPageFile<>(alpf0, als);
		SerializedPageFile<B_TreeImpl<Key, Value>.Superblock> sbpf = new SerializedPageFile<>(sbpf0, sbs);
		SerializedPageFile<B_TreeImpl<Key, Value>.Page> pf = new SerializedPageFile<>(pf0, ps);

		b_tree.setAllocator(new AllocatorImpl(alpf));
		b_tree.setSuperblockPageFile(sbpf);
		b_tree.setPageFile(pf);
		b_tree.setBranchFactor(16);

		if (isNew)
			b_tree.create();

		return b_tree;
	}

}
