#!/usr/bin/env python3
"""Validate repository entry points, local links, dependency pins and accidental local data."""
from pathlib import Path
import ipaddress
import json
import re
import subprocess
import sys

if sys.version_info < (3, 11):
    raise SystemExit('Repository checker requires Python 3.11 or newer.')

import tomllib
from urllib.parse import unquote

ROOT = Path(__file__).resolve().parents[1]


def git(*args):
    return subprocess.check_output(['git', '-C', str(ROOT), *args]).decode()


def main():
    errors = []
    required = [
        'README.md', 'README.zh-CN.md', 'AGENTS.md', 'CONTRIBUTING.md',
        'SECURITY.md', 'CODE_OF_CONDUCT.md', 'LICENSE', 'THIRD_PARTY_NOTICES.md',
        'CHANGELOG.md', 'docs/README.md', 'docs/dependencies.md',
        'docs/dependencies.json', 'docs/development/building.md',
        'docs/development/testing.md', 'docs/development/architecture.md',
        'docs/maintainers/releases.md', 'tools/package-lock.json',
        'tools/requirements-device.txt', '.github/workflows/ci.yml',
    ]
    for name in required:
        if not (ROOT / name).is_file():
            errors.append('Missing entry point: ' + name)

    files = {ROOT / name for name in git('ls-files', '--cached', '--others', '--exclude-standard', '-z').split('\0') if name}
    files = sorted(p for p in files if p.is_file())
    forbidden = re.compile(r'(^|/)(artifacts|node_modules|\.venv|\.secrets|\.tools|\.gradle|build|adbkey)(/|$)|(^|/)(local\.properties|\.env)$|\.(jks|keystore|p12|pfx|apk|aab)$')
    for p in files:
        name = p.relative_to(ROOT).as_posix()
        if forbidden.search(name):
            errors.append('Local/private file would enter source: ' + name)

    inventory = json.loads((ROOT / 'docs/dependencies.json').read_text())
    pins = {line.split('\t')[1]: line.split()[1] for line in git('ls-files', '--stage').splitlines() if line.startswith('160000 ')}
    if set(pins) != {item['path'] for item in inventory['native_submodules'].values()}:
        errors.append('Native inventory does not cover the committed submodules')
    for item in inventory['native_submodules'].values():
        if pins.get(item['path']) != item['revision']:
            errors.append('Submodule inventory differs: ' + item['path'])
    catalog = tomllib.loads((ROOT / 'gradle/libs.versions.toml').read_text())
    if inventory['catalog'] != 'gradle/libs.versions.toml' or not catalog['libraries'].get('nanohttpd'):
        errors.append('Dependency catalog/inventory is inconsistent')
    package = json.loads((ROOT / 'tools/package.json').read_text())
    lock = json.loads((ROOT / 'tools/package-lock.json').read_text())
    if package['devDependencies'] != lock['packages']['']['devDependencies']:
        errors.append('Node manifest and lockfile differ')
    wrapper = (ROOT / 'gradle/wrapper/gradle-wrapper.properties').read_text()
    if not re.search(r'^distributionSha256Sum=[a-f0-9]{64}$', wrapper, re.M):
        errors.append('Gradle distribution checksum is missing')

    links = skipped = 0
    for p in files:
        if p.suffix.lower() != '.md':
            continue
        content = p.read_text()
        destinations = re.findall(r'!?\[[^\]\n]*\]\(([^)]+)\)', content)
        destinations += re.findall(r'<img\b[^>]*\bsrc="([^"]+)"', content)
        for target in destinations:
            target = target.strip().split(' "', 1)[0].strip('<>')
            if target.startswith(('#', 'mailto:')) or re.match(r'^[a-z]+://', target):
                continue
            target = unquote(target.split('#', 1)[0])
            resolved = (p.parent / target).resolve()
            # A non-recursive clone still carries valid gitlink references.
            module = next((ROOT / path for path in pins if resolved.is_relative_to(ROOT / path)), None)
            if module is not None and not (module / '.git').exists():
                skipped += 1
                continue
            links += 1
            if not resolved.exists():
                errors.append(f'Broken link: {p.relative_to(ROOT)} -> {target}')
        for number, line in enumerate(content.splitlines(), 1):
            private = False
            for match in re.findall(r'(?<![\d.])(?:\d{1,3}\.){3}\d{1,3}(?![\d.])', line):
                try:
                    raw = ipaddress.IPv4Address(match).packed
                    private |= raw[0] == 10 or raw[:2] == bytes([192, 168]) or (raw[0] == 172 and 16 <= raw[1] <= 31)
                except ipaddress.AddressValueError:
                    continue
            if private or re.search(r'/Users/[^/\s]+/|[A-Z]:\\Users\\', line):
                errors.append(f'Host-specific address/path in {p.relative_to(ROOT)}:{number}')

    if errors:
        for error in errors:
            print('FAIL', error)
        return 1
    print(f'PASS repository: {len(files)} source files, {links} local links, {len(pins)} submodule pins')
    if skipped:
        print(f'NOTE {skipped} links into uninitialized submodules skipped; initialize them to check those paths')
    print('PASS dependency metadata, Gradle checksum and current-document privacy checks')
    return 0


if __name__ == '__main__':
    sys.exit(main())
