"""KOPIS full synchronization. Publish only after every request and validation succeeds."""
from __future__ import annotations

import argparse
import calendar
from datetime import date, datetime, timedelta
import json
import os
from pathlib import Path
import re
import shutil
import tempfile
import time
import uuid
import hashlib
from urllib.parse import urlencode, urlparse
from urllib.request import urlopen
import xml.etree.ElementTree as ET
from zoneinfo import ZoneInfo

BASE = "https://www.kopis.or.kr/openApi/restful/"
REGIONS = {"서울특별시": "서울", "부산광역시": "부산", "대구광역시": "대구", "인천광역시": "인천", "광주광역시": "광주", "대전광역시": "대전", "울산광역시": "울산", "세종특별자치시": "세종", "경기도": "경기", "강원특별자치도": "강원", "강원도": "강원", "충청북도": "충북", "충청남도": "충남", "전북특별자치도": "전북", "전라북도": "전북", "전라남도": "전남", "경상북도": "경북", "경상남도": "경남", "제주특별자치도": "제주"}
REGIONS.update({"전남광주통합특별시": "전남광주", "광주": "전남광주", "광주광역시": "전남광주", "전남": "전남광주", "전라남도": "전남광주"})
GENRES = {"대중음악": "콘서트", "서양음악(클래식)": "클래식", "한국음악(국악)": "국악", "무용(서양/한국무용)": "무용", "대중무용": "무용", "서커스/마술": "서커스/마술"}
# KOPIS OpenAPI common-code document; the listing response supplies genrenm.
GENRE_CODES = {"연극": "AAAA", "뮤지컬": "GGGA", "서양음악(클래식)": "CCCA", "한국음악(국악)": "CCCC", "대중음악": "CCCD", "무용(서양/한국무용)": "BBBC", "대중무용": "BBBR", "서커스/마술": "EEEB", "복합": "EEEA"}

def request(path: str, key: str, **params) -> ET.Element:
    # Never log the URL: it contains the service key.
    url = BASE + path + "?" + urlencode({"service": key, **params})
    for attempt in range(6):
        try:
            with urlopen(url, timeout=40) as response:
                result = ET.fromstring(response.read())
            if result.tag != "dbs":
                raise ValueError("Unexpected API response; publication cancelled")
            time.sleep(.15)
            return result
        except Exception as error:
            if attempt == 5:
                status = getattr(error, "code", None)
                category = f"HTTP {status}" if isinstance(status, int) else type(error).__name__
                raise RuntimeError(f"KOPIS request failed for {path} ({category}); previous publication retained") from None
            time.sleep(min(2 ** (attempt + 1), 30))
    raise AssertionError("unreachable")

def field(node: ET.Element, name: str, fallback: str = "") -> str:
    return (node.findtext(name) or fallback).strip()

def normalize(summary: ET.Element, detail: ET.Element) -> dict:
    def value(name):
        return field(detail, name, field(summary, name))
    def iso(name):
        return date.fromisoformat(value(name).replace(".", "-")).isoformat()
    poster = value("poster")
    if poster.startswith("http://www.kopis.or.kr/") or poster.startswith("http://kopis.or.kr/"):
        poster = poster.replace("http://", "https://", 1)
    links = []
    for relate in detail.findall("./relates/relate"):
        url = field(relate, "relateurl")
        if urlparse(url).scheme in ("http", "https") and urlparse(url).netloc:
            links.append({"name": field(relate, "relatenm", "공식 예매처"), "url": url})
    item = {
        "id": value("mt20id"), "title": value("prfnm"),
        "genre": GENRES.get(value("genrenm"), value("genrenm")),
        "sourceGenre": value("genrenm"), "genreCode": GENRE_CODES.get(value("genrenm"), ""),
        "region": REGIONS.get(value("area"), value("area")), "venue": value("fcltynm"),
        "startDate": iso("prfpdfrom"), "endDate": iso("prfpdto"), "posterUrl": poster,
        "artists": [s.strip() for s in re.split(r"[,，]", value("prfcast")) if s.strip()],
        "status": value("prfstate"), "ticketPrice": value("pcseguidance"),
        "bookingUrls": links, "lastUpdated": value("updatedate"), "runtime": value("prfruntime"),
        "age": value("prfage"), "schedule": value("dtguidance"),
        "description": value("sty"), "festival": value("festival") == "Y",
    }
    validate(item)
    return item

def validate(item: dict) -> None:
    if not re.fullmatch(r"PF\d+", item["id"]) or not item["title"]:
        raise ValueError("Missing or invalid performance identity")
    if date.fromisoformat(item["startDate"]) > date.fromisoformat(item["endDate"]):
        raise ValueError("Reversed performance dates")

