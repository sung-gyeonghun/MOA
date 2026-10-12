import importlib.util
import json
from pathlib import Path
import tempfile
import unittest
from unittest.mock import patch
from datetime import date
import xml.etree.ElementTree as ET

spec = importlib.util.spec_from_file_location("collect", Path(__file__).parents[1] / "collect.py")
collect = importlib.util.module_from_spec(spec)
spec.loader.exec_module(collect)

class CollectorTests(unittest.TestCase):
    def fixture(self):
        return ET.fromstring('''<db><mt20id>PF123456</mt20id><prfnm>테스트 공연</prfnm><prfpdfrom>2026.10.01</prfpdfrom><prfpdto>2026.11.10</prfpdto><area>대구광역시</area><genrenm>대중음악</genrenm><prfcast>가수 A, 가수 B</prfcast><festival>Y</festival><relates><relate><relatenm>예매처</relatenm><relateurl>https://example.com/ticket</relateurl></relate><relate><relateurl>javascript:alert(1)</relateurl></relate></relates></db>''')
    def test_normalization_and_safe_booking_links(self):
        item = collect.normalize(self.fixture(), self.fixture())
        self.assertEqual((item['region'], item['genre']), ('대구', '콘서트'))
        self.assertEqual(item['artists'], ['가수 A', '가수 B'])
        self.assertEqual(len(item['bookingUrls']), 1)
        self.assertTrue(item['festival'])
        self.assertEqual(item['genreCode'], 'CCCD')
        self.assertEqual(item['sourceGenre'], '대중음악')
    def test_cross_month_performance_is_in_both_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            collect.build_publication([collect.normalize(self.fixture(), self.fixture())], root, date(2026,10,13), 2, '2026-10-13')
            for month in ('2026-10', '2026-11'):
                self.assertEqual(len(json.loads((root / f'performances/{month}.json').read_text(encoding='utf-8'))), 1)
    def test_empty_collection_preserves_existing_files(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'manifest.json').write_text('old')
            with self.assertRaises(ValueError):
                collect.build_publication([], root, date(2026,10,13), 2, 'now')
            self.assertEqual((root / 'manifest.json').read_text(), 'old')
    def test_reversed_dates_rejected(self):
        item = collect.normalize(self.fixture(), self.fixture())
        item['endDate'] = '2020-01-01'
        with self.assertRaises(ValueError): collect.validate(item)

    def test_failed_swap_restores_previous_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            destination = root / 'public'
            destination.mkdir()
            (destination / 'manifest.json').write_text('old')
            staged = root / 'staged'
            staged.mkdir()
            (staged / 'manifest.json').write_text('new')
            original = Path.rename
            def fail_swap(path, target):
                if path == staged: raise OSError('simulated disk failure')
                return original(path, target)
            with patch.object(Path, 'rename', fail_swap):
                with self.assertRaises(OSError): collect.replace_publication(staged, destination)
            self.assertEqual((destination / 'manifest.json').read_text(), 'old')
            self.assertEqual(list(root.glob('public.previous-*')), [])

    def test_swap_uses_complete_validated_publication(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            destination = root / 'public'; destination.mkdir()
            (destination / 'manifest.json').write_text('old')
            staged = root / 'staged'
            collect.build_publication([collect.normalize(self.fixture(), self.fixture())], staged, date(2026,10,13), 2, '2026-10-13T00:00:00+09:00')
            collect.replace_publication(staged, destination)
            self.assertEqual(json.loads((destination / 'manifest.json').read_text())['count'], 1)
            self.assertTrue((destination / 'details/PF123456.json').exists())
            self.assertFalse(staged.exists())
            self.assertEqual(list(root.glob('public.previous-*')), [])

    def test_output_cannot_replace_sources_or_unrelated_files(self):
        with self.assertRaises(ValueError): collect.check_output(Path.cwd())
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / 'personal-note.txt').write_text('keep')
            with self.assertRaises(ValueError): collect.check_output(root)
            self.assertEqual((root / 'personal-note.txt').read_text(), 'keep')

    def test_api_failure_does_not_print_service_key(self):
        service_key = 'SECRET_NOT_FOR_LOGS'
        with patch.object(collect, 'urlopen', side_effect=OSError(service_key)), patch.object(collect.time, 'sleep'):
            with self.assertRaises(RuntimeError) as failure:
                collect.request('pblprfr', service_key)
        self.assertNotIn(service_key, str(failure.exception))

    def test_incremental_reuses_unchanged_and_drops_removed_ids(self):
        item = collect.normalize(self.fixture(), self.fixture())
        previous = {item['id']: item, 'PF999999': {**item, 'id': 'PF999999'}}
        with patch.object(collect, 'request') as request:
            items, refreshed = collect.collect_items('fake', {item['id']: self.fixture()}, previous, set(), False)
        self.assertEqual(items, [item])
        self.assertEqual(refreshed, 0)
        request.assert_not_called()

    def test_incremental_refetches_modified_and_new_details(self):
        item = collect.normalize(self.fixture(), self.fixture())
        detail = self.fixture()
        detail.find('prfcast').text = '새 출연자'
        root = ET.Element('dbs'); root.append(detail)
        with patch.object(collect, 'request', return_value=root) as request:
            items, count = collect.collect_items('fake', {item['id']: self.fixture()}, {item['id']: item}, {item['id']}, False)
        self.assertEqual(items[0]['artists'], ['새 출연자'])
        self.assertEqual(count, 1)
        self.assertEqual(request.call_count, 1)
        with patch.object(collect, 'request', return_value=root) as request:
            _, count = collect.collect_items('fake', {item['id']: self.fixture()}, {}, set(), False)
        self.assertEqual(count, 1)

    def test_full_sync_refetches_even_unchanged_items(self):
        item = collect.normalize(self.fixture(), self.fixture())
        root = ET.Element('dbs'); root.append(self.fixture())
        with patch.object(collect, 'request', return_value=root) as request:
            _, count = collect.collect_items('fake', {item['id']: self.fixture()}, {item['id']: item}, set(), True)
        self.assertEqual(count, 1)
        self.assertEqual(request.call_count, 1)

    def test_afterdate_passed_and_duplicate_listings_deduplicated(self):
        root = ET.Element('dbs'); root.append(self.fixture())
        with patch.object(collect, 'request', return_value=root) as request:
            summaries = collect.fetch_summaries('fake', date(2026,10,13), 2, '20261012')
        self.assertEqual(list(summaries), ['PF123456'])
        self.assertEqual(request.call_count, 4)
        self.assertTrue(all(call.kwargs['afterdate'] == '20261012' for call in request.call_args_list))

    def test_checkpoint_resumes_validated_details_after_interrupted_run(self):
        item = collect.normalize(self.fixture(), self.fixture())
        root = ET.Element('dbs'); root.append(self.fixture())
        with tempfile.TemporaryDirectory() as tmp:
            checkpoint = Path(tmp)
            with patch.object(collect, 'request', return_value=root):
                collect.collect_items('fake', {item['id']: self.fixture()}, {}, set(), True, checkpoint)
            self.assertTrue((checkpoint / f"{item['id']}.json").exists())
            with patch.object(collect, 'request') as request:
                items, count = collect.collect_items('fake', {item['id']: self.fixture()}, {}, set(), True, checkpoint)
            request.assert_not_called()
            self.assertEqual(items, [item])
            self.assertEqual(count, 0)

    def test_changed_item_cannot_reuse_checkpoint_from_previous_publication(self):
        item = collect.normalize(self.fixture(), self.fixture())
        root = ET.Element('dbs'); root.append(self.fixture())
        with tempfile.TemporaryDirectory() as tmp:
            checkpoint = Path(tmp)
            with patch.object(collect, 'request', return_value=root):
                collect.collect_items('fake', {item['id']: self.fixture()}, {}, set(), True, checkpoint)
            published = collect.datetime.now(collect.ZoneInfo('Asia/Seoul'))
            root.find('db/prfcast').text = '변경된 출연자'
            with patch.object(collect, 'request', return_value=root) as request:
                items, count = collect.collect_items('fake', {item['id']: self.fixture()}, {item['id']: item}, {item['id']}, False, checkpoint, published)
            self.assertEqual(request.call_count, 1)
            self.assertEqual(count, 1)
            self.assertEqual(items[0]['artists'], ['변경된 출연자'])

    def test_current_kopis_merged_region_and_domestic_scope(self):
        fixture = self.fixture()
        fixture.find('area').text = '전남광주통합특별시'
        item = collect.normalize(fixture, fixture)
        self.assertEqual(item['region'], '전남광주')
        overseas = {**item, 'id': 'PF999999', 'region': '해외'}
        self.assertEqual(collect.domestic_only([item, overseas]), [item])

if __name__ == '__main__': unittest.main()
