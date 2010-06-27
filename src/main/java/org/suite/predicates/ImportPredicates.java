package org.suite.predicates;

import java.io.FileInputStream;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.suite.doer.Formatter;
import org.suite.doer.Prover;
import org.suite.doer.TermParser;
import org.suite.node.Node;
import org.suite.predicates.SystemPredicates.SystemPredicate;

public class ImportPredicates {

	public static class Import implements SystemPredicate {
		public boolean prove(Prover prover, Node ps) {
			try {
				Node params[] = Predicate.getParameters(ps, 1);
				String filename = Formatter.display(params[1]);
				InputStream is = new FileInputStream(filename);
				prover.getRuleSet().importFrom(new TermParser().parse(is));
				return true;
			} catch (Exception ex) {
				log.info(this, ex);
				return false;
			}
		}

		private Log log = LogFactory.getLog(getClass());
	}

}
