package suite.btree;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.Random;

import org.junit.Before;
import org.junit.Test;

import suite.btree.impl.B_TreeBuilder;
import suite.cfg.Defaults;
import suite.file.impl.JournalledFileFactory;
import suite.node.util.Singleton;
import suite.object.Object_;
import suite.primitive.Ints_;
import suite.sample.Profiler;
import suite.serialize.Serialize;
import suite.util.To;

public class B_TreeTest {

	private static int nKeys = 1024;

	private Comparator<Integer> cmp = Object_::compare;
	private Random random = new Random();
	private Serialize ser = Singleton.me.serialize;
	private int[] keys;

	@Before
	public void before() {
		keys = Ints_.toArray(nKeys, i -> i);
	}

	@Test
	public void testDump() throws IOException {
		var pageSize = 4096;
		var path = Defaults.tmp("b_tree-dump");

		Files.deleteIfExists(path);
		var builder = new B_TreeBuilder<>(ser.int_, ser.string(16));

		try (var jpf = JournalledFileFactory.open(path, pageSize);
				var b_tree = builder.build(jpf, pageSize, cmp)) {
			b_tree.create();

			for (var i = 0; i < 32; i++)
				b_tree.put(i, Integer.toString(i));

			b_tree.dump(System.out);

			System.out.println(To.list(b_tree.keys(3, 10)));
			jpf.commit();
		}
	}

	@Test
	public void testAccess() throws IOException {
		var pageSize = 4096;
		var path = Defaults.tmp("b_tree-file");

		Files.deleteIfExists(path);
		var builder = new B_TreeBuilder<>(ser.int_, ser.string(16));

		shuffleNumbers();

		try (var jpf = JournalledFileFactory.open(path, pageSize);
				var b_tree = builder.build(jpf, pageSize, cmp)) {
			b_tree.create();
			testStep0(b_tree);
			jpf.commit();
			jpf.sync();
		}

		shuffleNumbers();

		try (var jpf = JournalledFileFactory.open(path, pageSize);
				var b_tree = builder.build(jpf, pageSize, cmp)) {
			testStep1(b_tree);
			jpf.commit();
			jpf.sync();
		}

		try (var jpf = JournalledFileFactory.open(path, pageSize);
				var b_tree = builder.build(jpf, pageSize, cmp)) {
			testStep2(b_tree);
			jpf.commit();
			jpf.sync();
			jpf.applyJournal();
		}
	}

	@Test
	public void testInsertPerformance() throws IOException {
		var nKeys = 16384;
		var pageSize = 4096;
		var path = Defaults.tmp("b_tree-file");

		keys = Ints_.toArray(nKeys, i -> i);

		for (var i = 0; i < nKeys; i++) {
			var j = random.nextInt(nKeys);
			var temp = keys[i];
			keys[i] = keys[j];
			keys[j] = temp;
		}

		Files.deleteIfExists(path);
		var builder = new B_TreeBuilder<>(ser.int_, ser.bytes(64));

		try (var jpf = JournalledFileFactory.open(path, pageSize); var b_tree = builder.build(jpf, 9999, cmp)) {
			new Profiler().profile(() -> {
				b_tree.create();
				for (var i = 0; i < nKeys; i++) {
					var key = keys[i];
					b_tree.put(key, To.bytes(Integer.toString(key)));
				}
				jpf.commit();
				jpf.sync();
			});
		}
	}

	private void testStep0(B_Tree<Integer, String> b_tree) {
		for (var i = 0; i < nKeys; i++)
			b_tree.put(keys[i], Integer.toString(keys[i]));

		for (var i = 0; i < nKeys; i++)
			assertEquals(Integer.toString(i), b_tree.get(i));

		assertEquals(nKeys / 2, To.list(b_tree.keys(0, nKeys / 2)).size());
		var count = 0;

		for (var e : b_tree.range(0, nKeys)) {
			var key = count++;
			assertEquals(key, e.k.intValue());
			assertEquals(Integer.toString(key), e.v);
		}
	}

	private void testStep1(B_Tree<Integer, String> b_tree) {
		for (var i = 0; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (var i = 0; i < nKeys; i += 2)
			assertNull(b_tree.get(keys[i]));
	}

	private void testStep2(B_Tree<Integer, String> b_tree) {
		for (var i = 1; i < nKeys; i += 2)
			b_tree.remove(keys[i]);

		b_tree.dump(System.out);

		for (var i = 0; i < nKeys; i++)
			assertNull(b_tree.get(keys[i]));
	}

	private void shuffleNumbers() {
		for (var i = 0; i < nKeys; i++) {
			var j = random.nextInt(nKeys);
			var temp = keys[i];
			keys[i] = keys[j];
			keys[j] = temp;
		}
	}

}
