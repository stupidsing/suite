package suite.immutable.btree;

import static org.junit.Assert.assertEquals;

import java.io.File;
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
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.Streamlet;
import suite.util.Copy;
import suite.util.To;

public class FileSystemTest {

	private interface TestCase {
		public void test(FileSystem fs) throws IOException;
	}

	@Test
	public void testIbTreeFileSystem() throws IOException {
		testIbTree(FileUtil.tmp + "/ibTree-fs", this::testWriteOneFile);
	}

	@Test
	public void testB_TreeFileSystem() throws IOException {
		testB_Tree(FileUtil.tmp + "/b_tree-fs", this::testWriteOneFile);
	}

	// Writing too many files (testWriteFiles1) would fail this test case. Do
	// not know why.
	@Test
	public void testIbTreeFileSystem1() throws IOException {
		testIbTree(FileUtil.tmp + "/ibTree-fs1", this::testWriteFiles0);
		testIbTree(FileUtil.tmp + "/ibTree-fs1", this::testReadFile);
	}

	@Test
	public void testB_TreeFileSystem1() throws IOException {
		testB_Tree(FileUtil.tmp + "/b_tree-fs1", this::testWriteFiles1);
		testB_Tree(FileUtil.tmp + "/b_tree-fs1", this::testReadFile);
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

	private void testWriteOneFile(FileSystem fs) {
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

	private void testWriteFiles0(FileSystem fs) throws IOException {
		testWriteFile(fs, "src/test/java/suite/immutable/");
	}

	private void testWriteFiles1(FileSystem fs) throws IOException {
		testWriteFile(fs, "src/test/java/");
	}

	private void testWriteFile(FileSystem fs, String pathName) throws IOException {
		Streamlet<Path> paths = FileUtil.findPaths(Paths.get(pathName));

		fs.create();
		FileSystemMutator fsm = fs.mutate();

		for (Path path : paths) {
			String filename = path.toString().replace(File.separatorChar, '/');
			Bytes name = Bytes.of(filename.getBytes(FileUtil.charset));
			fsm.replace(name, Bytes.of(Files.readAllBytes(path)));
		}
	}

	private void testReadFile(FileSystem fs) throws IOException {
		String filename = "src/test/java/suite/immutable/btree/FileSystemTest.java";
		FileSystemMutator fsm = fs.mutate();
		Bytes name = Bytes.of(filename.getBytes(FileUtil.charset));
		Copy.stream(fsm.read(name).asInputStream(), System.out);
	}

}
