const usp = new URLSearchParams(window.location.search);
const colorsp = usp.get('colors');
const sizep = usp.get('size');

let size = sizep != null ? +sizep : 7;
let nStoneTypes = colorsp != null ? +colorsp : 5;

let randomstone = () => ({ d: Math.floor(Math.random() * nStoneTypes) });

let randomstones = n => {
	let stones = [];
	for (let i = 0; i < n; i++) stones.push(randomstone());
	return stones;
};

let freeze = false; // if we are accepting game inputs

let mutate = (() => {
	let for_xy = f => {
		for (let x = 0; x < size; x++)
			for (let y = 0; y < size; y++)
				f(x, y);
	};

	let back_xy = f => {
		for (let x = size - 1; 0 <= x; x--)
			for (let y = size - 1; 0 <= y; y--)
				f(x, y);
	};

	let setcell = (vm, vmc1) =>  {
		let vmt0 = vm.board;
		let vmr0 = vmt0[vmc1.x];
		let vmc0 = vmr0[vmc1.y];
		let vmr1 = vmr0.map(vmc => vmc != vmc0 ? vmc : vmc1);
		let vmt1 = vmt0.map(vmr => vmr != vmr0 ? vmr : vmr1);
		return { ...vm, board: vmt1 };
	};

	return {
		back_xy,
		drop: (vm, stones) => {
			if (stones.length <= mutate.emptycount(vm))
				for (let stone of stones)
					while(true) {
						let x = Math.floor(Math.random() * size);
						let y = Math.floor(Math.random() * size);
						if (vm.board[x][y].d == null) {
							vm = setcell(vm, { ...vm.board[x][y], d: stone.d });
							break;
						}
					}
			else
				vm = mutate.lose(vm);
			return vm;
		},
		emptycount: vm => {
			let n = 0;
			for_xy((x, y) => n += vm.board[x][y].d != null ? 0 : 1);
			return n;
		},
		for_xy,
		lose: vm => {
			freeze = true;
			for_xy((x, y) => {
				if (vm.board[x][y].d == null)
					vm = setcell(vm, { ...vm.board[x][y], d: -1 });
			});
			return vm;
		},
		setcell,
	};
})();

let vw = (() => {
	let change = f => renderAgain(view, vm0 => {
		let vm1 = f(vm0);
		// console.log(vm1);
		return vm1;
	});

	let check_ = vm => {
		let isFiveInARow = false;
		if (!freeze)
			for (let [dx, dy] of eatdirs)
				(0 < dx * size + dy ? mutate.for_xy : mutate.back_xy)((x, y) => {
					let step = 0;
					let x1, y1;
					while (true
						&& (x1 = x + step * dx) != null
						&& (y1 = y + step * dy) != null
						&& 0 <= x1 && x1 < size && 0 <= y1 && y1 < size
						&& vm.board[x][y].d != null
						&& vm.board[x][y].d == vm.board[x1][y1].d) step++;
					if (5 <= step) {
						isFiveInARow = true;
						for (let i = 0; i < step; i++)
							vm = mutate.setcell(vm, { ...vm.board[x + i * dx][y + i * dy], d: null, });
						vm = { ...vm, score: vm.score + step };
						document.title = `${vm.score} - Five in a row`;
					}
				});
		return { isFiveInARow, vm };
	};

	let movefromto = (vmc0, vmcx) => {
		let board;
		change(vm => { board = vm.board; return vm; });

		let search = (xy0, xyx, isMovable) => {
			let key = xy => `${xy.x},${xy.y}`;
			let todos = [{ x: xy0.x, y: xy0.y, prev: null, }];
			let dones = {};
			let kx = key(xyx);
			while (0 < todos.length) { // breadth-first search
				let todo = todos.shift();
				let { x, y, } = todo;
				let k = key(todo);
				if (!dones.hasOwnProperty(k)) {
					let neighbours = neighbourdirs
						.map(([dx, dy]) => ({ x: x + dx, y: y + dy, prev: todo, }))
						.filter(({ x, y }) => 0 <= x && x < size && 0 <= y && y < size && isMovable(x, y));
					todos.push(...neighbours);
					dones[k] = todo;
					if (k == kx) return todo;
				}
			}
			return null;
		};

		let node = search(vmc0, vmcx, (x, y) => board[x][y].d == null);

		let rec = ({ x, y, prev, }, cb) => {
			if (prev != null)
				rec(prev, () => {
					let timeout = setTimeout(() => {
						change(vm => {
							let vmc0 = vm.board[prev.x][prev.y];
							let vmc1 = vm.board[x][y];
							let d = vmc0.d;
							vm = mutate.setcell(vm, { ...vmc0, d: null, });
							vm = mutate.setcell(vm, { ...vmc1, d, });
							return vm;
						});
						cb();
						clearTimeout(timeout);
					}, 50);
				});
			else
				cb();
		};

		if (node != null) {
			freeze = true;
			rec(node, () => {
				freeze = false;
				change(vm_ => {
					let { isFiveInARow, vm } = check_(vm_);
					if (!isFiveInARow) {
						vm = mutate.drop(vm, vm.nextstones);
						vm = { ...vm, nextstones: randomstones(3), };
						vm = check_(vm).vm;
					}				
					return vm;
				});
			});
		} else
			console.log('no path between', vmc0, vmcx);
	};

	return {
		change,
		init: () => change(vm0 => {
			var vm1 = {
				board: range(0, size).map(x => range(0, size).map(y => ({ d: null, x, y, })).list()).list(),
				nextstones: [randomstone(), randomstone(), randomstone(),],
				notifications: ['welcome!'],
				score: 0,
				select_xy: null,
			};
			return mutate.drop(vm1, randomstones(Math.ceil(size * size * .3)));
		}),
		movefromto,
		select: (x, y) => {
			change(vm => {
				let vmc = vm.board[x][y];
				vm = mutate.setcell(vm, { ...vmc, selected: true, });
				return { ...vm, select_xy: {x, y} };
			});
		},
		unselect: () => {
			let select_xy0;
			change(vm => {
				select_xy0 = vm.select_xy;
				if (select_xy0 != null) {
					let vmc = vm.board[select_xy0.x][select_xy0.y];
					vm = mutate.setcell(vm, { ...vmc, selected: false, });
					select_xy = null;
				}
				return { ...vm, select_xy: null };
			});
			return select_xy0;
		},
	}
})();

let handleclick = (vmc, ev) => {
	if (!freeze) {
		let select_xy0 = vw.unselect();

		if (vmc.d != null)
			vw.select(vmc.x, vmc.y);
		else if (select_xy0 != null)
			vw.movefromto(select_xy0, vmc);
	}
};

let handleclose = () => vw.change(vm => null);
