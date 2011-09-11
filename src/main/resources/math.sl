-------------------------------------------------------------------------------
-- symbolic mathematics

:- use t23.sl #

simplify .f .fx .maxDepth
	:- simplify0 .f .dl .maxDepth
	, once append .dl () .dl
	, !
	, fold .dl 256/.minc (
		(_c/_t)/_c0/_cx => once (_c > _c0, _cx = _c0; _cx = _c)
	)
	, once member .dl .minc/.cl
	, once append .cl () .cl
	, member .cl .fx/_
#

simplify0 .f .dl .maxDepth
	:- find.all .f1 (equate (.f = .f1)) .list
	, fold .list .list1/() (
		_f/_r0/_rx => once (
			complexity _f _c, _c <= .maxDepth
			, once member .dl _c/_cl, once member _cl _f/_first
			, not bound _first, _first = N
			, _r0 = (_f, _rx)
		;
			_r0 = _rx
		)
	)
	--, write NEXT-ROUND:, pp-list .list1, nl	
	, fold .list1 _ (_f/_ => simplify0 _f .dl .maxDepth)
	, !
#

equate (.f = .g) :- equate0 (.f = .g); equate0 (.g = .f) #

equate0 (.f = .f) #
equate0 (.f + .g = .g + .f) #
equate0 (.f * .g = .g * .f) #
equate0 (.f - .g = .f + .g * -1) #
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
equate0 (.f ^ (.g * .h) = .f ^ .g * .f ^ .h) #
equate0 (.tree = .tree1) :- tree .tree .f .op .g, equate0 (.f = .f1), tree .tree1 .f1 .op .g #
equate0 (.tree = .tree1) :- tree .tree .f .op .g, equate0 (.g = .g1), tree .tree1 .f .op .g1 #
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
