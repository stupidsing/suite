package suite.file.impl;

import suite.file.JournalledPageFile;
import suite.util.Serialize;

public class JournalledPageFileImpl extends JournalledDataFileImpl<Integer> implements JournalledPageFile {

	public JournalledPageFileImpl(String filename, int pageSize) {
		super( //
				new PageFileImpl(filename, pageSize) //
				, new PageFileImpl(filename + ".journal", pageSize + 4) //
				, new PageFileImpl(filename + ".pointer", 4) //
				, pageSize //
				, Serialize.int_);
	}

}
