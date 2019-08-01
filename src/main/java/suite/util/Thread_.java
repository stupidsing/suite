package suite.util;

import primal.Verbs.RunnableEx;
import primal.Verbs.Start;
import primal.Verbs.Th;
import suite.streamlet.Puller;
import suite.streamlet.Read;

public class Thread_ {

	public static void startJoin(RunnableEx... rs) {
		var threads1 = Read.from(rs).map(Start::thread).collect();
		threads1.sink(Th::join_);
	}

	public static Void startJoin(Puller<Th> threads0) {
		var threads1 = threads0.toList();
		threads1.forEach(Th::start);
		threads1.forEach(Th::join_);
		return null;
	}

}
