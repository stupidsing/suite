package suite.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.adt.pair.FixieArray;
import suite.adt.pair.Pair;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.Set_;
import suite.util.String_;
import suite.util.To;
import suite.util.Util;

public class Snapshot {

	private TextUtil textUtil = new TextUtil();

	public Map<String, List<Pair<Bytes, Bytes>>> diffDirs(String dir0, String dir1) {
		return diffMaps(readMap(dir0), readMap(dir1));
	}

	public void patchDir(String dir, Map<String, List<Pair<Bytes, Bytes>>> map) {
		var p0 = Paths.get(dir);
		var isFrom = false;

		for (var e : map.entrySet()) {
			var p = p0.resolve(e.getKey());
			var value = e.getValue();
			if (textUtil.isDiff(value)) {
				var data = textUtil.fromTo(value, isFrom);
				if (data != null)
					FileUtil.out(p).write(os -> os.write(data.toArray()));
				else
					Rethrow.ex(() -> Files.deleteIfExists(p));
			}
		}
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

	private List<Pair<Bytes, Bytes>> readPatch(InputStream is) {
		var list = new ArrayList<Pair<Bytes, Bytes>>();
		String line;
		while (!String_.equals(line = Util.readLine(is), "EOF"))
			list.add(FixieArray.of(line.split(" ")).map((s0, s1) -> Rethrow.ex(() -> {
				var size0 = !String_.equals(s0, "N") ? Integer.valueOf(s0) : null;
				var size1 = !String_.equals(s1, "N") ? Integer.valueOf(s1) : null;
				var bs0 = size0 != null && is.read() == '<' ? readBlock(is, size0) : null;
				var bs1 = size1 != null && is.read() == '>' ? readBlock(is, size1) : null;
				return Pair.of(Bytes.of(bs0), Bytes.of(bs1));
			})));
		return list;
	}

	private byte[] readBlock(InputStream is, int size) throws IOException {
		var bs0 = new byte[size];
		var p = 0;
		while (p < size) {
			var c = is.read(bs0, p, size - p);
			if (0 <= c)
				p += c;
			else
				return Fail.t();
		}
		is.read();
		return bs0;
	}

	private void writePatch(PrintWriter pw, List<Pair<Bytes, Bytes>> list) {
		for (var pair : list)
			pair.map((t0, t1) -> {
				var size0 = t0 != null ? Integer.toString(t0.size()) : "N";
				var size1 = t1 != null ? Integer.toString(t1.size()) : "N";
				var line = size0 + " " + size1;
				pw.println(line);
				if (t0 != null) {
					pw.print('<');
					pw.print(t0);
					pw.println();
				}
				if (t1 != null) {
					pw.print('>');
					pw.print(t1);
					pw.println();
				}
				return pw;
			});
		pw.println("EOF");
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
			var p = p0.resolve(e.getKey());
			var value = e.getValue();
			if (value != null)
				FileUtil.out(p).write(os -> os.write(value.toArray()));
			else
				Rethrow.ex(() -> Files.deleteIfExists(p));
		}
	}

	private Map<String, Bytes> getFiles(Map<String, List<Pair<Bytes, Bytes>>> diff, boolean isFrom) {
		return Read.from2(diff).mapValue(v -> textUtil.fromTo(v, isFrom)).toMap();
	}

}
