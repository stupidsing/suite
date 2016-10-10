package suite.ansi;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;

import suite.adt.Pair;
import suite.adt.Trie;
import suite.streamlet.Outlet;
import suite.streamlet.Reactive;
import suite.streamlet.Reactive.Redirector;
import suite.streamlet.Read;
import suite.util.FunUtil.Source;

public class Keyboard implements Closeable {

	private LibcJna libc = (LibcJna) Native.loadLibrary("c", LibcJna.class);
	private Termios termios = new Termios(libc);
	private Trie<Integer, VK> trie;

	public enum VK {
		UP___, //
		DOWN_, //
		LEFT_, //
		RIGHT, //
	}

	public Keyboard() {
		List<Pair<List<Integer>, VK>> pairs = new ArrayList<>();
		pairs.add(Pair.of(Arrays.asList(27, 91, 65), VK.UP___));
		pairs.add(Pair.of(Arrays.asList(27, 91, 66), VK.DOWN_));
		pairs.add(Pair.of(Arrays.asList(27, 91, 68), VK.LEFT_));
		pairs.add(Pair.of(Arrays.asList(27, 91, 67), VK.RIGHT));

		trie = new Trie<>();

		for (Pair<List<Integer>, VK> pair : pairs)
			trie.add(pair.t0, pair.t1);
	}

	@Override
	public void close() {
		termios.close();
	}

	public Outlet<Pair<VK, Character>> keys() {
		Source<Character> source0 = () -> {
			int ch = libc.getchar();
			return 0 <= ch ? (char) ch : null;
		};

		Outlet<Pair<VK, Character>> keys = Reactive.from(source0) //
				.redirect(new Redirector<Character, Pair<VK, Character>>() {
					private List<Character> chs = new ArrayList<>();
					private Trie<Integer, VK> t = trie;

					public void accept(Character ch_, Reactive<Pair<VK, Character>> reactive) {
						if (ch_ != null) {
							chs.add(ch_);
							VK vk;

							if ((t = t.getMap().get(ch_)) != null)
								if ((vk = t.getValue()) != null) {
									reactive.fire(Pair.of(vk, null));
									reset();
								} else
									;
							else
								flush(reactive);
						} else
							flush(reactive);
					}

					private void flush(Reactive<Pair<VK, Character>> reactive) {
						Read.from(chs).sink(ch -> reactive.fire(Pair.of(null, ch)));
						reset();
					}

					private void reset() {
						chs.clear();
						t = trie;
					}
				}) //
				.outlet();
		return keys;
	}

}
