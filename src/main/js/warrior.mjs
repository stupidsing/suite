import { existsSync, readdirSync, readFileSync } from 'fs';

let prefix = 'npt';
let stateDir = `.warrior`;

let stateByKey;

let readJsonIfExists = name => {
	let filename = name;
	if (existsSync(filename)) {
		let text = readFileSync(filename, 'ascii');
		return text ? JSON.parse(text) : null;
	} else {
		return null;
	}
};

let getStateFilename = key => `${stateDir}/${key}`;

let instanceClass = () => {
	let class_ = 'instance';

	let getKey = ({ name, attributes }) => [
		prefix,
		class_,
		name,
		attributes.InstanceType,
		attributes.ImageId,
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 terminate-instances \\`,
		`  --instance-ids ${state.InstanceId}`,
		`rm -f ${getStateFilename(key)}`,
	];

	let refreshById = (resource, id) => [
		`aws ec2 describe-instances \\`,
		`  --instance-ids ${id} \\`,
		`  | jq .Reservations[0].Instances[0] | tee ${getStateFilename_(resource)}`,
	];

	let upsert = (state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			let { name, attributes: { ImageId, InstanceType, SecurityGroups, SubnetId } } = resource;
			commands.push(
				`aws ec2 run-instances \\`,
				...(SecurityGroups ? [`  --security-groups ${SecurityGroup.join(',')} \\`] : []),
				`  --image-id ${ImageId} \\`,
				`  --instance-type ${InstanceType} \\`,
				`  --subnet-id ${SubnetId} \\`,
				`  --tag-specifications '${JSON.stringify([
					{ ResourceType: 'instance', Tags: [{ Key: 'Name', Value: `${prefix}-${name}` }] },
				])}' \\`,
				`  | jq .Instances[0] | tee ${getStateFilename_(resource)}`,
			);
			state = { SecurityGroups };
		}

		let InstanceId = `$(cat ${getStateFilename_(resource)} | jq -r .InstanceId)`;

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
		prefix,
		class_,
		name,
		attributes.VpcId,
		attributes.AvailabilityZone,
		attributes.CidrBlock.replaceAll('/', ':'),
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 delete-subnet \\`,
		`  --subnet-id ${state.SubnetId}`,
		`rm -f ${getStateFilename(key)}`,
	];

	let refreshById = (resource, id) => [
		`aws ec2 describe-subnets \\`,
		`  --subnet-ids ${id} \\`,
		`  | jq .Subnets[0] | tee ${getStateFilename_(resource)}`,
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
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: `${prefix}-${name}` }] },
				])}' \\`,
				`  --vpc-id ${VpcId} \\`,
				`  | jq .Subnet | tee ${getStateFilename_(resource)}`,
			);
			state = {};
		}

		let SubnetId = `$(cat ${getStateFilename_(resource)} | jq -r .SubnetId)`;

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

	let getKey = ({ name }) => [
		prefix,
		class_,
		name,
	].join('_');

	let getStateFilename_ = resource => getStateFilename(getKey(resource));

	let delete_ = (state, key) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`,
		`rm -f ${getStateFilename(key)}`,
		`rm -f ${getStateFilename(key)}#EnableDnsHostnames`,
		`rm -f ${getStateFilename(key)}#EnableDnsSupport`,
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
					{ ResourceType: class_, Tags: [{ Key: 'Name', Value: `${prefix}-${name}` }] },
				])}' | \\`,
				`  jq .Vpc | tee ${getStateFilename_(resource)}`,
			);
			state = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		}

		// let VpcId = `$(aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId)`;
		let VpcId = `$(cat ${getStateFilename_(resource)} | jq -r .VpcId)`;

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
					`echo ${attributes[prop]} | tee ${getStateFilename_(resource)}#${prop}`);
			}
		}
		{
			let prop = 'EnableDnsSupport';
			if (state[prop] !== attributes[prop]) {
				commands.push(
					`aws ec2 modify-vpc-attribute \\`,
					`  --${attributes[prop] ? `` : `no-`}enable-dns-support \\`,
					`  --vpc-id ${VpcId}`,
					`echo ${attributes[prop]} | tee ${getStateFilename_(resource)}#${prop}`);
			}
		}

		return commands;
	};

	return {
		class_,
		delete_,
		getKey,
		refresh: ({ VpcId }, resource) => [
			`aws ec2 describe-vpcs \\`,
			`  --vpc-ids ${VpcId} \\`,
			`  | jq .Vpcs[0] | tee ${getStateFilename_(resource)}`,
			`aws ec2 describe-vpc-attribute \\`,
			`  --attribute enableDnsHostnames \\`,
			`  --vpc-id ${VpcId} \\`,
			`  | jq -r .EnableDnsHostnames.Value | tee ${getStateFilename_(resource)}.EnableDnsHostnames`,
			`aws ec2 describe-vpc-attribute \\`,
			`  --attribute enableDnsSupport \\`,
			`  --vpc-id ${VpcId} \\`,
			`  | jq -r .EnableDnsSupport.Value | tee ${getStateFilename_(resource)}.EnableDnsSupport`,
		],
		upsert,
	};
};

