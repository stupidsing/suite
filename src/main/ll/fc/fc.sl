-------------------------------------------------------------------------------
-- functional program compiler
--
-- .mode can be EAGER or LAZY

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
	, once (not is.cyclic .do1; fc-error "Cyclic data detected")
	, !, once (.mode = LAZY, fc-lazyify-fun .do1 .do2
		; .mode = EAGER, .do1 = .do2
	)
	, !, once (.mode = LAZY, .do2 = .do3
		; .mode = EAGER, fc-reduce-tail-recursion .do2 .do3
	)
	, !, fc-optimize .do3 .dox
#

fc-load-library .lib .do0 .dox
	:- find.all.memoized .node0 (fc-load-library0 .lib .node0) (.do0 .dox,)
#

fc-load-library0 .lib .slfx
	:- fc-add-functions .lib .slfx, !
	; once (home.dir .homeDir
		, concat .homeDir "/src/main/fl/" .lib ".slf" .slfFilename
		, file.exists .slfFilename
		, file.read .slfFilename .slf
		, to.atom ".p" .var
		, concat .var " (" .slf .var ")" .slf1
		, parse .slf1 .slf2
		, generalize .slf2 .slfx
	; fc-error "Library" .lib "not found"
	)
#

fc-load-precompiled-library .lib .node
	:- find.all.memoized .node0 (fc-load-precompiled-library0 .lib .node0) (.node,)
#

fc-load-precompiled-library0 .lib .precompiled
	:- fc-precompiled-library-filename .lib .filename
	, persist.load .precompiled .filename
#

fc-precompiled-library-filename .lib .filename
	:- home.dir .homeDir
	, concat .homeDir "/precompiled/" .lib ".node.gz" .filename
#

fc-frame-difference .frame0 .frame1 0
	:- same .frame0 .frame1, !
#
fc-frame-difference .frame0 (.frame1 + 1) .frameDiff
	:- not is.tree .frame0, !
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

fc-instantiate () .t .t #
fc-instantiate (.v, .vs) .t0 .tx :- replace .v _ .t0 .t1 , fc-instantiate .vs .t1 .tx #

fc-dict-get .v .t :- rbt-get .v .t, ! #

-- use replace, necessary to redefine already-defined variables
fc-dict-add .v .t0/.t1 :- rbt-replace .v .t0/.t1, ! #

fc-dict-union-bind .t0 .t1 .t2 :- rbt-union-bind .t0 .t1 .t2, ! #

fc-dict-union-insert .t0 .t1 .t2 :- rbt-union-insert .t0 .t1 .t2, ! #

fc-dict-union-replace .t0 .t1 .t2 :- rbt-union-replace .t0 .t1 .t2, ! #

fc-dict-member .v .t :- rbt-member .v .t #

-- There are few functions that might have side effects: call%i*
-- Logs are theoretically 'invisible' to the execution environment, so they are
-- still considered pure.

