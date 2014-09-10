package suite.btree.impl;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;

import suite.btree.Allocator;
import suite.btree.B_Tree;
import suite.file.PageFile;
import suite.file.PageFileImpl;
import suite.file.SerializedPageFile;
import suite.primitive.Bytes;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class B_TreeHolder<Key, Value> implements Closeable {

	private int pageSize = PageFile.defaultPageSize;
	private Allocator al;
	private SerializedPageFile<B_TreeImpl<Key, Value>.Superblock> sbp;
	private SerializedPageFile<B_TreeImpl<Key, Value>.Page> pp;

	private B_Tree<Key, Value> b_tree;

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
		private Serializer<Key> keySerializer;
		private Serializer<Value> valueSerializer;

		private static final char LEAF = 'L';
		private static final char BRANCH = 'I';

		public B_TreePageSerializer(B_TreeImpl<Key, Value> b_tree, Serializer<Key> keySerializer, Serializer<Value> valueSerializer) {
			this.b_tree = b_tree;
			this.keySerializer = keySerializer;
			this.valueSerializer = valueSerializer;
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

	public B_TreeHolder(String pathName //
			, boolean isNew //
			, Comparator<Key> comparator //
			, Serializer<Key> ks //
			, Serializer<Value> vs) throws IOException {
		Files.createDirectories(Paths.get(pathName).getParent());

		String sbf = pathName + ".superblock";
		String amf = pathName + ".alloc";
		String pf = pathName + ".pages";

		if (isNew)
			for (String filename : new String[] { sbf, amf, pf })
				Files.deleteIfExists(Paths.get(filename));

		B_TreeImpl<Key, Value> b_tree = new B_TreeImpl<>(comparator);

		Serializer<Bytes> als = SerializeUtil.bytes(pageSize);
		B_TreeSuperblockSerializer sbs = new B_TreeSuperblockSerializer(b_tree);
		B_TreePageSerializer ps = new B_TreePageSerializer(b_tree, ks, vs);

		al = new AllocatorImpl(new SerializedPageFile<>(new PageFileImpl(amf, pageSize), als));
		sbp = new SerializedPageFile<>(new PageFileImpl(sbf, pageSize), sbs);
		pp = new SerializedPageFile<>(new PageFileImpl(pf, pageSize), ps);

		b_tree.setAllocator(al);
		b_tree.setSuperblockPageFile(sbp);
		b_tree.setPageFile(pp);
		b_tree.setBranchFactor(16);

		if (isNew)
			b_tree.create();

		this.b_tree = b_tree;
	}

	@Override
	public void close() throws IOException {
		pp.close();
		sbp.close();
		al.close();
	}

	public B_Tree<Key, Value> get() {
		return b_tree;
	}

}
