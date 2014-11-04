package suite.streamlet;

import java.util.Arrays;
import java.util.Collection;

import suite.util.FunUtil.Source;
import suite.util.To;

public class Read {

	@SafeVarargs
	public static <T> Streamlet<T> from(T... col) {
		return from(Arrays.asList(col));
	}

	public static <T> Streamlet<T> from(Collection<T> col) {
		return from(To.source(col));
	}

	public static <T> Streamlet<T> from(Source<T> source) {
		return new Streamlet<>(source);
	}

}