fc-add-functions STANDARD .p (
	data (optional {:t}) over :t as None >>
	data (optional {:t}) over :t as (Value :t) >>
	data (either {:a} {:b}) over some (:a, :b,) as (Left :a) >>
	data (either {:a} {:b}) over some (:a, :b,) as (Right :b) >>
	---------------------------------------------------------------------------
	define call%i-t1 := (data^%i -> any -> any) of skip-type-check (i => p0 => +call%i-t1 {i} {p0}) >>
	define call%i-t2 := (data^%i -> any -> any -> any) of skip-type-check (i => p0 => p1 => +call%i-t2 {i} {p0} {p1}) >>
	define call%i-t3 := (data^%i -> any -> any -> any -> any) of skip-type-check (i => p0 => p1 => p2 => +call%i-t3 {i} {p0} {p1} {p2}) >>
	define call%i-v1 := (data^%i -> any -> any) of skip-type-check (i => p0 => +call%i-v1 {i} {p0}) >>
	define call%i-v2 := (data^%i -> any -> any -> any) of skip-type-check (i => p0 => p1 => +call%i-v2 {i} {p0} {p1}) >>
	define call%i-v3 := (data^%i -> any -> any -> any -> any) of skip-type-check (i => p0 => p1 => p2 => +call%i-v3 {i} {p0} {p1} {p2}) >>
	define compare := (:t => (:t, :t) -> number) of skip-type-check (a => b => +compare {a} {b}) >>
	define cons := (:t => :t -> [:t] -> [:t]) of skip-type-check (head => tail => +lcons {head} {tail}) >>
	define first := (:a => :b => (:a, :b) -> :a) of skip-type-check (tuple => +pleft {tuple}) >>
	define get%i := (any -> data^%i) of skip-type-check (name => +get%i {name}) >>
	define head := (:t => [:t] -> :t) of skip-type-check (list => +lhead {list}) >>
	define is-list := (:t => [:t] -> boolean) of skip-type-check (n => +is-list {n}) >>
	define is-pair := (:a => :b => (:a, :b) -> boolean) of skip-type-check (n => +is-pair {n}) >>
	define second := (:a => :b => (:a, :b) -> :b) of skip-type-check (tuple => +pright {tuple}) >>
	define tail := (:t => [:t] -> [:t]) of skip-type-check (list => +ltail {list}) >>
	---------------------------------------------------------------------------
	define popen* := ([string] -> string -> (number, [data^Chars], [data^Chars])) of
		atom:INTRN!MonadIntrinsics.popen | get%i | call%i-v2
	>>
	define deep-seq := (:t => :t -> :t) of
		atom:INTRN!SeqIntrinsics.deepSeq | get%i | call%i-v1
	>>
	define log := (:t => :t -> :t) of
		atom:INTRN!BasicIntrinsics.log1 | get%i | call%i-v1
	>>
	define log2 := (:t => string -> :t -> :t) of
		atom:INTRN!BasicIntrinsics.log2 | get%i | call%i-v2
	>>
	---------------------------------------------------------------------------
	define and := x => y =>
		if x then y else false
	>>
	define drop := n => list =>
		if (n > 0 && is-list {list})
		then (list | tail | drop {n - 1})
		else list
	>>
	define fix := f =>
		let x := f {x} >> x
	>>
	define flip := f => x => y =>
		f {y} {x}
	>>
	define fold-left := fun => init => -- possible for tail call optimization in eager mode
		case
		|| `$h; $t` => fold-left {fun} {fun {init} {h}} {t}
		|| anything => init
	>>
	define fold-right := fun => init => -- possible for short-circuit evaluation in lazy mode
		case
		|| `$h; $t` => fun {h} {fold-right {fun} {init} {t}}
		|| anything => init
	>>
	define greater := a => b =>
		if (a > b) then a else b
	>>
	define id := v =>
		v
	>>
	define invoke := f => x =>
		f {x}
	>>
	define iterate := f => init =>
		init | f | iterate {f} | `init;`
	>>
	define lesser := a => b =>
		if (a > b) then b else a
	>>
	define lines :=
		case
		|| `$h; $t` =>
			let l1 := lines {t} >>
			if (h != 10 && is-list {l1})
			then (let `$p; $q` := l1 >> (h; p); q)
			else ((h;); l1)
		|| anything => ()
	>>
	define not := x =>
		if x then false else true
	>>
	define optional := cond => value =>
		if cond then (Value value) else None
	>>
	define or := x => y =>
		if x then true else y
	>>
	define repeat := e =>
		e; repeat {e}
	>>
	define scan-left := fun => init =>
		case
		|| `$h; $t` => init; scan-left {fun} {fun {init} {h}} {t}
		|| anything => init;
	>>
	define scan-right := fun => init =>
		case
		|| `$h; $t` =>
			let r := scan-right {fun} {init} {t} >>
			fun {h} {head {r}}; r
		|| anything => init;
	>>
	define take := n => list =>
		if (n > 0 && is-list {list})
		then (let `$h; $t` := list >> t | take {n - 1} | cons {h})
		else ()
	>>
	define take-drop := n => list =>
		if (n > 0 && is-list {list}) then
			let `$h; $t` := list >>
			let `$t1, $d1` := t | take-drop {n - 1} >>
			(h; t1), d1
		else (, list)
	>>
	define unfold-left := (:a => :b => (:a -> optional {:a, :b}) -> :a -> [:b]) of (
		define unfold-left0 := (:a => :b => [:b] -> (:a -> optional {:a, :b}) -> :a -> [:b]) of (
			list => fun => init =>
				if (fun {init} = `Value ($init1, $elem)`)
				then (unfold-left0 {elem; list} {fun} {init1})
				else list
		) >>
		unfold-left0 {}
	) >>
	define unfold-right := (:a => :b => (:a -> optional {:b, :a}) -> :a -> [:b]) of (
		fun => init =>
			if (fun {init} = `Value ($e, $init1)`)
			then (init1 | unfold-right {fun} | cons {e})
			else ()
	) >>
	define zip := fun => list0 => list1 =>
		unfold-right {
			case
			|| `($h0; $t0), ($h1; $t1)` => Value (fun {h0} {h1}, (t0, t1))
			|| anything => None
		} {list0, list1}
	>>
	---------------------------------------------------------------------------
	define append :=
		fold-right {cons} | flip
	>>
	define apply :=
		fold-right {`.`} {id}
	>>
	define bisect := fun =>
		fold-right {
			h => `$t0, $f0` =>
			if (fun {h}) then ((h; t0), f0) else (t0, (h; f0))
		} {(), ()}
	>>
	define chunk := n =>
		unfold-right {l => optional {l != ()} {take-drop {n} {l}}}
	>>
	define erase-type := (any -> any) of
		skip-type-check id
	>>
	define fold := fun => `$h; $t` =>
		fold-left {fun} {h} {t}
	>>
	define filter := fun =>
		fold-right {
			item => list => if (fun {item}) then (item; list) else list
		} {}
	>>
	define heads :=
		scan-left {cons/} {}
	>>
	define length :=
		fold-left {v => e => v + 1} {0}
	>>
	define map := fun =>
		fold-right {cons . fun} {}
	>>
	define partition := fun =>
		fold-right {
			e => `$l0, $l1` => if (fun {e}) then ((e; l0), l1) else (l0, (e; l1))
		} {(), ()}
	>>
	define popen := command => in =>
		do >> in | popen* {command}
	>>
	define replicate := flip {e =>
		unfold-left {i => optional {i != 0} {i - 1, e}}
	} >>
	define reverse :=
		fold-left {cons/} {}
	>>
	define substring := start => end => list =>
		let len := length {list} >>
		let s := if (start >= 0) then start else (len + start) >>
		let e := if (end > 0) then end else (len + end) >>
		list | take {e} | drop {s}
	>>
	define tails :=
		scan-right {cons} {}
	>>
	define take-while := fun =>
		fold-right {h => if (fun {h}) then (cons {h}) else (t => ())} {}
	>>
	define uniq :=
		fold-right {item => list =>
			case
			|| (list = `item; $t`) list
			|| item; list
		} {}
	>>
	---------------------------------------------------------------------------
	define concat :=
		fold-right {append} {}
	>>
	define cross := fun => l1 => l2 =>
		l1 | map {e1 => l2 | map {e1 | fun}}
	>>
	define get := n =>
		head . (tail | replicate {n} | apply)
	>>
	define maximum :=
		fold {greater}
	>>
	define merge := merger => list =>
		let len := length {list} >>
		if (len > 1) then
			let `$list0, $list1` := take-drop {len / 2} {list} >>
			merger {list0 | merge {merger}} {list1 | merge {merger}}
		else list
	>>
	define minimum :=
		fold {lesser}
	>>
	define range := start => end => inc =>
		unfold-right {i => optional {i < end} {i, i + inc}} {start}
	>>
	define starts-with :=
		case
		|| `$sh; $st` =>
			case
			|| `sh; $t` => starts-with {st} {t}
			|| anything => false
		|| anything => anything => true
	>>
	define split := separator =>
		map {take-while {`!= separator`} . tail}
		. filter {`= separator` . head}
		. filter {not . `=` {}}
		. tails . cons {separator}
	>>
	define transpose := m =>
		let height := length {m} >>
		let width := if (height > 0) then (m | head | length) else 0 >>
		if (width > 0) then
			let w1 := width - 1 >>
			let gets := tail | replicate {w1} | tails | reverse >>
			gets | map {f => map {head . apply {f}} {m}}
		else ()
	>>
	---------------------------------------------------------------------------
	define contains := m =>
		fold-left {or} {false} . map {m | starts-with} . tails
	>>
	define ends-with := end =>
		starts-with {end | reverse} . reverse
	>>
	define group :=
		define group0 := list0 => list1 =>
			if (list0 = `$k0, $v0; $t0`) then
				if (list1 = `$k1, $v1; $t1`) then
					case
					|| (k0 < k1) (k0, v0; group0 {t0} {list1})
					|| (k0 > k1) (k1, v1; group0 {list0} {t1})
					|| k0, append {v0} {v1}; group0 {t0} {t1}
				else list0
			else list1
		>>
		merge {group0} . map {`$k, $v` => k, (v;)}
	>>
	define join := separator =>
		concat . map {separator; | append/}
	>>
	define quick-sort := cmp =>
		case
		|| `$pivot; $t` =>
			let `$l0, $l1` := partition {cmp/ {pivot}} {quick-sort {cmp} {t}} >>
			concat {l0; (pivot;); l1;}
		|| anything => ()
	>>
	define replace := s0 => s1 =>
		define l := length {s0} >>
		define replace0 := s =>
			case
			|| (starts-with {s0} {s}) (s1 ++ (s | drop {l} | replace0))
			|| (`$h; $t` = s) (h; replace0 {t})
			|| ()
		>>
		replace0
	>>
	---------------------------------------------------------------------------
	define merge-sort :=
		concat . map {second} . group . map {v => v, v}
	>>
	.p
) #
