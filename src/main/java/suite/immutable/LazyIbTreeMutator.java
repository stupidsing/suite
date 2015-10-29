package suite.immutable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.SerializedPageFileImpl;
import suite.file.impl.SubPageFileImpl;
import suite.fs.KeyValueStoreMutator;
import suite.streamlet.Streamlet;
import suite.util.SerializeUtil;
import suite.util.SerializeUtil.Serializer;

public class LazyIbTreeMutator<Key, Value> implements KeyValueStoreMutator<Key, Value> {

	private class Node {
		private Key key;
		private Value value;
	}

	private SerializedPageFile<List<Integer>> superblockFile;
	private LazyIbTreePersister<Node> persister;
	private List<Integer> pointers;

	public LazyIbTreeMutator( //
			PageFile pageFile //
			, Comparator<Key> keyComparator //
			, Serializer<Key> keySerializer //
			, Serializer<Value> valueSerializer) {
		superblockFile = new SerializedPageFileImpl<>(new SubPageFileImpl(pageFile, 0, 1),
				SerializeUtil.list(SerializeUtil.intSerializer));

		persister = new LazyIbTreePersister<>( //
				new SubPageFileImpl(pageFile, 1, Integer.MAX_VALUE) //
				, (n0, n1) -> keyComparator.compare(n0.key, n1.key) //
				, new Serializer<Node>() {
					public Node read(DataInput dataInput) throws IOException {
						Node node = new Node();
						node.key = keySerializer.read(dataInput);
						node.value = valueSerializer.read(dataInput);
						return node;
					}

					public void write(DataOutput dataOutput, Node node) throws IOException {
						keySerializer.write(dataOutput, node.key);
						valueSerializer.write(dataOutput, node.value);
					}
				});

		pointers = superblockFile.load(0);
	}

	@Override
	public Streamlet<Key> keys(Key start, Key end) {
		return persister.load(pointers).stream(node(start), node(end)).map(node -> node.key);
	}

	@Override
	public Value get(Key key) {
		List<Value> values = new ArrayList<>();
		persister.load(pointers).update(node(key), node -> {
			values.add(node.value);
			return node;
		});
		return values.get(0);
	}

	@Override
	public void put(Key key, Value value) {
		Node node1 = new Node();
		node1.key = key;
		node1.value = value;
		pointers = persister.save(persister.load(pointers).update(node(key), node0 -> node1));
	}

	@Override
	public void remove(Key key) {
		pointers = persister.save(persister.load(pointers).update(node(key), node -> null));
	}

	@Override
	public void end(boolean isComplete) {
		superblockFile.save(0, pointers);
		persister.gc(pointers, 9);
	}

	private Node node(Key key) {
		Node node = new Node();
		node.key = key;
		return node;
	}

}
