-- functional program compiler
--
-- Also need to import one of the following backends:
-- fc-eager-evaluation.sl, fc-lazy-evaluation.sl
--
-- .mode can be EAGER or LAZY

() :- import.file 'fc-parse.sl'
	, import.file 'fc-type-inference.sl'
	, import.file 'rbt.sl'
#

compile-function-without-precompile .mode (.lib, .libs) .do .c
	:- !, load-library .lib
	, fc-add-functions .lib .do .do1
	, compile-function-without-precompile .mode .libs .do1 .c
#
compile-function-without-precompile .mode () .do .c
	:- compile-function .mode .do .c
#

compile-function .mode .do .c0
	:- .c0 = (_ ENTER, .c1)
	, !, fc-parse .do .parsed
	, !, infer-type-rule .parsed ()/()/() .tr/() _
	, !, resolve-types .tr
	, !, fc-compile .mode .parsed 0/() .c1/.c2/.d0/()/.reg
	, .c2 = (_ RETURN-VALUE .reg, _ LEAVE, .d0)
	, !, fc-assign-line-number 0 .c0
#

fc-compile .mode (USING .lib .do) .fve .cdr
	:- !, load-precompiled-library .lib
	, fc-compile-using-libs .mode (.lib,) .do .fve .cdr
#

infer-type-rule-using-libs () .do .uvto .tr .type
	:- infer-type-rule .do .uvto .tr .type
#

fc-compile-using-libs .mode () .do .fve .cdr
	:- !, fc-compile .mode .do .fve .cdr
#

load-precompiled-library .lib
	:- once (fc-imported-precompile-library .lib
		; home.dir .homeDir
		, concat .homeDir "/" .lib ".rpn" .rpnFilename
		, file.read .rpnFilename .rpn
		, rpn .precompiled .rpn
		, import .precompiled
	)
#

load-library .lib
	:- once (fc-imported .lib
		; home.dir .homeDir
		, concat .homeDir "/src/main/resources/" .lib ".slf" .slfFilename
		, whatever (file.exists .slfFilename
			, file.read .slfFilename .slf
			, to.atom ".p" .var
			, concat .slf .var .slf1
			, parse .slf1 .node
			, assert (fc-add-functions .lib .var .node)
			, assert (fc-imported-library .lib)
		)
	)
#

fc-frame-difference .frame0 .frame1 0 :- same .frame0 .frame1, ! #
fc-frame-difference .frame0 (.frame1 + 1) .frameDiff
	:- not is.tree .frame0, !
	, fc-frame-difference .frame0 .frame1 .frameDiff0
	, let .frameDiff (.frameDiff0 - 1)
#
fc-frame-difference (.frame0 + 1) (.frame1 + 1) .frameDiff
	:- !, fc-frame-difference .frame0 .frame1 .frameDiff
#

fc-define-default-fun 2 _compare COMPARE #
fc-define-default-fun 2 _cons CONS #
fc-define-default-fun 1 _lhead HEAD #
fc-define-default-fun 1 _log LOG #
fc-define-default-fun 2 _log2 LOG2 #
fc-define-default-fun 1 _ltail TAIL #
fc-define-default-fun 2 _popen POPEN #
fc-define-default-fun 1 _prove PROVE #
fc-define-default-fun 2 _subst SUBST #
fc-define-default-fun 1 _thead HEAD #
fc-define-default-fun 1 _ttail TAIL #
fc-define-default-fun 0 error ERROR #
fc-define-default-fun 1 fflush FFLUSH #
fc-define-default-fun 2 fgetc FGETC #
fc-define-default-fun 4 fputc FPUTC #
fc-define-default-fun 1 is-tree IS-TREE #
fc-define-default-fun 1 is-tuple IS-TREE #

fc-operator .oper
	:- member (' + ', ' - ', ' * ', ' / ', ' %% ',
		' = ', ' != ',
		' > ', ' < ', ' >= ', ' <= ',
	) .oper
#

fc-is-boolean true #
fc-is-boolean false #

fc-assign-line-number _ () #
fc-assign-line-number .n (.n _, .remains)
	:- let .n1 (.n + 1), fc-assign-line-number .n1 .remains
#

fc-error .m :- !, write .m, nl, fail #

fc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
fc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

fc-dict-merge-replace .t0 .t1 .t2 :- rbt-merge-replace .t0 .t1 .t2, ! #

fc-dict-member .v .t :- rbt-member .v .t #

