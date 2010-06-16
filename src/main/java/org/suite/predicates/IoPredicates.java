package org.suite.predicates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.doer.Formatter;
import org.suite.doer.Prover;
import org.suite.node.Node;
import org.suite.node.Str;
import org.suite.predicates.SystemPredicates.SystemPredicate;
import org.util.Util;

public class IoPredicates {

	public static class Dump implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			System.out.print(Formatter.dump(ps));
			return true;
		}
	}

	public static class Exec implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			if (ps instanceof Str)
				try {
					String cmd = ((Str) ps).getValue();
					return Runtime.getRuntime().exec(cmd).waitFor() == 0;
				} catch (Exception ex) { // IOException or InterruptedException
					log.error(this, ex);
				}
			return false;
		}

		private static Log log = LogFactory.getLog(Util.currentClass());
	}

	public static class Nl implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			System.out.println();
			return true;
		}
	}

	public static class Write implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			System.out.print(Formatter.display(ps));
			return true;
		}
	}

}
