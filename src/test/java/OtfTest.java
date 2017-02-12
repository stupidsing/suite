import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.os.Execute;
import suite.os.FileUtil;
import suite.streamlet.Read;
import suite.util.Util;

public class OtfTest {

	@Test
	public void otfTest() {
		String familyKey = "Family";
		String subfamilyKey = "Subfamily";
		List<String> keys = Arrays.asList(familyKey, subfamilyKey);

		List<String> commands = Read.<Path> empty() //
				.cons(Paths.get("/tmp/fonts")) //
				.concatMap(path -> FileUtil.findPaths(path)) //
				.map(Path::toString) //
				.filter(path -> false //
						|| path.toLowerCase().endsWith(".otf") //
						|| path.toLowerCase().endsWith(".ttf") //
				) //
				.map2(path -> path, path -> {
					Execute exec = new Execute(new String[] { "otfinfo", "-i", path, });
					return Read.each(exec.out.split("\n")) //
							.map(line -> line.split(":")) //
							.filter(arr -> 2 <= arr.length) //
							.map2(arr -> arr[0].trim(), arr -> arr[1].trim()) //
							.filter((k, v) -> keys.contains(k)) //
							.toMap();
				}) //
				.map((k, m) -> {
					String f = m.get(familyKey);
					String sf = m.get(subfamilyKey);
					String dir = "/home/ywsing/.fonts/" + f + "/";
					String ext = k.substring(k.lastIndexOf(".") + 1).toLowerCase();

					return "mkdir -p '" + dir + "'; mv '" + k + "' '" + dir + f + " " + sf + "." + ext + "'";
				}) //
				.sort(Util::compare) //
				.toList();

		for (String command : commands)
			System.out.println(command);
	}

}
