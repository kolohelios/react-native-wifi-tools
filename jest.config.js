module.exports = {
	preset: 'react-native',
	collectCoverageFrom: ['src/**/*.ts'],
	transform: { '^.+\\.(ts|tsx)$': 'ts-jest' },
	globals: {
		'ts-jest': {
			diagnostics: true,
		},
	},
};
