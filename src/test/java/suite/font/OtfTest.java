package suite.font;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;
import primal.Verbs.Compare;
import primal.Verbs.Mk;
import suite.os.Execute;
import suite.os.FileUtil;

/**
 * I've got like hundreds of TrueType fonts in my computer, which should reside
 * under ~/.fonts directory for desktop use. No one wants them to be scattered
 * all over the place; arranging/naming the files into appropriate
 * sub-directories became tiring. This program reads the type information from
 * OTF/TTF header and write a bash script to move the ttf files to appropriate
 * locations.
 *
 * Requires otfinfo - I used "apt install lcdf-typetools" to get it under
 * Debian.
 *
 * @author ywsing
 */
public class OtfTest {

	// main program and unit test combined.
	@Test
	public void otfTest() {

		// input variables.
		var directory = "/tmp/fonts";

		// constants.
		var familyKey = "Family";
		var subfamilyKey = "Subfamily";
		var keys = List.of(familyKey, subfamilyKey);

		// make sure the input directory exists; we are not supposed to show errors.
		Mk.dir(Paths.get(directory));

		var commands = Read //

				// get the input directory name(s), and convert them into Path object(s).
				.each(directory) //
				.map(Paths::get) //

				// find all files in the director(ies). I used an utility method.
				.concatMap(FileUtil::findPaths) //

				// find otf/ttfs only.
				.map(Path::toString) //
				.filter(path -> {
					var pathLower = path.toLowerCase();
					return pathLower.endsWith(".otf") || pathLower.endsWith(".ttf");
				}) //

				// invokes otfinfo and parse the output to a Java map of Strings.
				.map2(path -> {
					var exec = new Execute(new String[] { "otfinfo", "-i", path, });
					return Read //
							.from(exec.out.split("\n")) //
							.map(line -> line.split(":")) //
							.filter(arr -> 2 <= arr.length) //
							.map2(arr -> arr[0].trim(), arr -> arr[1].trim()) //
							.filterKey(keys::contains) //
							.toMap();
				}) //

				// gets family/sub-family information, and output a bash script.
				.map((k, m) -> {
					var f = m.get(familyKey);
					var sf = m.get(subfamilyKey);
					var dir = "/home/ywsing/.fonts/" + f + "/";
					var ext = k.substring(k.lastIndexOf(".") + 1).toLowerCase();

					return "mkdir -p '" + dir + "'; mv '" + k + "' '" + dir + f + " " + sf + "." + ext + "'\n";
				}) //

				// sort the script... looks nicer.
				.sort(Compare::objects) //

				// formality - this is actually a toList() and makes the data "stable."
				.toJoinedString();

		// output!
		System.out.println(commands);
	}

}
