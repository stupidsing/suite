package suite.editor;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import suite.os.Log_;

public class ClipboardUtil {

	public void setClipboardText(String text) {
		var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(text), null);
	}

	public String getClipboardText() {
		var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		var contents = clipboard.getContents(null);
		String text;

		if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
			try {
				text = contents.getTransferData(DataFlavor.stringFlavor).toString();
			} catch (UnsupportedFlavorException ex) {
				Log_.error(ex);
				text = "";
			} catch (IOException ex) {
				Log_.error(ex);
				text = "";
			}
		else
			text = "";

		return text;
	}

}
