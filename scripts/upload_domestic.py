#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 sources/dist 同步到国内对象存储（阿里云 OSS / 腾讯云 COS，可选）。

没有配置密钥时直接跳过，不影响每日任务。
环境变量：
  阿里云 OSS：OSS_ACCESS_KEY_ID / OSS_ACCESS_KEY_SECRET / OSS_ENDPOINT / OSS_BUCKET
  腾讯云 COS：COS_SECRET_ID / COS_SECRET_KEY / COS_REGION / COS_BUCKET
可选：
  DOMESTIC_PREFIX       对象前缀，默认 dist
"""

from __future__ import annotations

import os
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
DIST = ROOT / "sources" / "dist"
PREFIX = os.environ.get("DOMESTIC_PREFIX", "dist").strip("/")


def upload_oss() -> bool:
    key_id = os.environ.get("OSS_ACCESS_KEY_ID", "")
    key_secret = os.environ.get("OSS_ACCESS_KEY_SECRET", "")
    endpoint = os.environ.get("OSS_ENDPOINT", "")
    bucket_name = os.environ.get("OSS_BUCKET", "")
    if not all([key_id, key_secret, endpoint, bucket_name]):
        return False
    try:
        import oss2
    except ImportError:
        print("未安装 oss2，跳过 OSS 上传", file=sys.stderr)
        return False

    auth = oss2.Auth(key_id, key_secret)
    bucket = oss2.Bucket(auth, endpoint, bucket_name)
    for f in sorted(DIST.iterdir()):
        if not f.is_file():
            continue
        key = f"{PREFIX}/{f.name}" if PREFIX else f.name
        bucket.put_object_from_file(
            key, str(f), headers={"x-oss-object-acl": "public-read"}
        )
        print(f"OSS -> {key} ({f.stat().st_size} bytes)")
    return True


def upload_cos() -> bool:
    secret_id = os.environ.get("COS_SECRET_ID", "")
    secret_key = os.environ.get("COS_SECRET_KEY", "")
    region = os.environ.get("COS_REGION", "")
    bucket_name = os.environ.get("COS_BUCKET", "")
    if not all([secret_id, secret_key, region, bucket_name]):
        return False
    try:
        from qcloud_cos import CosConfig, CosS3Client
    except ImportError:
        print("未安装 cos-python-sdk-v5，跳过 COS 上传", file=sys.stderr)
        return False

    client = CosS3Client(CosConfig(Region=region, SecretId=secret_id, SecretKey=secret_key))
    for f in sorted(DIST.iterdir()):
        if not f.is_file():
            continue
        key = f"{PREFIX}/{f.name}" if PREFIX else f.name
        with f.open("rb") as fp:
            client.put_object(Bucket=bucket_name, Body=fp, Key=key, ACL="public-read")
        print(f"COS -> {key} ({f.stat().st_size} bytes)")
    return True


def main() -> int:
    if not DIST.exists():
        print("sources/dist 不存在", file=sys.stderr)
        return 1
    if upload_oss() or upload_cos():
        print("国内对象存储同步完成")
        return 0
    print("未配置 OSS/COS 密钥，跳过国内对象存储同步")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
