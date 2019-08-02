package suite.text;

import static primal.statics.Rethrow.ex;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import primal.Nouns.Utf8;
import primal.Verbs.Equals;
import primal.Verbs.ReadFile;
import primal.Verbs.ReadLine;
import primal.Verbs.Union;
import primal.Verbs.WriteFile;
import primal.adt.FixieArray;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.Read;
import suite.text.TextUtil.BytesPair;

public interface Snapshot {

	public static Snapshot me = new Impl();

	public Map<String, List<BytesPair>> diffDirs(Path path0, Path path1);

	public void patchDir(Path path, Map<String, List<BytesPair>> map);

	public Map<String, List<BytesPair>> merge( //
			Map<String, List<BytesPair>> map0, //
			Map<String, List<BytesPair>> map1);

	public List<BytesPair> readPatch(InputStream is);

	public void writePatch(OutputStream os, List<BytesPair> list) throws IOException;

}

class Impl implements Snapshot {

	private TextUtil textUtil = new TextUtil();

	public Map<String, List<BytesPair>> diffDirs(Path path0, Path path1) {
		return diffMaps(readMap(path0), readMap(path1));
	}

	public void patchDir(Path path0, Map<String, List<BytesPair>> map) {
		var isFrom = false;

		if (Boolean.TRUE)
			for (var e : map.entrySet()) {
				var p = path0.resolve(e.getKey());
				var value = e.getValue();
				if (textUtil.isDiff(value)) {
					var data = textUtil.fromTo(value, isFrom);
					if (data != null)
						WriteFile.to(p).doWrite(os -> os.write(data.toArray()));
					else
						FileUtil.deleteIfExists(p);
				}
			}
		else {
			var diff0 = diffMaps(fromTo(map, true), readMap(path0));
			var diff1 = map;
			var merged = merge(diff0, diff1);
			writeMap(path0, fromTo(merged, false));
		}
	}

	public Map<String, List<BytesPair>> merge( //
			Map<String, List<BytesPair>> map0, //
			Map<String, List<BytesPair>> map1) {
		return Read //
				.from(Union.of(map0.keySet(), map1.keySet())) //
				.map2(key -> textUtil.merge(map0.get(key), map1.get(key))) //
				.toMap();
	}

	private Map<String, List<BytesPair>> diffMaps(Map<String, Bytes> map0, Map<String, Bytes> map1) {
		var keys = Union.of(map0.keySet(), map1.keySet());
		var diffMap = new HashMap<String, List<BytesPair>>();

		for (var key : keys) {
			var bytes0 = map0.get(key);
			var bytes1 = map1.get(key);

			if (bytes0 == null || bytes1 == null)
				diffMap.put(key, List.of(new BytesPair(bytes0, bytes1)));
			else {
				var diff = textUtil.diff(bytes0, bytes1);
				if (textUtil.isDiff(diff))
					diffMap.put(key, diff);
			}
		}

		return diffMap;
	}

	public List<BytesPair> readPatch(InputStream is) {
		var list = new ArrayList<BytesPair>();
		String line;
		while (!Equals.string(line = ReadLine.from(is), "EOF"))
			list.add(FixieArray.of(line.split(" ")).map((f, s0, s1) -> ex(() -> {
				var size0 = !Equals.string(s0, "N") ? Integer.valueOf(s0) : null;
				var size1 = !Equals.string(s1, "N") ? Integer.valueOf(s1) : null;
				Bytes bs0, bs1;
				if (Equals.string("!", f)) {
					bs0 = readBlock(is, size0, '<');
					bs1 = readBlock(is, size1, '>');
				} else
					bs0 = bs1 = readBlock(is, size0, '=');
				return new BytesPair(bs0, bs1);
			})));
		return list;
	}

	public void writePatch(OutputStream os, List<BytesPair> list) {
		ex(() -> {
			for (var pair : list) {
				var bs0 = pair.t0;
				var bs1 = pair.t1;
				var size0 = bs0 != null ? Integer.toString(bs0.size()) : "N";
				var size1 = bs1 != null ? Integer.toString(bs1.size()) : "N";
				var isDiff = bs0 != bs1;
				var line = (isDiff ? "!" : "=") + " " + size0 + " " + size1 + "\n";
				os.write(line.getBytes(Utf8.charset));
				if (isDiff) {
					writeBlock(os, '<', bs0);
					writeBlock(os, '>', bs1);
				} else
					writeBlock(os, '=', bs0);
			}
			os.write("EOF\n".getBytes(Utf8.charset));
			return list;
		});
	}

	private Bytes readBlock(InputStream is, Integer size, char ch) throws IOException {
		return size != null && is.read() == ch ? readBlock(is, size) : null;
	}

	private Bytes readBlock(InputStream is, int size) throws IOException {
		var bs = new byte[size];
		is.readNBytes(bs, 0, size);
		is.read();
		return Bytes.of(bs);
	}

	private void writeBlock(OutputStream os, char ch, Bytes bs) throws IOException {
		if (bs != null) {
			os.write(ch);
			os.write(bs.toArray());
			os.write("\n".getBytes(Utf8.charset));
		}
	}

	private Map<String, Bytes> readMap(Path path) {
		return FileUtil //
				.findPaths(path) //
				.map2(Path::toString, p -> Bytes.of(ReadFile.from(p).readBytes())) //
				.toMap();
	}

	private void writeMap(Path path0, Map<String, Bytes> map) {
		for (var e : map.entrySet()) {
			var p = path0.resolve(e.getKey());
			var value = e.getValue();
			if (value != null)
				WriteFile.to(p).doWrite(os -> os.write(value.toArray()));
			else
				FileUtil.deleteIfExists(p);
		}
	}

	private Map<String, Bytes> fromTo(Map<String, List<BytesPair>> diff, boolean isFrom) {
		return Read.from2(diff).mapValue(v -> textUtil.fromTo(v, isFrom)).toMap();
	}

}
