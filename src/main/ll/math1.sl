-------------------------------------------------------------------------------
-- symbolic mathematics

sm-rewrite .e .e .es/.es :- is.int .e; is.atom .e #
sm-rewrite (.x0 + .y0) (.x1 + .y1) (.x0 .x1, .y0 .y1, .es)/.es #
sm-rewrite (.x0 * .y0) (.x1 * .y1) (.x0 .x1, .y0 .y1, .es)/.es #
sm-rewrite (.x0 ^ .y0) (.x1 ^ .y1) (.x0 .x1, .y0 .y1, .es)/.es #
sm-rewrite E E .es/.es #
sm-rewrite PI PI .es/.es #
sm-rewrite (LN .x0) (LN .x1) (.x0 .x1, .es)/.es #
sm-rewrite (SIN .x0) (SIN .x1) (.x0 .x1, .es)/.es #
sm-rewrite (COS .x0) (COS .x1) (.x0 .x1, .es)/.es #
sm-rewrite (DV .x0 .y0) (DV .x1 .y1) (.x0 .x1, .y0 .y1, .es)/.es #

reduce (.x - .y) (.x + -1 * .y) #
reduce (.x / .y) (.x * .y ^ -1) #
reduce (TAN .x) (SIN .x / COS .x) #
reduce .e .e1 :- sm-rewrite .e .e1 .es/(), reduce-list .es #

reduce-list () #
reduce-list (.e .e1, .es) :- reduce .e .e1, reduce-list .es #

equate (.f + .g = .g + .f) #
equate (.f * .g = .g * .f) #
equate (.f + (.g + .h) = (.f + .g) + .h) #
equate (.f * (.g * .h) = (.f * .g) * .h) #
equate (.f + 0 = .f) #
equate (_ * 0 = 0) #
equate (.f * 1 = .f) #
equate (0 ^ _ = 0) #
equate (1 ^ _ = 1) #
equate (_ ^ 0 = 1) #
equate (.f ^ 1 = .f) #
equate (.f * (.g + .h) = .f * .g + .f * .h) #
equate (.f ^ (.g + .h) = .f ^ .g * .f ^ .h) #
equate (.f ^ (.g * .h) = (.f ^ .g) ^ .h) #
equate (.tree = .value)
	:- tree .tree .f .op .g
	, member (' + ', ' * ',) .op -- Only perform exact calculations
	, is.int .f
	, is.int .g
	, let .value .tree
#

equate (E ^ LN .f = .f) #
equate (LN (E ^ .f) = .f) #
equate (LN (.f * .g) = LN .f + LN .g) #
equate (LN (.f ^ .g) = .g * LN .f) #
equate (ASIN SIN .f = .f) #
equate (ACOS COS .f = .f) #
equate (SIN 0 = 0) #
equate (SIN (PI * (1 + 1) ^ -1) = 1) #
equate (COS 0 = 1) #
equate (COS (PI * (1 + 1) ^ -1) = 0) #
equate (SIN (-1 * .f) = -1 * SIN .f) #
equate (COS (-1 * .f) = COS .f) #
equate (SIN (.f + .g) = SIN .f * COS .g + COS .f * SIN .g) #
equate (COS (.f + .g) = COS .f * COS .g + SIN .f * SIN .g * -1) #
equate ((1 + 1) * SIN .f * SIN .g = COS (.f + -1 * .g) + -1 * COS (.f + .g)) #
equate ((1 + 1) * SIN .f * COS .g = SIN (.f + .g) + SIN (.f + -1 * .g)) #
equate ((1 + 1) * COS .f * COS .g = COS (.f + -1 * .g) + COS (.f + .g)) #

equate (DV .y .x = DV .y .z * DV .z .x) #
equate (DV (.f + .g) .x = DV .f .x + DV .g .x) #
equate (DV (.f * .g) .x = DV .f .x * .g + .f * DV .g .x) #
equate (DV .y .x = (DV .x .y) ^ -1) #
equate (DV .f _ = 0) :- is.int .f #
equate (DV .x .x = 1) #
equate (DV (E ^ .x) .x = E ^ .x) #
equate (DV (LN .x) .x = .x ^ -1) #
equate (DV (SIN .x) .x = COS .x) #
equate (DV (COS .x) .x = -1 * SIN .x) #
equate (.e = .e1) :- sm-rewrite .e .e1 .es/(), member .es (.c .c1), equate (.c = .c1) #
