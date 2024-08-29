import { existsSync, readdirSync, readFileSync } from 'fs';

let stateDir = `${process.env.HOME}/.warrior`;

let stateByKey;

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let getStateFilename = key => `${stateDir}/${key}`;

let ec2Class = () => {
	let class_ = 'ec2';

	let getKey = ({ name, attributes }) => [
		class_,
		name,
		attributes['InstanceType'],
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 terminate-instance\\`,
		`  --instance-ids ${state.InstanceId}`,
		`rm -f ${getStateFilename(key)}.json`,
	];

	let refreshById = (resource, id) => [
		`aws ec2 describe-instances \\`,
		`  --instance-ids ${id} \\`,
		`  | jq '.Reservations[0] | .Instances[0]' \\`,
		`  > ${getStateFilename_(resource)}.json`,
	];

	let upsert = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			let { name, attributes: { InstanceType, SecurityGroups, SubnetId } } = resource;
			commands.push(
				`aws ec2 run-instances \\`,
				...(SecurityGroups ? [`  --security-groups ${SecurityGroup.join(',')} \\`] : []),
				`  --instance-type ${InstanceType} \\`,
				`  --subnet-id ${SubnetId} \\`,
				`  --tag-specifications '${JSON.stringify([
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: name }] }
				])}' \\`,
				`  | jq .Instances[0] > ${getStateFilename_(resource)}.json`,
			);
			state = { SecurityGroups };
		}

		let InstanceId = `$(cat ${getStateFilename_(resource)}.json | jq -r .InstanceId)`;

		{
			let prop = 'SecurityGroups';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-instance-attribute \\`,
					`  --groups ${attributes[prop].join(',')} \\`,
					`  --instance-id ${InstanceId}`,
					...refreshById(resource, InstanceId),
				);
			}
		}

		return commands;
	};

	return {
		class_,
		delete_,
		getKey,
		refresh: ({ InstanceId }, resource) => refreshById(resource, InstanceId),
		upsert,
	};
};

let subnetClass = () => {
	let class_ = 'subnet';

	let getKey = ({ name, attributes }) => [
		class_,
		name,
		attributes['VpcId'],
		attributes['AvailabilityZone'],
		attributes['CidrBlock'].replaceAll('.', ':').replaceAll('/', ':'),
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 delete-subnet \\`,
		`  --subnet-id ${state.SubnetId}`,
		`rm -f ${getStateFilename(key)}.json`,
	];

	let refreshById = (resource, id) => [
		`aws ec2 describe-subnets --subnet-ids ${id} | jq .Subnets[0] > ${getStateFilename_(resource)}.json`,
	];

	let upsert = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			let { name, attributes: { AvailabilityZone, CidrBlock, VpcId } } = resource;
			commands.push(
				`aws ec2 create-subnet \\`,
				`  --availability-zone ${AvailabilityZone} \\`,
				...(CidrBlock ? [`  --cidr-block ${CidrBlock} \\`] : []),
				`  --tag-specifications '${JSON.stringify([
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: name }] }
				])}' \\`,
				`  --vpc-id ${VpcId} \\`,
				`  | jq .Subnet > ${getStateFilename_(resource)}.json`,
			);
			state = {};
		}

		let SubnetId = `$(cat ${getStateFilename_(resource)}.json | jq -r .SubnetId)`;

		{
			let prop = 'MapPublicIpOnLaunch';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-subnet-attribute \\`,
					`  --${attributes[prop] ? `` : `no-`}map-public-ip-on-launch \\`,
					`  --subnet-id ${SubnetId}`,
					...refreshById(resource, SubnetId),
				);
			}
		}

		return commands;
	};

	return {
		class_,
		delete_,
		getKey,
		refresh: ({ SubnetId }, resource) => refresh(resource, SubnetId),
		upsert,
	};
};

let vpcClass = () => {
	let class_ = 'vpc';

	let getKey = ({ name }) => [class_, name].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`,
		`rm -f ${getStateFilename(key)}.json`,
		`rm -f ${getStateFilename(key)}.EnableDnsHostnames.json`,
		`rm -f ${getStateFilename(key)}.EnableDnsSupport.json`,
	];

	let upsert = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			let { name, attributes: { CidrBlockAssociationSet } } = resource;
			commands.push(
				`aws ec2 create-vpc \\`,
				`  --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} \\`,
				`  --tag-specifications '${JSON.stringify([
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: name }] }
				])}' | \\`,
				`  jq .Vpc > ${getStateFilename_(resource)}.json`,
			);
			state = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		} else {
			let stateFilename = getStateFilename_(resource);
			state = {
				...state,
				EnableDnsHostnames: readJsonIfExists(`${stateFilename}.EnableDnsHostnames`),
				EnableDnsSupport: readJsonIfExists(`${stateFilename}.EnableDnsSupport`),
			};
		}

		// let VpcId = `$(aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId)`;
		let VpcId = `$(cat ${getStateFilename_(resource)}.json | jq -r .VpcId)`;

		{
			let prop = 'CidrBlockAssociationSet';
			let map0 = Object.fromEntries(state[prop].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			let map1 = Object.fromEntries(attributes[prop].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			for (let [CidrBlock, AssociationId] of Object.entries(map0)) {
				if (!map1.hasOwnProperty(CidrBlock)) {
					commands.push(
						`aws ec2 disassociate-vpc-cidr-block \\`,
						`  --association-id ${AssociationId}`,
						`  --vpc-id ${VpcId} \\`,
					);
				}
			}
			for (let [CidrBlock, AssociationId] of Object.entries(map1)) {
				if (!map0.hasOwnProperty(CidrBlock)) {
					commands.push(
						`aws ec2 associate-vpc-cidr-block\\`,
						`  --cidr-block ${CidrBlock}`,
						`  --vpc-id ${VpcId}\\`,
					);
				}
			}
		}
		{
			let prop = 'EnableDnsHostnames';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-vpc-attribute \\`,
					`  --${attributes[prop] ? `` : `no-`}enable-dns-hostnames \\`,
					`  --vpc-id ${VpcId}`,
					`echo ${attributes[prop]} > ${getStateFilename_(resource)}.${prop}.json`);
			}
		}
		{
			let prop = 'EnableDnsSupport';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-vpc-attribute \\`,
					`  --${attributes[prop] ? `` : `no-`}enable-dns-support \\`,
					`  --vpc-id ${VpcId}`,
					`echo ${attributes[prop]} > ${getStateFilename_(resource)}.${prop}.json`);
			}
		}

		return commands;
	};

	return {
		class_,
		delete_,
		getKey,
		getState: resource => {
			let stateFilename = getStateFilename_(resource);
			let state = readJsonIfExists(`${stateFilename}.json`);
			return state ? {
				...state,
				EnableDnsHostnames: readJsonIfExists(`${stateFilename}.EnableDnsHostnames`),
				EnableDnsSupport: readJsonIfExists(`${stateFilename}.EnableDnsSupport`),
			} : null;
		},
		refresh: ({ VpcId }, resource) => [
			`aws ec2 describe-vpcs \\`,
			`  --vpc-ids ${VpcId} \\`,
			`  | jq .Vpcs[0] > ${getStateFilename_(resource)}.json`,
			`aws ec2 describe-vpc-attribute \\`,
			`  --attribute enableDnsHostnames \\`,
			`  --vpc-id ${VpcId} \\`,
			`  | jq -r .EnableDnsHostnames.Value > ${getStateFilename_(resource)}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute \\`,
			`  --attribute enableDnsSupport \\`,
			`  --vpc-id ${VpcId} \\`,
			`  | jq -r .EnableDnsSupport.Value > ${getStateFilename_(resource)}.EnableDnsSupport.json`,
		],
		upsert,
	};
};

