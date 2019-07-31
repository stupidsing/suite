package suite.game;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntPredicate;

import suite.adt.pair.Pair;
import suite.os.FileUtil;

// curl http://www.flygo.net/mjxj/WeiQiTianDi/dlxs_sdl.sgf | iconv -c -f CN-GB -t UTF-8
public class Sgf {

	public static class PosPair<T> {
		public int pos;
		public T t;
	}

	public class Node {
		public List<List<Pair<String, List<String>>>> commandsList = new ArrayList<>();
		public List<Node> variations = new ArrayList<>();
	}

	public Node fromFile(String filename) {
		return from(FileUtil.read(filename));
	}

	public Node from(String in) {
		return in.charAt(0) == '(' ? readNode(in, 1).t : fail();
	}

	private PosPair<Node> readNode(String in, int pos) {
		var node = new Node();

		while (pos < in.length())
			switch (in.charAt(pos++)) {
			case '\n':
			case '\r':
				break;
			case ';':
				var ipCommands = readCommands(in, pos);
				pos = ipCommands.pos;
				node.commandsList.add(ipCommands.t);
				break;
			case '(':
				var ipNode = readNode(in, pos);
				pos = ipNode.pos;
				node.variations.add(ipNode.t);
				break;
			case ')':
				return posPair(pos, node);
			default:
				fail();
			}

		return fail("unexpected end of input");
	}

	private PosPair<List<Pair<String, List<String>>>> readCommands(String in, int pos) {
		var commands = new ArrayList<Pair<String, List<String>>>();

		while (pos < in.length())
			switch (in.charAt(pos)) {
			case '\n':
			case '\r':
				pos++;
				break;
			case ';':
			case '(':
			case ')':
				return posPair(pos, commands);
			default:
				var ipCommand = readCommand(in, pos);
				pos = ipCommand.pos;
				commands.add(ipCommand.t);
			}

		return fail("unexpected end of input");
	}

	private PosPair<Pair<String, List<String>>> readCommand(String in, int pos) {
		var ids = new ArrayList<String>();
		var ipId = readIf(in, pos, ch -> Character.isAlphabetic(ch) || Character.isDigit(ch));
		pos = ipId.pos;

		while (pos < in.length())
			switch (in.charAt(pos)) {
			case '\n':
			case '\r':
				pos++;
				break;
			case '[':
				var ip = readIf(in, pos + 1, ch -> ch != ']');
				pos = ip.pos + 1;
				ids.add(ip.t);
				break;
			default:
				return posPair(pos, Pair.of(ipId.t, ids));
			}

		return fail("unexpected end of input");
	}

	private PosPair<String> readIf(String in, int pos, IntPredicate predicate) {
		var pos0 = pos;

		while (pos0 < in.length() && Character.isWhitespace(in.charAt(pos0)))
			pos0++;

		var pos1 = pos0;

		while (pos1 < in.length())
			if (predicate.test(in.charAt(pos1)))
				pos1++;
			else
				return posPair(pos1, in.substring(pos0, pos1));

		return fail("unexpected end of input");
	}

	public static <T> PosPair<T> posPair(int pos, T t) {
		var intPair = new PosPair<T>();
		intPair.pos = pos;
		intPair.t = t;
		return intPair;
	}

}
