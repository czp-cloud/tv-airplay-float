#!/usr/bin/env python3
"""Real TV HTTP smoke test. Moves/resizes only our window; restores settings in finally.
Pairing code is read from TV_REMOTE_CODE or --code-file, never written to the report.
"""
import argparse
import json
import os
from pathlib import Path
import urllib.error
import urllib.request


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', required=True)
    parser.add_argument('--code-file', type=Path, help='Ignored local JSON from the DUMP-protected web diagnostic')
    parser.add_argument('--out', type=Path, default=Path('artifacts/remote-smoke.json'))
    args = parser.parse_args()
    base = args.url.rstrip('/')
    opener = urllib.request.build_opener(urllib.request.ProxyHandler({}))
    code = json.loads(args.code_file.read_text())['pairingCode'] if args.code_file else os.environ['TV_REMOTE_CODE']
    token = ''
    checks = []

    def call(path, body=None, *, auth=True, extra=None, method=None):
        headers = dict(extra or {})
        if auth and token:
            headers['Authorization'] = 'Bearer ' + token
        data = json.dumps(body).encode() if body is not None else None
        if data is not None:
            headers['Content-Type'] = 'application/json'
        request = urllib.request.Request(base + path, data=data, headers=headers, method=method)
        try:
            response = opener.open(request, timeout=8)
        except urllib.error.HTTPError as error:
            response = error
        raw = response.read()
        value = json.loads(raw) if 'application/json' in response.headers.get('Content-Type', '') else raw.decode()
        return response.status, value, dict(response.headers)

    def check(name, condition):
        checks.append({'name': name, 'passed': bool(condition)})
        print(name, 'PASS' if condition else 'FAIL', flush=True)
        assert condition, name

    def ok(path, body=None):
        status, value, _ = call(path, body)
        assert status == 200, (path, status, value)
        return value

    original = None
    try:
        status, page, headers = call('/', auth=False)
        check('mobile_page_served', status == 200 and 'viewport-fit=cover' in page and 'sizeSlider' in page)
        check('no_frame_embedding', headers.get('X-Frame-Options') == 'DENY')
        check('unauthorized_state_rejected', call('/api/state', auth=False)[0] == 401)
        check('unauthorized_move_rejected', call('/api/move', {'dx': 24, 'dy': 0}, auth=False)[0] == 401)
        check('cross_origin_rejected', call('/api/pair', {'code': code}, auth=False, extra={'Origin': 'http://example.invalid'})[0] == 403)
        check('rebound_host_rejected', call('/', auth=False, extra={'Host': 'example.invalid:8765'})[0] == 403)
        check('wrong_code_rejected', call('/api/pair', {'code': '000000'}, auth=False)[0] == 401)
        status, pairing, _ = call('/api/pair', {'code': code}, auth=False)
        check('pairing_success', status == 200 and len(pairing.get('token', '')) == 48)
        token = pairing['token']
        original = ok('/api/state')
        check('receiver_running', original['server'] == 'RUNNING')
        check('state_does_not_leak_pairing_secret', 'pairingCode' not in original and 'token' not in original)
        # Do not alter an active user's layout while running installation smoke tests.
        check('no_active_mirror_for_layout_test', not original['mirroring'])
        check('invalid_delta_rejected', call('/api/move', {'dx': 999, 'dy': 0})[0] == 400)
        check('noninteger_delta_rejected', call('/api/move', {'dx': 1.5, 'dy': 0})[0] == 400)
        check('invalid_size_rejected', call('/api/size', {'width': 0})[0] == 400)
        check('invalid_audio_source_rejected', call('/api/audio', {'source': 'other'})[0] == 400)
        check('get_cannot_change_window', call('/api/reset', method='GET')[0] == 405)
        check('path_traversal_rejected', call('/../local.properties')[0] == 404)
        resized = ok('/api/size', {'width': 700})
        check('absolute_resize_and_ratio', resized['overlay']['width'] == 700 and resized['overlay']['height'] == 393)
        moved = ok('/api/move', {'dx': -24, 'dy': 24})
        check('direction_moves_window', moved['overlay']['x'] == resized['overlay']['x'] - 24 and moved['overlay']['y'] == resized['overlay']['y'] + 24)
        check('idle_controls_keep_overlay_hidden', not moved['overlay']['visible'])
        reset = ok('/api/reset', {})
        check('reset_small_corner', reset['overlay']['width'] <= 320 and reset['overlay']['x'] > 0)
        waiting_audio = ok('/api/audio', {'source': 'mirror'})['audio']
        check('idle_audio_does_not_claim_focus', waiting_audio['source'] == 'mirror' and not waiting_audio['outputActive'] and not waiting_audio['focusHeld'])
        tv_audio = ok('/api/audio', {'source': 'tv'})['audio']
        check('tv_audio_releases_output_and_focus', tv_audio['source'] == 'tv' and not tv_audio['outputActive'] and not tv_audio['focusHeld'])
        # Pairing throttles guesses even though the web page is publicly loadable on the LAN.
        results = [call('/api/pair', {'code': '000000'}, auth=False)[0] for _ in range(9)]
        check('pairing_attempts_rate_limited', 429 in results)
    finally:
        if original and token and not original['mirroring']:
            try:
                current = ok('/api/size', {'width': original['overlay']['width']})
                for _ in range(50):
                    dx = original['overlay']['x'] - current['overlay']['x']
                    dy = original['overlay']['y'] - current['overlay']['y']
                    if dx == 0 and dy == 0:
                        break
                    current = ok('/api/move', {'dx': max(-96, min(96, dx)), 'dy': max(-96, min(96, dy))})
                ok('/api/audio', {'source': 'tv'})
            except Exception as error:
                checks.append({'name': 'restore_settings', 'passed': False, 'error': str(error)})
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps({'kind': 'real device HTTP API; idle audio checks are not proof of audible HDMI switching', 'checks': checks}, ensure_ascii=False, indent=2) + '\n')


if __name__ == '__main__':
    main()
