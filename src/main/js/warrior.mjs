import { existsSync, readFileSync } from 'fs';

let stateDir = '/tmp';

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let stateFilename = name => `${stateDir}/${name}`;

let subnetClass = () => {
	let create = ({ name, attributes: { VpcId } }) => [
		`aws ec2 create-subnet --cidr-block ${CidrBlock} --tag-specifications '${JSON.stringify([
			{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
		])} --vpc-id ${VpcId}' | jq .Vpc > ${stateFilename(name)}.json`,
	];

	let delete_ = (name, state) => [
		`aws ec2 delete-vpc --subnet-id ${state.SubnetId}`, `rm -f ${stateFilename(name)}.json`,
	];

	// let findIdByName = name => `aws ec2 describe-subnets --filter Name:${name} | jq -r .Subnets[0].SubnetId`;
	let findIdByName = name => `cat ${stateFilename(name)}.json | jq -r .SubnetId`;

	let update = (resource, state) => {
		let { class_, name, attributes } = resource;
		let commands = [];
		let attributes0;

		if (state != null) {
			attributes0 = state;
		} else {
			commands.push(...create(resource));
			attributes0 = { CidrBlock: attributes['CidrBlock'], VpcId: attributes['VpcId'] };
		}

		let { attributes: attributes1 } = resource ?? { class_, name, attributes: {} };

		{
			let key = 'CidrBlock';
			if (attributes0[key] !== attributes1[key]) {
				return [...delete_(name, state), ...update(resource, null)];
			}
		}

		{
			let key = 'VpcId';
			if (attributes0[key] !== attributes1[key]) {
				return [...delete_(name, state), ...update(resource, null)];
			}
		}

		return commands;
	};

	return {
		create,
		delete_,
		findIdByName,
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

	// let findIdByName = name => `aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId`;
	let findIdByName = name => `cat ${stateFilename(name)}.json | jq -r .VpcId`;

	let update = (resource, state) => {
		let { class_, name, attributes } = resource;
		let commands = [];
		let attributes0;

		if (state != null) {
			attributes0 = state;
		} else {
			commands.push(...create(resource));
			attributes0 = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
		}

		let { attributes: attributes1 } = resource ?? { class_, name, attributes: {} };
		let VpcId = '$(' + findIdByName(name) + ')';

		{
			let key = 'CidrBlockAssociationSet';
			let map0 = Object.fromEntries(attributes0[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
			let map1 = Object.fromEntries(attributes1[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
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
			if (attributes0[key] !== attributes1[key]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes1[key] ? `--` : `--no-`}enable-dns-hostnames`,
					`echo ${attributes1[key]} > ${stateFilename(name)}.EnableDnsHostnames.json`);
			}
		}
		{
			let key = 'EnableDnsSupport';
			if (attributes0[key] !== attributes1[key]) {
				commands.push(
					`aws ec2 modify-vpc-attribute --vpc-id ${VpcId} ${attributes1[key] ? `--` : `--no-`}enable-dns-support`,
					`echo ${attributes1[key]} > ${stateFilename(name)}.EnableDnsSupport.json`);
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
		findIdByName,
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

let resource = {
	class_: 'vpc',
	name: 'npt-cloud-vpc',
	attributes: {
		CidrBlockAssociationSet: [{ CidrBlock: '10.25.0.0/16' }],
		EnableDnsHostnames: true,
		EnableDnsSupport: true,
	},
};

let objectByClass = {
	subnet: subnetClass(),
	vpc: vpcClass(),
};

let object = objectByClass[resource.class_];

console.log(object.update(resource, object.getState(resource)).join('\n'));
