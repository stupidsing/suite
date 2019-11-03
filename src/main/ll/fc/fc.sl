-------------------------------------------------------------------------------
-- functional program compiler
--
-- .mode can be EAGER or LAZY

() :- import.path "fc/fc-precompile.sl"
#
() :- import.path "cg-generate-code.sl"
	, import.path "fc/fc-bind.sl"
	, import.path "fc/fc-compile.sl"
	, import.path "fc/fc-infer-type.sl"
	, import.path "fc/fc-lazyify.sl"
	, import.path "fc/fc-optimize.sl"
	, import.path "fc/fc-parse.sl"
	, import.path "fc/fc-reduce-tail-recursion.sl"
	, import.path "fc/fc-rewrite.sl"
	, import.path "rbt.sl"
#

cg-is-opt-tail-calls :- fail # -- done by fc-reduce-tail-recursion instead

compile-function .mode .do0 (FRAME l:.c,)
	:- !, fc-process-function .mode .do0 .dox
	, !, .c0 = (ENTER, .c1)
	, fc-compile .dox 0/() .c1/.c2/.reg
	, .c2 = (SET-RESULT .reg
		, LEAVE
		, RETURN
		,)
	, !, cg-optimize .c0 .c
	, !, find.all.memoized.clear
#

fc-process-function .mode .do0 .dox
	:- !, fc-parse .do0 .do1
	, !, fc-infer-type .do1 _
	, once (not (is.cyclic .do1); fc-error "Cyclic data detected")
	, !, once (
		.mode = LAZY, fc-lazyify-fun .do1 .do2
		; .mode = EAGER, fc-reduce-tail-recursion .do1 .do2
	)
	, !, fc-optimize .do2 .dox
#

fc-load-precompiled-library .lib .node
	:- find.all.memoized .node0 (fc-load-precompiled-library0 .lib .node0) (.node,)
#

fc-load-precompiled-library0 .lib .precompiled
	:- fc-precompile-lib-if-required .lib
	, fc-precompiled-library-filename .lib .filename
	, persist.load .precompiled .filename
#

fc-load-library .lib .do0 .dox
	:- find.all.memoized .node0 (fc-load-library0 .lib .node0) (.do0 .dox,)
#

fc-load-library0 .lib .do
	:- once (fc-library-filename .lib .slfFilename
		, file.exists .slfFilename
		, file.read .slfFilename .slf
		, to.atom ".p" .var
		, concat .var " (" .slf .var ")" .slf1
		, parse .slf1 .slf2
		, generalize .slf2 .do
	; fc-error "Library" .lib "not found"
	)
#

fc-precompiled-library-filename .lib .filename
	:- suite.dir .suiteDir
	, concat .suiteDir "/target/precompiled/" .lib ".node.gz" .filename
#

fc-library-filename .lib .filename
	:- suite.dir .suiteDir
	, concat .suiteDir "/src/main/fl/" .lib ".slf" .filename
#

fc-frame-difference .frame0 .frame1 0
	:- same .frame0 .frame1, !
#
fc-frame-difference .frame0 (.frame1 + 1) .frameDiff
	:- not (is.tree .frame0), !
	, fc-frame-difference .frame0 .frame1 .frameDiff0
	, let .frameDiff (.frameDiff0 - 1)
#
fc-frame-difference (.frame0 + 1) (.frame1 + 1) .frameDiff
	:- !, fc-frame-difference .frame0 .frame1 .frameDiff
#

fc-define-default-fun 2 +call%i-t1 CALL-INTRINSIC #
fc-define-default-fun 3 +call%i-t2 CALL-INTRINSIC #
fc-define-default-fun 4 +call%i-t3 CALL-INTRINSIC #
fc-define-default-fun 2 +call%i-v1 CALL-INTRINSIC #
fc-define-default-fun 3 +call%i-v2 CALL-INTRINSIC #
fc-define-default-fun 4 +call%i-v3 CALL-INTRINSIC #
fc-define-default-fun 2 +compare COMPARE #
fc-define-default-fun 1 +get%i GET-INTRINSIC #
fc-define-default-fun 1 +is-list IS-CONS #
fc-define-default-fun 1 +is-pair IS-CONS #
fc-define-default-fun 2 +lcons CONS-LIST #
fc-define-default-fun 1 +lhead HEAD #
fc-define-default-fun 1 +ltail TAIL #
fc-define-default-fun 2 +pcons CONS-PAIR #
fc-define-default-fun 1 +pleft HEAD #
fc-define-default-fun 1 +pright TAIL #

fc-error .m :- !, write.error .m, nl, fail #

fc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
fc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

fc-dict-union-bind .t0 .t1 .t2 :- rbt-union-bind .t0 .t1 .t2, ! #

fc-dict-union-insert .t0 .t1 .t2 :- rbt-union-insert .t0 .t1 .t2, ! #

fc-dict-union-replace .t0 .t1 .t2 :- rbt-union-replace .t0 .t1 .t2, ! #

fc-dict-member .v .t :- rbt-member .v .t #
