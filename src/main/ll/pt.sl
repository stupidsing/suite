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
pt-prove0 (IMP .p0 .q0) (AND (NOT .px) .qx)
	:- !, pt-prove0 .p0 .px, pt-prove0 .q0 .qx
#
pt-prove0 .p0 .px :- pt-rewrite .p0 .px .ts, list.query .ts (.t0 .tx) (pt-prove0 .t0 .tx)
#

-- 1. Move negation inwards
pt-prove1 (NOT (AND .p0 .q0)) (NOT (OR (NOT .px) (NOT .qx)))
	:- !, pt-prove1 .p0 .px, pt-prove1 .q0 .qx
#
pt-prove1 (NOT (OR .p0 .q0)) (NOT (AND (NOT .px) (NOT .qx)))
	:- !, pt-prove1 .p0 .px, pt-prove1 .q0 .qx
#
pt-prove1 .p0 .px :- pt-rewrite .p0 .px .ts, list.query .ts (.t0 .tx) (pt-prove1 .t0 .tx)
#

-- 2. Remove double negatives
pt-prove2 (NOT NOT .p0) .px :- !, pt-prove2 .p0 .px #
pt-prove2 .p0 .px :- pt-rewrite .p0 .px .ts, list.query .ts (.t0 .tx) (pt-prove2 .t0 .tx) #

-- 3. Moving "for-all" outwards
pt-prove3 (AND (FOR-ALL .e .p) .q) (FOR-ALL .e .pq)
	:- !, pt-prove3 (AND .p .q) .pq
#
pt-prove3 (AND .p (FOR-ALL .e .q)) (FOR-ALL .e .pq)
	:- !, pt-prove3 (AND .p .q) .pq
#
pt-prove3 (NOT (FOR-ALL .e .p0)) (THERE-EXISTS .e (NOT .px))
	:- !, pt-prove3 .p0 .px
#
pt-prove3 (NOT (THERE-EXISTS .e .p0)) (FOR-ALL .e (NOT .px))
	:- !, pt-prove3 .p0 .px
#
pt-prove3 (OR (FOR-ALL .e .p) .q) (FOR-ALL .e .pq)
	:- !, pt-prove3 (OR .p .q) .pq
#
pt-prove3 (OR .p (FOR-ALL .e .q)) (FOR-ALL .e .pq)
	:- !, pt-prove3 (OR .p .q) .pq
#
pt-prove3 .p0 .px :- pt-rewrite .p0 .px .ts, list.query .ts (.t0 .tx) (pt-prove3 .t0 .tx)
#

-- 4. Skolemising
pt-prove4 .p0 .px :- pt-prove4-skolemise () .p0 .px
#
pt-prove4-skolemise .vars (FOR-ALL .var .p0) (FOR-ALL .var .px)
	:- !, pt-prove4-skolemise (.var, .vars) .p0 .px
#
pt-prove4-skolemise .vars (THERE-EXISTS .var .p0) .px
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .var .var1 .p0 .px
#
pt-prove4-skolemise .vars .p0 .px
	:- pt-rewrite .p0 .px .ts
	, list.query .ts (.t0 .tx) (pt-prove4-skolemise .vars .t0 .tx)
#

form-function () .p .p #
form-function (.var, .vars) .p0 (FN .var .px) :- form-function .vars .p0 .px #

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

-- basic transform function
pt-rewrite (IMP .p0 .q0) (IMP .px .qx) (.p0 .px, .q0 .qx,) :- ! #
pt-rewrite (AND .p0 .q0) (AND .px .qx) (.p0 .px, .q0 .qx,) :- ! #
pt-rewrite (FOR-ALL .e .p0) (FOR-ALL .e .px) (.p0 .px,) :- ! #
pt-rewrite (NOT .p0) (NOT .px) (.p0 .px,) :- ! #
pt-rewrite (OR .p0 .q0) (OR .px .qx) (.p0 .px, .q0 .qx,) :- ! #
pt-rewrite (THERE-EXISTS .e .p0) (THERE-EXISTS .e .px) (.p0 .px,) :- ! #
pt-rewrite (VAR .var) (VAR .var) () #
