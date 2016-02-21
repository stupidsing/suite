package suite.btree.impl;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.AllocatorImpl;
import suite.file.impl.JournalledPageFileImpl;
import suite.file.impl.PageFileImpl;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.primitive.Bytes;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.Util;

public class B_TreeBuilder<Key, Value> {

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
			superblock.root = Serialize.int_.read(dataInput);
			return superblock;
		}

		public void write(DataOutput dataOutput, B_TreeImpl<Key, Value>.Superblock value) throws IOException {
			Serialize.int_.write(dataOutput, value.root);
		}
	}

	private class B_TreePageSerializer implements Serializer<B_TreeImpl<Key, Value>.Page> {
		private B_TreeImpl<Key, Value> b_tree;

		private static final char BRANCH = 'I';
		private static final char LEAF = 'L';
		private static final char PAYLOAD = 'P';
		private static final char TERMINAL = 'T';

		public B_TreePageSerializer(B_TreeImpl<Key, Value> b_tree) {
			this.b_tree = b_tree;
		}

		public B_TreeImpl<Key, Value>.Page read(DataInput dataInput) throws IOException {
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

		public void write(DataOutput dataOutput, B_TreeImpl<Key, Value>.Page page) throws IOException {
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

	public B_TreeBuilder(Serializer<Key> keySerializer, Serializer<Value> valueSerializer) {
		this.keySerializer = Serialize.nullable(keySerializer);
		this.valueSerializer = valueSerializer;
	}

	public B_TreeImpl<Key, Value> build(String filename, boolean isNew, Comparator<Key> cmp, int nPages) {
		if (isNew)
			try {
				Files.deleteIfExists(Paths.get(filename));
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

		PageFile f = new JournalledPageFileImpl(filename, pageSize);
		return build(f, isNew, cmp, nPages);
	}

	public B_TreeImpl<Key, Value> build(PageFile f, boolean isNew, Comparator<Key> cmp, int nPages) {
		int nSuperblockPages = 1;
		int nAllocatorPages = nPages / pageSize;
		int p0 = 0, p1 = p0 + nAllocatorPages, p2 = p1 + nSuperblockPages, p3 = p2 + nPages;

		return build(isNew, cmp //
				, new SubPageFileImpl(f, p0, p1) //
				, new SubPageFileImpl(f, p1, p2) //
				, new SubPageFileImpl(f, p2, p3));
	}

	public B_TreeImpl<Key, Value> build(String pathName, boolean isNew, Comparator<Key> cmp) {
		String sbf = pathName + ".superblock";
		String alf = pathName + ".alloc";
		String f = pathName + ".pages";

		if (isNew)
			for (String filename : new String[] { sbf, alf, f, })
				try {
					Files.deleteIfExists(Paths.get(filename));
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}

		return build(isNew, cmp //
				, new PageFileImpl(alf, pageSize) //
				, new PageFileImpl(sbf, pageSize) //
				, new PageFileImpl(f, pageSize));
	}

	private B_TreeImpl<Key, Value> build(boolean isNew, Comparator<Key> comparator, PageFile alf0, PageFile sbf0, PageFile pf0) {
		B_TreeImpl<Key, Value> b_tree = new B_TreeImpl<>(Util.nullsFirst(comparator));

		Serializer<Bytes> als = Serialize.bytes(pageSize);
		B_TreeSuperblockSerializer sbs = new B_TreeSuperblockSerializer(b_tree);
		Serializer<Bytes> pys = Serialize.bytes(pageSize);
		B_TreePageSerializer ps = new B_TreePageSerializer(b_tree);

		SerializedPageFile<Bytes> alf = new SerializedPageFileImpl<>(alf0, als);
		SerializedPageFile<B_TreeImpl<Key, Value>.Superblock> sbf = new SerializedPageFileImpl<>(sbf0, sbs);
		SerializedPageFile<Bytes> pyf = new SerializedPageFileImpl<>(pf0, pys);
		SerializedPageFile<B_TreeImpl<Key, Value>.Page> pf = new SerializedPageFileImpl<>(pf0, ps);

		b_tree.setAllocator(new AllocatorImpl(alf));
		b_tree.setSuperblockPageFile(sbf);
		b_tree.setPayloadFile(pyf);
		b_tree.setPageFile(pf);
		b_tree.setBranchFactor(16);

		if (isNew)
			b_tree.create();

		return b_tree;
	}

}
