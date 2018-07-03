package suite.text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import suite.Defaults;
import suite.adt.pair.FixieArray;
import suite.adt.pair.Pair;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.util.Fail;
import suite.util.Rethrow;
import suite.util.Set_;
import suite.util.String_;
import suite.util.Util;

public interface Snapshot {

	public static Snapshot me = new Impl();

	public Map<String, List<Pair<Bytes, Bytes>>> diffDirs(Path path0, Path path1);

	public void patchDir(Path path, Map<String, List<Pair<Bytes, Bytes>>> map);

	public Map<String, List<Pair<Bytes, Bytes>>> merge( //
			Map<String, List<Pair<Bytes, Bytes>>> map0, //
			Map<String, List<Pair<Bytes, Bytes>>> map1);

	public List<Pair<Bytes, Bytes>> readPatch(InputStream is);

	public void writePatch(OutputStream os, List<Pair<Bytes, Bytes>> list) throws IOException;

}

class Impl implements Snapshot {

	private TextUtil textUtil = new TextUtil();

	public Map<String, List<Pair<Bytes, Bytes>>> diffDirs(Path path0, Path path1) {
		return diffMaps(readMap(path0), readMap(path1));
	}

	public Map<String, List<Pair<Bytes, Bytes>>> merge( //
			Map<String, List<Pair<Bytes, Bytes>>> map0, //
			Map<String, List<Pair<Bytes, Bytes>>> map1) {
		return Read //
				.from(Set_.union(map0.keySet(), map1.keySet())) //
				.map2(key -> textUtil.merge(map0.get(key), map1.get(key))) //
				.toMap();
	}

	public void patchDir(Path path0, Map<String, List<Pair<Bytes, Bytes>>> map) {
		var isFrom = false;

		if (Boolean.TRUE)
			for (var e : map.entrySet()) {
				var p = path0.resolve(e.getKey());
				var value = e.getValue();
				if (textUtil.isDiff(value)) {
					var data = textUtil.fromTo(value, isFrom);
					if (data != null)
						FileUtil.out(p).doWrite(os -> os.write(data.toArray()));
					else
						FileUtil.deleteIfExists(p);
				}
			}
		else {
			var p0 = diffMaps(fromTo(map, true), readMap(path0));
			var p1 = map;
			writeMap(path0, fromTo(merge(p0, p1), false));
		}
	}

	private Map<String, List<Pair<Bytes, Bytes>>> diffMaps(Map<String, Bytes> map0, Map<String, Bytes> map1) {
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

	public List<Pair<Bytes, Bytes>> readPatch(InputStream is) {
		var list = new ArrayList<Pair<Bytes, Bytes>>();
		String line;
		while (!String_.equals(line = Util.readLine(is), "EOF"))
			list.add(FixieArray.of(line.split(" ")).map((s0, s1) -> Rethrow.ex(() -> {
				var size0 = !String_.equals(s0, "N") ? Integer.valueOf(s0) : null;
				var size1 = !String_.equals(s1, "N") ? Integer.valueOf(s1) : null;
				var bs0 = size0 != null && is.read() == '<' ? readBlock(is, size0) : null;
				var bs1 = size1 != null && is.read() == '>' ? readBlock(is, size1) : null;
				return Pair.of(bs0, bs1);
			})));
		return list;
	}

	public void writePatch(OutputStream os, List<Pair<Bytes, Bytes>> list) throws IOException {
		for (var pair : list) {
			var t0 = pair.t0;
			var t1 = pair.t1;
			var size0 = t0 != null ? Integer.toString(t0.size()) : "N";
			var size1 = t1 != null ? Integer.toString(t1.size()) : "N";
			var line = size0 + " " + size1 + "\n";
			os.write(line.getBytes(Defaults.charset));
			if (t0 != null) {
				os.write('<');
				writeBlock(os, t0);
			}
			if (t1 != null) {
				os.write('>');
				writeBlock(os, t1);
			}
		}
		os.write("EOF\n".getBytes(Defaults.charset));
	}

	private Bytes readBlock(InputStream is, int size) throws IOException {
		var bs = new byte[size];
		var p = 0;
		while (p < size) {
			var c = is.read(bs, p, size - p);
			if (0 <= c)
				p += c;
			else
				return Fail.t();
		}
		is.read();
		return Bytes.of(bs);
	}

	private void writeBlock(OutputStream os, Bytes t0) throws IOException {
		os.write(t0.toArray());
		os.write("\n".getBytes(Defaults.charset));
	}

	private Map<String, Bytes> readMap(Path path) {
		return FileUtil //
				.findPaths(path) //
				.map2(Path::toString, p -> Bytes.of(FileUtil.in(p).readBytes())) //
				.toMap();
	}

	private void writeMap(Path path0, Map<String, Bytes> map) {
		for (var e : map.entrySet()) {
			var p = path0.resolve(e.getKey());
			var value = e.getValue();
			if (value != null)
				FileUtil.out(p).doWrite(os -> os.write(value.toArray()));
			else
				FileUtil.deleteIfExists(p);
		}
	}

	private Map<String, Bytes> fromTo(Map<String, List<Pair<Bytes, Bytes>>> diff, boolean isFrom) {
		return Read.from2(diff).mapValue(v -> textUtil.fromTo(v, isFrom)).toMap();
	}

}
