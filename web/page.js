// @ts-check
import { createInviteAnalytics } from "./analytics.js";
import { isMobileDevice, openAppOnce, parseInvitation } from "./invitation.js";

const title = document.getElementById("page-title");
const card = document.getElementById("code-card");
const value = document.getElementById("invite-code");
const copy = document.getElementById("copy-code");
const open = document.getElementById("open-app");
const notice = document.getElementById("notice");

if (!(title instanceof HTMLHeadingElement) || !(card instanceof HTMLElement) ||
    !(value instanceof HTMLElement) || !(copy instanceof HTMLButtonElement) ||
    !(open instanceof HTMLAnchorElement) || !(notice instanceof HTMLParagraphElement)) {
  throw new TypeError("Invitation page markup is incomplete");
}

const invitation = parseInvitation(window.location.search);
const mobile = isMobileDevice(navigator);
const projectToken = document.querySelector('meta[name="posthog-project-token"]')?.getAttribute("content") ?? "";
const environment = document.querySelector('meta[name="analytics-environment"]')?.getAttribute("content");
const analytics = createInviteAnalytics({
  projectToken: typeof globalThis.crypto?.randomUUID === "function" && typeof navigator.sendBeacon === "function" ? projectToken : "",
  environment: environment === "production" ? "production" : "development",
  distinctId: globalThis.crypto?.randomUUID?.() ?? "",
  origin: window.location.origin,
  sendBeacon: (url, body) => navigator.sendBeacon(url, body),
});

analytics.pageViewed(invitation.kind);
document.querySelectorAll("[data-store]").forEach((link) => {
  const store = link.getAttribute("data-store");
  if (store === "app_store" || store === "google_play") {
    link.addEventListener("click", () => analytics.storeSelected(store));
  }
});
document.documentElement.dataset.mobile = String(mobile);

switch (invitation.kind) {
  case "empty":
    break;
  case "invalid":
    title.textContent = "초대 링크를 확인해주세요";
    notice.textContent = "올바른 초대 링크를 다시 열어주세요.";
    break;
  case "valid": {
    title.textContent = "모임에 초대받았어요";
    card.hidden = false;
    value.textContent = invitation.code;
    open.href = invitation.appUri;
    open.hidden = false;
    open.addEventListener("click", () => analytics.appOpenSelected());
    const updateHint = () => {
      notice.textContent = !mobile && matchMedia("(min-width: 768px)").matches
        ? "휴대폰에서 초대 링크를 열어주세요."
        : "설치 후 초대 링크를 다시 열어주세요.";
    };
    updateHint();
    matchMedia("(min-width: 768px)").addEventListener("change", updateHint);
    const selectCode = () => {
      const range = document.createRange();
      range.selectNodeContents(value);
      const selection = window.getSelection();
      selection?.removeAllRanges();
      selection?.addRange(range);
      notice.textContent = "초대코드를 길게 눌러 복사해주세요.";
    };
    copy.addEventListener("click", async () => {
      if (!navigator.clipboard) {
        selectCode();
        analytics.codeCopySubmitted(false);
        return;
      }
      try {
        await navigator.clipboard.writeText(invitation.code);
        notice.textContent = "복사했어요.";
        analytics.codeCopySubmitted(true);
      } catch (error) {
        if (error instanceof DOMException) {
          selectCode();
          analytics.codeCopySubmitted(false);
          return;
        }
        throw error;
      }
    });
    if (mobile) openAppOnce(invitation.appUri, document, window.location);
    break;
  }
  default: {
    /** @type {never} */
    const unreachable = invitation;
    throw new TypeError(`Unknown invitation state: ${unreachable}`);
  }
}
