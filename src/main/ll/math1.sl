-------------------------------------------------------------------------------
-- symbolic mathematics

sm-rewrite 0 0 .es/.es #
sm-rewrite 1 1 .es/.es #
sm-rewrite -1 -1 .es/.es #
sm-rewrite .e .e .es/.es :- is.atom .e #
sm-rewrite (.x0 + .y0) (.x1 + .y1) (.x0 .x1, .y0 .y1, .es)/.es #
sm-rewrite (.x0 * .y0) (.x1 * .y1) (.x0 .x1, .y0 .y1, .es)/.es #
sm-rewrite (.x0 ^ .y0) (.x1 ^ .y1) (.x0 .x1, .y0 .y1, .es)/.es #
sm-rewrite E E .es/.es #
sm-rewrite PI PI .es/.es #
sm-rewrite (LN .x0) (LN .x1) (.x0 .x1, .es)/.es #
sm-rewrite (SIN .x0) (SIN .x1) (.x0 .x1, .es)/.es #
sm-rewrite (COS .x0) (COS .x1) (.x0 .x1, .es)/.es #
sm-rewrite (D .x0) (D .x1) (.x0 .x1, .es)/.es #

sm-reduce .c (.c0 + 1) :- is.int .c, .c > 0, let .c0 (.c - 1) #
sm-reduce .c (.c0 + -1) :- is.int .c, .c < 0, let .c0 (.c + 1) #
sm-reduce 0.5 ((1 + 1) ^ -1) #
sm-reduce (.x - .y) (.x1 + -1 * .y1) :- sm-reduce .x .x1, sm-reduce .y .y1 #
sm-reduce (.x / .y) (.x1 * .y1 ^ -1) :- sm-reduce .x .x1, sm-reduce .y .y1 #
sm-reduce (TAN .x) (SIN .x1 / COS .x1) :- sm-reduce .x .x1 #
sm-reduce (DV .y .x) (D .y1 * (D .x1) ^ -1) :- sm-reduce .x .x1, sm-reduce .y .y1 #
sm-reduce .e .e1 :- sm-rewrite .e .e1 .es/(), sm-reduce-list .es #

sm-reduce-list () #
sm-reduce-list (.e .e1, .es) :- sm-reduce .e .e1, sm-reduce-list .es #

sm-equate (.f = .g)
	:- sm-equate0 .f0 .g0
	, sm-reduce .f0 .f
	, sm-reduce .g0 .g
#

sm-equate0 (.f + .g = .g + .f) #
sm-equate0 (.f * .g = .g * .f) #
sm-equate0 (.f + (.g + .h) = (.f + .g) + .h) #
sm-equate0 (.f * (.g * .h) = (.f * .g) * .h) #
sm-equate0 (.f + 0 = .f) #
sm-equate0 (_ * 0 = 0) #
sm-equate0 (.f * 1 = .f) #
sm-equate0 (0 ^ _ = 0) #
sm-equate0 (1 ^ _ = 1) #
sm-equate0 (_ ^ 0 = 1) #
sm-equate0 (.f ^ 1 = .f) #
sm-equate0 (.f * (.g + .h) = .f * .g + .f * .h) #
sm-equate0 (.f ^ (.g + .h) = .f ^ .g * .f ^ .h) #
sm-equate0 (.f ^ (.g * .h) = (.f ^ .g) ^ .h) #

sm-equate0 (E ^ LN .f = .f) #
sm-equate0 (LN (E ^ .f) = .f) #
sm-equate0 (LN (.f * .g) = LN .f + LN .g) #
sm-equate0 (LN (.f ^ .g) = .g * LN .f) #
sm-equate0 (ASIN SIN .f = .f) #
sm-equate0 (ACOS COS .f = .f) #
sm-equate0 (SIN 0 = 0) #
sm-equate0 (SIN (PI * 0.5) = 1) #
sm-equate0 (COS 0 = 1) #
sm-equate0 (COS (PI * 0.5) = 0) #
sm-equate0 (SIN (-1 * .f) = -1 * SIN .f) #
sm-equate0 (COS (-1 * .f) = COS .f) #
sm-equate0 (SIN (.f + .g) = SIN .f * COS .g + COS .f * SIN .g) #
sm-equate0 (COS (.f + .g) = COS .f * COS .g - SIN .f * SIN .g) #
sm-equate0 (2 * SIN .f * SIN .g = COS (.f - .g) - COS (.f + .g)) #
sm-equate0 (2 * SIN .f * COS .g = SIN (.f + .g) + SIN (.f - .g)) #
sm-equate0 (2 * COS .f * COS .g = COS (.f - .g) + COS (.f + .g)) #

sm-equate0 (D (.f + .g) = D .f + D .g) #
sm-equate0 (DV (.f * .g) .x = DV .f .x * .g + .f * DV .g .x) #
sm-equate0 (D 0 = 0) #
sm-equate0 (D 1 = 0) #
sm-equate0 (D -1 = 0) #
sm-equate0 (DV (E ^ .x) .x = E ^ .x) #
sm-equate0 (DV (LN .x) .x = .x ^ -1) #
sm-equate0 (DV (SIN .x) .x = COS .x) #
sm-equate0 (DV (COS .x) .x = -1 * SIN .x) #

sm-equate0 (.e = .e1) :- sm-rewrite .e .e1 .es/(), member .es (.c .c1), sm-equate0 (.c = .c1) #
