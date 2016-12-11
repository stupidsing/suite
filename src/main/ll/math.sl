-------------------------------------------------------------------------------
-- symbolic mathematics

equate (.f = .f) #
equate (.f = .h) :- equate (.f = .g), equate1 (.g = .h) #

equate1 (.f = .g)
	:- once (bound .f; bound .g)
	, (equate0 (.f = .g); equate0 (.g = .f))
#

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
equate0 (.f ^ (.g * .h) = (.f ^ .g) ^ .h) #
equate0 (.tree0 = .tree1)
	:- tree .tree0 .f0 .op .g0
	, tree .tree1 .f1 .op .g1
	, (equate1 (.f0 = .f1), .g0 = .g1
		; equate1 (.g0 = .g1), .f0 = f1
	)
#
equate0 (.tree = .value)
	:- tree .tree .f .op .g
	, member (' + ',' - ',' * ',) .op -- only perform exact calculations
	, is.int .f, is.int .g
	, let .value .tree
#

equate0 (E ^ (LN .f) = .f) #
equate0 (LN (E ^ .f) = .f) #
equate0 (LN (.f * .g) = (LN .f) + (LN .g)) #
equate0 (LN (.f ^ .g) = .g * (LN .f)) #
equate0 (SIN (-1 * .f) = -1 * (SIN .f)) #
equate0 (COS (-1 * .f) = COS .f) #
equate0 (SIN (.f + .g) = (SIN .f) * (COS .g) + (COS .f) * (SIN .g)) #
equate0 (COS (.f + .g) = (COS .f) * (COS .g) - (SIN .f) * (SIN .g)) #
equate0 (.func .f0 = .func .f1)
	:- member ('LN', 'SIN', 'COS',) .func
	, equate1 (.f0 = .f1)
#

equate0 (DV .y .x = (DV .y .z) * (DV .z .x)) #
equate0 (DV (.f + .g) .x = (DV .f .x) + (DV .g .x)) #
equate0 (DV (.f * .g) .x = (DV .f .x) * .g + .f * (DV .g .x)) #
equate0 (DV .y .x = 1 / (DV .x .y)) #
equate0 (DV .f _ = 0) :- is.int .f #
equate0 (DV .x .x = 1) #
equate0 (DV (E ^ .x) .x = E ^ .x) #
equate0 (DV (LN .x) .x = 1 / .x) #
equate0 (DV (SIN .x) .x = COS .x) #
equate0 (DV (COS .x) .x = -1 * SIN .x) #
equate0 (DV .y .x0 = DV .y .x1) :- equate1 (.x0 = .x1) #
equate0 (DV .y0 .x = DV .y1 .x) :- equate1 (.y0 = .y1) #