let objectByClass = Object.fromEntries([instanceClass(), subnetClass(), vpcClass()].map(c => [c.class_, c]));

let get = (resource, prop) => {
	let { class_ } = resource;
	let { getKey } = objectByClass[class_];
	let key = getKey(resource);
	let state = stateByKey[key];
	return state ? state[prop] : `$(cat ${getStateFilename(key)} | jq -r .${prop})`;
};

let getResources = () => {
	let vpc = {
		class_: 'vpc',
		name: 'cloud',
		attributes: {
			CidrBlockAssociationSet: [{ CidrBlock: '10.88.0.0/16' }],
			EnableDnsHostnames: true,
			EnableDnsSupport: true,
		},
	};

	let subnetPublic = {
		class_: 'subnet',
		name: 'public',
		attributes: {
			AvailabilityZone: 'ap-southeast-1a',
			CidrBlock: '10.88.1.0/24',
			MapPublicIpOnLaunch: true,
			VpcId: get(vpc, 'VpcId'),
		},
	};

	let subnetPrivate = {
		class_: 'subnet',
		name: 'private',
		attributes: {
			AvailabilityZone: 'ap-southeast-1a',
			CidrBlock: '10.88.2.0/24',
			MapPublicIpOnLaunch: false,
			VpcId: get(vpc, 'VpcId'),
		},
	};

	let instance = {
		class_: 'instance',
		name: 'app-0',
		attributes: {
			ImageId: 'ami-05d6d0aae066c8d93', // aws ssm get-parameter --name /aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id | jq -r .Parameter.Value
			InstanceType: 't3.nano',
			SubnetId: get(subnetPrivate, 'SubnetId'),
		},
	};

	return [vpc, subnetPublic, subnetPrivate, instance];
};

let stateFilenames = readdirSync(stateDir);
let action = process.env.ACTION ?? 'up';

stateByKey = {};

for (let stateFilename of stateFilenames) {
	let [key, subKey] = stateFilename.split('#');
	let state = readJsonIfExists(`${stateDir}/${stateFilename}`);
	if (state) {
		if (subKey) state = { [subKey]: state };
		stateByKey[key] = { ...stateByKey[key] ?? {}, ...state };
	}
}

let resources = action === 'up' ? getResources() : [];

let resourceByKey = Object.fromEntries(resources.map(resource => {
	let { getKey } = objectByClass[resource.class_];
	let key = getKey(resource);
	return [key, resource];
}));

let commands = [];

if (action === 'refresh') {
	for (let [key, state] of Object.entries(stateByKey)) {
		commands.push(refresh(state, resourceByKey[key]));
	}
}

for (let [key, resource] of Object.entries(resourceByKey)) {
	let [prefix, class_, name] = key.split('_');
	let { upsert } = objectByClass[class_];
	let state = stateByKey[key];
	commands.push(
		'',
		`# ${state ? 'update' : 'create'} ${name}`,
		...upsert(state, resource));
}

for (let [key, state] of Object.entries(stateByKey)) {
	let [prefix, class_, name] = key.split('_');
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
