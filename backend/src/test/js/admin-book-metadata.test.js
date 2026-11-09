const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { createDescriptionEditor, createGenresEditor, createHashtagsEditor } =
    require('../../main/resources/static/admin-book-inventory.js');

function editorHarness(create, book, onSave) {
    const document = {
        createElement(tag) {
            return {
                tag, children: [], listeners: {}, attributes: {},
                append(...children) { this.children.push(...children); },
                setAttribute(name, value) { this.attributes[name] = value; },
                addEventListener(name, listener) { this.listeners[name] = listener; },
                set innerHTML(value) { throw new Error('메타데이터는 HTML로 렌더링하지 않는다'); }
            };
        }
    };
    const editor = create(document, book, onSave);
    const [summary, input, help] = editor.children;
    const error = editor.children.find(child => child.tag === 'div');
    const save = editor.children.find(child => child.tag === 'button');
    return { editor, summary, input, help, error, save };
}

test('해시태그는 문구·순서·중복·#를 보정하지 않고 전체 목록을 저장한다', async () => {
    const values = [];
    const h = editorHarness(createHashtagsEditor, {
        title: '책', status: 'ACTIVE', hashtags: ['첫사랑', '</textarea><script>']
    }, async value => values.push(value));
    assert.equal(h.input.value, JSON.stringify(['첫사랑', '</textarea><script>'], null, 2));
    h.input.value = JSON.stringify(['#재회', ' 첫사랑 ', '#재회']);
    await h.save.listeners.click();
    h.input.value = '';
    await h.save.listeners.click();
    assert.deepEqual(values, [['#재회', ' 첫사랑 ', '#재회'], []]);
});

test('장르는 자유 입력하며 빈 값 저장도 같은 저장 동작을 사용한다', async () => {
    const values = [];
    const h = editorHarness(createGenresEditor, { title: '책', genres: ['비극', '사실주의'], status: 'ACTIVE' },
        async value => values.push(value));
    assert.equal(h.input.value, JSON.stringify(['비극', '사실주의'], null, 2));
    h.input.value = '["로맨스", "비극"]';
    await h.save.listeners.click();
    h.input.value = '';
    await h.save.listeners.click();
    assert.deepEqual(values, [['로맨스', '비극'], []]);
});

test('장르와 해시태그는 원소 안의 개행·공백·중복·이스케이프 문자를 수정 없이 보존한다', async () => {
    for (const [create, field] of [[createGenresEditor, 'genres'], [createHashtagsEditor, 'hashtags']]) {
        const original = ['로맨스\n비극', '첫사랑\r\n재회', ' ', ' ', '"\\#'];
        const saved = [];
        const h = editorHarness(create, { title: '책', status: 'ACTIVE', [field]: original },
            async value => saved.push(value));
        await h.save.listeners.click();
        assert.deepEqual(saved, [original]);
    }
});

test('빈 문자열 원소와 빈 목록을 구분해 저장한다', async () => {
    for (const [create, field] of [[createGenresEditor, 'genres'], [createHashtagsEditor, 'hashtags']]) {
        const saved = [];
        const h = editorHarness(create, { title: '책', status: 'ACTIVE', [field]: [''] },
            async value => saved.push(value));
        await h.save.listeners.click();
        h.input.value = '[]';
        await h.save.listeners.click();
        h.input.value = '';
        await h.save.listeners.click();
        assert.deepEqual(saved, [[''], [], []]);
    }
});

test('잘못된 JSON이나 문자열 배열이 아닌 입력은 오류를 표시하고 수정 후 저장할 수 있다', async () => {
    for (const create of [createGenresEditor, createHashtagsEditor]) {
        const saved = [];
        const h = editorHarness(create, { title: '책', status: 'ACTIVE' }, async value => saved.push(value));
        for (const invalid of ['["비극"', 'null', '{}', '["비극", 1]', '[null]']) {
            h.input.value = invalid;
            await h.save.listeners.click();
            assert.deepEqual(saved, []);
            assert.ok(h.error.textContent);
            assert.equal(h.input.disabled, false);
            assert.equal(h.save.disabled, false);
        }
        h.input.value = '["비극"]';
        await h.save.listeners.click();
        assert.deepEqual(saved, [['비극']]);
        assert.equal(h.error.textContent, '');
    }
});

test('중복 저장 요청을 막고 요청 실패 후 재시도할 수 있다', async () => {
    let reject;
    let calls = 0;
    const h = editorHarness(createHashtagsEditor, { title: '책', status: 'ACTIVE' }, () => {
        calls++;
        return new Promise((resolve, rejectRequest) => { reject = rejectRequest; });
    });
    const pending = h.save.listeners.click();
    await h.save.listeners.click();
    assert.equal(calls, 1);
    assert.equal(h.input.disabled, true);
    reject(new Error('저장 실패'));
    await assert.rejects(pending, /저장 실패/);
    assert.equal(h.input.disabled, false);
    assert.equal(h.save.disabled, false);
});

