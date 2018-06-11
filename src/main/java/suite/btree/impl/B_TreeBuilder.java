package suite.btree.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import suite.adt.pair.FixieArray;
import suite.adt.pair.Pair;
import suite.btree.B_Tree;
import suite.file.PageFile;
import suite.file.impl.AllocatorImpl;
import suite.file.impl.FileFactory;
import suite.file.impl.JournalledFileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.fs.KeyDataStore;
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

	public static <Key> Pair<B_Tree<Key, Integer>, KeyDataStore<Key>> build( //
			boolean isNew, //
			Path path, //
			int pageSize, //
			int nPages, //
			Serializer<Key> ks, //
			Comparator<Key> cmp) {
		if (isNew)
			Rethrow.ex(() -> Files.deleteIfExists(path));

		var jpf = JournalledFileFactory.journalled(path, pageSize);
		var b_tree = new B_TreeBuilder<>(ks, serialize.int_).build(jpf, nPages, cmp);

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

	public B_Tree<Key, Value> build(PageFile f, int nPages, Comparator<Key> cmp) {
		var nSuperblockPages = 1;
		var nAllocatorPages = nPages / pageSize;

		var p0 = 0;
		var p1 = p0 + nAllocatorPages;
		var p2 = p1 + nSuperblockPages;
		var p3 = p2 + nPages;
		var pfs = FileFactory.subPageFiles(f, p0, p1, p2, p3);

		return FixieArray.of(pfs).map((alf0, sbf0, pf0) -> {
			var b_tree = new B_TreeImpl<Key, Value>(Object_.nullsFirst(cmp));

			var als = serialize.bytes(pageSize);
			var sbs = superblockSerializer(b_tree);
			var pys = serialize.bytes(pageSize);
			var ps = pageSerializer(b_tree);

			var alf = SerializedFileFactory.serialized(alf0, als);
			var sbf = SerializedFileFactory.serialized(sbf0, sbs);
			var pyf = SerializedFileFactory.serialized(pf0, pys);
			var pf = SerializedFileFactory.serialized(pf0, ps);

			b_tree.setAllocator(new AllocatorImpl(alf));
			b_tree.setSuperblockPageFile(sbf);
			b_tree.setPayloadFile(pyf);
			b_tree.setPageFile(pf);
			b_tree.setBranchFactor(16);
			return b_tree;
		});
	}

	private Serializer<B_TreeImpl<Key, Value>.Superblock> superblockSerializer(B_TreeImpl<Key, Value> b_tree) {
		return new Serializer<>() {
			public B_TreeImpl<Key, Value>.Superblock read(DataInput_ dataInput) throws IOException {
				var superblock = b_tree.new Superblock();
				superblock.root = serialize.int_.read(dataInput);
				return superblock;
			}

			public void write(DataOutput_ dataOutput, B_TreeImpl<Key, Value>.Superblock value) throws IOException {
				serialize.int_.write(dataOutput, value.root);
			}
		};
	}

	private Serializer<B_TreeImpl<Key, Value>.Page> pageSerializer(B_TreeImpl<Key, Value> b_tree) {
		return new Serializer<>() {
			public B_TreeImpl<Key, Value>.Page read(DataInput_ dataInput) throws IOException {
				var pointer = dataInput.readInt();
				var size = dataInput.readInt();
				var page = b_tree.new Page(pointer);

				for (var i = 0; i < size; i++) {
					var key = keySerializer.read(dataInput);
					var nodeType = dataInput.readChar();

					if (nodeType == BRANCH) {
						var branch = dataInput.readInt();
						page.add(b_tree.new KeyPointer(key, b_tree.new Branch(branch)));
					} else if (nodeType == LEAF) {
						var value = valueSerializer.read(dataInput);
						page.add(b_tree.new KeyPointer(key, b_tree.new Leaf(value)));
					} else if (nodeType == PAYLOAD) {
						var pointer1 = dataInput.readInt();
						page.add(b_tree.new KeyPointer(key, b_tree.new Payload(pointer1)));
					} else if (nodeType == TERMINAL)
						page.add(b_tree.new KeyPointer(key, b_tree.new Terminal()));
				}

				return page;
			}

			public void write(DataOutput_ dataOutput, B_TreeImpl<Key, Value>.Page page) throws IOException {
				dataOutput.writeInt(page.pointer);
				dataOutput.writeInt(page.size());

				for (var kp : page) {
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
		};
	}

}
