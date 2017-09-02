package suite.btree.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import suite.adt.pair.Pair;
import suite.btree.B_Tree;
import suite.file.JournalledPageFile;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.AllocatorImpl;
import suite.file.impl.FileFactory;
import suite.file.impl.JournalledFileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.fs.KeyDataStore;
import suite.primitive.Bytes;
import suite.util.DataInput_;
import suite.util.DataOutput_;
import suite.util.Object_;
import suite.util.Rethrow;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;

public class B_TreeBuilder<Key, Value> {

	private static Serialize serialize = Serialize.me;

	private static char BRANCH = 'I';
	private static char LEAF = 'L';
	private static char PAYLOAD = 'P';
	private static char TERMINAL = 'T';

	private int pageSize = PageFile.defaultPageSize;

	private Serializer<Key> keySerializer;
	private Serializer<Value> valueSerializer;

	private class B_TreeSuperblockSerializer implements Serializer<B_TreeImpl<Key, Value>.Superblock> {
		private B_TreeImpl<Key, Value> b_tree;

		public B_TreeSuperblockSerializer(B_TreeImpl<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_TreeImpl<Key, Value>.Superblock read(DataInput_ dataInput) throws IOException {
			B_TreeImpl<Key, Value>.Superblock superblock = b_tree.new Superblock();
			superblock.root = serialize.int_.read(dataInput);
			return superblock;
		}

		public void write(DataOutput_ dataOutput, B_TreeImpl<Key, Value>.Superblock value) throws IOException {
			serialize.int_.write(dataOutput, value.root);
		}
	}

	private class B_TreePageSerializer implements Serializer<B_TreeImpl<Key, Value>.Page> {
		private B_TreeImpl<Key, Value> b_tree;

		public B_TreePageSerializer(B_TreeImpl<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_TreeImpl<Key, Value>.Page read(DataInput_ dataInput) throws IOException {
			int pointer = dataInput.readInt();
			int size = dataInput.readInt();

			B_TreeImpl<Key, Value>.Page page = b_tree.new Page(pointer);

			for (int i = 0; i < size; i++) {
				Key key = keySerializer.read(dataInput);
				char nodeType = dataInput.readChar();

				if (nodeType == BRANCH) {
					int branch = dataInput.readInt();
					page.add(b_tree.new KeyPointer(key, b_tree.new Branch(branch)));
				} else if (nodeType == LEAF) {
					Value value = valueSerializer.read(dataInput);
					page.add(b_tree.new KeyPointer(key, b_tree.new Leaf(value)));
				} else if (nodeType == PAYLOAD) {
					int pointer1 = dataInput.readInt();
					page.add(b_tree.new KeyPointer(key, b_tree.new Payload(pointer1)));
				} else if (nodeType == TERMINAL)
					page.add(b_tree.new KeyPointer(key, b_tree.new Terminal()));
			}

			return page;
		}

		public void write(DataOutput_ dataOutput, B_TreeImpl<Key, Value>.Page page) throws IOException {
			dataOutput.writeInt(page.pointer);
			dataOutput.writeInt(page.size());

			for (B_TreeImpl<Key, Value>.KeyPointer kp : page) {
				keySerializer.write(dataOutput, kp.key);

				if (kp.pointer instanceof B_TreeImpl.Branch) {
					dataOutput.writeChar(BRANCH);
					dataOutput.writeInt(kp.getBranchPointer());
				} else if (kp.pointer instanceof B_TreeImpl.Leaf) {
					dataOutput.writeChar(LEAF);
					valueSerializer.write(dataOutput, kp.getLeafValue());
				} else if (kp.pointer instanceof B_TreeImpl.Payload) {
					dataOutput.writeChar(PAYLOAD);
					dataOutput.writeInt(kp.getPayloadPointer());
				} else if (kp.pointer instanceof B_TreeImpl.Terminal)
					dataOutput.writeChar(TERMINAL);
			}
		}
	}

	public static <Key> Pair<B_Tree<Key, Integer>, KeyDataStore<Key>> build( //
			Path path, //
			boolean isNew, //
			Serializer<Key> ks, //
			Comparator<Key> cmp, //
			int pageSize, //
			int nPages) {
		if (isNew)
			Rethrow.ex(() -> Files.deleteIfExists(path));

		JournalledPageFile jpf = JournalledFileFactory.journalled(path, pageSize);
		B_Tree<Key, Integer> b_tree = new B_TreeBuilder<>(ks, serialize.int_).build(jpf, cmp, nPages);

		if (isNew) {
			b_tree.create();
			jpf.commit();
		}

		return Pair.of(b_tree, new B_TreeStore<>(b_tree, jpf::commit));
	}

	public B_TreeBuilder(Serializer<Key> keySerializer, Serializer<Value> valueSerializer) {
		this.keySerializer = serialize.nullable(keySerializer);
		this.valueSerializer = valueSerializer;
	}

	public B_Tree<Key, Value> build(PageFile f, Comparator<Key> cmp, int nPages) {
		int nSuperblockPages = 1;
		int nAllocatorPages = nPages / pageSize;
		int p0 = 0, p1 = p0 + nAllocatorPages, p2 = p1 + nSuperblockPages, p3 = p2 + nPages;
		PageFile[] pfs = FileFactory.subPageFiles(f, p0, p1, p2, p3);
		return build(cmp, pfs[0], pfs[1], pfs[2]);
	}

	public B_Tree<Key, Value> build(boolean isNew, Path path, Comparator<Key> cmp) {
		Path filename = path.getFileName();
		Path sbp = path.resolveSibling(filename + ".superblock");
		Path alp = path.resolveSibling(filename + ".alloc");
		Path p = path.resolveSibling(filename + ".pages");

		if (isNew)
			for (Path p_ : new Path[] { sbp, alp, p, })
				Rethrow.ex(() -> Files.deleteIfExists(p_));

		B_Tree<Key, Value> b_tree = build(cmp //
				, FileFactory.pageFile(alp, pageSize) //
				, FileFactory.pageFile(sbp, pageSize) //
				, FileFactory.pageFile(p, pageSize));

		if (isNew)
			b_tree.create();

		return b_tree;
	}

	private B_Tree<Key, Value> build(Comparator<Key> comparator, PageFile alf0, PageFile sbf0, PageFile pf0) {
		B_TreeImpl<Key, Value> b_tree = new B_TreeImpl<>(Object_.nullsFirst(comparator));

		Serializer<Bytes> als = serialize.bytes(pageSize);
		B_TreeSuperblockSerializer sbs = new B_TreeSuperblockSerializer(b_tree);
		Serializer<Bytes> pys = serialize.bytes(pageSize);
		B_TreePageSerializer ps = new B_TreePageSerializer(b_tree);

		SerializedPageFile<Bytes> alf = SerializedFileFactory.serialized(alf0, als);
		SerializedPageFile<B_TreeImpl<Key, Value>.Superblock> sbf = SerializedFileFactory.serialized(sbf0, sbs);
		SerializedPageFile<Bytes> pyf = SerializedFileFactory.serialized(pf0, pys);
		SerializedPageFile<B_TreeImpl<Key, Value>.Page> pf = SerializedFileFactory.serialized(pf0, ps);

		b_tree.setAllocator(new AllocatorImpl(alf));
		b_tree.setSuperblockPageFile(sbf);
		b_tree.setPayloadFile(pyf);
		b_tree.setPageFile(pf);
		b_tree.setBranchFactor(16);
		return b_tree;
	}

}
