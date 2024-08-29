import { existsSync, readFileSync } from 'fs';

let stateDir = '/tmp';

let readJsonIfExists = name => {
	let filename = `${name}.json`;
	return existsSync(filename) ? JSON.parse(readFileSync(filename)) : null;
};

let vpcClass = () => {
	let stateFilename = name => `${stateDir}/${name}`;

	let create = ({ name, attributes: { CidrBlockAssociationSet } }) => [
		`aws ec2 create-vpc --cidr-block ${CidrBlockAssociationSet[0].CidrBlock} --tag-specifications '${JSON.stringify([
			{ ResourceType: 'vpc', Tags: [{ Key: 'Name', Value: name }] }
		])}' | jq .Vpc > ${stateFilename(name)}.json`,
	];

	let delete_ = (name, id) => [
		`aws ec2 delete-vpc --vpc-id ${id}`, `rm -f ${stateFilename(name)}.json`,
	];

	// let findIdByName = name => `aws ec2 describe-vpcs --filter Name:${name} | jq -r .Vpcs[0].VpcId`;
	let findIdByName = name => `cat ${stateFilename(name)}.json | jq -r .VpcId`;

	return {
		create,
		delete_,
		findIdByName,
		refresh: (name, id) => [
			`aws ec2 describe-vpcs --vpc-ids ${id} | jq .Vpcs[0] > ${stateFilename(name)}.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsHostnames | jq -r .EnableDnsHostnames.Value > ${stateFilename(name)}.EnableDnsHostnames.json`,
			`aws ec2 describe-vpc-attribute --vpc-id ${id} --attribute enableDnsSupport | jq -r .EnableDnsSupport.Value > ${stateFilename(name)}.EnableDnsSupport.json`,
		],
		update: resource => {
			let { class_, name, attributes } = resource;
			let commands = [];

			let stateFilename_ = stateFilename(name);
			let state = readJsonIfExists(`${stateFilename_}.json`);
			let attributes0;

			if (state != null) {
				attributes0 = {
					...state,
					EnableDnsHostnames: readJsonIfExists(`${stateFilename_}.EnableDnsHostnames`),
					EnableDnsSupport: readJsonIfExists(`${stateFilename_}.EnableDnsSupport`),
				};
			} else {
				commands.push(...create(resource));
				attributes0 = { CidrBlockAssociationSet: [{ CidrBlock: attributes['CidrBlockAssociationSet'][0]['CidrBlock'] }] };
			}

			let { attributes: attributes1 } = resource ?? { class_, name, attributes: {} };
			let vpcId = '$(' + findIdByName(name) + ')';

			/* {
				let key = 'xxx';
				if (attributes0[key] !== attributes1[key]) {
					return [...delete_(name, vpcId), ...create(name)];
				}
			} */
			{
				let key = 'CidrBlockAssociationSet';
				let map0 = Object.fromEntries(attributes0[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
				let map1 = Object.fromEntries(attributes1[key].map(({ CidrBlock, AssociationId }) => [CidrBlock, AssociationId]));
				for (let [CidrBlock, AssociationId] of Object.entries(map0)) {
					if (!map1.hasOwnProperty(CidrBlock)) {
						commands.push(`aws ec2 disassociate-vpc-cidr-block --vpc-id ${vpcId} --association-id ${AssociationId}`);
					}
				}
				for (let [CidrBlock, AssociationId] of Object.entries(map1)) {
					if (!map0.hasOwnProperty(CidrBlock)) {
						commands.push(`aws ec2 associate-vpc-cidr-block --vpc-id ${vpcId} --cidr-block ${CidrBlock}`);
					}
				}
			}
			{
				let key = 'EnableDnsHostnames';
				if (attributes0[key] !== attributes1[key]) {
					commands.push(
						`aws ec2 modify-vpc-attribute --vpc-id ${vpcId} ${attributes1[key] ? `--` : `--no-`}enable-dns-hostnames`,
						`echo ${attributes1[key]} > ${stateFilename(name)}.EnableDnsHostnames.json`);
				}
			}
			{
				let key = 'EnableDnsSupport';
				if (attributes0[key] !== attributes1[key]) {
					commands.push(
						`aws ec2 modify-vpc-attribute --vpc-id ${vpcId} ${attributes1[key] ? `--` : `--no-`}enable-dns-support`,
						`echo ${attributes1[key]} > ${stateFilename(name)}.EnableDnsSupport.json`);
				}
			}

			if (resource == null) {
				commands.push(...delete_(name, vpcId));
			}

			return commands;
		},
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

let objectByClass = { vpc: vpcClass() };

let object = objectByClass[resource.class_];

console.log(object.update(resource).join('\n'));
