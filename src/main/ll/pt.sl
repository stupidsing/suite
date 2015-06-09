pt-prove .n0 .nx
	:- pt-prove0 .n0 .n1
	, pt-prove1 .n1 .n2
	, pt-prove2 .n2 .n3
	, pt-prove3 .n3 .n4
	, pt-prove4 .n4 .n5
	, pt-prove5 .n5 ()/.n6
	, pt-prove6 .n6 .nx
#

-- 0. Convert implication operators: (P => Q) becomes (~P ^ Q)
pt-prove0 (IMP .p .q) (AND (NOT .p1) .q1)
	:- !, pt-prove0 .p .p1, pt-prove0 .q .q1
#
pt-prove0 .p .p1 :- pt-rewrite .p .p1 .ts, list.query .ts .t (pt-prove0 .t)
#

-- 1. Move negation inwards
pt-prove1 (NOT (AND .p .q)) (NOT (OR (NOT .p1) (NOT .q1)))
	:- !, pt-prove1 .p .p1, pt-prove1 .q .q1
#
pt-prove1 (NOT (OR .p .q)) (NOT (AND (NOT .p1) (NOT .q1)))
	:- !, pt-prove1 .p .p1, pt-prove1 .q .q1
#
pt-prove1 .p .p1 :- pt-rewrite .p .p1 .ts, list.query .ts .t (pt-prove1 .t)
#

-- 2. Remove double negatives
pt-prove2 (NOT NOT .p) .p1 :- !, pt-prove2 .p .p1 #
pt-prove2 .p .p1 :- pt-rewrite .p .p1 .ts, list.query .ts .t (pt-prove2 .t) #

-- 3. Moving "for-all" outwards
pt-prove3 (AND (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (AND .p .q) .pq1
#
pt-prove3 (AND .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (AND .p .q) .pq1
#
pt-prove3 (NOT (FOR-ALL .e .p)) (THERE-EXISTS .e (NOT .p1))
	:- !, pt-prove3 .p .p1
#
pt-prove3 (NOT (THERE-EXISTS .e .p)) (FOR-ALL .e (NOT .p1))
	:- !, pt-prove3 .p .p1
#
pt-prove3 (OR (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (OR .p .q) .pq1
#
pt-prove3 (OR .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (OR .p .q) .pq1
#
pt-prove3 .p .p1 :- pt-rewrite .p .p1 .ts, list.query .ts .t (pt-prove3 .t)
#

-- 4. Skolemising
pt-prove4 .p .p1 :- pt-prove4-skolemise () .p .p1 #

pt-prove4-skolemise .vars (FOR-ALL .var .p) (FOR-ALL .var .p1)
	:- !, pt-prove4-skolemise (.var, .vars) .p .p1
#
pt-prove4-skolemise .vars (THERE-EXISTS .var .p) .p1
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .var .var1 .p .p1
#
pt-prove4-skolemise .vars .p .p1
	:- pt-rewrite .p .p1 .ts
	, list.query .ts (.t .t1) (pt-prove4-skolemise .vars .t .t1)
#

form-function () .p .p #
form-function (.var, .vars) .p (FN .var .p1) :- form-function .vars .p .p1 #

-- 5. Distributing AND over OR
pt-prove5 (AND .p .q) .ands0/.andsx
	:- !
	, pt-prove5 .p .ands0/.ands1
	, pt-prove5 .q .ands1/.andsx
#
pt-prove5 .p0 .ands
	:- (.p0 = OR (AND .p .q) .r; .p0 = OR .r (AND .p .q))
	, !, pt-prove5 (AND (OR .p .r) (OR .q .r)) .ands
#
pt-prove5 .p .ands0/.andsx
	:- !, pt-prove5-ors .p ()/.os, .andsx = (.os, .ands0)
#

pt-prove5-ors (OR .p .q) .ors0/.orsx
	:- !
	, pt-prove5-ors .p .ors0/.ors1
	, pt-prove5-ors .q .ors1/.orsx
#
pt-prove5-ors .p .ors0/.orsx
	:- !, .orsx = (.p, .ors0)
#

-- 6. Put into clauses (ANDs of ORs form)
pt-prove6 .ands .results
	:- find.all .ors (
		pt-prove6-extract .ands (NOT .term) .corrs0
		, pt-prove6-extract .ands .term .corrs1
		, append .corrs0 .corrs1 .ors
	) .results
#

pt-prove6-extract (.ors, _) .term .corrs
	:- pt-prove6-extract-ors () .ors .term .corrs
#
pt-prove6-extract (_, .ands) .term .corrs
	:- pt-prove6-extract .ands .term .corrs
#

pt-prove6-extract-ors _ () _ _ #
pt-prove6-extract-ors .pre (.term, .post) .term .corrs
	:- append .pre .post .corrs
#

-- Basic transform function
pt-rewrite (IMP .p .q) (IMP .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-rewrite (AND .p .q) (AND .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-rewrite (FOR-ALL .e .p) (FOR-ALL .e .p1) (.p .p1,) :- ! #
pt-rewrite (NOT .p) (NOT .p1) (.p .p1,) :- ! #
pt-rewrite (OR .p .q) (OR .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-rewrite (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1) (.p .p1,) :- ! #
pt-rewrite (VAR .var) (VAR .var) () #
