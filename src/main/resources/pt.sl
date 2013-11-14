() :- import.file 'rbt.sl'
#

-- 0. Convert implication operators: (P => Q) becomes (~P ^ Q)
pt-prove0 (IMP .p .q) (AND (NOT .p1) .q1)
	:- !, pt-prove0 .p .p1, pt-prove0 .q .q1
#
pt-prove0 .p .p1
	:- pt-transform .p .p1 .ts
	, pt-prove0-list .ts
#

pt-prove0-list () #
pt-prove0-list (.p .p1, .ts) :- pt-prove0 .p .p1, pt-prove0-list .ts #

-- 1. Move negation inwards
pt-prove1 (NOT (AND .p .q)) (NOT (OR (NOT .p1) (NOT .q1)))
	:- !, pt-prove1 .p .p1, pt-prove1 .q .q1
#
pt-prove1 (NOT (OR .p .q)) (NOT (AND (NOT .p1) (NOT .q1)))
	:- !, pt-prove1 .p .p1, pt-prove1 .q .q1
#
pt-prove1 .p .p1 :- pt-transform .p .p1 .ts, pt-prove1-list .ts #

pt-prove1-list () #
pt-prove1-list (.p .p1, .ts) :- pt-prove1 .p .p1, pt-prove1-list .ts #

-- 2. Remove double negatives
pt-prove2 (NOT NOT .p) .p1 :- !, pt-prove2 .p .p1 #
pt-prove2 .p .p1 :- pt-transform .p .p1 .ts, pt-prove2-list .ts #

pt-prove2-list () #
pt-prove2-list (.p .p1, .ts) :- pt-prove2 .p .p1, pt-prove2-list .ts #

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
pt-prove3 .p .p1 :- pt-transform .p .p1 .ts, pt-prove3-list .ts #

pt-prove3-list () #
pt-prove3-list (.p .p1, .ts) :- pt-prove3 .p .p1, pt-prove3-list .ts #

-- 4. Skolemising
pt-prove4 .vars (FOR-ALL .var .p) (FOR-ALL .var .p1)
	:- !, pt-prove4 (.var, .vars) .p .p1
#
pt-prove4 .vars (THERE-EXISTS .var .p) .p1
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .var .var1 .p .p1
#
pt-prove4 .vars .p .p1
	:- pt-transform .p .p1 .ts
	, pt-prove4-list .vars .ts
#

pt-prove4-list _ () #
pt-prove4-list .vars (.p .p1, .ts)
	:- pt-prove4 .vars .p .p1
	, pt-prove4-list .vars .ts
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
		pt-prove6-extract .ands (NOT .term) .corrs1
		, pt-prove6-extract .ands .term .corrs0
		, append .corrs0 .corrs1 .ors
	) .results
#

pt-prove6-extract (.ors, .ands) .term .corrs
	:- pt-prove6-extract-ors () .ors .term .corrs
	, pt-prove6-extract .ands
#

pt-prove6-extract-ors _ () _ _ #
pt-prove6-extract-ors .pre (.term, .post) .term .corrs
	:- append .pre .post .corrs
#

-- Basic transform function
pt-transform (IMP .p .q) (IMP .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (AND .p .q) (AND .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (FOR-ALL .e .p) (FOR-ALL .e .p1) (.p .p1,) :- ! #
pt-transform (NOT .p) (NOT .p1) (.p .p1,) :- ! #
pt-transform (OR .p .q) (OR .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1) (.p .p1,) :- ! #
pt-transform (VAR .var) (VAR .var) () #
