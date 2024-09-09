package suite.ansi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.sun.jna.Native;

import primal.Verbs.ReadString;
import primal.fp.Funs.Sink;

// mvn compile exec:java -Dexec.mainClass=suite.ansi.TermEditorMain
public class TermEditorMain {

	private LibcJna libc = (LibcJna) Native.load("c", LibcJna.class);

	public static void main(String[] args) {
		new TermEditorMain().run();
	}

	private static class Handle {
		private Map<Integer, Handle> map;
		private Sink<Integer> sink;

		private Handle(Sink<Integer> sink) {
			this(Collections.emptyMap(), sink);
		}

		private Handle(Map<Integer, Handle> map, Sink<Integer> sink) {
			this.map = map;
			this.sink = sink;
		}
	}

	private void run() {
		try (var termios = new Termios(libc)) {
			var text = ReadString.from("src/main/java/suite/ansi/TermEditorMain.java");
			var lines = new HashMap<Integer, String>();
			var nLines = 0;

			for (var line : text.split("\n"))
				lines.put(nLines++, line);

			var nLines_ = nLines;

			var size = termios.getSize();
			var nCols = size.t1;
			var nRows = size.t0;

			termios.clear();

			var o = new Object() {
				private int basex = 0, basey = 0;
				private int cursorx = 0, cursory = 0;

				private void moveCursor(int dx, int dy) {
					int x1 = Math.max(0, Math.min(132, cursorx + dx));
					int y1 = Math.max(0, Math.min(nLines_, cursory + dy));
					if (x1 < basex) {
						basex = x1;
						redraw();
					}
					if (basex < x1 - (nCols - 1)) {
						basex = x1 - (nCols - 1);
						redraw();
					}
					if (y1 < basey) {
						basey = y1;
						redraw();
					}
					if (basey < y1 - (nRows - 1)) {
						basey = y1 - (nRows - 1);
						redraw();
					}
					gotoCursor(x1, y1);
				}

				private void redraw() {
					for (var r = 0; r < nRows; r++)
						redrawRow(r);

					setCursor();
				}

				private void redrawRow(int r) {
					var y_ = r + basey;
					var line = y_ < lines.size() ? lines.get(y_) : "";

					termios.gotoxy(0, r);

					for (var c = 0; c < nCols; c++) {
						var x_ = c + basex;
						var ch = 0 <= x_ && x_ < line.length() ? line.charAt(x_) : ' ';
						termios.putc(ch != 9 ? ch : ' ');
					}
				}

				private void gotoCursor(int cx, int cy) {
					cursorx = cx;
					cursory = cy;
					setCursor();
				}

				private void setCursor() {
					var x = cursorx - basex;
					var y = cursory - basey;
					if (0 <= x && x < nCols && 0 <= y && y < nRows)
						termios.gotoxy(x, y);
				}
			};

			var c = new Object() {
				private void delete() {
					var line = lines.get(o.cursory);
					lines.put(o.cursory, line.substring(0, o.cursorx) + line.substring(o.cursorx + 1));
					o.redrawRow(o.cursory - o.basey);
					o.setCursor();
				}

				private void end() {
					var line = lines.get(o.cursory);
					o.gotoCursor(line != null ? line.length() : 0, o.cursory);
				}

				private void moveCursor(int dx, int dy) {
					o.moveCursor(dx, dy);
				}

				private void insert(char ch) {
					var line = lines.get(o.cursory);
					lines.put(o.cursory, line.substring(0, o.cursorx) + ch + line.substring(o.cursorx));
					o.redrawRow(o.cursory - o.basey);
					o.moveCursor(1, 0);
				}

				private void pageDown() {
					if (o.basey == o.cursory) {
						o.cursory = o.basey + (nRows - 1);
						o.setCursor();
					} else {
						if (o.basey + (nRows - 1) == o.cursory) {
							o.basey = Math.min(nLines_, o.basey + nRows);
							o.cursory = o.basey + (nRows - 1);
						} else
							o.basey = o.cursory;
						o.redraw();
					}

				}

				private void pageUp() {
					if (o.basey + (nRows - 1) == o.cursory) {
						o.cursory = o.basey;
						o.setCursor();
					} else {
						if (o.basey == o.cursory) {
							o.basey = Math.max(0, o.basey - nRows);
							o.cursory = o.basey;
						} else
							o.basey = Math.max(0, o.cursory - (nRows - 1));
						o.redraw();
					}

				}
			};
			o.redraw();

			Handle handle = //
					new Handle(Map.ofEntries( //
							Map.entry(27, new Handle(Map.ofEntries( //
									Map.entry(91, new Handle(Map.ofEntries( //
											Map.entry(51, new Handle(Map.ofEntries( //
													Map.entry(126, new Handle(Map.ofEntries( //
													), ch -> c.delete())) //
											), ch -> {
											})), //
											Map.entry(53, new Handle(Map.ofEntries( //
													Map.entry(126, new Handle(ch -> c.pageUp())) //
											), ch -> {
											})), //
											Map.entry(54, new Handle(Map.ofEntries( //
													Map.entry(126, new Handle(ch -> c.pageDown())) //
											), ch -> {
											})), //
											Map.entry(65, new Handle(ch -> c.moveCursor(0, -1))), // up
											Map.entry(66, new Handle(ch -> c.moveCursor(0, +1))), // down
											Map.entry(67, new Handle(ch -> c.moveCursor(+1, 0))), // right
											Map.entry(68, new Handle(ch -> c.moveCursor(-1, 0))), // left
											Map.entry(70, new Handle(ch -> c.end())), //
											Map.entry(72, new Handle(ch -> o.gotoCursor(0, o.cursory))) // home
									), ch -> {
									})) //
							), ch -> {
							})), //
							Map.entry(127, new Handle(ch -> { // backspace
								if (0 < o.cursorx) {
									var line = lines.get(o.cursory);
									lines.put(o.cursory, line.substring(0, o.cursorx - 1) + line.substring(o.cursorx));
									o.redrawRow(o.cursory - o.basey);
									o.moveCursor(-1, 0);
								}
							})) //
					), ch -> {
						if (32 <= ch && ch < 127)
							c.insert((char) (int) ch);
					});

			Handle handle_ = handle;

			while (true) {
				var ch = libc.getchar();

				handle_.sink.f(ch);

				handle_ = handle_.map.get(ch);

				if (handle_ == null)
					handle_ = handle;
				else if (handle_.map.isEmpty()) {
					handle_.sink.f(ch);
					handle_ = handle;
				}

				if (ch == 'q')
					break;
				else
					;
			}
		}
	}

}
