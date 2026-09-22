const test = require('node:test');
const assert = require('node:assert/strict');
const { initDashboard } = require('../../main/resources/static/admin-dashboard.js');

class Element {
    constructor(tagName = 'div') {
        this.tagName = tagName;
        this.children = [];
        this.listeners = {};
        this.attributes = {};
        this.style = {};
        this.value = '';
        this.hidden = true;
        this.disabled = false;
        this.ownText = '';
    }
    set textContent(value) { this.ownText = String(value); this.children = []; }
    get textContent() { return this.ownText + this.children.map(child => child.textContent).join(''); }
    appendChild(child) { this.children.push(child); return child; }
    replaceChildren() { this.children = []; this.ownText = ''; }
    setAttribute(key, value) { this.attributes[key] = value; }
    addEventListener(name, listener) { this.listeners[name] = listener; }
}

function setup(fetcher) {
    const elements = new Map();
    const document = {
        getElementById(id) {
            if (!elements.has(id)) elements.set(id, new Element());
            return elements.get(id);
        },
        createElement(tag) { return new Element(tag); }
    };
    const get = id => document.getElementById(id);
    get('admin-token').value = 'test-admin';
    return { dashboard: initDashboard(document, fetcher), get };
}

function response(data, status = 200) {
    return { ok: status === 200, status, json: async () => data };
}

function deferred() {
    let resolve;
    const promise = new Promise(done => { resolve = done; });
    return { promise, resolve };
}

const emptyMembers = { members: [], averageClubCount: 0, distribution: [] };

test('전체 조회는 세 API를 독립 호출하며 실패한 영역만 숨긴다', async () => {
    const calls = [];
    const { dashboard, get } = setup(async (url, options) => {
        calls.push({ url, options });
        if (url.endsWith('/books')) return response({}, 500);
        return response(url.endsWith('/clubs') ? { clubs: [] } : emptyMembers);
    });

    await dashboard.refreshAll();

    assert.equal(calls.length, 3);
    assert.deepEqual(calls.map(call => call.url), [
        '/api/admin/dashboard/clubs', '/api/admin/dashboard/books', '/api/admin/dashboard/members'
    ]);
    assert.ok(calls.every(call => call.options.headers['X-Admin-Token'] === 'test-admin'
        && call.options.cache === 'no-store'));
    assert.equal(get('clubs-content').hidden, false);
    assert.equal(get('books-content').hidden, true);
    assert.equal(get('members-content').hidden, false);
    assert.match(get('books-status').textContent, /조회에 실패/);
});

test('영역 새로고침은 다른 영역 API를 호출하지 않고 모임 수와 상태를 표시한다', async () => {
    const calls = [];
    const { dashboard, get } = setup(async url => {
        calls.push(url);
        return response({ clubs: [{ clubId: 1, name: '여백', bookId: 7, bookTitle: '책',
            bookStatus: 'DELETED', memberCount: 0, commentCount: 3 }] });
    });
    await get('refresh-clubs').listeners.click();
    assert.deepEqual(calls, ['/api/admin/dashboard/clubs']);
    assert.equal(get('club-total').textContent, '1');
    assert.equal(get('clubs-body').children[0].children[4].textContent, '삭제됨');
    assert.equal(get('clubs-body').children[0].children[5].textContent, '0');
    assert.equal(get('clubs-body').children[0].children[6].textContent, '3');
});

test('평균은 소수 둘째 자리까지 표시하고 0개 참여 분포도 렌더링한다', async () => {
    const { dashboard, get } = setup(async () => response({
        members: [{ memberId: 1, nickname: '둘', clubCount: 2 }, { memberId: 2, nickname: '영', clubCount: 0 }],
        averageClubCount: 1,
        distribution: [{ clubCount: 0, memberCount: 1 }, { clubCount: 2, memberCount: 1 }]
    }));
    await dashboard.refreshSection('members');
    assert.equal(get('member-average').textContent, '1.00');
    assert.equal(get('member-total').textContent, '2');
    assert.equal(get('distribution').children[0].textContent, '0개1명');
    assert.equal(get('distribution').children[1].textContent, '2개1명');
});

