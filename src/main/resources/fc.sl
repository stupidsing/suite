-- functional program compiler
--
-- Also need to import one of the following backends:
-- fc-eager-evaluation.sl, fc-lazy-evaluation.sl
--
-- .mode can be EAGER or LAZY

() :- import.file 'fc-parse.sl'
	, import.file 'fc-type-inference.sl'
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
	, !, infer-type-rule .parsed ()/()/()/() .tr0/.trx _
	, !, resolve-types .tr0/.trx
	, !, fc-compile .mode .parsed 0/() .c1/.c2/.d0/()/.reg
	, .c2 = (_ EXIT .reg, _ LEAVE, .d0)
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
fc-define-default-fun 1 _ltail TAIL #
fc-define-default-fun 1 _prove PROVE #
fc-define-default-fun 2 _subst SUBST #
fc-define-default-fun 1 _thead HEAD #
fc-define-default-fun 1 _ttail TAIL #
fc-define-default-fun 1 fflush FFLUSH #
fc-define-default-fun 1 fgetc FGETC #
fc-define-default-fun 3 fputc FPUTC #
fc-define-default-fun 1 is-tree IS-TREE #
fc-define-default-fun 1 log LOG #
fc-define-default-fun 2 log2 LOG2 #

fc-is-tuple-name () :- ! # -- Empty atom is list terminator
fc-is-tuple-name .t
	:- is.atom .t, to.string .t .s, substring .s 0 1 .c
	, .c >= "A", .c <= "Z"
#

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

