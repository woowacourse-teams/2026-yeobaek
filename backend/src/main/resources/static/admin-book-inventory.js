(function (root, factory) {
    const api = factory();
    if (typeof module === 'object' && module.exports) {
        module.exports = api;
    } else {
        root.AdminBookInventory = api;
    }
}(typeof globalThis === 'object' ? globalThis : this, function () {
    const CSV_HEADERS = [
        'bookId',
        'title',
        'authorNames',
        'authorIds',
        'publisher',
        'publishedYear',
        'passageCount',
        'status'
    ];
    const FORMULA_PREFIX = /^[\t\r\n ]*[=+\-@]/;

    function createState() {
        let revision = 0;
        let loading = false;
        let snapshot = null;

        return {
            beginLoad() {
                revision++;
                loading = true;
                snapshot = null;
                return revision;
            },
            isCurrent(loadRevision) {
                return loadRevision === revision;
            },
            completeLoad(loadRevision, books) {
                if (loadRevision !== revision) {
                    return false;
                }
                loading = false;
                snapshot = books.slice();
                return true;
            },
            failLoad(loadRevision) {
                if (loadRevision !== revision) {
                    return false;
                }
                loading = false;
                snapshot = null;
                return true;
            },
            invalidate() {
                revision++;
                loading = false;
                snapshot = null;
            },
            hasSnapshot() {
                return snapshot !== null;
            },
            isLoading() {
                return loading;
            },
            snapshotForExport(tableVisible) {
                if (loading || snapshot === null || !tableVisible) {
                    return null;
                }
                return snapshot.slice();
            }
        };
    }

    function neutralizeFormula(value) {
        const text = value == null ? '' : String(value);
        return FORMULA_PREFIX.test(text) ? "'" + text : text;
    }

    function quoteText(value) {
        return '"' + neutralizeFormula(value).replaceAll('"', '""') + '"';
    }

    function numericCell(value) {
        return value == null ? '' : String(value);
    }

    function booksToCsv(books) {
        const rows = books.map(book => [
            numericCell(book.bookId),
            quoteText(book.title),
            quoteText(book.authors.map(author => author.name).join(' | ')),
            quoteText(book.authors.map(author => author.authorId).join(' | ')),
            quoteText(book.publisher),
            numericCell(book.publishedYear),
            numericCell(book.passageCount),
            quoteText(book.status)
        ].join(','));
        return '\uFEFF' + [CSV_HEADERS.join(','), ...rows].join('\r\n');
    }

    function createDescriptionEditor(document, book, onSave) {
        const editor = document.createElement('details');
        editor.className = 'description-editor';
        const summary = document.createElement('summary');
        summary.textContent = book.description ? '소개 보기·수정' : '소개 추가';
        const textarea = document.createElement('textarea');
        textarea.value = book.description ?? '';
        textarea.setAttribute('aria-label', book.title + ' 책 소개');
        textarea.disabled = book.status === 'DELETED';
        const save = document.createElement('button');
        save.type = 'button';
        save.textContent = '소개 저장';
        const clear = document.createElement('button');
        clear.type = 'button';
        clear.textContent = '소개 비우기';
        save.disabled = clear.disabled = textarea.disabled;
        let pending = false;
        async function submit(description) {
            if (pending || book.status === 'DELETED') return;
            pending = true;
            save.disabled = clear.disabled = textarea.disabled = true;
            try {
                await onSave(description);
            } finally {
                pending = false;
                save.disabled = clear.disabled = textarea.disabled = false;
            }
        }
        save.addEventListener('click', () => submit(textarea.value));
        clear.addEventListener('click', () => submit(null));
        editor.append(summary, textarea, save, clear);
        return editor;
    }

    function createMetadataEditor(document, book, field, label, onSave) {
        const isHashtags = field === 'hashtags';
        const editor = document.createElement('details');
        editor.className = 'metadata-editor';
        const summary = document.createElement('summary');
        summary.textContent = label + ' 보기·수정';
        const input = document.createElement('textarea');
        input.value = JSON.stringify(book[field] ?? [], null, 2);
        input.setAttribute('aria-label', book.title + ' ' + label);
        input.disabled = book.status === 'DELETED';
        const help = document.createElement('p');
        help.textContent = isHashtags
            ? 'JSON 문자열 배열로 입력하세요. 예: ["첫사랑", "재회"]. 최대 5개, 문구당 10자입니다. []를 입력하거나 모두 비우고 저장하면 삭제됩니다.'
            : 'JSON 문자열 배열로 입력하세요. 예: ["로맨스", "비극"]. []를 입력하거나 모두 비우고 저장하면 삭제됩니다.';
        const error = document.createElement('div');
        error.className = 'error';
        error.setAttribute('role', 'alert');
        error.textContent = '';
        const save = document.createElement('button');
        save.type = 'button';
        save.textContent = label + ' 저장';
        save.disabled = input.disabled;
        let pending = false;
        save.addEventListener('click', async () => {
            if (pending || book.status === 'DELETED') return;
            let value;
            try {
                value = input.value === '' ? [] : JSON.parse(input.value);
            } catch {
                error.textContent = 'JSON 형식을 확인하세요. 예: ["첫 번째 문구", "두 번째 문구"]';
                return;
            }
            if (!Array.isArray(value) || value.some(item => typeof item !== 'string')) {
                error.textContent = '문자열 배열로 입력하세요. 예: ["첫 번째 문구", "두 번째 문구"]';
                return;
            }
            error.textContent = '';
            pending = true;
            save.disabled = input.disabled = true;
            try {
                await onSave(value);
            } finally {
                pending = false;
                save.disabled = input.disabled = false;
            }
        });
        editor.append(summary, input, help, error, save);
        return editor;
    }

    function createGenresEditor(document, book, onSave) {
        return createMetadataEditor(document, book, 'genres', '장르', onSave);
    }

    function createHashtagsEditor(document, book, onSave) {
        return createMetadataEditor(document, book, 'hashtags', '해시태그', onSave);
    }

    return { booksToCsv, createState, createDescriptionEditor, createGenresEditor, createHashtagsEditor };
}));
