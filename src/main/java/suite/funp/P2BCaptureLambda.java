package suite.funp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

import suite.adt.pair.Pair;
import suite.funp.Funp_.Funp;
import suite.funp.P0.Fdt;
import suite.funp.P0.FunpDefine;
import suite.funp.P0.FunpDefineRec;
import suite.funp.P0.FunpDeref;
import suite.funp.P0.FunpDoAssignRef;
import suite.funp.P0.FunpDoAssignVar;
import suite.funp.P0.FunpDontCare;
import suite.funp.P0.FunpField;
import suite.funp.P0.FunpLambda;
import suite.funp.P0.FunpReference;
import suite.funp.P0.FunpStruct;
import suite.funp.P0.FunpVariable;
import suite.funp.P2.FunpLambdaCapture;
import suite.inspect.Inspect;
import suite.node.util.Singleton;
import suite.streamlet.FunUtil2.Fun2;
import suite.streamlet.Read;
import suite.util.Util;

public class P2BCaptureLambda {

	private Inspect inspect = Singleton.me.inspect;

	public Funp captureLambdas(Funp node0) {
		var grandLambda = FunpLambda.of("grand$", node0, false);
		var defByVars = Funp_.associateDefinitions(node0);
		var lambdaByFunp = new IdentityHashMap<Funp, FunpLambda>();

		class AssociateLambda {
			private FunpLambda lambda;

			private AssociateLambda(FunpLambda lambda) {
				this.lambda = lambda;
			}

			private Funp a(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					lambdaByFunp.put(n, lambda);
					return n.cast(FunpLambda.class, f -> f.apply((var, expr, isCapture) -> {
						new AssociateLambda(f).a(expr);
						return f;
					}));
				});
			}
		}

		new AssociateLambda(grandLambda).a(node0);

		class Li {
			private String capn = "cap$" + Util.temp();
			private FunpVariable cap = FunpVariable.of(capn);
			private Set<String> captureSet = new HashSet<>();
			private List<Pair<String, Funp>> captures = new ArrayList<>();
		}

		class Vi {
			private FunpLambda lambda; // variable defined here
			private boolean isRef;
			private FunpLambda varLambda; // variable read from here

			private Vi(Funp def) {
				lambda = def instanceof FunpLambda ? (FunpLambda) def : lambdaByFunp.get(def);
			}

			private FunpLambda setLambda(boolean isRef_, FunpLambda varLambda_) {
				isRef |= isRef_;
				return varLambda = varLambda_;
			}
		}

		var infoByLambda = Read.from2(lambdaByFunp).values().distinct().map2(lambda -> new Li()).toMap();
		var infoByVar = Read.from2(defByVars).mapValue(Vi::new).toMap();

		new Object() {
			private Funp associate(Funp node) {
				return inspect.rewrite(node, Funp.class, n -> {
					var lambda = lambdaByFunp.get(n);

					return n.sw( //
					).doIf(FunpDoAssignVar.class, f -> {
						infoByVar.get(f.var).setLambda(true, lambda);
					}).doIf(FunpReference.class, f -> {
						f.expr.cast(FunpVariable.class, var -> infoByVar.get(var).setLambda(true, lambda));
					}).doIf(FunpVariable.class, f -> {
						infoByVar.get(f).setLambda(defByVars.get(f) instanceof FunpDefineRec, lambda);
					}).result();
				});
			}
		}.associate(node0);

		Fun2<FunpVariable, FunpLambda, Funp> accessFun = (var, lambda) -> {
			var vi = infoByVar.get(var);
			var lambdaVar = vi.lambda;
			var isRef = vi.isRef;
			var vn = var.vn;
			var access = new Object() {
				private Funp access(FunpLambda lambda_) {
					if (lambda_ == lambdaVar)
						return isRef ? FunpReference.of(var) : var;
					else if (!lambda.isCapture)
						return access(lambdaByFunp.get(lambda_));
					else {
						var li = infoByLambda.get(lambda_);
						if (li.captureSet.add(vn))
							li.captures.add(Pair.of(vn, access(lambdaByFunp.get(lambda_))));
						return FunpField.of(FunpReference.of(li.cap), vn);
					}
				}
			}.access(lambda);
			return isRef ? FunpDeref.of(access) : access;
		};

		var accessors = Read.from2(infoByVar).concatMap2((var, vi) -> {
			return vi.varLambda != null ? Read.each2(Pair.of(var, accessFun.apply(var, vi.varLambda))) : Read.empty2();
		}).toMap();

		return new Object() {
			private Funp c(Funp n) {
				return inspect.rewrite(n, Funp.class, this::c_);
			}

			private Funp c_(Funp n) {
				return n.sw( //
				).applyIf(FunpDoAssignVar.class, f -> f.apply((var, value, expr) -> {
					var accessor = accessors.get(var);
					return accessor != null ? FunpDoAssignRef.of(FunpReference.of(accessor), c(value), c(expr)) : null;
				})).applyIf(FunpLambda.class, f -> f.apply((vn, expr, isCapture) -> {
					var li = infoByLambda.get(f);
					var captures = li.captures;
					if (!captures.isEmpty()) {
						var pcapn = "pcap$" + Util.temp();
						var pcap = FunpVariable.of(pcapn);
						var struct = FunpStruct.of(captures);
						var lc = FunpLambdaCapture.of(pcap, li.cap, struct, vn, c(expr));
						var assign = FunpDoAssignRef.of(FunpReference.of(FunpDeref.of(pcap)), struct, lc);
						return FunpDefine.of(pcapn, FunpDontCare.of(), assign, Fdt.L_HEAP);

						// FIXME now we free the capture immediately after first invocation; cannot
						// invoke again
					} else
						return null;
				})).applyIf(FunpVariable.class, f -> f.apply(vn -> {
					return accessors.get(f);
				})).result();
			}
		}.c(node0);
	}

}
