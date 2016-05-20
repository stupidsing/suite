package suite.file.impl;

import java.nio.file.Path;

import suite.file.JournalledPageFile;
import suite.os.FileUtil;
import suite.util.Serialize;

public class JournalledPageFileImpl extends JournalledDataFileImpl<Integer> implements JournalledPageFile {

	public JournalledPageFileImpl(Path path, int pageSize) {
		super( //
				new PageFileImpl(path, pageSize), //
				new PageFileImpl(FileUtil.ext(path, ".journal"), pageSize + 4), //
				new PageFileImpl(FileUtil.ext(path, ".pointer"), 4), //
				pageSize, //
				Serialize.int_);
	}

}
