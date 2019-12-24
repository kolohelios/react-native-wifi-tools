import { NativeModules } from 'react-native';
import wifi from './index';

describe('test react-native-wifi-tools interface', () => {
	beforeAll(() => {
		NativeModules.WifiTools = {
			isApiAvailable: _callback => undefined,
		};
	});

	it('should have same function properties exposed', () => {
		expect(wifi.connect).toBeDefined();
		expect(wifi.connectSecure).toBeDefined();
		expect(wifi.getSSID).toBeDefined();
		expect(wifi.isApiAvailable).toBeDefined();
		expect(wifi.removeSSID).toBeDefined();
	});

	it('should have isAPIAvailable with same signature', () => {
		const isApiAvailable = wifi.isApiAvailable();
		expect(isApiAvailable).toBe(undefined);
	});
});
