package suite.filelib;

import static suite.util.Friends.rethrow;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import org.apache.commons.codec.digest.Md5Crypt;

import suite.adt.pair.Pair;
import suite.os.FileUtil;
import suite.primitive.Ints_;
import suite.streamlet.Streamlet;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.To;

/**
 * Maintains library of files, probably images or documents.
 * 
 * Perform tagging and de-duplication.
 * 
 * @author ywsing
 */
public class LibraryMain extends ExecutableProgram {

	private String inputDir = "/data/storey/lg";
	private List<String> fileExtensions = List.of("jpg");
	private String libraryDir = "/data/photographs & memories/library";
	private String tagsDir = "/data/photographs & memories/tags";

	private class FileInfo {
		private String md5;
		private Streamlet<String> tags;
	}

	public static void main(String[] args) {
		RunUtil.run(LibraryMain.class, args);
	}

	protected boolean run(String[] args) {
		var partition = FileUtil.findPaths(Paths.get(inputDir)) //
				.filter(path -> fileExtensions.contains(FileUtil.getFileExtension(path))) //
				.map2(path -> rethrow(() -> Files.size(path))) //
				.partition((path, size) -> 0 < size);

		// remove empty files
		partition.t1.sink((path, size) -> FileUtil.delete(path));

		// get all file information
		var path_fileInfos = partition.t0 //
				.map2((path, size) -> {
					var attrs = rethrow(() -> Files.readAttributes(path, BasicFileAttributes.class));

					var tags = Ints_ //
							.range(path.getNameCount()) //
							.map(i -> path.getName(i).toString()) //
							.cons(To.string(attrs.lastModifiedTime().toInstant())) //
							.collect();

					var fileInfo = new FileInfo();
					fileInfo.md5 = Md5Crypt.md5Crypt(rethrow(() -> Files.readAllBytes(path)));
					fileInfo.tags = tags;
					return fileInfo;
				});

		// construct file listing
		FileUtil.out(inputDir + ".listing").doPrintWriter(pw -> {
			for (var path_fileInfo : path_fileInfos)
				pw.println(path_fileInfo.t0 + path_fileInfo.t1.md5);
		});

		path_fileInfos //
				.map2((path, fileInfo) -> {

					// move file to library, by md5
					var path1 = Paths.get(libraryDir, fileInfo.md5.substring(0, 2), fileInfo.md5);
					FileUtil.mkdir(path1.getParent());
					rethrow(() -> Files.move(path, path1, StandardCopyOption.REPLACE_EXISTING));
					return fileInfo;
				}) //
				.concatMap((path, fileInfo) -> fileInfo.tags.map(tag -> {

					// add to tag indices
					var path1 = Paths.get(tagsDir, tag, fileInfo.md5);
					return rethrow(() -> {
						Files.newOutputStream(path1).close();
						return Pair.of(tag, fileInfo);
					});
				}));

		return true;
	}

}
