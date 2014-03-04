package suite.immutable;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import suite.immutable.IbTree.Pointer;
import suite.primitive.Bytes;
import suite.util.FunUtil.Sink;
import suite.util.To;
import suite.util.Util;

public class IbTreeFileSystem implements Closeable {

	private int pageSize = 4096;

	private List<IbTree<Pointer>> pointerIbTrees = new ArrayList<>();
	private IbTree<Bytes> ibTree;

	public IbTreeFileSystem(String filename, long capacity) throws FileNotFoundException {
		long nPages = capacity / pageSize;
		IbTreeBuilder builder = new IbTreeBuilder(pageSize / 64, pageSize);

		int i = 0;
		IbTree<Pointer> pointerIbTree;
		pointerIbTrees.add(builder.buildPointerTree(filename + i++));

		while ((pointerIbTree = Util.last(pointerIbTrees)).guaranteedCapacity() < nPages)
			pointerIbTrees.add(builder.buildPointerTree(filename + i++, pointerIbTree));

		ibTree = builder.buildTree(filename + i++, Bytes.comparator, IbNameKeySet.serializer, pointerIbTree);
	}

	@Override
	public void close() throws IOException {
		ibTree.close();
		ListIterator<IbTree<Pointer>> li = pointerIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	public void replace(String name, final Bytes bytes) {
		final Bytes nameBytes = To.bytes(name);

		ibTree.holder().io(new Sink<IbTree<Bytes>.Transaction>() {
			public void sink(IbTree<Bytes>.Transaction transaction) {
				try {
					IbNameKeySet ibNameKeySet = new IbNameKeySet(transaction);
					Bytes sizeKey = key(nameBytes, 65, 0);

					if (bytes != null) {
						int pos = 0, seq = 0, size = bytes.size();

						while (pos < size) {
							int pos1 = Math.max(pos + pageSize, size);
							ByteBuffer buffer = ByteBuffer.wrap(bytes.getBytes(), pos, pos1 - pos);
							transaction.replace(key(nameBytes, 64, seq++), buffer);
						}

						transaction.replace(sizeKey, bufferInt(size));
						ibNameKeySet.add(nameBytes);
					} else {
						int size = transaction.payload(sizeKey).asIntBuffer().array()[0];
						for (int seq = 0; seq < size; seq++)
							transaction.remove(key(nameBytes, 64, seq));

						ibNameKeySet.remove(sizeKey);
						ibNameKeySet.remove(nameBytes);
					}
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			}
		});
	}

	private Bytes key(Bytes nameBytes, int id, int seq) {
		return Bytes.concat(hash(nameBytes) //
				, Bytes.asList((byte) id) //
				, new Bytes(bufferInt(seq).array()));
	}

	private ByteBuffer bufferInt(int seq) {
		return ByteBuffer.allocate(4).putInt(seq);
	}

	private Bytes hash(Bytes bytes) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}

		md.update(bytes.getBytes());
		return new Bytes(md.digest());
	}

}
