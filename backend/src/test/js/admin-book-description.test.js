const test = require('node:test');
const assert = require('node:assert/strict');
const { withUploadDescription, createDescriptionEditor } =
    require('../../main/resources/static/admin-book-inventory.js');

test('선택 입력이 비어 있으면 구형 JSON과 기존 소개를 보존한다', () => {
    const legacy = { title: '책', chapters: [] };
    assert.equal(withUploadDescription(legacy, ' \n'), legacy);
    assert.equal('description' in legacy, false);
    const existing = { ...legacy, description: '기존 소개' };
    assert.equal(withUploadDescription(existing, ''), existing);
    const result = withUploadDescription(existing, ' 검수한 소개\n');
    assert.equal(result.description, '검수한 소개');
    assert.equal(existing.description, '기존 소개');
    for (const invalid of [null, [], '책']) {
        assert.throws(() => withUploadDescription(invalid, '소개'), /객체/);
    }
});

function editorHarness(book, onSave) {
    const document = {
        createElement(tag) {
            return {
                tag, children: [], listeners: {}, attributes: {},
                append(...children) { this.children.push(...children); },
                setAttribute(name, value) { this.attributes[name] = value; },
                addEventListener(name, listener) { this.listeners[name] = listener; },
                set innerHTML(value) { throw new Error('소개에 HTML 렌더링 금지'); }
            };
        }
    };
    const editor = createDescriptionEditor(document, book, onSave);
    const [summary, textarea, save, clear] = editor.children;
    return { editor, summary, textarea, save, clear };
}

test('소개는 HTML로 해석하지 않고 textarea 값으로 보여주며 저장·비우기를 구분한다', async () => {
    const values = [];
    const dangerous = '</textarea><script>alert(1)</script>';
    const h = editorHarness({ title: '책', description: dangerous, status: 'ACTIVE' },
        async value => values.push(value));
    assert.equal(h.textarea.value, dangerous);
    h.textarea.value = '새 소개';
    await h.save.listeners.click();
    await h.clear.listeners.click();
    assert.deepEqual(values, ['새 소개', null]);
    assert.equal(h.save.disabled, false);
});

test('저장 중 중복 요청을 막고 실패해도 다시 편집할 수 있다', async () => {
    let release;
    let calls = 0;
    const h = editorHarness({ title: '책', status: 'ACTIVE' }, () => {
        calls++;
        return new Promise(resolve => { release = resolve; });
    });
    const pending = h.save.listeners.click();
    assert.equal(h.save.disabled, true);
    await h.clear.listeners.click();
    assert.equal(calls, 1);
    release(false);
    await pending;
    assert.equal(h.save.disabled, false);
    assert.equal(h.textarea.disabled, false);
});

test('삭제된 책의 소개는 조회만 허용한다', async () => {
    let calls = 0;
    const h = editorHarness({ title: '책', description: '소개', status: 'DELETED' },
        async () => { calls++; });
    assert.equal(h.textarea.disabled, true);
    assert.equal(h.save.disabled, true);
    assert.equal(h.clear.disabled, true);
    await h.save.listeners.click();
    assert.equal(calls, 0);
});
