#!/usr/bin/env python3
"""Read-only TV audio inventory. Does not change routing, focus, volume, or permissions."""
import argparse
import json
from pathlib import Path
import re
from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.sign_pythonrsa import PythonRSASigner


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', required=True)
    parser.add_argument('--expected-model', default='MiTV-MTEQ0')
    parser.add_argument('--out', type=Path, default=Path('artifacts/audio-inventory'))
    args = parser.parse_args()
    key = Path.home() / '.android/adbkey'
    signer = PythonRSASigner(key.with_suffix('.pub').read_text(), key.read_text())
    device = AdbDeviceTcp(args.host, 5555, default_transport_timeout_s=10)
    device.connect(rsa_keys=[signer], auth_timeout_s=10)
    try:
        model = device.shell('getprop ro.product.model').strip()
        if model != args.expected_model:
            raise RuntimeError('Wrong device model: ' + model)
        args.out.mkdir(parents=True, exist_ok=True)
        commands = {
            'identity': 'getprop ro.product.model && getprop ro.build.version.sdk && id',
            'audio-focus': 'dumpsys audio',
            'audio-policy': 'dumpsys media.audio_policy',
            'audio-flinger': 'dumpsys media.audio_flinger',
            'tv-input': 'dumpsys tv_input',
            'output-devices': 'am broadcast -n dev.czp.tvairplay/io.github.jqssun.airplay.floating.OverlayCommandsReceiver --es command audio-devices',
            'vendor-policy': 'cat /vendor/etc/audio_policy_configuration.xml /vendor/etc/audio_policy.conf',
            'system-policy': 'cat /system/etc/audio_policy_configuration.xml /system/etc/audio_policy.conf',
        }
        for name, command in commands.items():
            raw = device.shell(command, read_timeout_s=30)
            (args.out / (name + '.txt')).write_text(raw)
            print(name, 'saved', flush=True)
            if name == 'output-devices':
                match = re.search(r'data="(\{.*\})"', raw, re.S)
                if match:
                    (args.out / 'output-devices.json').write_text(json.dumps(json.loads(match.group(1)), ensure_ascii=False, indent=2) + '\n')
    finally:
        device.close()


if __name__ == '__main__':
    main()
