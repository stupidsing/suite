package suite.object;

import static primal.statics.Fail.fail;
import static primal.statics.Rethrow.ex;

import java.util.List;

import primal.MoreVerbs.Read;
import primal.Verbs.Equals;
import primal.adt.Fixie_.FixieFun0;
import primal.adt.Fixie_.FixieFun1;
import primal.adt.Fixie_.FixieFun2;
import primal.adt.Fixie_.FixieFun3;
import primal.adt.Fixie_.FixieFun4;
import primal.adt.Fixie_.FixieFun5;
import primal.adt.Fixie_.FixieFun6;
import primal.adt.Fixie_.FixieFun7;
import primal.adt.Fixie_.FixieFun8;
import primal.adt.Fixie_.FixieFun9;
import primal.adt.Fixie_.FixieFunA;

public class MapObject_ {

	public static <T extends MapObject<T>> MapObject<T> construct(Class<?> clazz, List<?> list) {
		return ex(() -> {
			var size = list.size();
			var m = Read
					.from(clazz.getMethods())
					.filter(method -> Equals.string(method.getName(), "of") && method.getParameterCount() == size)
					.uniqueResult();
			@SuppressWarnings("unchecked")
			var t = (MapObject<T>) m.invoke(null, list.toArray());
			return t;
		});
	}

	public static <T extends MapObject<?>> List<?> list(T object) {
		var clazz = object.getClass();

		var m = Read
				.from(clazz.getMethods())
				.filter(method -> Equals.string(method.getName(), "apply"))
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
			p = fail();

		return (List<?>) ex(() -> m.invoke(object, p));
	}

}
