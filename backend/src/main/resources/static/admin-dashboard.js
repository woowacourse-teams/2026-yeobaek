(function (root) {
    'use strict';

    function initDashboard(document, fetcher) {
        const areas = ['clubs', 'books', 'members'];
        const versions = { clubs: 0, books: 0, members: 0 };
        const tokenInput = document.getElementById('admin-token');
        const number = new Intl.NumberFormat('ko-KR');
        const getElementById = (id) => document.getElementById(id);

        function appendTextCell(row, text, numeric) {
            const td = document.createElement('td');
            td.textContent = text;
            if (numeric) td.className = 'number';
            row.appendChild(td);
            return td;
        }

        function appendBookStatusCell(row, status) {
            const td = appendTextCell(row, '');
            const badge = document.createElement('span');
            badge.className = status === 'DELETED' ? 'badge deleted' : 'badge';
            badge.textContent = status === 'DELETED' ? '삭제됨' : '활성';
            td.appendChild(badge);
        }

        function renderTableRows(id, items, columns, emptyMessage, render) {
            const body = getElementById(id);
            body.replaceChildren();
            if (items.length === 0) {
                const row = document.createElement('tr');
                const td = appendTextCell(row, emptyMessage);
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

        function renderClubActivityStatistics(data) {
            getElementById('club-total').textContent = number.format(data.clubs.length);
            renderTableRows('clubs-body', data.clubs, 7, '아직 만들어진 모임이 없습니다.', (row, club) => {
                appendTextCell(row, club.clubId);
                appendTextCell(row, club.name);
                appendTextCell(row, club.bookId);
                appendTextCell(row, club.bookTitle);
                appendBookStatusCell(row, club.bookStatus);
                appendTextCell(row, number.format(club.memberCount), true);
                appendTextCell(row, number.format(club.commentCount), true);
            });
        }

        function renderBookClubStatistics(data) {
            getElementById('book-total').textContent = number.format(data.books.length);
            renderTableRows('books-body', data.books, 4, '아직 등록된 책이 없습니다.', (row, book) => {
                appendTextCell(row, book.bookId);
                appendTextCell(row, book.title);
                appendBookStatusCell(row, book.status);
                appendTextCell(row, number.format(book.clubCount), true);
            });
        }

        function renderMemberParticipationStatistics(data) {
            getElementById('member-total').textContent = number.format(data.members.length);
            getElementById('member-average').textContent = Number(data.averageClubCount).toFixed(2);
            renderTableRows('members-body', data.members, 3, '아직 등록된 회원이 없습니다.', (row, member) => {
                appendTextCell(row, member.memberId);
                appendTextCell(row, member.nickname);
                appendTextCell(row, number.format(member.clubCount), true);
            });
            const distribution = getElementById('distribution');
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

        const statisticRenderers = {
            clubs: renderClubActivityStatistics,
            books: renderBookClubStatistics,
            members: renderMemberParticipationStatistics
        };

        function showSectionStatus(area, text, error) {
            const status = getElementById(area + '-status');
            status.className = error ? 'status error' : 'status';
            status.textContent = text;
        }

        async function refreshStatisticsSection(area) {
            const version = ++versions[area];
            const token = tokenInput.value.trim();
            const button = getElementById('refresh-' + area);
            const panel = getElementById(area + '-panel');
            const content = getElementById(area + '-content');
            content.hidden = true;
            if (!token) {
                showSectionStatus(area, '관리자 토큰을 입력해 주세요.', true);
                button.disabled = false;
                panel.setAttribute('aria-busy', 'false');
                return;
            }
            button.disabled = true;
            panel.setAttribute('aria-busy', 'true');
            showSectionStatus(area, '현재 현황을 불러오는 중입니다.', false);
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
                statisticRenderers[area](data);
                content.hidden = false;
                showSectionStatus(area, '갱신 완료 · ' + new Date().toLocaleString('ko-KR'), false);
            } catch {
                if (versions[area] !== version || tokenInput.value.trim() !== token) return;
                showSectionStatus(area, failureMessage, true);
            } finally {
                if (versions[area] === version) {
                    button.disabled = false;
                    panel.setAttribute('aria-busy', 'false');
                }
            }
        }

        function refreshAllStatistics() {
            return Promise.all(areas.map(refreshStatisticsSection));
        }

        function clearStatisticsForTokenChange() {
            for (const area of areas) {
                versions[area]++;
                getElementById(area + '-content').hidden = true;
                getElementById(area + '-body').replaceChildren();
                getElementById('refresh-' + area).disabled = false;
                getElementById(area + '-panel').setAttribute('aria-busy', 'false');
                showSectionStatus(area, '관리자 토큰을 입력한 뒤 조회해 주세요.', false);
            }
            getElementById('distribution').replaceChildren();
            for (const id of ['club-total', 'book-total', 'member-total', 'member-average']) {
                getElementById(id).textContent = '—';
            }
        }

        getElementById('refresh-all').addEventListener('click', refreshAllStatistics);
        for (const area of areas) {
            getElementById('refresh-' + area)
                .addEventListener('click', () => refreshStatisticsSection(area));
        }
        tokenInput.addEventListener('input', clearStatisticsForTokenChange);
        return { refreshAllStatistics, refreshStatisticsSection, clearStatisticsForTokenChange };
    }

    if (typeof module !== 'undefined' && module.exports) module.exports = { initDashboard };
    if (root.document) initDashboard(root.document, root.fetch.bind(root));
}(typeof window === 'undefined' ? globalThis : window));
