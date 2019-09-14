package suite.funp;

import static primal.statics.Fail.fail;

import java.util.Map;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.fp.Funs.Fun;
import primal.persistent.PerMap;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpApply;
import suite.funp.P0.FunpArray;
import suite.funp.P0.FunpBoolean;
import suite.funp.P0.FunpCoerce;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoFold;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpError;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpIf;
import suite.funp.P0.FunpIndex;
import suite.funp.P0.FunpIo;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpNumber;
import suite.funp.P0.FunpPredefine;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpRepeat;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpTree;
import suite.funp.P0.FunpTree2;
import suite.funp.P0.FunpVariable;
import suite.node.io.TermOp;
import suite.node.util.TreeUtil;
import suite.util.To;

public class P2GenerateLambda {

	private static class Rt {
		private Rt parent;
		private Value var;

		private Rt(Rt parent, Value var) {
			this.parent = parent;
			this.var = var;
		}
	}

	private interface Value {
	}

	private static class Bool implements Value {
		private boolean b;

		private Bool(boolean b) {
			this.b = b;
		}
	}

	private static class Int implements Value {
		private final int i;

		private Int(int i) {
			this.i = i;
		}
	}

	private static class Ref implements Value {
		private Value v;

		private Ref(Value v) {
			this.v = v;
		}
	}

	private static class Struct implements Value {
		private Map<String, Value> map;

		private Struct(Map<String, Value> map) {
			this.map = map;
		}
	}

	private static class Vec implements Value {
		private Value[] values;

		private Vec(Value[] values) {
			this.values = values;
		}
	}

	private interface Thunk extends Fun<Rt, Value>, Value {
	}

	private interface Fun_ extends Fun<Value, Value>, Value {
	}

	public int eval(Funp f) {
		var thunk = compile(0, PerMap.empty(), f);
		var value = thunk.apply(new Rt(null, null));
		return ((Int) value).i;
	}

	private Thunk compile(int fs, PerMap<String, Integer> env, Funp n) {
		return new Compile(fs, env).compile_(n);
	}

	private class Compile {
		private int fs;
		private PerMap<String, Integer> env;

		private Compile(int fs, PerMap<String, Integer> env) {
			this.fs = fs;
			this.env = env;
		}

		private Thunk compile_(Funp n0) {
			return n0.<Thunk> switch_( //
			).applyIf(FunpApply.class, f -> f.apply((value, lambda) -> {
				var lambda1 = compile_(lambda);
				var value1 = compile_(value);
				return rt -> ((Fun_) lambda1.apply(rt)).apply(value1.apply(rt));
			})).applyIf(FunpArray.class, f -> f.apply(elements -> {
				var thunks = Read.from(elements).map(this::compile_);
				return rt -> new Vec(thunks.map(thunk -> thunk.apply(rt)).toArray(Value.class));
			})).applyIf(FunpBoolean.class, f -> f.apply(b -> {
				var b1 = new Bool(b);
				return rt -> b1;
			})).applyIf(FunpCoerce.class, f -> f.apply((from, to, expr) -> {
				return compile_(expr);
			})).applyIf(FunpDefine.class, f -> f.apply((vn, value, expr, fdt) -> {
				var b = fdt == Fdt.L_MONO || fdt == Fdt.L_POLY;
				return b ? compile_(FunpApply.of(value, FunpLambda.of(vn, expr, false))) : null;
			})).applyIf(FunpDeref.class, f -> {
				var p = compile_(f);
				return rt -> ((Ref) p.apply(rt)).v;
			}).applyIf(FunpDoFold.class, f -> f.apply((init, cont, next) -> {
				var vn1 = "fold$" + Get.temp();
				var fs1 = fs + 1;
				var init_ = compile_(init);
				var var_ = FunpVariable.of(vn1);
				var compile1 = new Compile(fs1, env.replace(vn1, fs1));
				var cont_ = compile1.compile_(FunpApply.of(var_, cont));
				var next_ = compile1.compile_(FunpApply.of(var_, next));
				return rt -> {
					var rt1 = new Rt(rt, init_.apply(rt));
					while (b(rt1, cont_))
						rt1.var = next_.apply(rt1);
					return rt1.var;
				};
			})).applyIf(FunpDontCare.class, f -> {
				return rt -> new Int(0);
			}).applyIf(FunpError.class, f -> {
				return rt -> fail();
			}).applyIf(FunpField.class, f -> f.apply((ref, field) -> {
				var p = compile_(ref);
				return rt -> ((Struct) ((Ref) p.apply(rt)).v).map.get(field);
			})).applyIf(FunpIf.class, f -> f.apply((if_, then, else_) -> {
				var if1 = compile_(if_);
				var then1 = compile_(then);
				var else1 = compile_(else_);
				return rt -> (b(rt, if1) ? then1 : else1).apply(rt);
			})).applyIf(FunpIndex.class, f -> f.apply((reference, index) -> {
				var array = compile_(FunpDeref.of(reference));
				var index1 = compile_(index);
				return rt -> ((Vec) array.apply(rt)).values[i(rt, index1)];
			})).applyIf(FunpIo.class, f -> f.apply(expr -> {
				return compile_(expr);
			})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
				var fs1 = fs + 1;
				var thunk = compile(fs1, env.replace(vn, fs1), expr);
				return rt -> (Fun_) p -> thunk.apply(new Rt(rt, p));
			})).applyIf(FunpNumber.class, f -> f.apply(i -> {
				var i1 = new Int(i.value());
				return rt -> i1;
			})).applyIf(FunpPredefine.class, f -> f.apply((vn, expr) -> {
				return compile_(expr);
			})).applyIf(FunpReference.class, f -> {
				var v = compile_(f);
				return rt -> new Ref(v.apply(rt));
			}).applyIf(FunpRepeat.class, f -> f.apply((count, expr) -> {
				var expr_ = compile_(expr);
				return rt -> new Vec(To.array(count, Value.class, i -> expr_.apply(rt)));
			})).applyIf(FunpStruct.class, f -> f.apply(pairs -> {
				var funs = Read.from2(pairs).mapValue(this::compile_).collect();
				return rt -> new Struct(funs.mapValue(v -> v.apply(rt)).toMap());
			})).applyIf(FunpTree.class, f -> f.apply((op, lhs, rhs, size) -> {
				var v0 = compile_(lhs);
				var v1 = compile_(rhs);
				if (op == TermOp.BIGAND)
					return rt -> new Bool(b(rt, v0) && b(rt, v1));
				else if (op == TermOp.BIGOR_)
					return rt -> new Bool(b(rt, v0) || b(rt, v1));
				else {
					var fun = TreeUtil.evaluateOp(op);
					return rt -> new Int(fun.apply(i(rt, v0), i(rt, v1)));
				}
			})).applyIf(FunpTree2.class, f -> f.apply((op, lhs, rhs) -> {
				var v0 = compile_(lhs);
				var v1 = compile_(rhs);
				var fun = TreeUtil.evaluateOp(op);
				return rt -> new Int(fun.apply(i(rt, v0), i(rt, v1)));
			})).applyIf(FunpVariable.class, f -> f.apply(var -> {
				var fd = fs - env.get(var);
				return rt -> {
					for (var i = 0; i < fd; i++)
						rt = rt.parent;
					return rt.var;
				};
			})).nonNullResult();
		}
	}

	private static int i(Rt rt, Thunk value) {
		return ((Int) value.apply(rt)).i;
	}

	private static boolean b(Rt rt, Thunk value) {
		return ((Bool) value.apply(rt)).b;
	}

}
