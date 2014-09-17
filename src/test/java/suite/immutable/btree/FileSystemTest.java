package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import suite.file.PageFile;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.fs.impl.B_TreeFileSystemImpl;
import suite.fs.impl.IbTreeFileSystemImpl;
import suite.immutable.btree.impl.IbTreeConfiguration;
import suite.primitive.Bytes;
import suite.util.FileUtil;
import suite.util.To;

public class FileSystemTest {

	@Test
	public void testIbTreeFileSystem() throws IOException {
		IbTreeConfiguration<Bytes> config = new IbTreeConfiguration<>();
		config.setFilenamePrefix(FileUtil.tmp + "/ibTree-fs");
		config.setPageSize(PageFile.defaultPageSize);
		config.setMaxBranchFactor(PageFile.defaultPageSize / 64);
		config.setCapacity(64 * 1024);

		try (FileSystem fs = new IbTreeFileSystemImpl(config)) {
			test(fs);
		}
	}

	@Test
	public void testB_TreeFileSystem() throws IOException {
		try (FileSystem fs = new B_TreeFileSystemImpl(FileUtil.tmp + "/b_tree-fs", 4096)) {
			test(fs);
		}
	}

	private void test(FileSystem fs) throws IOException {
		Bytes filename = To.bytes("file");
		Bytes data = To.bytes("data");

		fs.create();
		FileSystemMutator fsm = fs.mutate();

		fsm.replace(filename, data);
		assertEquals(1, fsm.list(filename, null).size());
		assertEquals(data, fsm.read(filename));

		fsm.replace(filename, null);
		assertEquals(0, fsm.list(filename, null).size());
	}

}
