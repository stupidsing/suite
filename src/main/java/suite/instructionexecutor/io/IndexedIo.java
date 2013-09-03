package suite.instructionexecutor.io;

import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import suite.node.Node;

/**
 * Implements input devices that read/write at specified byte positions rather
 * than serially.
 * 
 * @author ywsing
 */
public class IndexedIo implements AutoCloseable {

	private Map<Node, IndexedInput> inputs = Collections.synchronizedMap(new HashMap<Node, IndexedInput>());

	public interface IndexedInput {
		public int read(int p);

		public void close();
	}

	public IndexedInput get(Node key) {
		return inputs.get(key);
	}

	public IndexedInput put(Node key, Reader reader) {
		return inputs.put(key, new IndexedReader(reader));
	}

	@Override
	public void close() {
		for (IndexedInput input : inputs.values())
			input.close();
	}

}
