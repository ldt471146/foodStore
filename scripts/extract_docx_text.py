from __future__ import annotations

import sys
import zipfile
from pathlib import Path
from xml.etree import ElementTree as ET


WORD_NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}


def extract_text(docx_path: Path) -> str:
    with zipfile.ZipFile(docx_path) as archive:
        root = ET.fromstring(archive.read("word/document.xml"))

    paragraphs: list[str] = []
    for para in root.findall(".//w:p", WORD_NS):
        text = "".join(node.text or "" for node in para.findall(".//w:t", WORD_NS)).strip()
        if text:
            paragraphs.append(text)
    return "\n".join(paragraphs)


def main() -> int:
    if len(sys.argv) == 2:
        target = Path(sys.argv[1])
    else:
        candidates = [path for path in Path.cwd().glob("*.docx") if path.is_file()]
        if len(candidates) != 1:
            print("usage: py scripts/extract_docx_text.py <docx-path>", file=sys.stderr)
            print(f"found {len(candidates)} docx files in cwd", file=sys.stderr)
            return 1
        target = candidates[0]

    print(extract_text(target))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
