const test = require('node:test');
const assert = require('node:assert/strict');
const { booksToCsv, createState } = require('../../main/resources/static/admin-book-inventory.js');

test('CSV는 공동 작가를 한 행에 보존하고 nullable 값과 줄바꿈을 올바르게 표현한다', () => {
    const csv = booksToCsv([
        {
            bookId: 1,
            title: '쉼표, 따옴표 "와\n줄바꿈',
            authors: [
                { authorId: 7, name: '첫 작가' },
                { authorId: 8, name: '둘째 작가' }
            ],
            publisher: null,
            publishedYear: null,
            passageCount: 42,
            status: 'ACTIVE'
        }
    ]);

    assert.equal(csv, '\uFEFFbookId,title,authorNames,authorIds,publisher,publishedYear,passageCount,status\r\n'
        + '1,"쉼표, 따옴표 ""와\n줄바꿈","첫 작가 | 둘째 작가","7 | 8","",,42,"ACTIVE"');
});

test('CSV는 텍스트 수식 접두사를 무력화하되 음수 출판 연도는 숫자로 유지한다', () => {
    const csv = booksToCsv([
        {
            bookId: 2,
            title: '=DANGEROUS()',
            authors: [{ authorId: 9, name: '  +DANGEROUS()' }],
            publisher: '@DANGEROUS()',
            publishedYear: -300,
            passageCount: 1,
            status: 'DELETED'
        }
    ]);

    assert.match(csv, /"'=DANGEROUS\(\)"/);
    assert.match(csv, /"'  \+DANGEROUS\(\)"/);
    assert.match(csv, /"'@DANGEROUS\(\)"/);
    assert.match(csv, /,-300,1,"DELETED"$/);
});

test('빈 조회 결과는 헤더만 포함한 CSV로 내보낸다', () => {
    assert.equal(
        booksToCsv([]),
        '\uFEFFbookId,title,authorNames,authorIds,publisher,publishedYear,passageCount,status');
});

test('가장 최근 성공 조회만 내보낼 수 있고 실패나 무효화 뒤에는 이전 결과를 내보내지 않는다', () => {
    const state = createState();
    const firstLoad = state.beginLoad();
    const secondLoad = state.beginLoad();

    assert.equal(state.completeLoad(firstLoad, [{ bookId: 1 }]), false);
    assert.equal(state.snapshotForExport(true), null);
    assert.equal(state.completeLoad(secondLoad, [{ bookId: 2 }]), true);
    assert.deepEqual(state.snapshotForExport(true), [{ bookId: 2 }]);
    assert.equal(state.snapshotForExport(false), null);

    const failedLoad = state.beginLoad();
    assert.equal(state.snapshotForExport(true), null);
    assert.equal(state.failLoad(failedLoad), true);
    assert.equal(state.snapshotForExport(true), null);

    const successfulLoad = state.beginLoad();
    state.completeLoad(successfulLoad, []);
    assert.deepEqual(state.snapshotForExport(true), []);
    const invalidatedLoad = state.beginLoad();
    assert.equal(state.isLoading(), true);
    state.invalidate();
    assert.equal(state.isLoading(), false);
    assert.equal(state.completeLoad(invalidatedLoad, [{ bookId: 3 }]), false);
    assert.equal(state.snapshotForExport(true), null);
});
