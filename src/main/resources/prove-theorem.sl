() :- import.file 'rbt.sl'
#

-- 1. convert implication operators: (P => Q) becomes (~P ^ Q)
pt-prove0 (IMP .p .q) (AND (NOT .p1) .q1)
	:- !, pt-prove0 .p .p1, pt-prove0 .q .q1
#
pt-prove0 .p .p1
	:- pt-transform .p .p1 .ts
	, pt-prove0-list .ts
#

pt-prove0-list () #
pt-prove0-list (.p .p1, .ts) .ts :- pt-prove0 .p .p1 , pt-prove0-list .ts
#

-- 2. move negation inwards
pt-prove1 (NOT (AND .p .q)) (NOT (OR (NOT .p1) (NOT .q1)))
	:- !, pt-prove1 .p .p1, pt-prove1 .q .q1
#
pt-prove1 (NOT (OR .p .q)) (NOT (AND (NOT .p1) (NOT .q1)))
	:- !, pt-prove1 .p .p1, pt-prove1 .q .q1
#
pt-prove1 .p .p1 :- pt-transform .p .p1 .ts, pt-prove1-list .ts #

pt-prove1-list () #
pt-prove1-list (.p .p1, .ts) .ts :- pt-prove1 .p .p1, pt-prove1-list .ts #

-- 2a. remove double negatives
pt-prove1a (NOT NOT .p) .p1 :- !, pt-prove1a .p .p1 #
pt-prove1a .p .p1 :- pt-transform .p .p1 .ts, pt-prove1a-list .ts #

pt-prove1a-list () #
pt-prove1a-list (.p .p1, .ts) .ts :- pt-prove1a .p .p1, pt-prove1a-list .ts #

-- 3. skolemising
pt-prove2 .vars (FOR-ALL .var .p) (FOR-ALL .var .p1)
	:- !, pt-prove2 (.var, .vars) .p .p1
#
pt-prove2 .vars (THERE-EXISTS .var .p) .p1
	:- !, temp .functionName
	, form-function .vars .functionName .var1
	, replace .var .var1 .p .p1
#
pt-prove2 .vars .p .p1
	:- pt-transform .p .p1 .ts
	, pt-prove2-list .vars .ts
#

pt-prove2-list _ () #
pt-prove2-list .vars (.p .p1, .ts) .ts
	:- pt-prove2 .vars .p .p1
	, pt-prove2-list .vars .ts
#

form-function () .p .p #
form-function (.var, .vars) .p (FN .var .p1) :- form-function .vars .p .p1 #

-- 4. moving "for-all" outwards
pt-prove3 (AND (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (AND .p .q) .pq1
#
pt-prove3 (AND .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (AND .p .q) .pq1
#
pt-prove3 (NOT (FOR-ALL .e .p)) (FOR-ALL .e (NOT .p1))
	:- !, pt-prove3 .p .p1
#
pt-prove3 (OR (FOR-ALL .e .p) .q) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (OR .p .q) .pq1
#
pt-prove3 (OR .p (FOR-ALL .e .q)) (FOR-ALL .e .pq1)
	:- !, pt-prove3 (OR .p .q) .pq1
#
pt-prove3 .p .p1 :- pt-transform .p .p1 .ts, pt-prove3-list .ts
#

pt-prove3-list () #
pt-prove3-list (.p .p1, .ts) .ts :- pt-prove3 .p .p1 , pt-prove3-list .ts #

-- 5. distributing AND over OR
pt-prove4 (OR (AND .p .q) .r) .pqr :- !, pt-prove4 (AND (OR .p .r) (OR .q .r)) .pqr #
pt-prove4 (OR .p (AND .q .r)) .pqr :- !, pt-prove4 (AND (OR .p .q) (OR .p .r)) .pqr #
pt-prove4 .p .p1 :- pt-transform .p .p1 .ts, pt-prove4-list .ts
#

pt-prove4-list () #
pt-prove4-list (.p .p1, .ts) .ts :- pt-prove4 .p .p1 , pt-prove4-list .ts #

-- 6. putting into clauses (ANDs of ORs form)
pt-prove5-index (OR .p .q)
	:- !, pt-prove5-index .p, pt-prove5-index .q
#
pt-prove5-index .p
	:- pt-prove5-index-ands .p ()/.rbt
#
-- ...

pt-prove5-index-ands (AND .p .q) .rb0/.rbx
	:- !
	, pt-prove5-index-ands .p .rb0/.rb1
	, pt-prove5-index-ands .q .rb1/.rbx
#
pt-prove5-index-ands .p .rb0/.rbx
	:- rbt-get .rb0 .p:.list
	, once (bound .list; .list = ())
	, rbt-replace .p:(.pre:.post, .list) .rb0/.rbx
#

pt-transform (IMP .p .q) (AND (NOT .p1) .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (AND .p .q) (AND .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (FOR-ALL .e .p) (FOR-ALL .e .p1) (.p .p1,) :- ! #
pt-transform (NOT .p) (NOT .p1) (.p .p1,) :- ! #
pt-transform (OR .p .q) (OR .p1 .q1) (.p .p1, .q .q1,) :- ! #
pt-transform (THERE-EXISTS .e .p) (THERE-EXISTS .e .p1) (.p .p1,) :- ! #
pt-transform (VAR .var) (VAR .var) () #
