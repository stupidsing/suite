package suite.file.impl;

import java.nio.file.Path;

import suite.file.JournalledPageFile;
import suite.os.FileUtil;
import suite.util.Serialize;

public class JournalledPageFileImpl extends JournalledDataFileImpl<Integer> implements JournalledPageFile {

	public JournalledPageFileImpl(Path path, int pageSize) {
		super( //
				PageFileFactory.pageFile(path, pageSize), //
				PageFileFactory.pageFile(FileUtil.ext(path, ".journal"), pageSize + 4), //
				PageFileFactory.pageFile(FileUtil.ext(path, ".pointer"), 4), //
				pageSize, //
				Serialize.int_);
	}

}
