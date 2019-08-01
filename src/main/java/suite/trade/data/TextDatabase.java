package suite.trade.data;

import static java.lang.Math.max;
import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import primal.String_;
import primal.Verbs.Compare;
import primal.Verbs.Equals;
import primal.Verbs.Sleep;
import suite.cfg.HomeDir;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.primitive.Bytes_;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.To;

public class TextDatabase {

	private Path path = HomeDir.resolve("quote-database.csv");
	private TreeSet<Datum> data = new TreeSet<>();
	private long lastSaveTime;
	private long saveTime;
	private volatile Thread saveThread;

	public TextDatabase(Path path) {
		this.path = path;
		load();
	}

	public void join() {
		var thread = saveThread;
		ex(() -> {
			if (thread != null)
				thread.join();
			return thread;
		});
	}

	public synchronized SortedSet<Datum> range(Datum start, Datum end) {
		return data.subSet(start, end);
	}

	public synchronized void merge(Streamlet<Datum> data_) {
		for (var datum : data_)
			merge(datum);

		// 30 seconds between every save
		saveTime = max(lastSaveTime + 30 * 1000l, System.currentTimeMillis());

		if (saveThread == null) {
			saveThread = new Thread(() -> {
				long now;
				while ((now = System.currentTimeMillis()) < saveTime)
					Sleep.quietly(saveTime - now);
				synchronized (TextDatabase.this) {
					save();
					saveThread = null;
				}
			});
			saveThread.setDaemon(false);
			saveThread.start();
			lastSaveTime = saveTime;
		}
	}

	private synchronized void load() {
		if (Files.exists(path))
			merge(path);
	}

	private synchronized void save() {
		var puller = Read //
				.from(data) //
				.map(this::toBytes) //
				.collect(Bytes_::buffer);

		FileUtil.out(path).doWrite(os -> Bytes_.copy(puller, os::write));
	}

	private void merge(Path path) {
		Read.bytes(path).collect(As::lines).map(this::toDatum).forEach(this::merge);
	}

	private void merge(Datum datum) {
		var datum0 = data.ceiling(datum);
		if (datum0 == null || !Equals.ab(datum, datum0))
			data.add(datum);
		else if (!Equals.ab(datum0.value, datum.value))
			fail("value mismatch for key " + datum.key + ": " + datum0.value + " != " + datum.value);
	}

	private Datum toDatum(String line) {
		var array = line.split(",");
		return datum(array[0], array[1]);
	}

	private Bytes toBytes(Datum datum) {
		return To.bytes(datum.key + "," + datum.value + "\n");
	}

	public Datum datum(String key, String value) {
		return new Datum(key, value);
	}

	public class Datum implements Comparable<Datum> {
		public final String key;
		public final String value;

		private Datum(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public int compareTo(Datum other) {
			return Compare.objects(key, other.key);
		}

		public boolean equals(Object object) {
			if (object.getClass() == Datum.class) {
				var other = (Datum) object;
				return String_.equals(key, other.key);
			} else
				return false;
		}

		public int hashCode() {
			return Objects.hashCode(key);
		}
	}

}
