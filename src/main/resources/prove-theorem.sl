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

-- 3. Skolemising
pt-prove3 .vars (FOR-ALL .var .p) (FOR-ALL .var .p1)
	:- !, pt-prove3 (.var, .vars) .p .p1
#
pt-prove3 .vars (THERE-EXISTS .var .p) .p1
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .var .var1 .p .p1
#
pt-prove3 .vars .p .p1
	:- pt-transform .p .p1 .ts
	, pt-prove3-list .vars .ts
#

pt-prove3-list _ () #
pt-prove3-list .vars (.p .p1, .ts)
	:- pt-prove3 .vars .p .p1
	, pt-prove3-list .vars .ts
#

form-function () .p .p #
form-function (.var, .vars) .p (FN .var .p1) :- form-function .vars .p .p1 #

-- 4. Moving "for-all" outwards
pt-prove4 (AND (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, pt-prove4 (AND .p .q) .pq1
#
pt-prove4 (AND .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, pt-prove4 (AND .p .q) .pq1
#
pt-prove4 (NOT (FOR-ALL .e .p)) (FOR-ALL .e (NOT .p1))
	:- !, pt-prove4 .p .p1
#
pt-prove4 (OR (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, pt-prove4 (OR .p .q) .pq1
#
pt-prove4 (OR .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, pt-prove4 (OR .p .q) .pq1
#
pt-prove4 .p .p1 :- pt-transform .p .p1 .ts, pt-prove4-list .ts
#

pt-prove4-list () #
pt-prove4-list (.p .p1, .ts) :- pt-prove4 .p .p1, pt-prove4-list .ts #

-- 5. Distributing AND over OR
pt-prove5 (AND .p .q) .as0/.asx
	:- !
	, pt-prove5 .p .as0/.as1
	, pt-prove5 .q .as1/.asx
#
pt-prove5 .p0 .as
	:- (.p0 = OR (AND .p .q) .r; .p0 = OR .r (AND .p .q))
	, !, pt-prove5 (AND (OR .p .r) (OR .q .r)) .as
#
pt-prove5 .p .as0/.asx
	:- !, pt-prove5-ors .p ()/.os, .asx = (.os, .as0)
#

pt-prove5-ors (OR .p .q) .os0/.osx
	:- !
	, pt-prove5-ors .p .os0/.os1
	, pt-prove5-ors .q .os1/.osx
#
pt-prove5-ors .p .os0/.osx
	:- !, .osx = (.p, .os0)
#

-- 6. Put into clauses (ANDs of ORs form)
-- Moved to Java, easier coding

-- Basic transform function
pt-transform (IMP .p .q) (IMP .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (AND .p .q) (AND .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (FOR-ALL .e .p) (FOR-ALL .e .p1) (.p .p1,) :- ! #
pt-transform (NOT .p) (NOT .p1) (.p .p1,) :- ! #
pt-transform (OR .p .q) (OR .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1) (.p .p1,) :- ! #
pt-transform (VAR .var) (VAR .var) () #