let objectByClass = Object.fromEntries([ec2Class(), subnetClass(), vpcClass()].map(c => [c.class_, c]));

let get = (resource, prop) => {
	let { class_ } = resource;
	let { getKey } = objectByClass[class_];
	let key = getKey(resource);
	let state = stateByKey[key];
	return state ? state[prop] : `$(cat ${getStateFilename(key)}.json | jq -r .${prop})`;
};

let getResources = () => {
	let vpc = {
		class_: 'vpc',
		name: 'npt-cloud-vpc',
		attributes: {
			CidrBlockAssociationSet: [{ CidrBlock: '10.88.0.0/16' }],
			EnableDnsHostnames: true,
			EnableDnsSupport: true,
		},
	};

	let subnetPublic = {
		class_: 'subnet',
		name: 'npt-cloud-subnet-public',
		attributes: {
			AvailabilityZone: 'ap-southeast-1a',
			CidrBlock: '10.88.1.0/24',
			MapPublicIpOnLaunch: true,
			VpcId: get(vpc, 'VpcId'),
		},
	};

	let subnetPrivate = {
		class_: 'subnet',
		name: 'npt-cloud-subnet-private',
		attributes: {
			AvailabilityZone: 'ap-southeast-1a',
			CidrBlock: '10.88.2.0/24',
			MapPublicIpOnLaunch: false,
			VpcId: get(vpc, 'VpcId'),
		},
	};

	let ec2 = {
		class_: 'ec2',
		name: 'npt-cloud-ec2-0',
		attributes: {
			InstanceType: 't3.nano',
			SubnetId: get(subnetPrivate, 'SubnetId'),
		},
	};

	return [vpc, subnetPublic, subnetPrivate, ec2];
};

let stateFilenames = readdirSync(stateDir);
let action = process.env.ACTION ?? 'up';

if (action === 'refresh') {
	stateFilenames.map(stateFilename => {
		let [key] = stateFilename.split('.');
		let [class_] = key.split('_');
		let state = JSON.parse(readFileSync(`${stateDir}/${stateFilename}`));
		let { refresh } = objectByClass[class_];
		commands.push(refresh(state, resource));
	});
}

stateByKey = Object.fromEntries(stateFilenames.map(stateFilename => {
	let [key] = stateFilename.split('.');
	let state = JSON.parse(readFileSync(`${stateDir}/${stateFilename}`));
	return [key, state];
}));

let resources = action === 'up' ? getResources() : [];

let resourceByKey = Object.fromEntries(resources.map(resource => {
	let { getKey } = objectByClass[resource.class_];
	let key = getKey(resource);
	return [key, resource];
}));

let commands = [];

for (let [key, resource] of Object.entries(resourceByKey)) {
	let [class_, name] = key.split('_');
	let { upsert } = objectByClass[class_];
	let state = stateByKey[key];
	commands.push(
		'',
		`# ${state ? 'update' : 'create'} ${name}`,
		...upsert(state, resource));
}

for (let [key, state] of Object.entries(stateByKey)) {
	let [class_, name] = key.split('_');
	let { delete_ } = objectByClass[class_];
	let resource = resourceByKey[key];
	if (resource == null) {
		commands.push(
			'',
			`# delete ${name}`,
			...delete_(state, key));
	}
}

console.log(commands.join('\n'));
