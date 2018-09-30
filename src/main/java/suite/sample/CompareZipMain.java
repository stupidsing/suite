package suite.sample;

import java.util.zip.ZipFile;

import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.text.TextUtil;
import suite.util.RunUtil;
import suite.util.Set_;

/**
 * Compare contents of two zip files.
 *
 * @author ywsing
 */
public class CompareZipMain {

	public static void main(String[] args) {
		RunUtil.run(() -> {
			var textUtil = new TextUtil();
			var filename0 = "/tmp/a";
			var filename1 = "/tmp/b";
			var zf0 = new ZipFile(filename0);
			var zf1 = new ZipFile(filename1);

			var names = Set_.union(FileUtil.listZip(zf0), FileUtil.listZip(zf1));
			var isChanged = false;

			for (var name : names) {
				var e0 = zf0.getEntry(name);
				var e1 = zf1.getEntry(name);
				var b = e0 != null && e1 != null;

				if (b) {
					var bytes0 = Read.bytes(zf0.getInputStream(e0)).collect(Bytes::of);
					var bytes1 = Read.bytes(zf1.getInputStream(e1)).collect(Bytes::of);
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
		});
	}

}
