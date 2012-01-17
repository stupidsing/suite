-------------------------------------------------------------------------------
-- symbolic mathematics

() :- use t23.sl #

simplify .f .fx .maxComplexity
	:- simplify0 (.f,) ()/.dl .maxComplexity
	, !
	, find.all .key (t23-traverse .dl .key/_) .keys
	, fold .keys 256/.minKey (_k _k0/_kx => once (_k > _k0, _kx = _k0; _kx = _k))
	, once t23-traverse .dl .minKey/.cl
	, t23-traverse .cl .fx/_
#

simplify0 () .dl/.dl _ #
simplify0 (.f, .tailf) .dl0/.dlx .maxComplexity
	:- find.all .f1 (equate1 (.f = .f1)) .list0
	, fold .list0 .dl0:.list1/.dl1:() (
		_f _dl0:_r0/_dlx:_rx => once (
			complexity _f _c, _c <= .maxComplexity
			, t23-map _dl0/_dl1 _c/_cl0
			, t23-map _cl0/_clx _f/_first
			, not bound _first, _first = N
			, once t23-replace _dl1/_dlx _c/_cl0/_clx
			, _r0 = (_f, _rx)
		;
			_dl0:_r0 = _dlx:_rx
		)
	)
	, append .tailf .list1 .list2
	--, write NEXT-ROUND:, pp-list .list2, nl
	, !, simplify0 .list2 .dl1/.dlx .maxComplexity
#

equate1 (.f = .g) :- equate0 (.f = .g); equate0 (.g = .f) #

equate0 (.f = .f) #
equate0 (.f + .g = .g + .f) #
equate0 (.f - .g = .f + .g * -1) #
equate0 (.f * .g = .g * .f) #
equate0 (.f / .g = .f * .g ^ -1) #
equate0 (.f + (.g + .h) = (.f + .g) + .h) #
equate0 (.f * (.g * .h) = (.f * .g) * .h) #
equate0 (.f + 0 = .f) #
equate0 (_ * 0 = 0) #
equate0 (.f * 1 = .f) #
equate0 (0 ^ _ = 0) #
equate0 (1 ^ _ = 1) #
equate0 (_ ^ 0 = 1) #
equate0 (.f ^ 1 = .f) #
equate0 (.f * (.g + .h) = .f * .g + .f * .h) #
equate0 (.f ^ (.g + .h) = .f ^ .g * .f ^ .h) #
equate0 (.tree0 = .tree1)
	:- tree .tree0 .f0 .op .g0
	, equate1 (.f0 = .f1)
	, equate1 (.g0 = .g1)
	, tree .tree1 .f1 .op .g1
#
equate0 (.tree = .value)
	:- tree .tree .f .op .g
	, member (' + ',' - ',' * ',) .op -- Only perform exact calculations
	, is.int .f, is.int .g
	, let .value .tree
#

equate0 (LN (E ^ .f) = .f) #
equate0 (LN (.f * .g) = (LN .f) + (LN .g)) #
equate0 (LN (.f ^ .g) = .g * (LN .f)) #
equate0 (LN (.f ^ .g) = .g * (LN .f)) #
equate0 (SIN (.f + .g) = (SIN .f) * (COS .g) + (COS .f) * (SIN .g)) #
equate0 (COS (.f + .g) = (SIN .f) * (SIN .g) + (COS .f) * (COS .g)) #

equate0 (.func .f0 = .func .f1)
	:- member ('LN', 'SIN', 'COS',) .func, equate1 (.f0 = .f1)
#
equate0 (DV .y .x = (DV .y .z) * (DV .z .x)) #
equate0 (DV (.f + .g) .x = (DV .f .x) * (DV .g .x)) #
equate0 (DV (.f * .g) .x = (DV .f .x) * .g + .f * (DV .g .x)) #
equate0 (DV .y .x = 1 / (DV .x .y)) #
equate0 (DV .f .x = 0) :- is.int .f #
equate0 (DV .x .x = 1) #
equate0 (DV (E ^ .x) = E ^ .x) #
equate0 (DV (LN .x) = 1 / .x) #
equate0 (DV (SIN .x) = COS .x) #
equate0 (DV (COS .x) = -1 * SIN .x) #
equate0 (DV .y0 .x0 = DV .y1 .x1) :- equate1 (.y0 = .y1), equate1 (.x0 = .x1) #

complexity .f 0 :- (is.int .f; is.atom .f), ! #
complexity .tree .n
	:- tree .tree .left _ .right
	, complexity .left .ln
	, complexity .right .rn
	, let .n (.ln + .rn + 1)
#
