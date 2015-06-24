package suite.lp.sewing;

import suite.lp.sewing.impl.SewingExpressionImpl.Evaluate;
import suite.node.Node;

public interface SewingExpression {

	public Evaluate compile(Node node);

}