fc-add-functions STANDARD .p (
	define cons = (head => tail => _cons {head} {tail}) >>
	define head = (list => _lhead {list}) >>
	define log = (m => _log {m}) >>
	define log2 = (m => n => _log2 {m} {n}) >>
	define popen = (command => in => _popen {command} {in}) >>
	define prove = (goal => _prove {goal}) >>
	define subst = (var => node => _subst {var} {node}) >>
	define tail = (list => _ltail {list}) >>
	define tuple-head = (list => _thead {list}) >>
	define tuple-tail = (list => _ttail {list}) >>
	define and = (x => y =>
		if x then y else false
	) >>
	define compare = type (:t :- :t => :t => number)
		no-type-check (a => b =>
			if (is-tree {a} && is-tree {b}) then
				let c0 = compare {a | head} {b | head} >>
				if:: c0 = 0
				then:: compare {a | tail} {b | tail}
				else:: c0
			else:: _compare {a} {b}
		)
	>>
	define drop = (n => list =>
		if:: n > 0 && is-tree {list}
		then:: list | tail | drop {n - 1}
		else:: list
	) >>
	define flip = (f => x => y =>
		f {y} {x}
	) >>
	define fold-left = (fun => init =>
		$h, $t => fold-left {fun} {fun {init} {h}} {t}
		|| otherwise init
	) >>
	define fold-right = (fun => init =>
		$h, $t => fun {h} {fold-right {fun} {init} {t}}
		|| otherwise init
	) >>
	define id = (v =>
		v
	) >>
	define maximum = (a => b =>
		if (a > b) then a else b
	) >>
	define minimum = (a => b =>
		if (a > b) then b else a
	) >>
	define not = (x =>
		if x then false else true
	) >>
	define or = (x => y =>
		if x then true else y
	) >>
	define repeat = (n => elem =>
		if (n > 0) then (elem, repeat {n - 1} {elem}) else ()
	) >>
	define scan-left = (fun => init =>
		$h, $t => init, scan-left {fun} {fun {init} {h}} {t}
		|| otherwise (init,)
	) >>
	define scan-right = (fun => init =>
		$h, $t =>
			let r = scan-right {fun} {init} {t} >>
			fun {h} {head {r}}, r
		|| otherwise (init,)
	) >>
	define sink = (os =>
		define fputs = (pos =>
			$c, $cs => fputc {os} {pos} {c} {fputs {pos + 1} {cs}}
			|| otherwise os
		) >>
		fputs {0}
	) >>
	define source = (is =>
		define fgets = (pos =>
			define c = fgetc {is} {pos} >>
			if (c >= 0) then (c, fgets {pos + 1}) else ()
		) >>
		fgets {0}
	) >>
	define str-to-int = (s =>
		let unsigned-str-to-int = fold-left {v => d => v * 10 + d - 48} {0} >>
			if:: is-tree {s} && head {s} = 45
			then:: `0 - ` . unsigned-str-to-int . tail
			else:: unsigned-str-to-int
		{s}
	) >>
	define tails = (
		$h, $t => (h, t), tails {t}
		|| otherwise ()
	) >>
	define take = (n => list =>
		if:: n > 0 && is-tree {list}
		then:: list | tail | take {n - 1} | cons {list | head}
		else:: ()
	) >>
	define take-while = (fun =>
		$elem, $elems =>
			if (fun {elem}) then (elem, take-while {fun} {elems}) else ()
		|| otherwise ()
	) >>
	define tget0 =
		tuple-head
	>>
	define tget1 =
		tuple-head . tuple-tail
	>>
	define tget2 =
		tuple-head . tuple-tail . tuple-tail
	>>
	define unfold-right = (fun => init =>
		let r = fun {init} >>
		if:: is-tree {r}
		then:: r | tail | head | unfold-right {fun} | cons {r | head}
		else:: ()
	) >>
	define zip = (fun =>
		$h0, $t0 => (
			$h1, $t1 => fun {h0} {h1}, zip {fun} {t0} {t1}
			|| otherwise ()
		)
		|| otherwise (anything => ())
	) >>
	define append = (
		$h, $t => cons {h} . append {t}
		|| otherwise id
	) >>
	define apply =
		flip {fold-left {x => f => f {x}}}
	>>
	define equals = type (:t :- :t => :t => boolean)
		no-type-check (a => b => compare {a} {b} = 0)
	>>
	define fold = (fun => list =>
		fold-left {fun} {list | head} {list | tail}
	) >>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item, list) else list
		} {}
	) >>
	define get = (n =>
		head . (tail | repeat {n} | apply)
	) >>
	define length =
		fold-left {v => e => v + 1} {0}
	>>
	define map = (fun =>
		fold-right {i => list => fun {i}, list} {}
	) >>
	define merge = (list0 => list1 =>
		if-bind (list0 = ($h0, $t0)) then
			if-bind (list1 = ($h1, $t1)) then
				if:: h0 < h1
				then:: h0, merge {t0} {list1}
				else-if:: h0 > h1
				then:: h1, merge {list0} {t1}
				else:: h0, h1, merge {t0} {t1}
			else
				list0
		else
			list1
	) >>
	define reverse =
		fold-left {a => b => b, a} {}
	>>
	define substring = (start => end => list =>
		let len = length {list} >>
		let s = (if (start >= 0) then start else (len + start)) >>
		let e = (if (end > 0) then end else (len + end)) >>
		list | take {e} | drop {s}
	) >>
	define uniq =
		fold-right {item => list =>
			if-bind (list = (item, $t)) then list else (item, list)
		} {}
	>>
	define concat =
		fold-left {append} {}
	>>
	define cross = (fun => l1 => l2 =>
		l1 | map {e1 => l2 | map {e1 | fun}}
	) >>
	define int-to-str = (i =>
		let unsigned-int-to-str =
			reverse
			. map {`+ 48`}
			. unfold-right {i => if (i != 0) then (i % 10, i / 10,) else ()}
		>> i |
			if (i > 0) then
				unsigned-int-to-str
			else-if (i < 0) then
				append {"-"} . unsigned-int-to-str . `0 -`
			else
				anything => "0"
	) >>
	define merge-sort = (merge => list =>
		let len = length {list} >>
		if (len > 1) then
			let len2 = len / 2 >>
			define list0 = (list | take {len2} | merge-sort {merge}) >>
			define list1 = (list | drop {len2} | merge-sort {merge}) >>
			merge {list0} {list1}
		else
			list
	) >>
	define range = (start => end => inc =>
		unfold-right {i => if (i < end) then (i, i + inc,) else ()} {start}
	) >>
	define starts-with = (
		$sh, $st => (
			sh, $t => starts-with {st} {t}
			|| otherwise false
		)
		|| otherwise (anything => true)
	) >>
	define split = (separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. tails . cons {separator}
	) >>
	define transpose = type (
		:t :- list-of list-of :t => list-of list-of :t
	) (m =>
		let height = length {m} >>
		let width = if (height > 0) then (m | head | length) else 0 >>
		if (width > 0) then
			let w1 = width - 1 >>
			let gets = (tail | repeat {w1} | tails | reverse | cons {id}) >>
			gets | map {f => map {head . apply {f}} {m}}
		else
			()
	) >>
	define contains = (m =>
		fold-left {or} {false} . map {m | starts-with} . tails
	) >>
	define dump  = type (:t :- :t => list-of number) no-type-check (
		let dump-string = (s =>
			let length = prove-with-result /_s:s (string.length _s _l) _l >>
			0 until length | map {i =>
				prove-with-result /_s:s/_i:i (
					substring _s _i 0 _c, to.int _c _asc
				) _asc
			}
		) >>
		let dump0 = (prec => n =>
			if (is-tree {n}) then
				concat {dump0 {true} {n | head}, ", ", dump0 {false} {n | tail},}
				| if prec then (s => concat {"(", s, ")",}) else id
			else-if (equals {n} {}) then
				"()"
			else-if (prove /_n:n (is.atom _n)) then
				prove-with-result /_n:n (to.string _n _s) _s | dump-string
			else
				int-to-str {n}
		) >>
		dump0 {false}
	) >>
	define ends-with = (end =>
		starts-with {end | reverse} . reverse
	) >>
	define join = (separator =>
		concat . map {separator, | flip {append}}
	) >>
	define quick-sort = (cmp =>
		$pivot, $t =>
			let filter0 = (not . cmp {pivot}) >>
			let filter1 = cmp {pivot} >>
			let l0 = (t | filter {filter0} | quick-sort {cmp}) >>
			let l1 = (t | filter {filter1} | quick-sort {cmp}) >>
			concat {l0, (pivot,), l1,}
		|| otherwise ()
	) >>
	.p
) #
