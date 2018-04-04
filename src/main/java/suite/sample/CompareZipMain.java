package suite.sample;

import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.text.TextUtil;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.Set_;
import suite.util.To;

/**
 * Compare content of two zip files.
 *
 * @author ywsing
 */
public class CompareZipMain extends ExecutableProgram {

	private TextUtil textUtil = new TextUtil();

	public static void main(String[] args) {
		RunUtil.run(CompareZipMain.class, args);
	}

	@Override
	protected boolean run(String[] args) throws IOException {
		var filename0 = "/tmp/a";
		var filename1 = "/tmp/b";
		ZipFile zf0 = new ZipFile(filename0);
		ZipFile zf1 = new ZipFile(filename1);

		Set<String> names = Set_.union(FileUtil.listZip(zf0), FileUtil.listZip(zf1));

		boolean isChanged = false;

		for (String name : names) {
			ZipEntry e0 = zf0.getEntry(name);
			ZipEntry e1 = zf1.getEntry(name);
			boolean b = e0 != null && e1 != null;

			if (b) {
				Bytes bytes0 = To.bytes(zf0.getInputStream(e0));
				Bytes bytes1 = To.bytes(zf1.getInputStream(e1));
				b = !textUtil.isDiff(textUtil.diff(bytes0, bytes1));
				if (!b)
					System.out.println(name + " differs");
			} else if (e0 == null)
				System.out.println(name + " not exist in " + filename0);
			else if (e1 == null)
				System.out.println(name + " not exist in " + filename1);

			isChanged |= !b;
		}

		return !isChanged;
	}

}
