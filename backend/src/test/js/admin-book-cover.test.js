const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const page = fs.readFileSync(path.join(__dirname, '../../main/resources/templates/admin.html'), 'utf8');
const coverCode = page.slice(page.indexOf('    async function resizeToJpeg('),
    page.indexOf('    async function replaceBookCover('));

function createHarness({ width = 1200, height = 1800, noContext = false,
    blob = new Blob(['jpeg'], { type: 'image/jpeg' }), drawError = null } = {}) {
    const draws = [];
    const backgrounds = [];
    const encodings = [];
    let closed = 0;
    const bitmap = { width, height, close() { closed++; } };
    const ctx = {
        fillRect(...args) { backgrounds.push({ color: this.fillStyle, args }); },
        drawImage(...args) {
            if (drawError) throw drawError;
            draws.push(args);
        }
    };
    const canvas = {
        getContext() { return noContext ? null : ctx; },
        toBlob(callback, type, quality) {
            encodings.push({ type, quality });
            callback(blob);
        }
    };
    const sandbox = {
        createImageBitmap: async () => bitmap,
        document: { createElement: () => canvas }
    };
    vm.createContext(sandbox);
    vm.runInContext(coverCode, sandbox);
    return { sandbox, bitmap, ctx, canvas, draws, backgrounds, encodings, blob,
        closed: () => closed };
}

test('표지를 300×450 JPEG 품질 0.8로 변환하고 흰 배경을 먼저 적용한다', async () => {
    const h = createHarness();
    const result = await h.sandbox.resizeToJpeg(new Blob(['original']));
    assert.equal(result, h.blob);
    assert.deepEqual([h.canvas.width, h.canvas.height], [300, 450]);
    assert.deepEqual(h.backgrounds, [{ color: '#ffffff', args: [0, 0, 300, 450] }]);
    assert.deepEqual(h.draws[0], [h.bitmap, 0, 0, 300, 450]);
    assert.deepEqual(h.encodings, [{ type: 'image/jpeg', quality: 0.8 }]);
    assert.equal(h.ctx.imageSmoothingEnabled, true);
    assert.equal(h.ctx.imageSmoothingQuality, 'high');
    assert.equal(h.closed(), 1);
});

test('가로·세로·작은 표지의 비율을 유지하고 캔버스 중앙에 배치한다', async () => {
    for (const input of [
        { width: 800, height: 400, expected: [0, 150, 300, 150] },
        { width: 400, height: 1200, expected: [75, 0, 150, 450] },
        { width: 100, height: 150, expected: [0, 0, 300, 450] }
    ]) {
        const h = createHarness(input);
        await h.sandbox.resizeToJpeg(new Blob(['original']), 0.7);
        assert.deepEqual(h.draws[0].slice(1), input.expected);
        assert.deepEqual(h.encodings, [{ type: 'image/jpeg', quality: 0.7 }]);
        assert.equal(h.closed(), 1);
    }
});

test('Canvas 생성·그리기·JPEG 인코딩 실패를 전파하고 bitmap을 닫는다', async () => {
    for (const input of [
        { noContext: true, message: 'Canvas를 사용할 수 없습니다.' },
        { drawError: new Error('그리기 실패'), message: '그리기 실패' },
        { blob: null, message: 'JPEG 변환에 실패했습니다.' }
    ]) {
        const h = createHarness(input);
        await assert.rejects(h.sandbox.resizeToJpeg(new Blob(['original'])),
            { message: input.message });
        assert.equal(h.closed(), 1);
    }
});

function stubUploads(h, { failAt = null, issueError = false } = {}) {
    const calls = [];
    const id = '123e4567-e89b-12d3-a456-426614174000';
    const targets = Object.fromEntries(['original', 'low', 'legacy'].map(name => [name, {
        uploadUrl: 'https://s3.example/' + name,
        requiredHeaders: {
            'Content-Type': name === 'original' ? 'image/png' : 'image/jpeg',
            'Cache-Control': 'public,max-age=31536000,immutable'
        }
    }]));
    h.sandbox.adminToken = () => 'admin-token';
    h.sandbox.fetch = async (url, options) => {
        calls.push({ url, options });
        if (url === '/api/admin/book-covers/upload-url') {
            return { ok: !issueError, json: async () => issueError
                ? { code: 'INVALID_REQUEST', message: '서명 실패' }
                : { coverImageKey: id, ...targets } };
        }
        return { ok: url !== 'https://s3.example/' + failAt, status: 403 };
    };
    return { calls, id, targets };
}

test('원본과 동일한 JPEG 두 객체를 모두 업로드한 뒤 UUID를 반환한다', async () => {
    const h = createHarness();
    const { calls, id, targets } = stubUploads(h);
    const file = new Blob(['original png'], { type: 'image/png' });
    assert.equal(await h.sandbox.uploadCover(file), id);
    assert.deepEqual(calls.map(call => call.url), [
        '/api/admin/book-covers/upload-url',
        targets.original.uploadUrl, targets.low.uploadUrl, targets.legacy.uploadUrl
    ]);
    assert.deepEqual(JSON.parse(calls[0].options.body), {
        contentType: file.type, contentLength: file.size, lowContentLength: h.blob.size
    });
    assert.equal(calls[0].options.headers['X-Admin-Token'], 'admin-token');
    assert.equal(calls[1].options.body, file);
    assert.equal(calls[2].options.body, h.blob);
    assert.equal(calls[3].options.body, h.blob);
    for (const [index, name] of ['original', 'low', 'legacy'].entries()) {
        assert.equal(calls[index + 1].options.method, 'PUT');
        assert.equal(calls[index + 1].options.headers, targets[name].requiredHeaders);
        assert.equal(calls[index + 1].options.headers['X-Admin-Token'], undefined);
    }
});

test('서명 발급 또는 각 S3 업로드가 실패하면 UUID를 반환하지 않고 후속 업로드를 중단한다', async () => {
    for (const [index, name] of ['original', 'low', 'legacy'].entries()) {
        const h = createHarness();
        const { calls } = stubUploads(h, { failAt: name });
        await assert.rejects(h.sandbox.uploadCover(new Blob(['original'], { type: 'image/png' })),
            { message: 'S3 표지 업로드 실패 (403)' });
        assert.equal(calls.length, index + 2);
    }
    const h = createHarness();
    const { calls } = stubUploads(h, { issueError: true });
    await assert.rejects(h.sandbox.uploadCover(new Blob(['original'], { type: 'image/png' })),
        { message: 'INVALID_REQUEST: 서명 실패' });
    assert.equal(calls.length, 1);
});

test('지원하지 않는 파일·잘못된 용량·이미지 변환 실패는 업로드를 시작하지 않는다', async () => {
    for (const file of [
        { type: 'image/gif', size: 100 },
        { type: 'image/png', size: 0 },
        { type: 'image/jpeg', size: 5 * 1024 * 1024 + 1 }
    ]) {
        const h = createHarness();
        const { calls } = stubUploads(h);
        await assert.rejects(h.sandbox.uploadCover(file));
        assert.equal(calls.length, 0);
    }
    const h = createHarness({ blob: null });
    const { calls } = stubUploads(h);
    await assert.rejects(h.sandbox.uploadCover(new Blob(['original'], { type: 'image/png' })),
        { message: 'JPEG 변환에 실패했습니다.' });
    assert.equal(calls.length, 0);
    assert.equal(h.closed(), 1);
});
