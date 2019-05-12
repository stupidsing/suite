package suite.editor;

import suite.streamlet.Pusher;

public class EditorModel {

	private ValueSignal<Boolean> isModified = ValueSignal.of(false);
	private ValueSignal<String> filename = ValueSignal.of("");
	private ValueSignal<String> searchText = ValueSignal.of("");

	public void changeIsModified(boolean modified_) {
		isModified.change(modified_);
	}

	public void changeFilename(String filename_) {
		filename.change(filename_);
	}

	public void changeSearchText(String searchText_) {
		searchText.change(searchText_);
	}

	public Pusher<Boolean> isModifiedChanged() {
		return isModified.changed;
	}

	public Pusher<String> filenameChanged() {
		return filename.changed;
	}

	public Pusher<String> searchTextChanged() {
		return searchText.changed;
	}

	public boolean isModified() {
		return isModified.get();
	}

	public String filename() {
		return filename.get();
	}

	public String searchText() {
		return searchText.get();
	}

}
