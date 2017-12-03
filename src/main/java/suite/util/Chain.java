package suite.util;

import java.util.Collection;
import java.util.Map;

import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.streamlet.Streamlet2;
import suite.util.FunUtil.Fun;

public class Chain<T> {

	private T t;

	private Chain(T t) {
		this.t = t;
	}

	public static <T> Chain<T> of(T t) {
		return new Chain<>(t);
	}

	public <U> U apply(Fun<T, U> fun) {
		return fun.apply(t);
	}

	public <U> Chain<U> map(Fun<T, U> fun) {
		return of(fun.apply(t));
	}

	public <U> Streamlet<U> read(Fun<T, Collection<U>> fun) {
		return Read.from(fun.apply(t));
	}

	public <U> Streamlet<U> readArray(Fun<T, U[]> fun) {
		return Read.from(fun.apply(t));
	}

	public <K, V> Streamlet2<K, V> read2(Fun<T, Map<K, V>> fun) {
		return Read.from2(fun.apply(t));
	}

}
