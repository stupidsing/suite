package suite.node;

import suite.util.Util;

public class Data<T> extends Node {

	private T data;

	public Data(T data) {
		this.data = data;
	}

	public T getData() {
		return data;
	}

	@Override
	public int hashCode() {
		return Util.hashCode(data);
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Node) {
			Node node = ((Node) object).finalNode();
			if (node instanceof Data)
				return Util.equals(data, ((Data<?>) node).data);
			else
				return false;
		} else
			return false;
	}

}
