#!/usr/bin/env python3
"""生成并签名内置直播源清单。

清单由 APK 同一签名证书签出；App 运行时用自身证书验签，再校验每个 m3u
的 SHA-256。这样即使镜像/CDN 被替换，也无法让 App 接受被篡改的源。

用法：
  python3 scripts/sign_sources.py \
    --keystore keystore.jks --store-password "$KEYSTORE_PASSWORD" \
    --key-alias "$KEY_ALIAS" --key-password "$KEY_PASSWORD"
"""
import argparse
import base64
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "core" / "data" / "src" / "main" / "assets" / "sources"
DIST = ROOT / "sources" / "dist"
MANIFEST = "source-manifest.json"
SIGNATURE = "source-manifest.sig"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def build_manifest(assets: Path) -> bytes:
    files = {}
    for path in sorted(assets.glob("*.m3u")):
        files[path.name] = {"size": path.stat().st_size, "sha256": sha256(path)}
    if not files:
        raise SystemExit(f"没有找到 m3u：{assets}")
    payload = {"version": 1, "files": files}
    return json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode("utf-8")


def run(cmd: list[str]) -> None:
    subprocess.run(cmd, check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def sign_manifest(manifest: bytes, keystore: Path, store_password: str, alias: str, key_password: str) -> str:
    with tempfile.TemporaryDirectory(prefix="tv-source-sign-") as tmp:
        tmpdir = Path(tmp)
        p12 = tmpdir / "key.p12"
        key_pem = tmpdir / "key.pem"
        manifest_path = tmpdir / "manifest.json"
        signature_path = tmpdir / "manifest.sig"
        manifest_path.write_bytes(manifest)

        run([
            "keytool", "-importkeystore", "-noprompt",
            "-srckeystore", str(keystore), "-srcstorepass", store_password,
            "-srcalias", alias, "-srckeypass", key_password,
            "-destkeystore", str(p12), "-deststoretype", "PKCS12",
            "-deststorepass", store_password, "-destkeypass", key_password,
        ])
        run([
            "openssl", "pkcs12", "-in", str(p12), "-nocerts", "-nodes",
            "-passin", f"pass:{store_password}", "-out", str(key_pem),
        ])
        run([
            "openssl", "dgst", "-sha256", "-sign", str(key_pem),
            "-out", str(signature_path), str(manifest_path),
        ])
        return base64.b64encode(signature_path.read_bytes()).decode("ascii") + "\n"


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--keystore", required=True, type=Path)
    parser.add_argument("--store-password", required=True)
    parser.add_argument("--key-alias", required=True)
    parser.add_argument("--key-password", required=True)
    parser.add_argument("--assets-dir", type=Path, default=ASSETS)
    parser.add_argument("--dist-dir", type=Path, default=DIST)
    args = parser.parse_args()

    keystore = args.keystore.expanduser().resolve()
    if not keystore.is_file():
        raise SystemExit(f"keystore 不存在：{keystore}")

    manifest = build_manifest(args.assets_dir)
    signature = sign_manifest(
        manifest, keystore, args.store_password, args.key_alias, args.key_password,
    )

    for directory in (args.assets_dir, args.dist_dir):
        directory.mkdir(parents=True, exist_ok=True)
        (directory / MANIFEST).write_bytes(manifest)
        (directory / SIGNATURE).write_text(signature, encoding="ascii")

    payload = json.loads(manifest)
    print(f"已签名 {len(payload['files'])} 个源文件：{', '.join(payload['files'])}")
    print(f"清单：{args.assets_dir / MANIFEST}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
