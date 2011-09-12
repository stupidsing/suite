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
	:- find.all .f1 (equate (.f = .f1)) .list0
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

equate (.f = .g) :- equate0 (.f = .g); equate0 (.g = .f) #

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
equate0 (.tree = .tree1) :- tree .tree .f .op .g, equate (.f = .f1), tree .tree1 .f1 .op .g #
equate0 (.tree = .tree1) :- tree .tree .f .op .g, equate (.g = .g1), tree .tree1 .f .op .g1 #
equate0 (.tree = .value)
	:- tree .tree .f .op .g
	, member (' + ',' - ',' * ',) .op -- Only perform exact calculations
	, is.int .f, is.int .g
	, let .value .tree
#

complexity .f 0 :- (is.int .f; is.atom .f), ! #
complexity .tree .n
	:- tree .tree .left _ .right
	, complexity .left .ln
	, complexity .right .rn
	, let .n (.ln + .rn + 1)
#
