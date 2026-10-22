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

    function withUploadDescription(payload, reviewedDescription) {
        if (payload === null || typeof payload !== 'object' || Array.isArray(payload)) {
            throw new Error('도서 JSON은 객체여야 합니다.');
        }
        const description = reviewedDescription.trim();
        return description ? { ...payload, description } : payload;
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

    return { booksToCsv, createState, withUploadDescription, createDescriptionEditor };
}));
