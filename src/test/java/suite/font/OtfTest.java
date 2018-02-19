package suite.font;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.Test;

import suite.os.Execute;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.Object_;

public class OtfTest {

	@Test
	public void otfTest() {
		String familyKey = "Family";
		String subfamilyKey = "Subfamily";
		List<String> keys = List.of(familyKey, subfamilyKey);

		List<String> commands = Read //
				.each("/tmp/fonts") //
				.map(Paths::get) //
				.concatMap(FileUtil::findPaths) //
				.map(Path::toString) //
				.filter(path -> {
					String pathl = path.toLowerCase();
					return pathl.endsWith(".otf") || pathl.endsWith(".ttf");
				}) //
				.map2(path -> {
					Execute exec = new Execute(new String[] { "otfinfo", "-i", path, });
					return Read.from(exec.out.split("\n")) //
							.map(line -> line.split(":")) //
							.filter(arr -> 2 <= arr.length) //
							.map2(arr -> arr[0].trim(), arr -> arr[1].trim()) //
							.filterKey(keys::contains) //
							.toMap();
				}) //
				.map((k, m) -> {
					String f = m.get(familyKey);
					String sf = m.get(subfamilyKey);
					String dir = "/home/ywsing/.fonts/" + f + "/";
					String ext = k.substring(k.lastIndexOf(".") + 1).toLowerCase();

					return "mkdir -p '" + dir + "'; mv '" + k + "' '" + dir + f + " " + sf + "." + ext + "'";
				}) //
				.sort(Object_::compare) //
				.toList();

		for (String command : commands)
			System.out.println(command);
	}

}
