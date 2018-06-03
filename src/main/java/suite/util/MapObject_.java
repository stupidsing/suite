package suite.util;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import suite.adt.pair.Fixie_.FixieFun0;
import suite.adt.pair.Fixie_.FixieFun1;
import suite.adt.pair.Fixie_.FixieFun2;
import suite.adt.pair.Fixie_.FixieFun3;
import suite.adt.pair.Fixie_.FixieFun4;
import suite.adt.pair.Fixie_.FixieFun5;
import suite.adt.pair.Fixie_.FixieFun6;
import suite.adt.pair.Fixie_.FixieFun7;
import suite.adt.pair.Fixie_.FixieFun8;
import suite.adt.pair.Fixie_.FixieFun9;
import suite.adt.pair.Fixie_.FixieFunA;
import suite.streamlet.Read;

public class MapObject_ {

	public static <T extends MapObject<T>> int compare(T t0, T t1) {
		var class0 = t0.getClass();
		var class1 = t1.getClass();
		int c;
		if (class0 == class1) {
			var iter0 = (Iterator<?>) list(t0).iterator();
			var iter1 = (Iterator<?>) list(t1).iterator();
			boolean b0, b1;
			c = 0;
			while (c == 0 && (c = Boolean.compare(b0 = iter0.hasNext(), b1 = iter1.hasNext())) == 0)
				if (b0 && b1)
					c = Object_.compareAnyway(iter0.next(), iter1.next());
		} else
			c = String_.compare(class0.getName(), class1.getName());
		return c;
	}

	public static <T extends MapObject<T>> boolean equals(T t0, T t1) {
		var list0 = list(t0);
		var list1 = list(t1);
		var size0 = list0.size();
		var size1 = list1.size();
		var b = true;
		if (size0 == size1)
			for (var i = 0; i < size0; i++)
				b &= Objects.equals(list0.get(i), list1.get(i));
		return b;
	}

	public static <T extends MapObject<T>> MapObject<T> construct(Class<?> clazz, List<?> list) {
		return Rethrow.ex(() -> {
			var size = list.size();
			var m = Read //
					.from(clazz.getMethods()) //
					.filter(method -> String_.equals(method.getName(), "of") && method.getParameterCount() == size) //
					.uniqueResult();
			@SuppressWarnings("unchecked")
			var t = (MapObject<T>) m.invoke(null, list.toArray());
			return t;
		});
	}

	public static <T extends MapObject<T>> List<?> list(Object object) {
		var clazz = object.getClass();

		var m = Read //
				.from(clazz.getMethods()) //
				.filter(method -> String_.equals(method.getName(), "apply")) //
				.uniqueResult();

		var type = m.getParameters()[0].getType();
		Object p;

		if (type == FixieFun0.class)
			p = (FixieFun0<?>) List::of;
		else if (type == FixieFun1.class)
			p = (FixieFun1<?, ?>) List::of;
		else if (type == FixieFun2.class)
			p = (FixieFun2<?, ?, ?>) List::of;
		else if (type == FixieFun3.class)
			p = (FixieFun3<?, ?, ?, ?>) List::of;
		else if (type == FixieFun4.class)
			p = (FixieFun4<?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun5.class)
			p = (FixieFun5<?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun6.class)
			p = (FixieFun6<?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun7.class)
			p = (FixieFun7<?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun8.class)
			p = (FixieFun8<?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFun9.class)
			p = (FixieFun9<?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else if (type == FixieFunA.class)
			p = (FixieFunA<?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?>) List::of;
		else
			p = Fail.t();

		return (List<?>) Rethrow.ex(() -> m.invoke(object, p));
	}

}
