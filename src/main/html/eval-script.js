'use strict';

let loadedmodule = null;

let { evalscript, loadsrcscript, } = (() => {
	let cache = {};

	let loadsrcscript = url => new Promise((resolve, reject) => {
		let cb = () => resolve(cache[url] = loadedmodule);

		let script = document.createElement('script');
		script.onreadystatechange = cb;
		script.onload = cb;
		script.onerror = reject;
		script.src = url;
		script.type = 'text/javascript';

		document.head.appendChild(script);
	});

	let evalscript = async url => {
		let r = cache[url];

		if (r != null)
			return r;
		else if (document.location.protocol == 'file:')
			return loadsrcscript(url);
		else {
			let response = await fetch(url, {
				cache: 'default',
				credentials: 'omit',
				method: 'GET',
				mode: 'cors',
				redirect: 'follow',
				referrer: 'no-referrer',
			});
			if (response.ok)
				return cache[url] = eval(await response.text());
			else
				throw response.statusText;
		}
	};

	return { evalscript, loadsrcscript, };
})();
