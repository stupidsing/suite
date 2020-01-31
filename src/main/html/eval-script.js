'use strict';

let evalscript = (() => {
	let cache = {};

	return (url, expr) => {
		if (document.URL.startsWith('file://'))
			url = 'https://raw.githubusercontent.com/stupidsing/suite/master/src/main/html/' + url;

		let e = expr != null ? ';' + expr : '';
		let key = url + '::' + e;
		let r = cache[key];
		
		if (r != null)
			return Promise.resolve(r);
		else
			return fetch(url, {
				cache: 'default',
				credentials: 'omit',
				method: 'GET',
				mode: 'cors',
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

let loadsrcscript = (url, cb) => new Promise((resolve, reject) => {
	globalThis.loadedmodule = null;

	let cb = () => resolve(loadedmodule);
	let script = document.createElement('script');
	script.onreadystatechange = cb;
	script.onload = cb;
	script.src = url;
	script.type = 'text/javascript';

	document.head.appendChild(script);
});
