'use strict';

let evalscript = (() => {
	let cache = {};

	return (url, expr) => {
		let e = expr != null ? ';' + expr : '';
		let key = url + '::' + e;
		let r = cache[key];

		return r != null
			? Promise.resolve(r)
			: fetch(url, {
				cache: 'default',
				credentials: 'omit',
				mode: 'cors',
				method: 'GET',
				redirect: 'follow',
				referrer: 'no-referrer',
			})
			.then(response => {
				if (response.ok) return response.text(); else throw response.statusText;
			})
			.then(text => eval(text + e))
			.then(result => cache[key] = result)
			.catch(error => console.error('evalscript()', url, e, error));
	};
})();