def month_ranges(today: date, months: int):
    for offset in range(months):
        absolute = today.year * 12 + today.month - 1 + offset
        year, month0 = divmod(absolute, 12)
        month = month0 + 1
        yield date(year, month, 1), date(year, month, calendar.monthrange(year, month)[1])

def build_publication(items: list[dict], destination: Path, today: date, months: int, updated: str):
    if not items:
        raise ValueError("Empty collection; previous publication retained")
    for item in items:
        validate(item)
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "performances").mkdir(exist_ok=True)
    (destination / "details").mkdir(exist_ok=True)
    files = []
    for start, end in month_ranges(today, months):
        # Include performances spanning month boundaries in every overlapping month.
        relevant = [p for p in items if p["startDate"] <= str(end) and p["endDate"] >= str(start)]
        path = f"performances/{start:%Y-%m}.json"
        write_json(destination / path, relevant)
        files.append(path)
    for item in items:
        write_json(destination / "details" / (item["id"] + ".json"), item)
    hashes = {filename: hashlib.sha256((destination / filename).read_bytes()).hexdigest() for filename in files}
    write_json(destination / "manifest.json", {"version": updated, "updatedAt": updated, "files": files, "source": "KOPIS", "count": len(items), "fileHashes": hashes})
    (destination / "index.html").write_text('<!doctype html><meta charset="utf-8"><title>모아 공연 데이터</title><h1>모아 · 공연 데이터</h1><p>출처: (재)예술경영지원센터 공연예술통합전산망(www.kopis.or.kr)</p><a href="manifest.json">데이터 갱신 정보</a>', encoding="utf-8")
    (destination / ".nojekyll").touch()

def write_json(path: Path, value):
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")

def check_output(destination: Path) -> Path:
    """Never replace source folders, filesystem roots, or unrelated user files."""
    destination = destination.resolve()
    protected = {Path.cwd().resolve(), *Path(__file__).resolve().parents}
    if destination in protected:
        raise ValueError("Output must be a dedicated publication directory")
    if destination.exists():
        allowed = {"manifest.json", "performances", "details", "index.html", ".nojekyll"}
        if not destination.is_dir() or any(p.name not in allowed for p in destination.iterdir()):
            raise ValueError("Output contains unrelated files; no files modified")
    return destination

def replace_publication(staged: Path, destination: Path):
    """Swap a complete directory; restore the previous publication if the swap fails."""
    destination = check_output(destination)
    backup = destination.with_name(destination.name + ".previous-" + uuid.uuid4().hex)
    if backup.parent.resolve() != destination.parent.resolve():
        raise ValueError("Invalid backup location")
    had_previous = destination.exists()
    if had_previous:
        destination.rename(backup)
    try:
        staged.rename(destination)
    except BaseException:
        if had_previous:
            backup.rename(destination)
        raise
    if had_previous:
        # Backup is generated above, and constrained to the publication's parent.
        shutil.rmtree(backup)

def fetch_summaries(key: str, today: date, months: int, afterdate: str | None = None):
    summaries = {}
    for start, end in month_ranges(today, months):
        for state in ("01", "02"):
            page = 1
            while True:
                params = dict(stdate=f"{start:%Y%m%d}", eddate=f"{end:%Y%m%d}", cpage=page, rows=100, prfstate=state)
                if afterdate:
                    params["afterdate"] = afterdate
                rows = request("pblprfr", key, **params).findall("db")
                for row in rows:
                    identity = field(row, "mt20id")
                    if not re.fullmatch(r"PF\d+", identity):
                        raise ValueError("Invalid listing identity")
                    summaries[identity] = row
                if len(rows) < 100:
                    break
                page += 1
                if page > 1000:
                    raise ValueError("Unexpected pagination; collection cancelled")
    return summaries

def load_previous(destination: Path):
    try:
        manifest = json.loads((destination / "manifest.json").read_text(encoding="utf-8"))
        items = {}
        for filename in manifest["files"]:
            if not re.fullmatch(r"performances/\d{4}-\d{2}\.json", filename):
                raise ValueError("Invalid cached path")
            for item in json.loads((destination / filename).read_text(encoding="utf-8")):
                validate(item)
                items[item["id"]] = item
        datetime.fromisoformat(manifest["updatedAt"])
        return manifest, items
    except (OSError, ValueError, KeyError, TypeError):
        return None, {}

