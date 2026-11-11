const test = require('node:test');
const assert = require('node:assert/strict');
const { initMonitoring, measurementStatus } = require('../../main/resources/static/admin-deployment-monitoring.js');

class Element {
    constructor(tagName = 'div') {
        this.tagName = tagName; this.children = []; this.listeners = {}; this.attributes = {};
        this.value = ''; this.hidden = true; this.disabled = false; this.ownText = '';
    }
    set textContent(value) { this.ownText = String(value); this.children = []; }
    get textContent() { return this.ownText + this.children.map(child => child.textContent).join(''); }
    appendChild(child) { this.children.push(child); return child; }
    replaceChildren() { this.children = []; this.ownText = ''; }
    setAttribute(key, value) { this.attributes[key] = value; }
    addEventListener(name, listener) { this.listeners[name] = listener; }
}
function response(data, status = 200) {
    return { ok: status >= 200 && status < 300, text: async () => data === null ? '' : JSON.stringify(data) };
}
function setup(fetcher) {
    const elements = new Map();
    const document = {
        getElementById(id) { if (!elements.has(id)) elements.set(id, new Element()); return elements.get(id); },
        createElement(tag) { return new Element(tag); }
    };
    const get = id => document.getElementById(id);
    get('admin-token').value = 'admin-test-token';
    const scheduled = [];
    const scheduler = { setTimeout: (fn, delay) => { scheduled.push({ fn, delay }); return scheduled.length; }, clearTimeout: () => {} };
    return { app: initMonitoring(document, fetcher, scheduler), get, scheduled };
}
const run = { id: 42, attempt: 1, session: 'session-one', title: 'monitoring', status: 'in_progress',
    conclusion: null, sha: 'a'.repeat(40), createdAt: '2026-11-11T00:00:00Z', url: 'https://github.com/a/b/actions/runs/42' };
const overview = { environment: 'dev', measurements: [run], results: [] };
const waiting = { run, state: null, deployments: [], commands: [], resultAvailable: false, checkedAt: '2026-11-11T00:00:01Z', observationEndsAt: null };

test('작업 실행 중이어도 실제 요청 시작 기록 전에는 측정 중으로 표시하지 않는다', () => {
    assert.equal(measurementStatus(waiting), '시작 대기');
    assert.equal(measurementStatus({ ...waiting, state: { startedAt: run.createdAt } }), '측정 중');
    assert.equal(measurementStatus({ ...waiting, resultAvailable: true }), '결과 저장 완료');
});

test('재접속하면 GitHub 기록에서 진행 중인 측정을 복원하고 중복 시작을 막는다', async () => {
    const calls = [];
    const { app, get, scheduled } = setup(async (url, options) => {
        calls.push({ url, options });
        return response(url.endsWith('/session-one') ? waiting : overview);
    });
    await app.connect();
    assert.equal(get('start').disabled, true);
    assert.equal(get('measurement-status').textContent, '시작 대기');
    assert.equal(get('stop').disabled, false);
    assert.ok(calls.every(call => call.options.headers['X-Admin-Token'] === 'admin-test-token' && call.options.cache === 'no-store'));
    assert.equal(scheduled.at(-1).delay, 10000);
});

test('조회 실패를 배포 완료로 오인하지 않고 마지막 확인 시각을 유지한다', async () => {
    let failed = false;
    const { app, get } = setup(async url => failed ? response({ message: '연결 오류' }, 502)
        : response(url.endsWith('/session-one') ? waiting : overview));
    await app.connect();
    const checked = get('checked').textContent;
    failed = true;
    await app.refresh();
    assert.equal(get('measurement-status').textContent, '상태 확인 불가');
    assert.equal(get('checked').textContent, checked);
    assert.equal(get('start').disabled, true);
});

test('종료는 취소 API가 아닌 종료 명령으로 전달하고 결과 저장을 기다린다', async () => {
    const calls = [];
    const { app, get } = setup(async (url, options) => {
        calls.push({ url, options });
        if (options.method === 'POST') return response(null, 202);
        return response(url.endsWith('/session-one') ? waiting : overview);
    });
    await app.connect(); await app.stop();
    assert.equal(calls.at(-1).url, '/api/admin/deployment-monitoring/sessions/session-one/stop');
    assert.equal(calls.at(-1).options.method, 'POST');
    assert.equal(get('measurement-status').textContent, '종료 요청 중');
});

test('배포 목록에서 고른 실행 ID와 재실행 번호만 연결한다', async () => {
    const calls = [];
    const deployment = { ...run, id: 99, attempt: 2, title: '<script>unsafe</script>' };
    const { app, get } = setup(async (url, options) => {
        calls.push({ url, options });
        if (options.method === 'POST') return response(null, 202);
        if (url.endsWith('/deployments')) return response([deployment]);
        return response(url.endsWith('/session-one') ? waiting : overview);
    });
    await app.connect(); await app.loadDeployments();
    const row = get('deployment-options').children[0];
    assert.match(row.children[0].textContent, /<script>unsafe<\/script>/);
    await row.children[1].listeners.click();
    assert.deepEqual(JSON.parse(calls.at(-1).options.body), { runId: 99, attempt: 2 });
});

test('복구 미확인 구간은 기간을 꾸며내지 않고 실패 원인을 보여준다', () => {
    const { app, get } = setup();
    app.showResult({ session: 's', target: 'https://example/health', startedAt: run.createdAt,
        sendingStoppedAt: '2026-11-11T00:00:02Z', stopReason: 'manual', deployments: [], controlErrors: 0,
        missedScheduledRequests: 0, fatalError: null,
        summary: { total: 1, successes: 0, failures: 1, estimatedRecoveredSeconds: 0, recoveryUnconfirmed: true,
            versions: [], outages: [{ startedAt: run.createdAt, recoveredAt: null, estimatedSeconds: null }] },
        samples: [{ startedAt: run.createdAt, success: false, error: 'timeout', elapsedMs: 3000, statusCode: 0, commitSha: null }] });
    assert.match(get('outages').textContent, /복구 미확인/);
    assert.match(get('outage-total').textContent, /합계: 0초/);
    assert.match(get('failed-requests').textContent, /응답 시간 초과/);
    assert.equal(get('timeline').children[0].className, 'failed');
    assert.equal(get('failures').textContent, '1건');
});

test('관리자 토큰을 바꾼 뒤 이전 응답이 도착해도 화면을 갱신하지 않는다', async () => {
    let finish;
    const { app, get } = setup(() => new Promise(resolve => { finish = resolve; }));
    const pending = app.connect();
    get('admin-token').value = 'different-token';
    get('admin-token').listeners.input();
    finish(response(overview));
    await pending;
    assert.equal(get('runs').children.length, 0);
});
