package org.util;

import java.util.ArrayList;
import java.util.Collection;

public class FunUtil {

	public interface Event extends EventEx<RuntimeException> {
		public void apply();
	}

	public interface EventEx<Ex extends Exception> {
		public void apply() throws Ex;
	}

	public interface Source<O> {
		public O source();
	}

	public interface Sink<I> {
		public void sink(I i);
	}

	public interface Fun<I, O> {
		public O apply(I i);
	}

	public interface FunEx<I, O, Ex extends Exception> {
		public O apply(I i) throws Ex;
	}

	public static class Sinks<I> implements Sink<I> {
		private Collection<Sink<I>> sinks = new ArrayList<>();

		public void sink(I i) {
			for (Sink<I> sink : sinks)
				sink.sink(i);
		}

		public void add(Sink<I> sink) {
			sinks.add(sink);
		}
	}

	public static Event nullEvent() {
		return new Event() {
			public void apply() {
			}
		};
	}

	public static <Ex extends Exception> EventEx<Ex> nullEventEx() {
		return new EventEx<Ex>() {
			public void apply() {
			}
		};
	}

	public static <O> Source<O> nullSource() {
		return source(null);
	}

	public static <I> Sink<I> nullSink() {
		return new Sink<I>() {
			public void sink(I i) {
			}
		};
	}

	public static <O> Source<O> source(final O o) {
		return new Source<O>() {
			public O source() {
				return o;
			}
		};
	}

	public static <I, O> Collection<O> map(Fun<I, O> t, Collection<I> in) {
		ArrayList<O> out = new ArrayList<>(in.size());
		for (I i : in)
			out.add(t.apply(i));
		return out;
	}

}