def summary_matches(item: dict, summary: ET.Element):
    expected = {
        "title": field(summary, "prfnm"), "venue": field(summary, "fcltynm"),
        "startDate": field(summary, "prfpdfrom").replace(".", "-"),
        "endDate": field(summary, "prfpdto").replace(".", "-"),
        "region": REGIONS.get(field(summary, "area"), field(summary, "area")),
        "genre": GENRES.get(field(summary, "genrenm"), field(summary, "genrenm")),
        "status": field(summary, "prfstate"),
    }
    return all(not value or item.get(name) == value for name, value in expected.items())

def read_checkpoint(directory: Path | None, identity: str, summary: ET.Element, after: datetime | None = None):
    if directory is None:
        return None
    try:
        record = json.loads((directory / f"{identity}.json").read_text(encoding="utf-8"))
        fetched = datetime.fromisoformat(record["fetchedAt"])
        if after is not None and fetched <= after:
            return None
        if datetime.now(ZoneInfo("Asia/Seoul")) - fetched > timedelta(hours=3):
            return None
        validate(record["item"])
        if record["item"]["id"] != identity or not summary_matches(record["item"], summary):
            return None
        return record["item"]
    except (OSError, ValueError, KeyError, TypeError):
        return None

def collect_items(key, summaries, previous, changed, full, checkpoint: Path | None = None, checkpoint_after: datetime | None = None):
    items = []
    refreshed = 0
    for identity, summary in summaries.items():
        cached = previous.get(identity)
        if not full and cached and identity not in changed and summary_matches(cached, summary):
            items.append(cached)
        else:
            resumed = read_checkpoint(checkpoint, identity, summary, checkpoint_after)
            if resumed:
                items.append(resumed)
                continue
            detail = request("pblprfr/" + identity, key).find("db")
            if detail is None:
                raise ValueError("Missing detail; collection cancelled")
            item = normalize(summary, detail)
            if item["id"] != identity:
                raise ValueError("Detail identity mismatch; collection cancelled")
            items.append(item)
            if checkpoint is not None:
                checkpoint.mkdir(parents=True, exist_ok=True)
                temporary = checkpoint / f"{identity}.tmp"
                write_json(temporary, {"fetchedAt": datetime.now(ZoneInfo("Asia/Seoul")).isoformat(), "item": item})
                temporary.replace(checkpoint / f"{identity}.json")
            refreshed += 1
            if refreshed % 100 == 0:
                print(f"Refreshed {refreshed} details", flush=True)
    return items, refreshed

def domestic_only(items):
    return [item for item in items if item["region"] != "해외"]

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, default=Path("public"))
    parser.add_argument("--months", type=int, default=6)
    parser.add_argument("--full", action="store_true", help="Force complete detail refresh")
    parser.add_argument("--checkpoint-dir", type=Path, default=Path(".kopis-cache"))
    args = parser.parse_args()
    if not 1 <= args.months <= 12:
        parser.error("months must be 1..12")
    args.output = check_output(args.output)
    key = os.environ.get("KOPIS_API_KEY", "")
    if not key:
        raise SystemExit("KOPIS_API_KEY is required; no files modified")
    today = datetime.now(ZoneInfo("Asia/Seoul")).date()
    previous_manifest, previous = load_previous(args.output)
    last_full = (previous_manifest or {}).get("lastFullSyncAt", "")
    full = args.full or not previous or not last_full or date.fromisoformat(last_full[:10]) <= today - timedelta(days=7)
    summaries = fetch_summaries(key, today, args.months)
    print(f"Found {len(summaries)} unique performances")
    changed = set()
    if not full:
        since = datetime.fromisoformat(previous_manifest["updatedAt"]).date() - timedelta(days=1)
        changed = set(fetch_summaries(key, today, args.months, f"{since:%Y%m%d}"))
    checkpoint_after = datetime.fromisoformat(previous_manifest["updatedAt"]) if previous_manifest else None
    items, refreshed = collect_items(key, summaries, previous, changed, full, args.checkpoint_dir, checkpoint_after)
    print(f"Mode: {'full' if full else 'incremental'}; refreshed {refreshed}, reused {len(items)-refreshed}")
    items = domestic_only(items)
    updated = datetime.now(ZoneInfo("Asia/Seoul")).isoformat(timespec="seconds")
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=args.output.parent) as staging:
        staged = Path(staging) / "public"
        build_publication(items, staged, today, args.months, updated)
        manifest = json.loads((staged / "manifest.json").read_text(encoding="utf-8"))
        manifest["lastFullSyncAt"] = updated if full else last_full
        manifest["refreshedCount"] = refreshed
        write_json(staged / "manifest.json", manifest)
        replace_publication(staged, args.output)
    print(f"Validated {len(items)} performances; generated at {updated}")

if __name__ == "__main__":
    main()
