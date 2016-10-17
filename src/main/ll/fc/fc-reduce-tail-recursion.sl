-- in :: Param
-- iter :: Param -> (Either Param Value)
-- tco :: (Param -> (Either Param Value)) -> Param -> Value

fc-reduce-tail-recursion (DEF-VARS (.fun .do0,) .value0) (DEF-VARS (.fun .dox,) .valuex)
	:- fc-reduce-tail-recursion0 .fun ()/.vars .do0 .iter .flag
	, bound .flag
	, !
	, temp .in
	, fc-vars-expression .vars .expr0
	, fc-bind-expression .vars .expr1
	, fc-bind (VAR .in) .expr1 .iter (ERROR ()) .do1
	, fc-reduce-tail-recursion .do1 .do2
	, fc-vars-fun .vars (TCO (FUN .in .do2) .expr0) .dox
	, fc-reduce-tail-recursion .value0 .valuex
#
fc-reduce-tail-recursion .do0 .dox
	:- fc-rewrite .do0 .dox .ts/()
	, list.query .ts (.t0 .t1) (fc-reduce-tail-recursion .t0 .t1)
#

fc-reduce-tail-recursion0 .fun .vars/.vars .do (CONS P (BOOLEAN false) CONS P .expr (PRAGMA TYPE-SKIP-CHECK NUMBER 0)) Y
	:- length .vars .length
	, length .values .length
	, fc-expression .values .expr
	, fc-values-invoke .values (VAR .fun) .do
	, !
#
fc-reduce-tail-recursion0 .fun .vars0/.varsx (DEF-VARS .vars .do0) (DEF-VARS .vars .dox) .flag
	:- !
	, fc-reduce-tail-recursion0 .fun .vars0/.varsx .do0 .dox .flag
#
fc-reduce-tail-recursion0 .fun .vars0/.varsx (FUN .var .do) .pair .flag
	:- !
	, .vars1 = (.var, .vars0)
	, fc-reduce-tail-recursion0 .fun .vars1/.varsx .do .pair .flag
#
fc-reduce-tail-recursion0 .fun .vars (PRAGMA .pragma .do0) (PRAGMA .pragma .dox) .flag
	:- !
	, fc-reduce-tail-recursion0 .fun .vars .do0 .dox .flag
#
fc-reduce-tail-recursion0 .fun .vars (IF .if .then .else) (IF .if .then1 .else1) .flag
	:- !
	, fc-reduce-tail-recursion0 .fun .vars .then .then1 .flag
	, fc-reduce-tail-recursion0 .fun .vars .else .else1 .flag
#
fc-reduce-tail-recursion0 _ .vars/.vars .do (CONS P (BOOLEAN true) CONS P (PRAGMA TYPE-SKIP-CHECK NUMBER 0) .do) _
#

fc-expression () NIL #
fc-expression (.var, .vars) (CONS P .var .vars1) :- fc-expression .vars .vars1 #

fc-vars-expression () NIL #
fc-vars-expression (.var, .vars) (CONS P (VAR .var) .vars1) :- fc-vars-expression .vars .vars1 #

fc-bind-expression () NIL #
fc-bind-expression (.var, .vars) (CONS P (PRAGMA NEW (VAR .var)) .vars1) :- fc-bind-expression .vars .vars1 #

fc-vars-fun () .do .do #
fc-vars-fun (.var, .vars) .do .do1 :- fc-vars-fun .vars (FUN .var .do) .do1 #

fc-values-invoke () .do .do #
fc-values-invoke (.value, .values) .do (APPLY .value .do1) :- fc-values-invoke .values .do .do1 #
