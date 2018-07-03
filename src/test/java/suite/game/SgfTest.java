package suite.game;

import org.junit.Test;

public class SgfTest {

	@Test
	public void testFile() {
		new Sgf().fromFile("/tmp/sgf");
	}

	@Test
	public void testString() {
		new Sgf().from("" //
				+ "(;FF[4]GM[1]SZ[19]\n" //
				+ "\n" //
				+ "GN[Copyright goproblems.com]\n" //
				+ "PB[Black]\n" //
				+ "HA[0]\n" //
				+ "PW[White]\n" //
				+ "KM[5.5]\n" //
				+ "DT[1999-07-21]\n" //
				+ "TM[1800]\n" //
				+ "RU[Japanese]\n" //
				+ "\n" //
				+ ";AW[bb][cb][cc][cd][de][df][cg][ch][dh][ai][bi][ci]\n" //
				+ "AB[ba][ab][ac][bc][bd][be][cf][bg][bh]\n" //
				+ "C[Black to play and live.]\n" //
				+ "(;B[af];W[ah]\n" //
				+ "(;B[ce];W[ag]C[only one eye this way])\n" //
				+ "(;B[ag];W[ce]))\n" //
				+ "(;B[ah];W[af]\n" //
				+ "(;B[ae];W[bf];B[ag];W[bf]\n" //
				+ "(;B[af];W[ce]C[oops! you can't take this stone])\n" //
				+ "(;B[ce];W[af];B[bg]C[RIGHT black plays under the stones and lives]))\n" //
				+ "(;B[bf];W[ae]))\n" //
				+ "(;B[ae];W[ag]))\n");
	}

}
