package suite.editor;

import suite.streamlet.Reactive;

public class EditorModel {

	private Reactive<Boolean> modifiedChanged = new Reactive<>();
	private boolean isModified;

	private Reactive<String> filenameChanged = new Reactive<>();
	private String filename;

	private Reactive<String> searchTextChanged = new Reactive<>();
	private String searchText;

	public void changeModified(boolean modified) {
		setIsModified(modified);
		modifiedChanged.fire(modified);
	}

	public void changeFilename(String filename) {
		setFilename(filename);
		filenameChanged.fire(filename);
	}

	public void changeSearchText(String searchText) {
		setSearchText(searchText);
		searchTextChanged.fire(searchText);
	}

	public Reactive<Boolean> getModifiedChanged() {
		return modifiedChanged;
	}

	public Reactive<String> getFilenameChanged() {
		return filenameChanged;
	}

	public Reactive<String> getSearchTextChanged() {
		return searchTextChanged;
	}

	public boolean getIsModified() {
		return isModified;
	}

	public void setIsModified(boolean isModified) {
		this.isModified = isModified;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public String getSearchText() {
		return searchText;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}

}
