// @ts-check

/**
 * @typedef {{ kind: "empty" } | { kind: "invalid" } |
 *   { kind: "valid", code: string, appUri: string, shareId?: string }} Invitation
 */

/** @param {string} search
 * @param {string} [webVisitId]
 * @returns {Invitation} */
export function parseInvitation(search, webVisitId) {
  const params = new URLSearchParams(search);
  const values = params.getAll("code");
  if (values.length === 0) return { kind: "empty" };
  const code = values[0];
  if (values.length !== 1 || !/^[A-Z0-9]{6}$/.test(code)) {
    return { kind: "invalid" };
  }
  const legacyAppUri = `yeobaek://invite?code=${code}`;
  const shareIds = params.getAll("share_id");
  const shareId = shareIds.length === 1 ? shareIds[0] : undefined;
  if (!isAttributionId(shareId)) return { kind: "valid", code, appUri: legacyAppUri };

  const visit = isAttributionId(webVisitId) ? `&web_visit_id=${webVisitId}` : "";
  return { kind: "valid", code, appUri: `${legacyAppUri}&share_id=${shareId}${visit}`, shareId };
}

/**
 * @param {string | undefined} value
 * @returns {value is string}
 */
function isAttributionId(value) {
  return value !== undefined && value.length === 36 &&
    /^[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}$/.test(value);
}

/**
 * @param {Pick<Navigator, "userAgent" | "platform" | "maxTouchPoints">} navigator */
export function isMobileDevice(navigator) {
  return /Android|iPhone|iPad|iPod/i.test(navigator.userAgent) ||
    (navigator.platform === "MacIntel" && navigator.maxTouchPoints > 1);
}

/**
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
