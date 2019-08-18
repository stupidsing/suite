package suite.concurrent;

import static primal.statics.Fail.fail;

import primal.adt.Mutable;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.os.Log_;
import primal.persistent.PerList;

public class Fut<T> {

	private CasReference<Holder> ref = new CasReference<>(new H0Running(PerList.end()));

	public interface Holder {
	}

	private class H0Running implements Holder {
		private PerList<Pair<Sink<T>, Sink<Exception>>> handlers;

		private H0Running(PerList<Pair<Sink<T>, Sink<Exception>>> handlers) {
			this.handlers = handlers;
		}
	}

	private class H1Completed implements Holder {
		private T t;
		private Exception ex;

		private void handle(Sink<T> okay, Sink<Exception> fail) {
			if (t != null)
				okay.f(t);
			else
				fail.f(ex);
		}
	}

	public static Fut<Boolean> begin() {
		return begin(true);
	}

	public static <T> Fut<T> begin(T t) {
		return of(fut -> fut.complete(t));
	}

	public static <T> Fut<T> of(Source<T> source) {
		return of(fut -> applyOr(source::g, fut::complete, fut::error));
	}

	public static <T> Fut<T> of(Sink<Fut<T>> sink) {
		var fut = new Fut<T>();
		sink.f(fut);
		return fut;
	}

	public static <T> Fut<T> of() {
		return new Fut<T>();
	}

	private Fut() {
	}

	public void complete(T t) {
		var h1 = new H1Completed();
		h1.t = t;
		complete_(h1);
	}

	public <U> Fut<U> concatMap(Fun<T, Fut<U>> fun) {
		return of(fut0 -> handle( //
				t -> Fut.applyOr( //
						() -> fun.apply(t), //
						fut1 -> fut1.handle(fut0::complete, fut0::error), //
						fut0::error), //
				fut0::error));
	}

	public void error(Exception ex) {
		var h1 = new H1Completed();
		h1.ex = ex;
		complete_(h1);
	}

	public void handle(Sink<T> okay) {
		handle(okay, Log_::error);
	}

	public void handle(Sink<T> okay, Sink<Exception> fail) {
		var mut = Mutable.<H1Completed> nil();

		ref.apply(h -> {
			if (h instanceof Fut.H0Running) {
				@SuppressWarnings("unchecked")
				var h0 = (Fut<T>.H0Running) h;
				return new H0Running(PerList.cons(Pair.of(okay, fail), h0.handlers));
			} else if (h instanceof Fut.H1Completed) {
				@SuppressWarnings("unchecked")
				var h1 = (Fut<T>.H1Completed) h;
				mut.set(h1);
				return h;
			} else
				return fail();
		});

		var h1 = mut.value();

		if (h1 != null)
			h1.handle(okay, fail);
	}

	public <U> Fut<U> map(Fun<T, U> fun) {
		return of(fut -> handle( //
				t -> applyOr(() -> fun.apply(t), fut::complete, fut::error), //
				fut::error));
	}

	private void complete_(H1Completed h1) {
		var mut = Mutable.<H0Running> nil();

		ref.apply(h -> {
			if (h instanceof Fut.H0Running) {
				@SuppressWarnings("unchecked")
				var h0 = (Fut<T>.H0Running) h;
				mut.set(h0);
				return h1;
			} else
				return fail("already completed");
		});

		var h0 = mut.value();

		if (h0 != null)
			for (var pair : h0.handlers)
				h1.handle(pair.k, pair.v);
	}

	private static <T> void applyOr(Source<T> source, Sink<T> okay, Sink<Exception> fail) {
		try {
			okay.f(source.g());
		} catch (Exception ex) {
			fail.f(ex);
		}
	}

}
