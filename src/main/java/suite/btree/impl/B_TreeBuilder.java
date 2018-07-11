package suite.btree.impl; import static suite.util.Friends.fail;

import java.io.IOException;
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
import suite.node.util.Singleton;
import suite.object.Object_;
import suite.os.FileUtil;
import suite.serialize.SerInput;
import suite.serialize.SerOutput;
import suite.serialize.Serialize;
import suite.serialize.Serialize.Serializer;

public class B_TreeBuilder<Key, Value> {

	private static Serialize serialize = Singleton.me.serialize;

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
			FileUtil.deleteIfExists(path);

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
			public B_TreeImpl<Key, Value>.Superblock read(SerInput si) throws IOException {
				var superblock = b_tree.new Superblock();
				superblock.root = serialize.int_.read(si);
				return superblock;
			}

			public void write(SerOutput so, B_TreeImpl<Key, Value>.Superblock value) throws IOException {
				serialize.int_.write(so, value.root);
			}
		};
	}

	private Serializer<B_TreeImpl<Key, Value>.Page> pageSerializer(B_TreeImpl<Key, Value> b_tree) {
		return new Serializer<>() {
			public B_TreeImpl<Key, Value>.Page read(SerInput si) throws IOException {
				var pointer = si.readInt();
				var size = si.readInt();
				var page = b_tree.new Page(pointer);

				for (var i = 0; i < size; i++) {
					var key = keySerializer.read(si);
					var nodeType = si.readChar();

					if (nodeType == BRANCH) {
						var branch = si.readInt();
						page.add(b_tree.new KeyPointer(key, b_tree.new Branch(branch)));
					} else if (nodeType == LEAF) {
						var value = valueSerializer.read(si);
						page.add(b_tree.new KeyPointer(key, b_tree.new Leaf(value)));
					} else if (nodeType == PAYLOAD) {
						var pointer1 = si.readInt();
						page.add(b_tree.new KeyPointer(key, b_tree.new Payload(pointer1)));
					} else if (nodeType == TERMINAL)
						page.add(b_tree.new KeyPointer(key, b_tree.new Terminal()));
					else
						fail();
				}

				return page;
			}

			public void write(SerOutput so, B_TreeImpl<Key, Value>.Page page) throws IOException {
				so.writeInt(page.pointer);
				so.writeInt(page.size());

				for (var kp : page) {
					keySerializer.write(so, kp.key);

					if (kp.pointer instanceof B_TreeImpl.Branch) {
						so.writeChar(BRANCH);
						so.writeInt(kp.getBranchPointer());
					} else if (kp.pointer instanceof B_TreeImpl.Leaf) {
						so.writeChar(LEAF);
						valueSerializer.write(so, kp.getLeafValue());
					} else if (kp.pointer instanceof B_TreeImpl.Payload) {
						so.writeChar(PAYLOAD);
						so.writeInt(kp.getPayloadPointer());
					} else if (kp.pointer instanceof B_TreeImpl.Terminal)
						so.writeChar(TERMINAL);
					else
						fail();
				}
			}
		};
	}

}
