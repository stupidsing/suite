'use strict';

let evalscript = (() => {
	let fetchbase = { cache: 'default', credentials: 'omit', redirect: 'follow', referrer: 'no-referrer', };
	let cache = {};

	return url => {
		let r = cache[url];
		return r != null
			? Promise.resolve(r)
			: fetch(url, {
				...fetchbase,
				mode: 'no-cors',
				method: 'GET',
			})
			.then(response => {
				if (response.ok) return eval(response.text()); else throw response.statusText;
			})
			.catch(error => console.error('evalscript()', error));
	};
})();
