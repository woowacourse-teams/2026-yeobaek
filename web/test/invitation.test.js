import assert from "node:assert/strict";
import { test } from "node:test";
import { isMobileDevice, openAppOnce, parseInvitation } from "../invitation.js";

test("keeps a valid code unchanged when building the app URI", () => {
  const search = "?code=ABC123";
  const invitation = parseInvitation(search);
  assert.deepEqual(invitation, {
    kind: "valid", code: "ABC123", appUri: "yeobaek://invite?code=ABC123",
  });
});

test("passes the share and web visit IDs to the app", () => {
  const shareId = "79e37893-cd98-4db9-83db-ff90c3b80595";
  const webVisitId = "24a97a25-77e2-4b65-8432-d73ded1d5b47";

  const invitation = parseInvitation(`?code=ABC123&share_id=${shareId}`, webVisitId);

  assert.deepEqual(invitation, {
    kind: "valid", code: "ABC123", shareId,
    appUri: `yeobaek://invite?code=ABC123&share_id=${shareId}&web_visit_id=${webVisitId}`,
  });
});

test("ignores duplicated or malformed share IDs while retaining a valid invitation", () => {
  for (const search of [
    "?code=ABC123&share_id=foo",
    "?code=ABC123&share_id=79e37893-cd98-4db9-83db-ff90c3b80595&share_id=bar",
  ]) {
    assert.deepEqual(parseInvitation(search), {
      kind: "valid", code: "ABC123", appUri: "yeobaek://invite?code=ABC123",
    });
  }
});

test("shows the download state when no invitation code was provided", () => {
  assert.deepEqual(parseInvitation(""), { kind: "empty" });
});

test("rejects duplicate codes and malformed values without normalizing them", () => {
  const invalid = ["?code=", "?code=abc123", "?code=ABCDE", "?code=ABCDEFG",
    "?code=ABC123&code=DEF456", "?code=ABC12%0A", "?code=ABC123%0A", "?code=javascript%3Aalert(1)",
    "?code=%E3%81%82ABC12", "?code=ABC+12"];
  for (const search of invalid) {
    assert.deepEqual(parseInvitation(search), { kind: "invalid" }, search);
  }
});

test("does not use other query parameters as an app execution destination", () => {
  const invitation = parseInvitation("?code=ABC123&redirect=https://example.com");
  assert.equal(invitation.appUri, "yeobaek://invite?code=ABC123");
});

test("recognizes Android and iOS without assuming an app is installed", () => {
  for (const userAgent of ["Android", "iPhone", "iPad", "iPod"]) {
    assert.equal(isMobileDevice({ userAgent, platform: "", maxTouchPoints: 0 }), true);
  }
  assert.equal(isMobileDevice({ userAgent: "Mac", platform: "MacIntel", maxTouchPoints: 5 }), true);
  assert.equal(isMobileDevice({ userAgent: "Mac", platform: "MacIntel", maxTouchPoints: 0 }), false);
  assert.equal(isMobileDevice({ userAgent: "Windows", platform: "Win32", maxTouchPoints: 0 }), false);
});

class VisibilityDocument extends EventTarget {
  constructor(state) {
    super();
    this.visibilityState = state;
  }
  change(state) {
    this.visibilityState = state;
    this.dispatchEvent(new Event("visibilitychange"));
  }
}

test("attempts opening once even after the user returns from the app", () => {
  const document = new VisibilityDocument("visible");
  const calls = [];
  openAppOnce("yeobaek://invite?code=ABC123", document, { assign: uri => calls.push(uri) });
  document.change("hidden");
  document.change("visible");
  assert.deepEqual(calls, ["yeobaek://invite?code=ABC123"]);
});

test("waits for a background page to become visible before the single attempt", () => {
  const document = new VisibilityDocument("hidden");
  const calls = [];
  openAppOnce("yeobaek://invite?code=ABC123", document, { assign: uri => calls.push(uri) });
  assert.deepEqual(calls, []);
  document.change("visible");
  document.change("hidden");
  document.change("visible");
  assert.deepEqual(calls, ["yeobaek://invite?code=ABC123"]);
});

test("keeps a rejected automatic launch from retrying on app return", () => {
  const document = new VisibilityDocument("visible");
  let attempts = 0;
  openAppOnce("yeobaek://invite?code=ABC123", document, {
    assign() { attempts++; throw new DOMException("Blocked", "SecurityError"); },
  });
  document.change("visible");
  assert.equal(attempts, 1);
});
