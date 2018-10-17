package suite.primitive;

import java.io.IOException;

public interface IoSink<I> {

	public void f(I i) throws IOException;

}
