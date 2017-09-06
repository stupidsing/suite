package suite.editor;

import suite.streamlet.Signal;

public class EditorModel {

	private boolean isModified;
	private String filename;
	private String searchText;

	private Signal<Boolean> modifiedChanged = Signal.of();
	private Signal<String> filenameChanged = Signal.of();
	private Signal<String> searchTextChanged = Signal.of();

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

	public Signal<Boolean> getModifiedChanged() {
		return modifiedChanged;
	}

	public Signal<String> getFilenameChanged() {
		return filenameChanged;
	}

	public Signal<String> getSearchTextChanged() {
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
