package suite.fs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import suite.Constants;
import suite.fs.impl.B_TreeFileSystemImpl;
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
	public void testB_TreeFileSystem0() throws IOException {
		testB_Tree(Constants.tmp.resolve("b_tree-fs0"), this::testWriteOneFile);
	}

	@Test
	public void testB_TreeFileSystem1() throws IOException {
		testB_Tree(Constants.tmp.resolve("b_tree-fs1"), this::testWriteFiles);
		testB_Tree(Constants.tmp.resolve("b_tree-fs1"), this::testReadFile);
	}

	private void testB_Tree(Path path, TestCase testCase) throws IOException {
		try (FileSystem fs = new B_TreeFileSystemImpl(path, 4096)) {
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

	private void testWriteFiles(FileSystem fs) throws IOException {
		testWriteFile(fs, "src/test/java/");
	}

	private void testWriteFile(FileSystem fs, String pathName) throws IOException {
		Streamlet<Path> paths = FileUtil.findPaths(Paths.get(pathName));

		fs.create();
		FileSystemMutator fsm = fs.mutate();

		for (Path path : paths) {
			String filename = path.toString().replace(File.separatorChar, '/');
			Bytes name = Bytes.of(filename.getBytes(Constants.charset));
			fsm.replace(name, Bytes.of(Files.readAllBytes(path)));
		}
	}

	private void testReadFile(FileSystem fs) throws IOException {
		String filename = "src/test/java/suite/fs/FileSystemTest.java";
		FileSystemMutator fsm = fs.mutate();
		Bytes name = Bytes.of(filename.getBytes(Constants.charset));
		Copy.stream(fsm.read(name).asInputStream(), System.out);
	}

}
