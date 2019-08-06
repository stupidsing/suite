package suite.fs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import primal.Nouns.Utf8;
import primal.primitive.adt.Bytes;
import suite.cfg.Defaults;
import suite.fs.impl.B_TreeFileSystemImpl;
import suite.fs.impl.LazyPbTreeFileSystemImpl;
import suite.os.FileUtil;
import suite.streamlet.As;
import suite.util.Copy;
import suite.util.To;

public class FileSystemTest {

	private interface TestCase {
		public void test(FileSystem fs) throws IOException;
	}

	@Test
	public void testB_TreeFileSystem0() throws IOException {
		testB_Tree(Defaults.tmp("b_tree-fs0"), true, this::testWriteOneFile);
	}

	@Test
	public void testB_TreeFileSystem1() throws IOException {
		testB_Tree(Defaults.tmp("b_tree-fs1"), true, this::testWriteFiles);
		testB_Tree(Defaults.tmp("b_tree-fs1"), false, this::testReadFile);
	}

	@Test
	public void testLazyIbTreeFileSystem0() throws IOException {
		testLazyIbTree(Defaults.tmp("lazyIbTree-fs0"), true, this::testWriteOneFile);
	}

	@Test
	public void testLazyIbTreeFileSystem1() throws IOException {
		testLazyIbTree(Defaults.tmp("lazyIbTree-fs1"), true, this::testWriteFiles);
		testLazyIbTree(Defaults.tmp("lazyIbTree-fs1"), false, this::testReadFile);
	}

	private void testB_Tree(Path path, boolean isNew, TestCase testCase) throws IOException {
		try (var fs = new B_TreeFileSystemImpl(path, isNew, 4096)) {
			testCase.test(fs);
		}
	}

	private void testLazyIbTree(Path path, boolean isNew, TestCase testCase) throws IOException {
		if (isNew) {
			Files.deleteIfExists(path);
			Files.deleteIfExists(FileUtil.ext(path, ".journal"));
			Files.deleteIfExists(FileUtil.ext(path, ".pointer"));
		}

		try (var fs = new LazyPbTreeFileSystemImpl(path, 4096)) {
			testCase.test(fs);
		}
	}

	private void testWriteOneFile(FileSystem fs) {
		var filename = To.bytes("file");
		var data = To.bytes("data");
		var fsm = fs.mutate();

		fsm.replace(filename, data);
		assertEquals(1, fsm.list(filename, null).size());
		assertEquals(data, fsm.read(filename));

		fsm.replace(filename, null);
		assertEquals(0, fsm.list(filename, null).size());
	}

	private void testWriteFiles(FileSystem fs) throws IOException {
		var paths = FileUtil.findPaths(Paths.get("src/test/java/"));
		var fsm = fs.mutate();

		for (var path : paths) {
			var filename = path.toString().replace(File.separatorChar, '/');
			var name = Bytes.of(filename.getBytes(Utf8.charset));
			fsm.replace(name, Bytes.of(Files.readAllBytes(path)));
		}
	}

	private void testReadFile(FileSystem fs) throws IOException {
		var filename = "src/test/java/suite/fs/FileSystemTest.java";
		var fsm = fs.mutate();
		var name = Bytes.of(filename.getBytes(Utf8.charset));
		Copy.stream(fsm.read(name).collect(As::inputStream), System.out);
	}

}
