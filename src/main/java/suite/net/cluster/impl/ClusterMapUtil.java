package suite.net.cluster.impl;

import java.io.Serializable;

public class ClusterMapUtil {

	public static class GetQuery {
		public static class Request implements Serializable {
			private static final long serialVersionUID = 1l;
			public Object key;
		}

		public static class Response implements Serializable {
			private static final long serialVersionUID = 1l;
			public Object value;
		}
	}

	public static class PutQuery {
		public static class Request implements Serializable {
			private static final long serialVersionUID = 1l;
			public Object key, value;
		}

		public static class Response implements Serializable {
			private static final long serialVersionUID = 1l;
			public Object value;
		}
	}

}