test('삭제 도서는 두 메타데이터 편집기에서 수정할 수 없다', async () => {
    for (const create of [createGenresEditor, createHashtagsEditor]) {
        let calls = 0;
        const h = editorHarness(create, { title: '책', status: 'DELETED' }, async () => { calls++; });
        assert.equal(h.input.disabled, true);
        assert.equal(h.save.disabled, true);
        await h.save.listeners.click();
        assert.equal(calls, 0);
    }
});

function requestHarness(response, onLoad = () => {}) {
    const page = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/admin.html'), 'utf8');
    const source = page.slice(page.indexOf('    async function saveBookMetadata('),
        page.indexOf('    function escapeHtml('));
    const requests = [];
    let loads = 0;
    let invalidations = 0;
    const messages = [];
    const sandbox = {
        document: { getElementById: () => ({}) },
        inventoryState: { invalidate: () => { invalidations++; } },
        downloadBooksCsvButton: { disabled: false },
        adminToken: () => 'test-token',
        fetch: async (url, options) => { requests.push({ url, options }); return response; },
        loadBooks: async () => { loads++; onLoad(); return true; },
        show: (element, message, error) => messages.push({ message, error })
    };
    vm.createContext(sandbox);
    vm.runInContext(source, sandbox);
    return { sandbox, requests, messages, loads: () => loads, invalidations: () => invalidations };
}

test('메타데이터 비우기는 독립 PUT 요청을 보내고 저장한 목록만 갱신한다', async () => {
    const h = requestHarness({ ok: true });
    const book = { bookId: 42, title: '책' };
    await h.sandbox.saveBookMetadata(book, 'genres', [], '장르');
    await h.sandbox.saveBookMetadata(book, 'hashtags', [], '해시태그');
    assert.deepEqual(h.requests.map(request => ({
        url: request.url, method: request.options.method, body: JSON.parse(request.options.body)
    })), [
        { url: '/api/admin/books/42/genres', method: 'PUT', body: { genres: [] } },
        { url: '/api/admin/books/42/hashtags', method: 'PUT', body: { hashtags: [] } }
    ]);
    assert.equal(h.requests[0].options.headers['X-Admin-Token'], 'test-token');
    assert.deepEqual(book.genres, []);
    assert.deepEqual(book.hashtags, []);
    assert.equal(h.loads(), 0);
    assert.equal(h.invalidations(), 0);
    assert.equal(h.sandbox.downloadBooksCsvButton.disabled, false);
});

test('장르·해시태그를 저장해도 다른 필드와 다른 책의 미저장 초안을 보존한다', async () => {
    const book = { bookId: 42, title: '책', status: 'ACTIVE', genres: ['비극'], hashtags: ['첫사랑'], description: '소개' };
    const otherBook = { bookId: 43, title: '다른 책', status: 'ACTIVE', genres: ['풍자'] };
    let editors;
    const h = requestHarness({ ok: true }, renderEditors);
    function renderEditors() {
        editors = {
            genres: editorHarness(createGenresEditor, book,
                value => h.sandbox.saveBookMetadata(book, 'genres', value, '장르')),
            hashtags: editorHarness(createHashtagsEditor, book,
                value => h.sandbox.saveBookMetadata(book, 'hashtags', value, '해시태그')),
            description: editorHarness(createDescriptionEditor, book, async () => {}),
            otherGenres: editorHarness(createGenresEditor, otherBook, async () => {})
        };
    }
    renderEditors();
    editors.genres.input.value = '["로맨스"]';
    editors.hashtags.input.value = '["재회"]';
    editors.description.input.value = '아직 저장하지 않은 소개';
    editors.otherGenres.input.value = '["성장"]';

    await editors.genres.save.listeners.click();
    assert.equal(editors.hashtags.input.value, '["재회"]');
    assert.deepEqual(book.genres, ['로맨스']);
    editors.genres.input.value = '["다음 장르 초안"]';
    await editors.hashtags.save.listeners.click();
    assert.deepEqual(book.hashtags, ['재회']);
    assert.equal(editors.genres.input.value, '["다음 장르 초안"]');
    assert.equal(editors.description.input.value, '아직 저장하지 않은 소개');
    assert.equal(editors.otherGenres.input.value, '["성장"]');
    assert.deepEqual(book.genres, ['로맨스']);
    assert.equal(book.description, '소개');
    assert.deepEqual(otherBook.genres, ['풍자']);
});

test('서버의 제한 초과 응답을 표시하고 실패한 저장을 성공으로 안내하지 않는다', async () => {
    const h = requestHarness({ ok: false, json: async () => ({ code: 'INVALID_REQUEST', message: '최대 5개' }) });
    const book = { bookId: 42, hashtags: ['기존 태그'] };
    const result = await h.sandbox.saveBookMetadata(book, 'hashtags', ['a'], '해시태그');
    assert.equal(result, false);
    assert.deepEqual(book.hashtags, ['기존 태그']);
    assert.equal(h.loads(), 0);
    assert.deepEqual(h.messages, [{ message: 'INVALID_REQUEST: 최대 5개', error: true }]);
});
