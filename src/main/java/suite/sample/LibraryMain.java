package suite.sample;

import static primal.statics.Rethrow.ex;
import static suite.util.Streamlet_.forInt;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.codec.digest.Md5Crypt;

import primal.Verbs.DeleteFile;
import primal.Verbs.Format;
import primal.Verbs.Get;
import primal.Verbs.Mk;
import primal.Verbs.ReadFile;
import primal.Verbs.WriteFile;
import primal.adt.Pair;
import primal.streamlet.Streamlet;
import suite.os.FileUtil;
import suite.util.RunUtil;

/**
 * Maintains library of files, probably images or documents.
 * 
 * Perform tagging and de-duplication.
 * 
 * @author ywsing
 */
public class LibraryMain {

	private String inputDir = "/data/storey/lg";
	private List<String> fileExtensions = List.of("jpg");
	private String libraryDir = "/data/photographs & memories/library";
	private String tagsDir = "/data/photographs & memories/tags";

	private class FileInfo {
		private String md5;
		private Streamlet<String> tags;
	}

	public static void main(String[] args) {
		RunUtil.run(new LibraryMain()::run);
	}

	private boolean run() {
		var partition = FileUtil //
				.findPaths(Paths.get(inputDir)) //
				.filter(path -> fileExtensions.contains(Get.fileExtension(path))) //
				.map2(path -> ex(() -> Files.size(path))) //
				.partition((path, size) -> 0 < size);

		// remove empty files
		partition.v.sink((path, size) -> DeleteFile.on(path));

		// get all file information
		var path_fileInfos = partition.k //
				.map2((path, size) -> {
					var attrs = ex(() -> Files.readAttributes(path, BasicFileAttributes.class));

					var tags = forInt(path.getNameCount()) //
							.map(i -> path.getName(i).toString()) //
							.cons(Format.ymdHms(attrs.lastModifiedTime().toInstant())) //
							.collect();

					var fileInfo = new FileInfo();
					fileInfo.md5 = Md5Crypt.md5Crypt(ReadFile.from(path).readBytes());
					fileInfo.tags = tags;
					return fileInfo;
				});

		// construct file listing
		WriteFile.to(inputDir + ".listing").doPrintWriter(pw -> {
			for (var path_fileInfo : path_fileInfos)
				pw.println(path_fileInfo.k + path_fileInfo.v.md5);
		});

		path_fileInfos //
				.map2((path, fileInfo) -> {

					// move file to library, by md5
					var path1 = Paths.get(libraryDir, fileInfo.md5.substring(0, 2), fileInfo.md5);
					Mk.dir(path1.getParent());
					ex(() -> Files.move(path, path1, StandardCopyOption.REPLACE_EXISTING));
					return fileInfo;
				}) //
				.concatMap((path, fileInfo) -> fileInfo.tags.map(tag -> {

					// add to tag indices
					var path1 = Paths.get(tagsDir, tag, fileInfo.md5);
					return ex(() -> {
						Files.newOutputStream(path1).close();
						return Pair.of(tag, fileInfo);
					});
				}));

		return true;
	}

}
