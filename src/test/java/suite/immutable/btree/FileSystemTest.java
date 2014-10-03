package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import suite.file.PageFile;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.fs.impl.B_TreeFileSystemImpl;
import suite.fs.impl.IbTreeFileSystemImpl;
import suite.immutable.btree.impl.IbTreeConfiguration;
import suite.primitive.Bytes;
import suite.util.FileUtil;
import suite.util.FunUtil;
import suite.util.FunUtil.Source;
import suite.util.To;

public class FileSystemTest {

	private interface TestCase {
		public void test(FileSystem fs) throws IOException;
	}

	@Test
	public void testIbTreeFileSystem() throws IOException {
		testIbTree(FileUtil.tmp + "/ibTree-fs", this::test0);
	}

	@Test
	public void testB_TreeFileSystem() throws IOException {
		testB_Tree(FileUtil.tmp + "/b_tree-fs", this::test0);
	}

	private void testIbTree(String name, TestCase testCase) throws IOException {
		IbTreeConfiguration<Bytes> config = new IbTreeConfiguration<>();
		config.setFilenamePrefix(name);
		config.setPageSize(PageFile.defaultPageSize);
		config.setMaxBranchFactor(PageFile.defaultPageSize / 64);
		config.setCapacity(64 * 1024);

		try (FileSystem fs = new IbTreeFileSystemImpl(config)) {
			testCase.test(fs);
		}
	}

	private void testB_Tree(String name, TestCase testCase) throws IOException {
		try (FileSystem fs = new B_TreeFileSystemImpl(name, 4096)) {
			testCase.test(fs);
		}
	}

	private void test0(FileSystem fs) throws IOException {
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

	private void test1(FileSystem fs) throws IOException {
		Source<Path> paths = FileUtil.findPaths(Paths.get("src"));

		fs.create();
		FileSystemMutator fsm = fs.mutate();

		for (Path path : FunUtil.iter(paths))
			fsm.replace(Bytes.of(path.toString().getBytes(FileUtil.charset)), Bytes.of(Files.readAllBytes(path)));
	}

}
