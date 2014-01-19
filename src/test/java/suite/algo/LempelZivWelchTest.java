package suite.algo;

import static org.junit.Assert.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suite.net.Bytes.BytesBuilder;
import suite.util.FileUtil;
import suite.util.FunUtil.Pipe;
import suite.util.FunUtil.Source;
import suite.util.To;

public class LempelZivWelchTest {

	@Test
	public void test0() {
		String s = "abcabcab";
		assertEquals(s, doTest(s));
	}

	@Test
	public void test1() {
		String s = "abababababababababababababababab";
		assertEquals(s, doTest(s));
	}

	@Test
	public void test2() throws IOException {
		String s = To.string(new FileInputStream("src/main/java/suite/algo/LempelZivWelch.java"));
		assertEquals(s, doTest(s));
	}

	private String doTest(String s0) {
		final byte bs[] = s0.getBytes(FileUtil.charset);

		Source<Byte> inputSource = new Source<Byte>() {
			private int index;

			public Byte source() {
				return index < bs.length ? bs[index++] : null;
			}
		};

		Pipe<Integer> pipe0 = new Pipe<>();
		Pipe<Byte> pipe1 = new Pipe<>();

		LempelZivWelch<Byte> lzw = new LempelZivWelch<>(alBytes());
		lzw.encode(inputSource, pipe0.sink());
		lzw.decode(pipe0.source(), pipe1.sink());

		BytesBuilder bb = new BytesBuilder();
		Source<Byte> outputSource = pipe1.source();
		Byte b;

		while ((b = outputSource.source()) != null)
			bb.append(b);

		return new String(bb.toBytes().getBytes(), FileUtil.charset);
	}

	private List<Byte> alBytes() {
		List<Byte> bytes = new ArrayList<>();
		byte b = 0;

		do
			bytes.add(b);
		while (++b != 0);
		return bytes;
	}

}
