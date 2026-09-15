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

    return { booksToCsv, createState };
}));
