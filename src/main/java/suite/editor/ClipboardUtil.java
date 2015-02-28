package suite.editor;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import suite.os.LogUtil;

public class ClipboardUtil {

	public void setClipboardText(String text) {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents(new StringSelection(text), null);
	}

	public String getClipboardText() {
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		Transferable contents = clipboard.getContents(null);
		String text;

		if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor))
			try {
				text = contents.getTransferData(DataFlavor.stringFlavor).toString();
			} catch (UnsupportedFlavorException ex) {
				LogUtil.error(ex);
				text = "";
			} catch (IOException ex) {
				LogUtil.error(ex);
				text = "";
			}
		else
			text = "";

		return text;
	}

}
