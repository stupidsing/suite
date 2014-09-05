package suite.primitive;

import java.io.IOException;

public interface IoSink<I> {

	public void sink(I i) throws IOException;

}
