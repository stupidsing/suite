package suite.pkgmanager.action;

import suite.util.Copy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipFile;

public class ExtractFileAction implements InstallAction {

	private String packageFile;
	private String filename0;
	private String filename1;

	public ExtractFileAction(String packageFilename, String filename0, String filename1) {
		this.packageFile = packageFilename;
		this.filename0 = filename0;
		this.filename1 = filename1;
	}

	public void act() throws IOException {
		try (var zipFile = new ZipFile(packageFile);
				var is = zipFile.getInputStream(zipFile.getEntry(filename0));
				var fos = new FileOutputStream(filename1)) {
			Copy.stream(is, fos);
		}
	}

	public void unact() throws IOException {
		new File(filename1).delete();
	}

}
