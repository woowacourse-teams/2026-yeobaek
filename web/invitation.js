// @ts-check

/**
 * @typedef {{ kind: "empty" } | { kind: "invalid" } |
 *   { kind: "valid", code: string, appUri: string }} Invitation
 */

/** @param {string} search
 * @returns {Invitation} */
export function parseInvitation(search) {
  const values = new URLSearchParams(search).getAll("code");
  if (values.length === 0) return { kind: "empty" };
  const code = values[0];
  if (values.length !== 1 || !/^[A-Z0-9]{6}$/.test(code)) {
    return { kind: "invalid" };
  }
  return { kind: "valid", code, appUri: `yeobaek://invite?code=${code}` };
}

/** Device hint only: this does not detect whether the app is installed.
 * @param {Pick<Navigator, "userAgent" | "platform" | "maxTouchPoints">} navigator */
export function isMobileDevice(navigator) {
  return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

/**
 * A background tab waits until visible. Returning from the app does not reopen it.
 * The browser can reject automatic navigation; the page keeps its manual link.
 * @param {string} appUri
 * @param {Pick<Document, "visibilityState" | "addEventListener" | "removeEventListener">} document
 * @param {Pick<Location, "assign">} location
 */
export function openAppOnce(appUri, document, location) {
  let attempted = false;
  const attempt = () => {
    if (attempted || document.visibilityState !== "visible") return;
    attempted = true;
    document.removeEventListener("visibilitychange", attempt);
    try {
      location.assign(appUri);
    } catch (error) {
      if (error instanceof DOMException && error.name === "SecurityError") return;
      throw error;
    }
  };
  document.addEventListener("visibilitychange", attempt);
  attempt();
}
