import { existsSync, readFileSync } from 'fs';

let stateDir = '/tmp';

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let stateFilename = name => `${stateDir}/${name}`;

let subnetClass = () => {
	let create = ({ name, attributes: { VpcId } }) => [
		`aws ec2 create-subnet --tag-specifications '${JSON.stringify([
			{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
		])} --vpc-id ${VpcId}' | jq .Vpc > ${stateFilename(name)}.json`,
	];

	let delete_ = (name, state) => [
		`aws ec2 delete-vpc --subnet-id ${state.SubnetId}`, `rm -f ${stateFilename(name)}.json`,
	];

	let update = (name, state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = { CidrBlock: attributes['CidrBlock'], VpcId: attributes['VpcId'] };
		}

		{
			let key = 'CidrBlock';
			if (state[key] !== attributes[key]) {
				return [...delete_(name, state), ...update(name, null, resource)];
			}
		}

		{
			let key = 'VpcId';
			if (state[key] !== attributes[key]) {
				return [...delete_(name, state), ...update(name, null, resource)];
			}
		}

		return commands;
	};

	return {
		create,
		delete_,
		getKey: resource => [resource.name, resource.CidrBlock, resource.VpcId],
		getState: ({ name }) => readJsonIfExists(`${stateFilename(name)}.json`),
		refresh: (name, id) => [
			`aws ec2 describe-subnets --subnet-ids ${id} | jq .Subnets[0] > ${stateFilename(name)}.json`,
		],
		update,
	};
};

let vpcClass = () => {
	let create = ({ name, attributes: { CidrBlockAssociationSet } }) => [
		`aws ec2 create-vpc --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} --tag-specifications '${JSON.stringify([
			{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
		])}' | jq .Vpc > ${stateFilename(name)}.json`,
	];

	let delete_ = (name, state) => [
		`aws ec2 delete-vpc --vpc-id ${state.VpcId}`, `rm -f ${stateFilename(name)}.json`,
	];

	let update = (name, state, resource) => {
		let { attributes } = resource;
		let commands = [];

		if (state == null) {
			commands.push(...create(resource));
			state = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		}

		// let VpcId = `$(aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId)`;
		let VpcId = `$(cat ${stateFilename(name)}.json | jq -r .VpcId)`;

		{
			let key = 'CidrBlockAssociationSet';
			let map0 = Object.fromEntries(state[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			let map1 = Object.fromEntries(attributes[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			for (let [CidrBlock, AssociationId] of Object.entries(map0)) {
				if (!map1.hasOwnProperty(CidrBlock)) {
					commands.push(`aws ec2 disassociate-vpc-cidr-block --vpc-id ${VpcId} --association-id ${AssociationId}`);
				}
			}
			for (let [CidrBlock, AssociationId] of Object.entries(map1)) {
				if (!map0.hasOwnProperty(CidrBlock)) {
					commands.push(`aws ec2 associate-vpc-cidr-block --vpc-id ${VpcId} --cidr-block ${CidrBlock}`);
				}
			}
		}
		{
			let key = 'EnableDnsHostnames';
			if (state[key] !== attributes[key]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[key] ? `--` : `--no-`}enable-dns-hostnames`,
					`echo ${attributes[key]} > ${stateFilename(name)}.EnableDnsHostnames.json`);
			}
		}
		{
			let key = 'EnableDnsSupport';
			if (state[key] !== attributes[key]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes[key] ? `--` : `--no-`}enable-dns-support`,
					`echo ${attributes[key]} > ${stateFilename(name)}.EnableDnsSupport.json`);
			}
		}

		if (resource == null) {
			commands.push(...delete_(name, state));
		}

		return commands;
	};

	return {
		create,
		delete_,
		getKey: resource => [resource.name],
		getState: ({ name }) => {
			let stateFilename_ = stateFilename(name);
			let state = readJsonIfExists(`${stateFilename_}.json`);
			return state ? {
				...state,
				EnableDnsHostnames: readJsonIfExists(`${stateFilename_}.EnableDnsHostnames`),
				EnableDnsSupport: readJsonIfExists(`${stateFilename_}.EnableDnsSupport`),
			} : null;
		},
		refresh: (name, id) => [
			`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] > ${stateFilename(name)}.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsHostnames | jq -r .EnableDnsHostnames.Value > ${stateFilename(name)}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsSupport | jq -r .EnableDnsSupport.Value > ${stateFilename(name)}.EnableDnsSupport.json`,
		],
		update,
	};
};

let objectByClass = {
	subnet: subnetClass(),
	vpc: vpcClass(),
};

let vpc = {
	class_: 'vpc',
	name: 'npt-cloud-vpc',
	attributes: {
		CidrBlockAssociationSet: [{ CidrBlock: '10.25.0.0/16' }],
		EnableDnsHostnames: true,
		EnableDnsSupport: true,
	},
};

let subnet = {
	class_: 'subnet',
	name: 'npt-cloud-subnet',
	attributes: {
		VpcId: vpc.VpcId,
	},
};

for (let resource of [vpc, subnet]) {
	let { getState, update } = objectByClass[resource.class_];
	let state = getState(resource);
	console.log(update(resource.name, state, resource).join('\n'));
}
