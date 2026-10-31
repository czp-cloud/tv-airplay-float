#!/usr/bin/env python3
"""Real TV smoke test; installs/operates only dev.czp.tvairplay. Requires adb-shell==0.4.4."""
import argparse
import base64
import hashlib
import json
from pathlib import Path
import re
import time

from adb_shell.adb_device import AdbDeviceTcp
from adb_shell.auth.sign_pythonrsa import PythonRSASigner

PACKAGE = 'dev.czp.tvairplay'
NS = 'io.github.jqssun.airplay'
RECEIVER = f'{PACKAGE}/{NS}.floating.OverlayCommandsReceiver'
ACTIVITY = f'{PACKAGE}/{NS}.floating.FloatControlActivity'


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--host', required=True)
    parser.add_argument('--expected-model', required=True)
    parser.add_argument('--apk', type=Path)
    parser.add_argument('--already-uploaded', action='store_true', help='Skip sync push after verified b64push; still verify SHA-256')
    parser.add_argument('--compact-only', action='store_true', help='Background-only checks, never open control Activity')
    parser.add_argument('--out', type=Path, default=Path('artifacts/device-smoke'))
    args = parser.parse_args()
    args.out.mkdir(parents=True, exist_ok=True)
    key = Path.home() / '.android/adbkey'
    signer = PythonRSASigner(key.with_suffix('.pub').read_text(), key.read_text())
    device = AdbDeviceTcp(args.host, 5555, default_transport_timeout_s=20)
    device.connect(rsa_keys=[signer], auth_timeout_s=12)
    report = {'checks': [], 'started_at': time.strftime('%Y-%m-%dT%H:%M:%S%z')}

    def shell(command, timeout=30):
        return device.shell(command, read_timeout_s=timeout).strip()

    def check(name, condition, evidence=None):
        report['checks'].append({'name': name, 'passed': bool(condition), 'evidence': evidence})
        print(name, 'PASS' if condition else 'FAIL', flush=True)
        if not condition:
            raise AssertionError(name + ': ' + str(evidence))

    def command(name, extras=''):
        raw = shell(f'am broadcast -n {RECEIVER} --es command {name} {extras}')
        match = re.search(r'data="(\{.*\})"', raw, re.S)
        if not match:
            raise RuntimeError('No diagnostic result: ' + raw)
        return json.loads(match.group(1))

    def screenshot(name):
        data = base64.b64decode(shell('screencap -p | base64'))
        if not data.startswith(b'\x89PNG'):
            raise RuntimeError('Invalid screenshot')
        (args.out / name).write_bytes(data)

    def music_volume():
        lines = shell('dumpsys audio').splitlines()
        return next((lines[i:i + 7] for i, s in enumerate(lines) if '- STREAM_MUSIC:' in s), [])

    stop_on_error = False
    try:
        model = shell('getprop ro.product.model')
        check('target_model', model == args.expected_model, model)
        volume_before = music_volume()
        report['volume_before'] = volume_before
        if args.apk:
            before_install = command('state')
            check('no_active_mirror_before_install', not before_install['mirroring'] and before_install['connections'] == 0, before_install)
            apk = args.apk.resolve()
            digest = hashlib.sha256(apk.read_bytes()).hexdigest()
            target = '/data/local/tmp/tv-airplay-float.apk'
            if not args.already_uploaded:
                device.push(str(apk), target, read_timeout_s=120)
            check('apk_transfer_sha256', shell('sha256sum ' + target).split()[0] == digest, digest)
            result = shell('pm install -r ' + target, timeout=90)
            check('apk_install', result == 'Success', result)
            shell('rm ' + target)
            report['apk_sha256'] = digest
        shell('appops set ' + PACKAGE + ' SYSTEM_ALERT_WINDOW allow')
        check('overlay_appop', 'allow' in shell('appops get ' + PACKAGE + ' SYSTEM_ALERT_WINDOW'))
        if args.compact_only:
            focus_before = [s.strip() for s in shell('dumpsys window').splitlines() if 'mCurrentFocus=' in s]
            command('start')
            stop_on_error = True
            time.sleep(2)
            compact = command('compact')
            check('airplay_server_running', compact['server'] == 'RUNNING', compact)
            window = compact['overlay']
            check('idle_window_hidden', not window['visible'] and not compact['mirroring'], window)
            check('frameless_compact_geometry', window['width'] <= 320 and window['height'] == window['width'] * 9 // 16 and not window['chrome'], window)
            check('fully_opaque', abs(window['opacity'] - 1) < 0.001, window)
            focus_after = [s.strip() for s in shell('dumpsys window').splitlines() if 'mCurrentFocus=' in s]
            check('foreground_unchanged', focus_after == focus_before, focus_after)
            volume_after = music_volume()
            report['volume_after'] = volume_after
            check('tv_volume_unchanged', volume_after == volume_before, volume_after)
            report['final_state'] = command('state')
            stop_on_error = False
            return
        activity = shell('am start -n ' + ACTIVITY)
        check('control_activity_start', 'Error' not in activity, activity)
        time.sleep(1)
        screenshot('01-controls.png')
        command('start')
        stop_on_error = True
        deadline = time.monotonic() + 15
        state = command('state')
        while state['server'] != 'RUNNING' and time.monotonic() < deadline:
            time.sleep(0.5)
            state = command('state')
        check('airplay_server_running', state['server'] == 'RUNNING', state)
        check('idle_window_hidden', not state['overlay']['visible'], state)
        screenshot('02-overlay.png')
        packages = shell('dumpsys package ' + PACKAGE)
        uid = re.search(r'\buserId=(\d+)', packages)
        check('ordinary_application_uid', uid is not None and int(uid.group(1)) >= 10000,
              uid.group(1) if uid else 'missing')
        moved = command('move', '--ei dx -100000 --ei dy -100000')['overlay']
        check('move_clamped_at_origin', moved['x'] == 0 and moved['y'] == 0, moved)
        smaller = command('resize', '--ef scale 0.75')['overlay']
        check('resize_smaller', smaller['width'] < moved['width'], smaller)
        larger = command('resize', '--ef scale 1.5')['overlay']
        check('resize_larger', larger['width'] > smaller['width'], larger)
        # Move to upper right; clamping must keep the whole window visible.
        right = command('move', '--ei dx 100000 --ei dy 24')['overlay']
        dimensions = re.search(r'Physical size: (\d+)x(\d+)', shell('wm size'))
        sw, sh = map(int, dimensions.groups())
        check('window_inside_screen', right['x'] >= 0 and right['y'] >= 0 and
              right['x'] + right['width'] <= sw and right['y'] + right['height'] <= sh, right)
        screenshot('03-resized.png')
        # Activity Back restores the previous task; the overlay itself must not take input focus.
        shell('input keyevent KEYCODE_BACK')
        time.sleep(1)
        windows = shell('dumpsys window')
        focus = [s.strip() for s in windows.splitlines() if 'mCurrentFocus=' in s or 'mFocusedApp=' in s]
        report['focus_after_back'] = focus
        check('underlying_window_keeps_focus', focus and all(PACKAGE not in s and NS not in s for s in focus), focus)
        screenshot('04-overlay-on-tv.png')
        volume_after = music_volume()
        check('tv_volume_unchanged', volume_after == volume_before, volume_after)
        command('stop')
        time.sleep(1)
        stopped = command('state')
        check('stop_removes_overlay', stopped['server'] == 'STOPPED' and not stopped['overlay']['visible'], stopped)
        command('start')
        time.sleep(2)
        restarted = command('state')
        check('receiver_restarts', restarted['server'] == 'RUNNING' and not restarted['overlay']['visible'], restarted)
        report['final_state'] = restarted
        stop_on_error = False  # Leave receiver running for the user's iPhone test.
    finally:
        if stop_on_error:
            try:
                command('stop')
            except Exception:
                shell('am force-stop ' + PACKAGE)
        report['finished_at'] = time.strftime('%Y-%m-%dT%H:%M:%S%z')
        (args.out / 'report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n')
        device.close()


if __name__ == '__main__':
    main()
