package org.suite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.doer.Generalizer;
import org.suite.doer.TermParser;
import org.suite.doer.Prover;
import org.suite.doer.TermParser.Operator;
import org.suite.doer.Prover.Backtracks;
import org.suite.kb.RuleSet;
import org.suite.node.Node;
import org.suite.node.Station;
import org.suite.node.Tree;
import org.util.LogUtil;
import org.util.Util;

/**
 * Logic interpreter. Likes Prolog.
 * 
 * @author ywsing
 */
public class Main {

	public static void main(String args[]) {
		try {
			new Main().run();
		} catch (Throwable ex) {
			log.error(Main.class, ex);
		}
	}

	public enum InputType {
		FACT, QUERY, ELABORATE
	};

	public void run() throws IOException {
		LogUtil.initLog4j();

		RuleSet rs = new RuleSet();
		Prover prover = new Prover(rs);
		importAuto(rs);

		InputStreamReader is = new InputStreamReader(System.in);
		BufferedReader br = new BufferedReader(is);

		quit: while (true) {
			StringBuilder sb = new StringBuilder();
			String line;

			do {

				System.out.print((sb.length() == 0) ? "=> " : "   ");

				line = br.readLine();
				if (line == null)
					break quit;

				sb.append(line);
			} while (!line.isEmpty() && !line.endsWith("#"));

			String input = sb.toString();
			InputType type;

			if (input.startsWith("?")) {
				type = InputType.QUERY;
				input = input.substring(1);
			} else if (input.startsWith("/")) {
				type = InputType.ELABORATE;
				input = input.substring(1);
			} else
				type = InputType.FACT;

			if (input.endsWith("#"))
				input = input.substring(0, input.length() - 1);

			final Generalizer generalizer = new Generalizer();
			final int count[] = { 0 };
			Node node = new TermParser().parse(input.trim());
			node = generalizer.generalize(node);

			switch (type) {
			case FACT:
				rs.addRule(node);
				break;
			case QUERY:
				System.out.println(prover.prove(node) ? "Yes\n" : "No\n");
				break;
			case ELABORATE:
				Node elab = new Station() {
					public boolean run(Backtracks backtracks) {
						System.out.println(generalizer.dumpVariables());
						count[0]++;
						return false;
					}
				};

				prover.prove(new Tree(Operator.AND___, node, elab));

				if (count[0] == 1)
					System.out.println(count[0] + " solution\n");
				else
					System.out.println(count[0] + " solutions\n");
			}
		}
	}

	private void importAuto(RuleSet rs) throws IOException {
		ClassLoader cl = getClass().getClassLoader();
		InputStream is = cl.getResourceAsStream("auto.sl");
		rs.importFrom(new TermParser().parse(is));
	}

	private static Log log = LogFactory.getLog(Util.currentClass());

}
