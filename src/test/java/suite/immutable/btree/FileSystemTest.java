package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.file.PageFile;
import suite.immutable.btree.impl.FileSystemImpl;
import suite.immutable.btree.impl.IbTreeConfiguration;
import suite.primitive.Bytes;
import suite.util.FileUtil;
import suite.util.To;

public class FileSystemTest {

	@Test
	public void test() throws IOException {
		Bytes filename = To.bytes("file");
		Bytes data = To.bytes("data");

		IbTreeConfiguration<Bytes> config = new IbTreeConfiguration<>();
		config.setFilenamePrefix(FileUtil.tmp + "/fs");
		config.setPageSize(PageFile.defaultPageSize);
		config.setMaxBranchFactor(PageFile.defaultPageSize / 64);
		config.setCapacity(64 * 1024);

		try (FileSystem fs = new FileSystemImpl(config)) {
			fs.create();
			fs.replace(filename, data);
			assertEquals(1, fs.list(filename, null).size());
			assertEquals(data, fs.read(filename));

			fs.replace(filename, null);
			assertEquals(0, fs.list(filename, null).size());
		}
	}

}
