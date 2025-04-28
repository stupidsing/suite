package suite.funp.p2;

import static primal.statics.Fail.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import primal.MoreVerbs.Read;
import primal.Verbs.Get;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs2.Fun2;
import suite.funp.Funp_;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fct;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpDoHeapNew;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpVariable;
import suite.funp.P2;
import suite.funp.P2.FunpLambdaCapSingle;
import suite.funp.P2.FunpLambdaCapture;
import suite.funp.P2.FunpTypeAssign;
import suite.inspect.Inspect;
import suite.node.util.Singleton;

public class P21CaptureLambda {

	private Inspect inspect = Singleton.me.inspect;

	public Funp captureLambdas(Funp node0) {
		var grandLambda = FunpLambda.of("grand$", node0);
		var defByVariables = Funp_.associateDefinitions(node0);
		var lambdaByFunp = new IdentityHashMap<Funp, FunpLambda>();

		class AssociateLambda {
			private FunpLambda lambda;

			private AssociateLambda(FunpLambda lambda) {
				this.lambda = lambda;
			}

			private Funp a(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					lambdaByFunp.put(n, lambda);
					return n.castOpt(FunpLambda.class).map(f -> {
						new AssociateLambda(f).a(f.expr);
						return f;
					}).or(null);
				});
			}
		}

		new AssociateLambda(grandLambda).a(node0);

		class Li {
			private String capn = "cap$" + Get.temp();
			private FunpVariable cap = FunpVariable.of(capn);
			private Set<String> captureSet = new HashSet<>();
			private List<Pair<String, Funp>> captures = new ArrayList<>();
		}

		class Vi {
			private FunpLambda defLambda; // the variable is defined in this scope
			private FunpLambda varLambda; // the variable being read from this scope
			private boolean isRef; // whether the variable needs to be stored by-reference

			private Vi(Funp def) {
				defLambda = def instanceof FunpLambda fl ? fl : lambdaByFunp.get(def);
			}

			private FunpLambda setLambda(boolean isRef_, FunpLambda varLambda_) {
				isRef |= isRef_;
				return varLambda = varLambda_;
			}
		}

		var infoByLambda = Read.from2(lambdaByFunp).values().distinct().map2(lambda -> new Li()).toMap();
		var infoByVar = Read.from2(defByVariables).mapValue(pair -> new Vi(pair.v)).toMap();

		// capture-by-reference if necessary, e.g. assignments or references occurred
		new Object() {
			private Funp associate(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					var lambda = lambdaByFunp.get(n);

					return n.sw( //
					).applyIf(FunpReference.class, f -> {
						if (f.expr instanceof FunpVariable var) {
							infoByVar.get(var).setLambda(true, lambda);
							return f;
						} else
							return null;
					}).doIf(FunpVariable.class, f -> {
						infoByVar.get(f).setLambda(defByVariables.get(f).v instanceof FunpDefineRec, lambda);
					}).result();
				});
			}
		}.associate(node0);

		Fun2<FunpVariable, Vi, Funp> accessFun = (var, vi) -> {
			var defLambda = vi.defLambda;
			var varLambda = vi.varLambda;
			var isRef = vi.isRef;
			var vn = var.vn;
			var access = new Object() {
				private Funp access(FunpLambda lambda) {
					if (lambda == defLambda) // accessing from the same scope
						return isRef ? FunpReference.of(var) : var;
					else if (lambda.fct == Fct.MANUAL || lambda.fct == Fct.ONCE__) { // access from captured frame
						var li = infoByLambda.get(lambda);
						if (li.captureSet.add(vn))
							li.captures.add(Pair.of(vn, access(lambdaByFunp.get(lambda))));
						return FunpField.of(FunpReference.of(li.cap), vn);
					}
					else if (lambda.fct == Fct.SINGLE) { // access from frame variable
						var li = infoByLambda.get(lambda);
						if (li.captureSet.add(vn))
							li.captures.add(Pair.of(vn, access(lambdaByFunp.get(lambda))));
						return li.cap;
					} else if (lambda.fct == Fct.STACKF) // accessible through stack frame
						return access(lambdaByFunp.get(lambda));
					else
						return fail();
				}
			}.access(varLambda);

			// if we are capturing the reference, dereference to get the actual value
			if (!isRef)
				return access;
			else if (access instanceof FunpReference fr)
				return fr.expr;
			else
				return FunpDeref.of(access);
		};

		var accessors = Read.from2(infoByVar).concatMap2((var, vi) -> {
			return vi.varLambda != null ? Read.each2(Pair.of(var, accessFun.apply(var, vi))) : Read.empty2();
		}).toMap();

		return new Object() {
			private Funp c(Funp n) {
				return inspect.rewrite(n, Funp.class, this::c_);
			}

			private Funp c_(Funp n) {
				return n.sw( //
				).applyIf(FunpDoAssignRef.class, f -> f.apply((lambdaRef, value, expr) -> {
					if (lambdaRef.expr instanceof FunpVariable var) {
						var accessor = c(var);
						var value_ = c(value);
						var ref = FunpReference.of(accessor);
						var assign = FunpDoAssignRef.of(ref, value_, c(expr));
						return FunpTypeAssign.of(var, value_, assign);
					} else
						return null;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, fct) -> {
					Li li = infoByLambda.get(f);
					var captures = li.captures;

					Fun<Boolean, Funp> capturef = isDynamicSize -> {
						var pcapn = "pcap$" + Get.temp();
						var pcap = FunpVariable.of(pcapn);
						var struct = FunpStruct.of(captures);
						var lc = FunpLambdaCapture.of(pcap, li.cap, struct, vn, c(expr), fct);
						var assign = FunpDoAssignRef.of(FunpReference.of(FunpDeref.of(pcap)), struct, lc);
						return FunpDefine.of(pcapn, FunpDoHeapNew.of(isDynamicSize, null), assign, Fdt.L_MONO);
					};

					return switch (fct) {
					case MANUAL -> capturef.apply(true);
					case NOSCOP -> {
						if (captures.isEmpty())
							yield null;
						else
							yield Funp_.fail(f, "scopeless lambda <" + vn + "> capturing variables " + li.captureSet);
					}
					case ONCE__ -> capturef.apply(false);
					case SINGLE -> {
						var capture = Read.from(captures).uniqueResult();
						var pcap = FunpVariable.of(capture.k);
						yield FunpLambdaCapSingle.of(pcap, li.cap, capture.v, vn, c(expr), fct);
					}
					case STACKF -> null;
					default -> fail();
					};
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					return accessors.get(f);
				})).result();
			}
		}.c(node0);
	}

}
