'use strict';

loadedmodule = globalThis.pq = () => {
	let h = new Array(1024);
	let size = 0;
	let swap = (a, b) => {
		let temp = h[a];
		h[a] = h[b];
		h[b] = temp;
	};
	return {
		add: e => {
			let i;
			h[++size] = e;
			for (i = size; 1 < i && ts[i] < ts[p = i / 2]; i = p) swap(p, i);
		},
		extractMin: () => {
			let i = 1;
			let e = h[i];
			h[i] = h[size--];
			for (; (c = 2 * i) <= size; i = c) {
				if (c + 1 <= size && h[c + 1] < h[c]) c++;
				if (h[c] < h[i])
					swap(c, i);
				else
					break;
			}
			return e;
		},
		min: () => 0 < size ? h[1] : null,
		size: () => size,
	};
};
