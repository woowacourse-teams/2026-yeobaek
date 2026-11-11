(function (root) {
    'use strict';
    const BASE = '/api/admin/deployment-monitoring';
    const statusNames = { queued: '실행 대기', pending: '실행 대기', waiting: '실행 대기', requested: '실행 대기',
        in_progress: '실행 중', completed: '종료', success: '성공', failure: '실패', cancelled: '취소',
        timed_out: '시간 초과', skipped: '건너뜀', building: '빌드 중', deploying: '배포 중', running: '작업 진행 중' };
    const reasonNames = { manual: '관리자 종료', maximum_duration: '최대 측정 시간 30분 도달',
        deployment_finished: '연결한 배포·롤백 종료 후 5분 경과', measurement_error: '측정 프로그램 오류' };

    function label(value) { return statusNames[value] || value || '확인 불가'; }
    function date(value) { return value ? new Date(value).toLocaleString('ko-KR', { hour12: false }) : '—'; }
    function duration(seconds) {
        const value = Math.max(0, Math.floor(seconds));
        return Math.floor(value / 60) + '분 ' + String(value % 60).padStart(2, '0') + '초';
    }
    function measurementStatus(data) {
        if (data.resultAvailable) return '결과 저장 완료';
        if (data.run.status === 'completed') return '종료 · 결과 없음';
        const stop = data.commands.some(command => command.command === 'stop'
            && (command.status !== 'completed' || command.conclusion === 'success'));
        if (stop) return '종료 요청 중 · 결과 저장 대기';
        return data.state ? '측정 중' : '시작 대기';
    }
    function initMonitoring(document, fetcher, scheduler = root) {
        const get = id => document.getElementById(id);
        let session = null;
        let current = null;
        let savedResult = null;
        let generation = 0;
        let refreshSequence = 0;
        let timer = null;
        let connected = false;
        let working = false;

        function message(text, error = false) {
            get('message').textContent = text;
            get('message').className = error ? 'status error' : 'status';
        }
        async function api(path = '', method = 'GET', body) {
            const token = get('admin-token').value.trim();
            if (!token) throw new Error('관리자 토큰을 입력해 주세요.');
            const response = await fetcher(BASE + path, {
                method, cache: 'no-store', headers: { 'X-Admin-Token': token, 'Content-Type': 'application/json' },
                ...(body === undefined ? {} : { body: JSON.stringify(body) })
            });
            const text = await response.text();
            const data = text ? JSON.parse(text) : null;
            if (!response.ok) throw new Error(data?.message || '상태를 조회할 수 없습니다.');
            return data;
        }
        function element(tag, text, className) {
            const node = document.createElement(tag);
            if (text !== undefined) node.textContent = text;
            if (className) node.className = className;
            return node;
        }
        function button(text, callback) {
            const node = element('button', text, 'secondary');
            node.type = 'button';
            node.addEventListener('click', callback);
            return node;
        }
        function safeLink(node, url) {
            if (url && /^https:\/\/github\.com\//.test(url)) {
                node.href = url;
                node.hidden = false;
            } else node.hidden = true;
        }
        function appendRow(body, cells, action) {
            const row = element('tr');
            cells.forEach(value => row.appendChild(element('td', value)));
            if (action) {
                const cell = element('td');
                cell.appendChild(action);
                row.appendChild(cell);
            }
            body.appendChild(row);
        }
        function showHistory(data) {
            get('environment').textContent = data.environment === 'dev' ? '개발 환경' : '운영 환경';
            const runs = get('runs');
            runs.replaceChildren();
            data.measurements.forEach(run => appendRow(runs,
                [date(run.createdAt), label(run.conclusion || run.status)], button('실행 보기', async () => {
                    session = run.session;
                    await refresh();
                })));
            if (!data.measurements.length) appendRow(runs, ['아직 측정 기록이 없습니다.', '', '']);
            const results = get('results');
            results.replaceChildren();
            data.results.forEach(result => appendRow(results, [date(result.createdAt), date(result.expiresAt)],
                button('결과 보기', () => loadResult(result.id))));
            if (!data.results.length) appendRow(results, ['보관 중인 결과가 없습니다.', '', '']);
        }
        function showSession(data) {
            current = data;
            const text = measurementStatus(data);
            get('measurement-status').textContent = text;
            get('measurement-status').setAttribute('data-state', text === '측정 중' ? 'measuring' : 'waiting');
            get('elapsed').textContent = data.state ? duration((Date.now() - Date.parse(data.state.startedAt)) / 1000)
                + (data.run.status === 'completed' ? ' · 정확한 시간은 결과에서 확인' : ' / 최대 30분') : '—';
            if (data.run.status === 'completed') get('elapsed').textContent = '종료 · 결과에서 측정 시간 확인';
            const maximum = data.state ? Date.parse(data.state.startedAt) + 1800 * 1000 : null;
            const deadline = data.observationEndsAt ? Math.min(maximum || Infinity, Date.parse(data.observationEndsAt)) : maximum;
            get('deadline').textContent = deadline && data.run.status !== 'completed'
                ? '자동 종료까지 ' + duration((deadline - Date.now()) / 1000)
                + ' · 실행 상태를 확인한 시점 기준'
                : '배포·롤백 종료 후 최대 5분 관찰 · 전체 최대 30분';
            get('checked').textContent = '마지막 확인: ' + date(data.checkedAt);
            safeLink(get('measurement-link'), data.run.url);
            get('stop').disabled = working || data.run.status === 'completed' || data.resultAvailable;
            get('load-deployments').disabled = working || data.run.status === 'completed' || data.resultAvailable;
            const failed = data.commands.filter(command => command.status === 'completed' && command.conclusion !== 'success');
            const pending = data.commands.filter(command => command.status !== 'completed');
            get('command-status').textContent = pending.length ? '명령 전달 중입니다. GitHub 실행 대기 후 반영됩니다.'
                : failed.length ? '실패한 종료·연결 명령이 있습니다. GitHub 실행 기록을 확인하고 다시 요청해 주세요.' : '';
            const linked = get('linked-deployments');
            linked.replaceChildren();
            data.deployments.forEach(run => {
                const row = element('div', undefined, 'deployment-item');
                row.appendChild(element('span', '#' + run.id + ' · 시도 ' + run.attempt + ' · ' + run.sha.slice(0, 7)
                    + ' · ' + label(run.conclusion || run.phase)));
                const link = element('a', 'GitHub에서 보기 ↗');
                link.target = '_blank'; link.rel = 'noopener noreferrer'; safeLink(link, run.url);
                row.appendChild(link); linked.appendChild(row);
            });
            if (!data.deployments.length) linked.textContent = '연결한 배포 없음';
        }
        function schedule() {
            scheduler.clearTimeout(timer);
            if (connected) timer = scheduler.setTimeout(refresh, 10000);
        }
        async function refresh() {
            const version = generation;
            const sequence = ++refreshSequence;
            const fresh = () => version === generation && sequence === refreshSequence;
            try {
                const overview = await api();
                if (!fresh()) return;
                connected = true;
                showHistory(overview);
                const active = overview.measurements.find(run => run.status !== 'completed');
                if (!session && active) session = active.session;
                get('start').disabled = working || Boolean(active) || Boolean(session && !overview.measurements.some(run => run.session === session));
                if (session) {
                    const data = await api('/sessions/' + session);
                    if (!fresh()) return;
                    showSession(data);
                }
                message('상태는 10초마다 갱신합니다. 요청별 통계는 측정 결과 저장 후 확인할 수 있습니다.');
            } catch (error) {
                if (!fresh()) return;
                message(error.message, true);
                get('measurement-status').textContent = session ? '상태 확인 불가' : '연결 확인 필요';
                get('measurement-status').setAttribute('data-state', 'error');
                get('start').disabled = true;
                // Do not invent a completion when the deployed application or GitHub is unavailable.
            } finally {
                if (fresh()) schedule();
            }
        }
        async function connect() {
            generation++;
            connected = true;
            scheduler.clearTimeout(timer);
            await refresh();
        }
        async function mutate(action) {
            if (working) return;
            working = true;
            get('start').disabled = true;
            get('stop').disabled = true;
            get('load-deployments').disabled = true;
            try { await action(); }
            catch (error) { message(error.message + ' 상태를 새로고침해 반영 여부를 확인해 주세요.', true); }
            finally { working = false; schedule(); }
        }
        async function start() {
            const version = generation;
            await mutate(async () => {
                const response = await api('/sessions', 'POST');
                if (version !== generation) return;
                session = response.session;
                current = null;
                connected = true;
                get('measurement-status').textContent = '시작 요청 완료 · 실행 대기';
                message('실제 ‘측정 중’ 상태를 확인한 뒤 배포를 시작해 주세요.');
                await refresh();
            });
        }
        async function stop() {
            if (!session) return;
            const version = generation;
            await mutate(async () => {
                await api('/sessions/' + session + '/stop', 'POST');
                if (version !== generation) return;
                get('measurement-status').textContent = '종료 요청 중';
                message('종료 명령 전달 후 진행 중인 요청과 결과 저장을 마무리합니다.');
            });
        }
        async function loadDeployments() {
            const version = generation;
            try {
                const runs = await api('/deployments');
                if (version !== generation) return;
                const container = get('deployment-options');
                container.replaceChildren();
                runs.forEach(run => {
                    const row = element('div', undefined, 'deployment-item');
                    row.appendChild(element('span', '#' + run.id + ' · ' + date(run.createdAt) + ' · '
                        + run.sha.slice(0, 7) + ' · ' + label(run.conclusion || run.status) + ' · ' + run.title));
                    const choose = button('연결', () => mutate(async () => {
                        await api('/sessions/' + session + '/deployments', 'POST', { runId: run.id, attempt: run.attempt });
                        if (version !== generation) return;
                        choose.disabled = true;
                        get('command-status').textContent = '연결 명령 전달 중입니다. 반영되면 연결한 배포에 표시됩니다.';
                    }));
                    choose.disabled = !session || current?.run.status === 'completed' || Boolean(current?.resultAvailable)
                        || Boolean(current?.deployments.some(linked => linked.id === run.id && linked.attempt === run.attempt));
                    row.appendChild(choose); container.appendChild(row);
                });
                if (!runs.length) container.textContent = '아직 배포 실행이 없습니다. 배포를 시작한 뒤 다시 불러와 주세요.';
                get('deployment-picker').hidden = false;
            } catch (error) { message(error.message, true); }
        }
        function showResult(result) {
            savedResult = result;
            get('result-panel').hidden = false;
            get('result-period').textContent = date(result.startedAt) + ' ~ ' + date(result.sendingStoppedAt)
                + ' · ' + duration((Date.parse(result.sendingStoppedAt) - Date.parse(result.startedAt)) / 1000);
            get('result-target').textContent = '대상: ' + result.target + ' · 1초 간격 · 응답 제한 3초';
            get('result-reason').textContent = '종료 사유: ' + (reasonNames[result.stopReason] || result.stopReason);
            const warnings = [];
            if (result.fatalError) warnings.push('측정 도구 오류로 일부 기록만 남았습니다.');
            if (result.missedScheduledRequests) warnings.push('측정하지 못한 예정 요청 ' + result.missedScheduledRequests + '건');
            if (result.controlErrors) warnings.push('배포 상태·명령 조회 실패 ' + result.controlErrors + '회');
            get('result-warning').textContent = warnings.join(' / ');
            ['total', 'successes', 'failures'].forEach(key => { get(key).textContent = result.summary[key].toLocaleString('ko-KR') + '건'; });
            get('failures').setAttribute('data-failed', String(result.summary.failures > 0));
            const timeline = get('timeline'); timeline.replaceChildren();
            const failures = get('failed-requests'); failures.replaceChildren();
            const all = get('all-requests'); all.replaceChildren();
            result.samples.forEach(sample => {
                const mark = element('span', undefined, sample.success ? '' : 'failed');
                mark.title = date(sample.startedAt) + ' · ' + (sample.success ? '성공' : sample.error);
                timeline.appendChild(mark);
                const outcome = sample.error === 'timeout' ? '응답 시간 초과' : sample.error === 'connection_error'
                    ? '연결 오류' : 'HTTP ' + sample.statusCode;
                appendRow(all, [date(sample.startedAt), outcome, sample.elapsedMs + 'ms', sample.commitSha || '확인 불가']);
                if (!sample.success) appendRow(failures, [date(sample.startedAt), outcome, sample.elapsedMs + 'ms']);
            });
            if (!result.summary.failures) appendRow(failures, ['측정한 요청에서 실패 없음', '', '']);
            get('outage-total').textContent = '복구 확인된 구간 합계: ' + result.summary.estimatedRecoveredSeconds + '초'
                + (result.summary.recoveryUnconfirmed ? ' · 복구 미확인 구간 있음' : '');
            const outages = get('outages'); outages.replaceChildren();
            result.summary.outages.forEach(outage => outages.appendChild(element('p', date(outage.startedAt) + ' → '
                + (outage.recoveredAt ? date(outage.recoveredAt) + ' · 약 ' + outage.estimatedSeconds + '초' : '복구 미확인'))));
            if (!result.summary.outages.length) outages.textContent = '관측된 중단 구간 없음';
            const versions = get('versions'); versions.replaceChildren();
            result.summary.versions.forEach(version => versions.appendChild(element('p', date(version.at) + ' · ' + version.commitSha)));
            if (!result.summary.versions.length) versions.textContent = '커밋 SHA를 확인하지 못했습니다.';
            const deployments = get('result-deployments'); deployments.replaceChildren();
            result.deployments.forEach(run => deployments.appendChild(element('p', '#' + run.id + ' · 시도 ' + run.run_attempt
                + ' · ' + run.head_sha + ' · ' + label(run.conclusion || run.status)
                + ' · 시작 ' + date(run.measurementStartedAt) + ' · 종료 ' + date(run.measurementCompletedAt))));
            if (!result.deployments.length) deployments.textContent = '연결한 배포 없음';
        }
        async function loadResult(id) {
            const version = generation;
            try {
                const result = await api('/results/' + id);
                if (version === generation) showResult(result);
            }
            catch (error) { message(error.message, true); }
        }
        function download() {
            if (!savedResult) return;
            const url = root.URL.createObjectURL(new root.Blob([JSON.stringify(savedResult, null, 2)], { type: 'application/json' }));
            const link = element('a'); link.href = url; link.download = 'measurement-' + savedResult.session + '.json';
            link.click(); scheduler.setTimeout(() => root.URL.revokeObjectURL(url), 1000);
        }
        get('connect').addEventListener('click', connect);
        get('start').addEventListener('click', start);
        get('stop').addEventListener('click', stop);
        get('load-deployments').addEventListener('click', loadDeployments);
        get('download').addEventListener('click', download);
        get('admin-token').addEventListener('input', () => {
            generation++; connected = false; scheduler.clearTimeout(timer);
            session = null; current = null; savedResult = null;
            get('result-panel').hidden = true; get('deployment-picker').hidden = true;
            ['runs', 'results', 'linked-deployments', 'deployment-options'].forEach(id => get(id).replaceChildren());
            get('measurement-status').textContent = '인증 대기';
            get('elapsed').textContent = '—'; get('checked').textContent = '마지막 확인: —';
            get('deadline').textContent = ''; get('command-status').textContent = '';
            get('measurement-link').hidden = true;
            get('start').disabled = true; get('stop').disabled = true; get('load-deployments').disabled = true;
        });
        return { connect, refresh, start, stop, loadDeployments, loadResult, showResult };
    }
    if (typeof module !== 'undefined' && module.exports) module.exports = { initMonitoring, measurementStatus, duration };
    if (root.document) root.document.addEventListener('DOMContentLoaded', () => initMonitoring(root.document, root.fetch.bind(root)));
})(typeof window === 'undefined' ? globalThis : window);