fc-add-functions STANDARD .p (
	define cons = (head => tail => _cons {head} {tail}) >>
	define head = (list => _lhead {list}) >>
	define prove = (goal => _prove {goal}) >>
	define subst = (var => node => _subst {var} {node}) >>
	define tail = (list => _ltail {list}) >>
	define tuple-head = (list => _thead {list}) >>
	define tuple-tail = (list => _ttail {list}) >>
	define and = (x => y =>
		if x then y else false
	) >>
	define compare as (any :t in (:t => :t => number)) = no-type-check (
		a => b =>
		if (is-tree {a} && is-tree {b}) then
			let c0 = compare {head | a} {head | b} >>
			if:: c0 = 0
			then:: compare {tail | a} {tail | b}
			else:: c0
		else:: _compare {a} {b}
	) >>
	define drop = (n => list =>
		if:: n > 0 && is-tree {list}
		then:: drop {n - 1} | tail | list
		else:: list
	) >>
	define flip = (f => x => y =>
		f {y} {x}
	) >>
	define fold-left = (fun => init =>
		if-match:: \h, \t
		then:: fold-left {fun} {fun {init} {h}} {t}
		else:: init
	) >>
	define fold-right = (fun => init =>
		if-match:: \h, \t
		then:: fun {h} {fold-right {fun} {init} {t}}
		else:: init
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
		if-match:: \h, \t
		then:: init, scan-left {fun} {fun {init} {h}} {t}
		else:: init,
	) >>
	define scan-right = (fun => init =>
		if-match (\h, \t) then
			let r = scan-right {fun} {init} {t} >>
			fun {h} {head {r}}, r
		else
			init,
	) >>
	define str-to-int = (s =>
		let unsigned-str-to-int = fold-left {v => d => v * 10 + d - 48} {0} >>
			if:: is-tree {s} && head {s} = 45
			then:: `0 - ` . unsigned-str-to-int . tail
			else:: unsigned-str-to-int
		| s
	) >>
	define tails =
		if-match:: \h, \t
		then:: (h, t), tails {t}
		else:: ()
	>>
	define take = (n => list =>
		if:: n > 0 && is-tree {list}
		then:: cons {head | list} | take {n - 1} | tail | list
		else:: ()
	) >>
	define take-while = (fun =>
		if-match:: \elem, \elems
		then:: if (fun {elem}) then (elem, take-while {fun} {elems}) else ()
		else:: ()
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
		then:: cons {head | r} | unfold-right {fun} | head | tail | r
		else:: ()
	) >>
	define zip = (fun =>
		if-match (\h0, \t0) then
			if-match:: \h1, \t1
			then:: fun {h0} {h1}, zip {fun} {t0} {t1}
			else:: ()
		else
			anything => ()
	) >>
	define apply =
		fold-left {x => f => f {x}}
	>>
	define equals as (any :t in (:t => :t => boolean)) =
		no-type-check (a => b => compare {a} {b} = 0)
	>>
	define fold = (fun => list =>
		fold-left {fun} {head | list} {tail | list}
	) >>
	define concat2 = (
		if-match:: \h, \t
		then:: cons {h} . concat2 {t}
		else:: id
	) >>
	define filter = (fun =>
		fold-right {
			item => list => if (fun {item}) then (item, list) else list
		} {}
	) >>
	define get = (n =>
		head . (flip {apply} | repeat {n} | tail)
	) >>
	define length =
		fold-left {v => e => v + 1} {0}
	>>
	define map = (fun =>
		fold-right {i => list => fun {i}, list} {}
	) >>
	define merge = (list0 => list1 =>
		if-bind (list0 = (\h0, \t0)) then
			if-bind (list1 = (\h1, \t1)) then
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
		drop {s} | take {e} | list
	) >>
	define uniq =
		fold-right {item => list =>
			if-bind (list = (item, \t)) then list else (item, list)
		} {}
	>>
	define concat =
		fold-left {concat2} {}
	>>
	define cross = (fun => l1 => l2 =>
		map {e1 => map {fun | e1} | l2} | l1
	) >>
	define int-to-str = (i =>
		let unsigned-int-to-str =
			reverse
			. map {`+ 48`}
			. unfold-right {i => if (i != 0) then (i % 10, i / 10,) else ()}
		>>
		if (i > 0) then
			unsigned-int-to-str
		else-if (i < 0) then
			concat2 {"-"} . unsigned-int-to-str . `0 -`
		else
			anything => "0"
		| i
	) >>
	define merge-sort = (merge => list =>
		let len = length {list} >>
		if (len > 1) then
			let len2 = len / 2 >>
			define list0 = (merge-sort {merge} | take {len2} | list) >>
			define list1 = (merge-sort {merge} | drop {len2} | list) >>
			merge {list0} {list1}
		else
			list
	) >>
	define range = (start => end => inc =>
		unfold-right {i => if (i < end) then (i, i + inc,) else ()} | start
	) >>
	define starts-with = (
		if-match (\sh, \st) then
			if-match:: sh, \t
			then:: starts-with {st} {t}
			else:: false
		else
			anything => true
	) >>
	define split = (separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. tails . cons {separator}
	) >>
	define transpose = (m =>
		let height = length {m} >>
		let width = if (height > 0) then (length . head | m) else 0 >>
		if (width > 0) then
			let w1 = width - 1 >>
			let gets = (cons {id} | reverse | tails | repeat {w1} | tail) >>
			map {f => map {head . flip {apply} {f}} {m}} | gets
		else
			()
	) >>
	define contains = (m =>
		fold {or} . map {starts-with | m} . tails
	) >>
	define dump as (any :t in (:t => list-of number)) = no-type-check (
		let dump-string = (s =>
			let length = prove-with-result /_s:s (string.length _s _l) _l >>
			map {i =>
				prove-with-result /_s:s/_i:i (
					substring _s _i 0 _c, to.int _c _asc
				) _asc
			} | 0 until length
		) >>
		let dump0 = (prec => n =>
			if (is-tree {n}) then
				if prec then (s => concat {"(", s, ")",}) else id
				| concat {dump0 {true} {head | n}, ", ", dump0 {false} {tail | n},}
			else-if (equals {n} {}) then
				"()"
			else-if (prove /_n:n (is.atom _n)) then
				dump-string | prove-with-result /_n:n (to.string _n _s) _s
			else
				int-to-str {n}
		) >>
		dump0 {false}
	) >>
	define ends-with = (end =>
		starts-with {reverse | end} . reverse
	) >>
	define quick-sort = (cmp =>
		if-match (\pivot, \t) then
			let filter0 = (not . cmp {pivot}) >>
			let filter1 = cmp {pivot} >>
			let l0 = (quick-sort {cmp} | filter {filter0} | t) >>
			let l1 = (quick-sort {cmp} | filter {filter1} | t) >>
			concat {l0, (pivot,), l1,}
		else
			()
	) >>
	.p
) #
