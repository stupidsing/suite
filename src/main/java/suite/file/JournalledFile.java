package suite.file;

import java.io.IOException;

public interface JournalledFile {

	public void commit() throws IOException;

}