test('데이터가 없는 경우 각 영역의 빈 상태와 0 평균을 표시한다', async () => {
    const { dashboard, get } = setup(async url => response(url.endsWith('/members')
        ? emptyMembers : url.endsWith('/clubs') ? { clubs: [] } : { books: [] }));
    await dashboard.refreshAll();
    assert.match(get('clubs-body').textContent, /모임이 없습니다/);
    assert.match(get('books-body').textContent, /책이 없습니다/);
    assert.match(get('members-body').textContent, /회원이 없습니다/);
    assert.match(get('distribution').textContent, /회원이 없습니다/);
    assert.equal(get('member-average').textContent, '0.00');
});

test('토큰이 비어 있으면 API를 호출하지 않는다', async () => {
    let calls = 0;
    const { dashboard, get } = setup(async () => { calls++; });
    get('admin-token').value = '  ';
    await dashboard.refreshAll();
    assert.equal(calls, 0);
    assert.match(get('clubs-status').textContent, /토큰을 입력/);
});

test('기존 관리자 화면처럼 토큰 앞뒤 공백을 제거해 전송한다', async () => {
    let sentToken;
    const { dashboard, get } = setup(async (url, options) => {
        sentToken = options.headers['X-Admin-Token'];
        return response({ books: [] });
    });
    get('admin-token').value = '  test-admin  ';
    await dashboard.refreshSection('books');
    assert.equal(sentToken, 'test-admin');
    assert.equal(get('books-content').hidden, false);
});

test('인증 오류 후 이전 결과를 숨기고 재조회 성공 시 복구한다', async () => {
    let status = 200;
    const { dashboard, get } = setup(async () => response({ books: [] }, status));
    await dashboard.refreshSection('books');
    assert.equal(get('books-content').hidden, false);
    status = 401;
    await dashboard.refreshSection('books');
    assert.equal(get('books-content').hidden, true);
    assert.match(get('books-status').textContent, /토큰을 확인/);
    assert.equal(get('refresh-books').disabled, false);
    status = 200;
    await dashboard.refreshSection('books');
    assert.equal(get('books-content').hidden, false);
});

test('이전 요청이 늦게 도착해도 최신 조회 결과를 덮어쓰지 않는다', async () => {
    const first = deferred();
    let calls = 0;
    const { dashboard, get } = setup(async () => ++calls === 1 ? first.promise : response({ books: [] }));
    const pending = dashboard.refreshSection('books');
    await dashboard.refreshSection('books');
    first.resolve(response({ books: [{ bookId: 1, title: '오래된 결과', status: 'ACTIVE', clubCount: 3 }] }));
    await pending;
    assert.equal(get('book-total').textContent, '0');
    assert.doesNotMatch(get('books-body').textContent, /오래된/);
});

test('토큰을 바꾸면 표시된 데이터를 지우고 진행 중 요청의 응답을 무시한다', async () => {
    const pending = deferred();
    const { dashboard, get } = setup(async url => url.endsWith('/books')
        ? pending.promise : response(emptyMembers));
    await dashboard.refreshSection('members');
    const request = dashboard.refreshSection('books');
    get('admin-token').value = 'different-token';
    get('admin-token').listeners.input();
    pending.resolve(response({ books: [] }));
    await request;
    assert.equal(get('members-content').hidden, true);
    assert.equal(get('books-content').hidden, true);
    assert.equal(get('member-average').textContent, '—');
    assert.equal(get('members-body').children.length, 0);
    assert.equal(get('books-panel').attributes['aria-busy'], 'false');
});

test('책 제목에 HTML이 들어 있어도 텍스트로만 표시한다', async () => {
    const title = '<img src=x onerror=alert(1)>';
    const { dashboard, get } = setup(async () => response({ books: [
        { bookId: 1, title, status: 'ACTIVE', clubCount: 2 }
    ] }));
    await dashboard.refreshSection('books');
    const titleCell = get('books-body').children[0].children[1];
    assert.equal(titleCell.textContent, title);
    assert.equal(titleCell.children.length, 0);
});

test('네트워크 오류 후 로딩을 해제하며 다른 영역은 계속 조회할 수 있다', async () => {
    const { dashboard, get } = setup(async url => {
        if (url.endsWith('/clubs')) throw new Error('네트워크 연결 실패');
        return response({ books: [] });
    });
    await dashboard.refreshSection('clubs');
    await dashboard.refreshSection('books');
    assert.equal(get('clubs-content').hidden, true);
    assert.equal(get('refresh-clubs').disabled, false);
    assert.equal(get('clubs-panel').attributes['aria-busy'], 'false');
    assert.equal(get('books-content').hidden, false);
});
