package suite.btree.impl;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Comparator;
import java.util.List;

import suite.btree.Allocator;
import suite.btree.B_Tree;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class B_TreeHolder<Key, Value> implements Closeable {

	private Allocator al;
	private SerializedPageFile<B_TreeImpl<Key, Value>.Superblock> sbp;
	private SerializedPageFile<B_TreeImpl<Key, Value>.Page> pp;

	private B_Tree<Key, Value> b_tree;

	private class B_TreeSuperblockSerializer implements Serializer<B_TreeImpl<Key, Value>.Superblock> {
		private B_TreeImpl<Key, Value> b_tree;

		public B_TreeSuperblockSerializer(B_TreeImpl<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_TreeImpl<Key, Value>.Superblock read(ByteBuffer buffer) {
			B_TreeImpl<Key, Value>.Superblock superblock = b_tree.new Superblock();
			superblock.root = SerializeUtil.intSerializer.read(buffer);
			return superblock;
		}

		public void write(ByteBuffer buffer, B_TreeImpl<Key, Value>.Superblock value) {
			SerializeUtil.intSerializer.write(buffer, value.root);
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

		public B_TreeImpl<Key, Value>.Page read(ByteBuffer buffer) {
			int pageNo = buffer.getInt();
			int size = buffer.getInt();

			B_TreeImpl<Key, Value>.Page page = b_tree.new Page(pageNo);

			for (int i = 0; i < size; i++) {
				char nodeType = buffer.getChar();
				Key key = keySerializer.read(buffer);

				if (nodeType == BRANCH) {
					int branch = buffer.getInt();
					addBranch(page, key, branch);
				} else if (nodeType == LEAF) {
					Value value = valueSerializer.read(buffer);
					addLeaf(page, key, value);
				}
			}

			return page;
		}

		public void write(ByteBuffer buffer, B_TreeImpl<Key, Value>.Page page) {
			buffer.putInt(page.pageNo);
			buffer.putInt(page.size());

			for (B_TreeImpl<Key, Value>.KeyPointer kp : page)
				if (kp.pointer instanceof B_TreeImpl.Branch) {
					buffer.putChar(BRANCH);
					keySerializer.write(buffer, kp.key);
					buffer.putInt(kp.getBranchPageNo());
				} else if (kp.pointer instanceof B_TreeImpl.Leaf) {
					buffer.putChar(LEAF);
					keySerializer.write(buffer, kp.key);
					valueSerializer.write(buffer, kp.getLeafValue());
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
		new File(pathName).getParentFile().mkdirs();

		String sbf = pathName + ".superblock";
		String amf = pathName + ".alloc";
		String pf = pathName + ".pages";

		if (isNew)
			for (String filename : new String[] { sbf, amf, pf })
				new File(filename).delete();

		B_TreeImpl<Key, Value> b_tree = new B_TreeImpl<>(comparator);
		B_TreeSuperblockSerializer sbs = new B_TreeSuperblockSerializer(b_tree);
		B_TreePageSerializer ps = new B_TreePageSerializer(b_tree, ks, vs);

		al = new AllocatorImpl(amf);
		sbp = new SerializedPageFile<>(new PageFile(sbf, 4096), sbs);
		pp = new SerializedPageFile<>(new PageFile(pf, 4096), ps);

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
