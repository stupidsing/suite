package suite.btree;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

import suite.btree.Serializer.B_TreePageSerializer;
import suite.btree.Serializer.B_TreeSuperBlockSerializer;

public class B_TreeHolder<Key, Value> implements Closeable {

	private B_Tree<Key, Value> b_tree;

	private FileAllocator al;
	private FilePersister<B_Tree<Key, Value>.SuperBlock> sbp;
	private FilePersister<B_Tree<Key, Value>.Page> pp;

	public B_TreeHolder(String path //
			, String name //
			, boolean isNew //
			, Comparator<Key> comparator //
			, Serializer<Key> ks //
			, Serializer<Value> vs) throws IOException {
		new File(path).mkdirs();

		String prefix = path + "/" + name;
		String sbf = prefix + ".superblock";
		String amf = prefix + ".alloc";
		String pf = prefix + ".pages";

		if (isNew)
			for (String filename : new String[] { sbf, amf, pf })
				new File(filename).delete();

		b_tree = new B_Tree<>(comparator);

		B_TreeSuperBlockSerializer<Key, Value> sbs = new B_TreeSuperBlockSerializer<>(b_tree);
		B_TreePageSerializer<Key, Value> ps = new B_TreePageSerializer<>(b_tree, ks, vs);

		al = new FileAllocator(amf);
		sbp = new FilePersister<>(sbf, sbs);
		pp = new FilePersister<>(pf, ps);

		b_tree.setAllocator(al);
		b_tree.setSuperBlockPersister(sbp);
		b_tree.setPagePersister(pp);
		b_tree.setBranchFactor(16);

		if (isNew)
			b_tree.create();
	}

	public B_Tree<Key, Value> get() {
		return b_tree;
	}

	@Override
	public void close() throws IOException {
		pp.close();
		sbp.close();
		al.close();
	}

}
