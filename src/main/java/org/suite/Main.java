package org.suite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.doer.Generalizer;
import org.suite.doer.Prover;
import org.suite.doer.Prover.Backtracks;
import org.suite.doer.TermParser;
import org.suite.doer.TermParser.TermOp;
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

		quit: while (true)
			try {
				StringBuilder sb = new StringBuilder();
				String line;

				do {
					System.out.print((sb.length() == 0) ? "=> " : "   ");

					if ((line = br.readLine()) != null)
						sb.append(line + "\n");
					else
						break quit;
				} while (!line.isEmpty() && !line.endsWith("#"));

				String input = sb.toString();
				InputType type;

				if (Util.isBlank(input))
					continue;

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

				final int count[] = { 0 };
				Node node = new TermParser().parse(input.trim());

				final Generalizer generalizer = new Generalizer();
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
							String dump = generalizer.dumpVariables();
							if (!dump.isEmpty())
								System.out.println(dump);

							count[0]++;
							return false;
						}
					};

					prover.prove(new Tree(TermOp.AND___, node, elab));

					if (count[0] == 1)
						System.out.println(count[0] + " solution\n");
					else
						System.out.println(count[0] + " solutions\n");
				}
			} catch (Throwable ex) {
				log.error(Main.class, ex);
			}
	}

	private void importAuto(RuleSet rs) throws IOException {
		SuiteUtil.importResource(rs, "auto.sl");
	}

	private static Log log = LogFactory.getLog(Util.currentClass());

}
