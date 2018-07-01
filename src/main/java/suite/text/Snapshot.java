package suite.text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.Pair;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.util.Rethrow;
import suite.util.Set_;
import suite.util.To;

public class Snapshot {

	private TextUtil textUtil = new TextUtil();

	public Map<String, List<Pair<Bytes, Bytes>>> diffDirs(String dir0, String dir1) {
		return diffMaps(readMap(dir0), readMap(dir1));
	}

	private Map<String, Bytes> getFiles(Map<String, List<Pair<Bytes, Bytes>>> diff, boolean isFrom) {
		var map = new HashMap<String, Bytes>();
		for (var e : diff.entrySet())
			map.put(e.getKey(), textUtil.fromTo(e.getValue(), isFrom));
		return map;
	}

	public Map<String, List<Pair<Bytes, Bytes>>> diffMaps(Map<String, Bytes> map0, Map<String, Bytes> map1) {
		var keys = Set_.union(map0.keySet(), map1.keySet());
		var diffMap = new HashMap<String, List<Pair<Bytes, Bytes>>>();

		for (var key : keys) {
			var bytes0 = map0.get(key);
			var bytes1 = map1.get(key);

			if (bytes0 == null || bytes1 == null)
				diffMap.put(key, List.of(Pair.of(bytes0, bytes1)));
			else {
				var diff = textUtil.diff(bytes0, bytes1);
				if (textUtil.isDiff(diff))
					diffMap.put(key, diff);
			}
		}

		return diffMap;
	}

	private Map<String, Bytes> readMap(String dir) {
		return FileUtil //
				.findPaths(Paths.get(dir)) //
				.map2(Path::toString, path -> To.bytes(Rethrow.ex(() -> Files.newInputStream(path)))) //
				.toMap();
	}

	private void writeMap(String dir, Map<String, Bytes> map) {
		var p0 = Paths.get(dir);
		for (var e : map.entrySet()) {
			var key = e.getKey();
			var value = e.getValue();
			if (value != null)
				FileUtil.out(p0.resolve(key)).write(os -> os.write(value.toArray()));
			else
				Rethrow.ex(() -> Files.deleteIfExists(p0.resolve(key)));
		}
	}

	private boolean isCreate(List<Pair<Bytes, Bytes>> e) {
		return e.size() == 1 && e.iterator().next().t0 == null;
	}

	private boolean isDelete(List<Pair<Bytes, Bytes>> e) {
		return e.size() == 1 && e.iterator().next().t0 == null;
	}

}
