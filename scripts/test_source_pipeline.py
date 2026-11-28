#!/usr/bin/env python3
"""源流水线的无网络回归测试。"""

from __future__ import annotations

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import build_pack
import refresh_sources
import sign_sources
from sources_pipeline import channels, parsing


class BuildPackTest(unittest.TestCase):
    def test_parse_preserves_line_metadata(self) -> None:
        text = """#EXTM3U
#EXTINF:-1 tvg-id="CCTV1" tvg-name="CCTV1" tvg-logo="logo" http-user-agent="UA/1" group-title="央视",CCTV1
#EXTVLCOPT:http-user-agent=UA/1
#KODIPROP:inputstream.adaptive.license_type=clearkey
http://example.com/a.m3u8
"""
        items = build_pack.parse(text)
        self.assertEqual(1, len(items))
        item = items[0]
        self.assertEqual("UA/1", item["http_user_agent"])
        self.assertEqual(
            ["#EXTVLCOPT:http-user-agent=UA/1",
             "#KODIPROP:inputstream.adaptive.license_type=clearkey"],
            item["directives"],
        )
        self.assertIn('http-user-agent="UA/1"', build_pack.extinf_line(
            "CCTV1", "CCTV-1 综合", "logo", "央视", item
        ))

    def test_pick_lines_prefers_playable(self) -> None:
        items = [
            {"url": "http://a/1", "alive": True, "playable": False, "cost": 0.1, "source": "a"},
            {"url": "http://b/1", "alive": True, "playable": True, "cost": 0.5, "source": "b"},
            {"url": "http://c/1", "alive": False, "playable": None, "cost": 0.0, "source": "c"},
        ]
        picked = build_pack.pick_lines(items, 2)
        self.assertEqual("http://b/1", picked[0]["url"])


class SourcesPipelinePackageTest(unittest.TestCase):
    def test_alias_table_is_shared_with_android(self) -> None:
        self.assertEqual("CCTV1", channels.standard_name("CCTV-1 高清"))
        self.assertEqual("CCTV1", channels.standard_name("央视一套"))
        self.assertEqual("湖南卫视", channels.standard_name("Hunan TV"))
        self.assertEqual("CCTV5+", channels.standard_name("CCTV5+"))
        self.assertIsNone(channels.standard_name("不存在的频道"))

    def test_parsing_helpers(self) -> None:
        self.assertEqual("http://a/b.m3u8", parsing.clean_url("http://a/b.m3u8|User-Agent=x"))
        self.assertFalse(parsing.valid_url("file:///etc/passwd"))
        items = parsing.parse("#EXTM3U\n#EXTINF:-1,CCTV1\nhttp://a/1.m3u8\n")
        self.assertEqual("CCTV1", items[0]["raw_name"])

    def test_cli_entry_still_imports(self) -> None:
        self.assertTrue(callable(build_pack.main))
        self.assertEqual("CCTV-1 综合", build_pack.display_name("CCTV1"))


class SourceManifestScriptTest(unittest.TestCase):
    def test_content_version_changes_with_file_hash(self) -> None:
        import json

        with tempfile.TemporaryDirectory() as tmp:
            assets = Path(tmp)
            (assets / "a.m3u").write_text("#EXTM3U\n#EXTINF:-1,A\nhttp://a/1\n", encoding="utf-8")
            first = json.loads(sign_sources.build_manifest(assets))
            self.assertEqual(16, len(first["contentVersion"]))
            (assets / "a.m3u").write_text("#EXTM3U\n#EXTINF:-1,A\nhttp://a/2\n", encoding="utf-8")
            second = json.loads(sign_sources.build_manifest(assets))
            self.assertNotEqual(first["contentVersion"], second["contentVersion"])


class RefreshSourcesTest(unittest.TestCase):
    def test_group_by_standard_channel_id(self) -> None:
        blocks = [
            '#EXTINF:-1 tvg-id="A@SD",A\nhttp://a/1',
            '#EXTINF:-1 tvg-id="A@HD",A\nhttp://a/2',
            '#EXTINF:-1 tvg-id="B@SD",B\nhttp://b/1',
        ]
        self.assertEqual("A", refresh_sources._block_key(blocks[0]))
        self.assertEqual("B", refresh_sources._block_key(blocks[2]))

    def test_probe_blocks_drops_dead_when_alive(self) -> None:
        blocks = [
            '#EXTINF:-1 tvg-id="A@SD",A\nhttp://a/1',
            '#EXTINF:-1 tvg-id="A@HD",A\nhttp://a/2',
            '#EXTINF:-1 tvg-id="B@SD",B\nhttp://b/1',
        ]

        def fake_probe(items: list[dict]) -> list[dict]:
            for index, item in enumerate(items):
                item["alive"] = index in (1, 2)
                item["cost"] = 0.1 * (index + 1)
                item["kind"] = "hls"
            return items

        original = refresh_sources.probe_all
        original_dist = refresh_sources.DIST
        refresh_sources.probe_all = fake_probe
        with tempfile.TemporaryDirectory() as tmp:
            refresh_sources.DIST = Path(tmp)
            try:
                selected = refresh_sources.probe_blocks(blocks)
            finally:
                refresh_sources.probe_all = original
                refresh_sources.DIST = original_dist

        joined = "\n".join(selected)
        self.assertIn("http://a/2", joined)
        self.assertIn("http://b/1", joined)
        self.assertNotIn("http://a/1", joined)


if __name__ == "__main__":
    unittest.main()
