(function (root) {
    'use strict';

    function initDashboard(document, fetcher) {
        const areas = ['clubs', 'books', 'members'];
        const versions = { clubs: 0, books: 0, members: 0 };
        const tokenInput = document.getElementById('admin-token');
        const number = new Intl.NumberFormat('ko-KR');
        const element = (id) => document.getElementById(id);

        function cell(row, text, numeric) {
            const td = document.createElement('td');
            td.textContent = text;
            if (numeric) td.className = 'number';
            row.appendChild(td);
            return td;
        }

        function statusCell(row, status) {
            const td = cell(row, '');
            const badge = document.createElement('span');
            badge.className = status === 'DELETED' ? 'badge deleted' : 'badge';
            badge.textContent = status === 'DELETED' ? '삭제됨' : '활성';
            td.appendChild(badge);
        }

        function renderRows(id, items, columns, emptyMessage, render) {
            const body = element(id);
            body.replaceChildren();
            if (items.length === 0) {
                const row = document.createElement('tr');
                const td = cell(row, emptyMessage);
                td.colSpan = columns;
                td.className = 'empty';
                body.appendChild(row);
                return;
            }
            for (const item of items) {
                const row = document.createElement('tr');
                render(row, item);
                body.appendChild(row);
            }
        }

        function renderClubs(data) {
            element('club-total').textContent = number.format(data.clubs.length);
            renderRows('clubs-body', data.clubs, 7, '아직 만들어진 모임이 없습니다.', (row, club) => {
                cell(row, club.clubId);
                cell(row, club.name);
                cell(row, club.bookId);
                cell(row, club.bookTitle);
                statusCell(row, club.bookStatus);
                cell(row, number.format(club.memberCount), true);
                cell(row, number.format(club.commentCount), true);
            });
        }

        function renderBooks(data) {
            element('book-total').textContent = number.format(data.books.length);
            renderRows('books-body', data.books, 4, '아직 등록된 책이 없습니다.', (row, book) => {
                cell(row, book.bookId);
                cell(row, book.title);
                statusCell(row, book.status);
                cell(row, number.format(book.clubCount), true);
            });
        }

        function renderMembers(data) {
            element('member-total').textContent = number.format(data.members.length);
            element('member-average').textContent = Number(data.averageClubCount).toFixed(2);
            renderRows('members-body', data.members, 3, '아직 등록된 회원이 없습니다.', (row, member) => {
                cell(row, member.memberId);
                cell(row, member.nickname);
                cell(row, number.format(member.clubCount), true);
            });
            const distribution = element('distribution');
            distribution.replaceChildren();
            if (data.distribution.length === 0) {
                const message = document.createElement('p');
                message.className = 'empty';
                message.textContent = '아직 등록된 회원이 없습니다.';
                distribution.appendChild(message);
                return;
            }
            const maximum = data.distribution.reduce((max, bucket) => Math.max(max, bucket.memberCount), 1);
            for (const bucket of data.distribution) {
                const row = document.createElement('div');
                row.className = 'distribution-row';
                const label = document.createElement('span');
                label.textContent = number.format(bucket.clubCount) + '개';
                const track = document.createElement('div');
                track.className = 'bar-track';
                track.setAttribute('aria-hidden', 'true');
                const bar = document.createElement('div');
                bar.className = 'bar';
                bar.style.width = (bucket.memberCount / maximum * 100) + '%';
                track.appendChild(bar);
                const count = document.createElement('span');
                count.className = 'number';
                count.textContent = number.format(bucket.memberCount) + '명';
                row.appendChild(label);
                row.appendChild(track);
                row.appendChild(count);
                distribution.appendChild(row);
            }
        }

        const renderers = { clubs: renderClubs, books: renderBooks, members: renderMembers };

        function showStatus(area, text, error) {
            const status = element(area + '-status');
            status.className = error ? 'status error' : 'status';
            status.textContent = text;
        }

        async function refreshSection(area) {
            const version = ++versions[area];
            const token = tokenInput.value.trim();
            const button = element('refresh-' + area);
            const panel = element(area + '-panel');
            const content = element(area + '-content');
            content.hidden = true;
            if (!token) {
                showStatus(area, '관리자 토큰을 입력해 주세요.', true);
                button.disabled = false;
                panel.setAttribute('aria-busy', 'false');
                return;
            }
            button.disabled = true;
            panel.setAttribute('aria-busy', 'true');
            showStatus(area, '현재 현황을 불러오는 중입니다.', false);
            let failureMessage = '조회에 실패했습니다. 다시 새로고침해 주세요.';
            try {
                const response = await fetcher('/api/admin/dashboard/' + area, {
                    headers: { 'X-Admin-Token': token }, cache: 'no-store'
                });
                if (!response.ok) {
                    failureMessage = response.status === 401
                        ? '관리자 토큰을 확인해 주세요.'
                        : failureMessage;
                    throw new Error(failureMessage);
                }
                const data = await response.json();
                if (versions[area] !== version || tokenInput.value.trim() !== token) return;
                renderers[area](data);
                content.hidden = false;
                showStatus(area, '갱신 완료 · ' + new Date().toLocaleString('ko-KR'), false);
            } catch {
                if (versions[area] !== version || tokenInput.value.trim() !== token) return;
                showStatus(area, failureMessage, true);
            } finally {
                if (versions[area] === version) {
                    button.disabled = false;
                    panel.setAttribute('aria-busy', 'false');
                }
            }
        }

        function refreshAll() {
            return Promise.all(areas.map(refreshSection));
        }

        function reset() {
            for (const area of areas) {
                versions[area]++;
                element(area + '-content').hidden = true;
                element(area + '-body').replaceChildren();
                element('refresh-' + area).disabled = false;
                element(area + '-panel').setAttribute('aria-busy', 'false');
                showStatus(area, '관리자 토큰을 입력한 뒤 조회해 주세요.', false);
            }
            element('distribution').replaceChildren();
            for (const id of ['club-total', 'book-total', 'member-total', 'member-average']) {
                element(id).textContent = '—';
            }
        }

        element('refresh-all').addEventListener('click', refreshAll);
        for (const area of areas) {
            element('refresh-' + area).addEventListener('click', () => refreshSection(area));
        }
        tokenInput.addEventListener('input', reset);
        return { refreshAll, refreshSection, reset };
    }

    if (typeof module !== 'undefined' && module.exports) module.exports = { initDashboard };
    if (root.document) initDashboard(root.document, root.fetch.bind(root));
}(typeof window === 'undefined' ? globalThis : window));
