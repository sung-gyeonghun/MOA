"""Verify an existing complete catalog before deployment, without requiring credentials."""
import hashlib
import json
from pathlib import Path
import re
import sys
from datetime import datetime
from collect import validate

def verify(root: Path):
    manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
    datetime.fromisoformat(manifest["updatedAt"])
    unique = {}
    for filename in manifest["files"]:
        if not re.fullmatch(r"performances/\d{4}-\d{2}\.json", filename):
            raise ValueError("Invalid catalog filename")
        data = (root / filename).read_bytes()
        if hashlib.sha256(data).hexdigest() != manifest["fileHashes"][filename]:
            raise ValueError(f"Catalog checksum mismatch: {filename}")
        for item in json.loads(data):
            validate(item)
            if item["region"] == "해외":
                raise ValueError("Overseas performance in domestic catalog")
            unique[item["id"]] = item
    if not unique or len(unique) != manifest["count"]:
        raise ValueError("Catalog count mismatch")
    for identity, item in unique.items():
        detail = json.loads((root / "details" / (identity + ".json")).read_text(encoding="utf-8"))
        if detail != item:
            raise ValueError(f"Detail mismatch: {identity}")
    return len(unique)

if __name__ == "__main__":
    print(f"Validated {verify(Path(sys.argv[1] if len(sys.argv) > 1 else 'public'))} domestic performances")
