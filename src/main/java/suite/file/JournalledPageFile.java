package suite.file;

public interface JournalledPageFile extends PageFile {

	public void applyJournal();

	public void commit();

}
