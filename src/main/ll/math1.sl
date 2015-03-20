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

sm-reduce (.x - .y) (.x + -1 * .y) #
sm-reduce (.x / .y) (.x * .y ^ -1) #
sm-reduce (TAN .x) (SIN .x / COS .x) #
sm-reduce .e .e1 :- sm-rewrite .e .e1 .es/(), sm-reduce-list .es #

sm-reduce-list () #
sm-reduce-list (.e .e1, .es) :- sm-reduce .e .e1, sm-reduce-list .es #

sm-equate (.f + .g = .g + .f) #
sm-equate (.f * .g = .g * .f) #
sm-equate (.f + (.g + .h) = (.f + .g) + .h) #
sm-equate (.f * (.g * .h) = (.f * .g) * .h) #
sm-equate (.f + 0 = .f) #
sm-equate (_ * 0 = 0) #
sm-equate (.f * 1 = .f) #
sm-equate (0 ^ _ = 0) #
sm-equate (1 ^ _ = 1) #
sm-equate (_ ^ 0 = 1) #
sm-equate (.f ^ 1 = .f) #
sm-equate (.f * (.g + .h) = .f * .g + .f * .h) #
sm-equate (.f ^ (.g + .h) = .f ^ .g * .f ^ .h) #
sm-equate (.f ^ (.g * .h) = (.f ^ .g) ^ .h) #
sm-equate (.tree = .value)
	:- tree .tree .f .op .g
	, member (' + ', ' * ',) .op -- Only perform exact calculations
	, is.int .f
	, is.int .g
	, let .value .tree
#

sm-equate (E ^ LN .f = .f) #
sm-equate (LN (E ^ .f) = .f) #
sm-equate (LN (.f * .g) = LN .f + LN .g) #
sm-equate (LN (.f ^ .g) = .g * LN .f) #
sm-equate (ASIN SIN .f = .f) #
sm-equate (ACOS COS .f = .f) #
sm-equate (SIN 0 = 0) #
sm-equate (SIN (PI * (1 + 1) ^ -1) = 1) #
sm-equate (COS 0 = 1) #
sm-equate (COS (PI * (1 + 1) ^ -1) = 0) #
sm-equate (SIN (-1 * .f) = -1 * SIN .f) #
sm-equate (COS (-1 * .f) = COS .f) #
sm-equate (SIN (.f + .g) = SIN .f * COS .g + COS .f * SIN .g) #
sm-equate (COS (.f + .g) = COS .f * COS .g + SIN .f * SIN .g * -1) #
sm-equate ((1 + 1) * SIN .f * SIN .g = COS (.f + -1 * .g) + -1 * COS (.f + .g)) #
sm-equate ((1 + 1) * SIN .f * COS .g = SIN (.f + .g) + SIN (.f + -1 * .g)) #
sm-equate ((1 + 1) * COS .f * COS .g = COS (.f + -1 * .g) + COS (.f + .g)) #

sm-equate (DV .y .x = DV .y .z * DV .z .x) #
sm-equate (DV (.f + .g) .x = DV .f .x + DV .g .x) #
sm-equate (DV (.f * .g) .x = DV .f .x * .g + .f * DV .g .x) #
sm-equate (DV .y .x = (DV .x .y) ^ -1) #
sm-equate (DV .f _ = 0) :- is.int .f #
sm-equate (DV .x .x = 1) #
sm-equate (DV (E ^ .x) .x = E ^ .x) #
sm-equate (DV (LN .x) .x = .x ^ -1) #
sm-equate (DV (SIN .x) .x = COS .x) #
sm-equate (DV (COS .x) .x = -1 * SIN .x) #
sm-equate (.e = .e1) :- sm-rewrite .e .e1 .es/(), member .es (.c .c1), sm-equate (.c = .c1) #
