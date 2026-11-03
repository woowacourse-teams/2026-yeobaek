const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { createCategoryEditor, createHashtagsEditor } =
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
    const [summary, input, help, save] = editor.children;
    return { editor, summary, input, help, save };
}

test('해시태그는 문구·순서·중복·#를 보정하지 않고 전체 목록을 저장한다', async () => {
    const values = [];
    const h = editorHarness(createHashtagsEditor, {
        title: '책', status: 'ACTIVE', hashtags: ['첫사랑', '</textarea><script>']
    }, async value => values.push(value));
    assert.equal(h.input.value, '첫사랑\n</textarea><script>');
    h.input.value = '#재회\r\n 첫사랑 \r\n#재회';
    await h.save.listeners.click();
    h.input.value = '';
    await h.save.listeners.click();
    assert.deepEqual(values, [['#재회', ' 첫사랑 ', '#재회'], []]);
});

test('장르는 자유 입력하며 빈 값 저장도 같은 저장 동작을 사용한다', async () => {
    const values = [];
    const h = editorHarness(createCategoryEditor, { title: '책', category: '비극', status: 'ACTIVE' },
        async value => values.push(value));
    assert.equal(h.input.value, '비극');
    h.input.value = '로맨스';
    await h.save.listeners.click();
    h.input.value = '';
    await h.save.listeners.click();
    assert.deepEqual(values, ['로맨스', '']);
    assert.equal(h.editor.children.filter(child => child.tag === 'button').length, 1);
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
    for (const create of [createCategoryEditor, createHashtagsEditor]) {
        let calls = 0;
        const h = editorHarness(create, { title: '책', status: 'DELETED' }, async () => { calls++; });
        assert.equal(h.input.disabled, true);
        assert.equal(h.save.disabled, true);
        await h.save.listeners.click();
        assert.equal(calls, 0);
    }
});

function requestHarness(response) {
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
        loadBooks: async () => { loads++; return true; },
        show: (element, message, error) => messages.push({ message, error })
    };
    vm.createContext(sandbox);
    vm.runInContext(source, sandbox);
    return { sandbox, requests, messages, loads: () => loads, invalidations: () => invalidations };
}

test('메타데이터 비우기는 DELETE 없이 독립 PUT 요청을 보내고 저장 후 목록을 다시 조회한다', async () => {
    const h = requestHarness({ ok: true });
    const book = { bookId: 42, title: '책' };
    await h.sandbox.saveBookMetadata(book, 'category', '', '장르');
    await h.sandbox.saveBookMetadata(book, 'hashtags', [], '해시태그');
    assert.deepEqual(h.requests.map(request => ({
        url: request.url, method: request.options.method, body: JSON.parse(request.options.body)
    })), [
        { url: '/api/admin/books/42/category', method: 'PUT', body: { category: '' } },
        { url: '/api/admin/books/42/hashtags', method: 'PUT', body: { hashtags: [] } }
    ]);
    assert.equal(h.requests[0].options.headers['X-Admin-Token'], 'test-token');
    assert.equal(h.loads(), 2);
    assert.equal(h.invalidations(), 2);
});

test('서버의 제한 초과 응답을 표시하고 실패한 저장을 성공으로 안내하지 않는다', async () => {
    const h = requestHarness({ ok: false, json: async () => ({ code: 'INVALID_REQUEST', message: '최대 5개' }) });
    const result = await h.sandbox.saveBookMetadata({ bookId: 42 }, 'hashtags', ['a'], '해시태그');
    assert.equal(result, false);
    assert.equal(h.loads(), 0);
    assert.deepEqual(h.messages, [{ message: 'INVALID_REQUEST: 최대 5개', error: true }]);
});
