"""Supplemental SQLite tests of the actual DAO SQL and migration statements.
These do not execute Room-generated code, Android UI, Hilt, or Room Flow invalidation.
Run: python3 verification/test_sql_contract.py
"""
from pathlib import Path
import re
import sqlite3
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
LOCAL = ROOT / 'Enhanced/app/src/main/java/com/MatthewBaerg/multitasked/data/local'
SCHEMA = (ROOT / 'Enhanced/app/src/androidTest/assets/schema-v1.sql').read_text()
MIGRATIONS = re.findall(r'db.execSQL\("([^"\n]+)"\)', (LOCAL / 'AppDatabase.kt').read_text())

def queries(file):
    return dict((name, sql) for sql, name in re.findall(
        r'@Query\("([^"\n]+)"\)\s*(?:abstract\s+)?(?:suspend\s+)?fun\s+(\w+)',
        (LOCAL / file).read_text()))

B = queries('BoardDao.kt')
T = queries('TaskDao.kt')

def connect(name=':memory:'):
    db = sqlite3.connect(name)
    db.execute('PRAGMA foreign_keys = ON')
    return db

def initialize(db):
    db.executescript(SCHEMA)
    for sql in MIGRATIONS:
        db.execute(sql)
    db.commit()

class DatabaseContractTest(unittest.TestCase):
    def setUp(self):
        self.db = connect()
        initialize(self.db)
    def tearDown(self):
        self.db.close()
    def board(self, id='b'):
        self.db.execute('INSERT INTO local_boards VALUES (?, ?, ?)', (id, 'Board', 1))
    def task(self, id='t', board='b', title='Task'):
        self.db.execute('INSERT INTO local_tasks(id, boardId, title, completed, createdAt) VALUES (?, ?, ?, 0, 2)', (id, board, title))
    def test_crud_uses_production_queries(self):
        self.board(); self.task()
        self.assertEqual(1, self.db.execute(B['rename'], {'id':'b', 'name':'Renamed'}).rowcount)
        self.assertEqual('Renamed', self.db.execute(B['observeBoard'], {'id':'b'}).fetchone()[1])
        values = dict(boardId='b', id='t', title='Edited', notes='Note', dueDate=123, priority='HIGH')
        self.assertEqual(1, self.db.execute(T['updateDetails'], values).rowcount)
        self.db.execute(T['setCompleted'], dict(boardId='b', id='t', completed=1))
        row = self.db.execute(T['get'], dict(boardId='b', id='t')).fetchone()
        self.assertEqual(('Edited', 1), row[2:4])
        self.assertEqual(('Note', 123, 'HIGH'), row[5:])
        self.assertEqual(1, self.db.execute(T['delete'], dict(boardId='b', id='t')).rowcount)
        self.assertIsNone(self.db.execute(T['get'], dict(boardId='b', id='t')).fetchone())
    def test_board_scope_for_all_task_mutations(self):
        self.board(); self.board('other'); self.task()
        params = dict(boardId='other', id='t', title='Wrong', notes='', dueDate=None, priority='NONE', completed=1)
        for method in ['updateDetails', 'setCompleted', 'delete']:
            self.assertEqual(0, self.db.execute(T[method], params).rowcount)
        self.assertIsNone(self.db.execute(T['get'], params).fetchone())
        self.assertEqual('Task', self.db.execute(T['get'], dict(boardId='b', id='t')).fetchone()[2])
    def test_foreign_key_rejects_orphans(self):
        with self.assertRaises(sqlite3.IntegrityError): self.task()
    def test_cascade_is_limited_to_one_board(self):
        self.board(); self.board('other'); self.task(); self.task('second','other')
        self.db.execute(B['delete'], {'id':'b'})
        self.assertEqual([], self.db.execute(T['observeTasks'], {'boardId':'b'}).fetchall())
        self.assertEqual(1, len(self.db.execute(T['observeTasks'], {'boardId':'other'}).fetchall()))
    def test_duplicate_insert_aborts_without_replacement(self):
        self.board(); self.task()
        with self.assertRaises(sqlite3.IntegrityError): self.board()
        self.assertEqual(1, self.db.execute('SELECT COUNT(*) FROM local_tasks').fetchone()[0])
    def test_transaction_rolls_back_parent_and_children(self):
        with self.assertRaises(sqlite3.IntegrityError):
            with self.db:
                self.board(); self.task(); self.task()
        self.assertEqual(0, self.db.execute('SELECT COUNT(*) FROM local_boards').fetchone()[0])
        self.assertEqual(0, self.db.execute('SELECT COUNT(*) FROM local_tasks').fetchone()[0])
    def test_join_counts_zero_and_multiple_tasks(self):
        self.board(); self.board('empty'); self.task(); self.task('second')
        rows = {row[0]: row[2] for row in self.db.execute(B['observeBoards'])}
        self.assertEqual({'b':2,'empty':0}, rows)
    def test_bind_parameters_keep_sql_like_text_literal(self):
        self.board(); self.task()
        title = "Robert'); DROP TABLE local_tasks;--"
        self.db.execute(T['updateDetails'], dict(boardId='b',id='t',title=title,notes='',dueDate=None,priority='NONE'))
        self.assertEqual(title, self.db.execute(T['get'], dict(boardId='b',id='t')).fetchone()[2])
    def test_task_query_uses_board_index(self):
        plan = self.db.execute('EXPLAIN QUERY PLAN ' + T['observeTasks'], {'boardId':'b'}).fetchall()
        self.assertTrue(any('index_local_tasks_boardId_createdAt' in row[3] for row in plan))
    def test_migration_preserves_rows_and_adds_defaults(self):
        with connect() as legacy:
            legacy.executescript(SCHEMA)
            legacy.execute("INSERT INTO local_boards VALUES ('b','Board',1)")
            legacy.execute("INSERT INTO local_tasks VALUES ('t','b','Keep',1,2)")
            for sql in MIGRATIONS: legacy.execute(sql)
            self.assertEqual(('t','b','Keep',1,2,'',None,'NONE'), legacy.execute('SELECT * FROM local_tasks').fetchone())
            self.assertEqual([], legacy.execute('PRAGMA foreign_key_check').fetchall())
    def test_file_persistence(self):
        with tempfile.TemporaryDirectory() as directory:
            name = str(Path(directory) / 'test.db')
            db = connect(name); initialize(db)
            db.execute("INSERT INTO local_boards VALUES ('b','Persist',1)")
            db.commit(); db.close()
            reopened = connect(name)
            try: self.assertEqual('Persist', reopened.execute(B['observeBoard'], {'id':'b'}).fetchone()[1])
            finally: reopened.close()

if __name__ == '__main__':
    unittest.main(verbosity=2)
