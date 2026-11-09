// @ts-check

const CAPTURE_URL = "https://us.i.posthog.com/i/v0/e/";

/**
 * @param {{ readonly projectToken: string, readonly environment: "development" | "production",
 * readonly distinctId: string, readonly origin: string,
 * readonly sendBeacon: (url: string, body: string) => boolean }} options
 */
export function createInviteAnalytics(options) {
  /**
   * @param {string} event
   * @param {Readonly<Record<string, string | boolean>>} properties
   */
  function capture(event, properties) {
    if (!options.projectToken) return;

    const payload = JSON.stringify({
      api_key: options.projectToken,
      distinct_id: options.distinctId,
      event,
      properties: {
        ...properties,
        environment: options.environment,
        $current_url: options.origin + "/",
        $process_person_profile: false,
      },
    });
    try {
      options.sendBeacon(CAPTURE_URL, payload);
    } catch (error) {
      if (!(error instanceof DOMException || error instanceof TypeError)) throw error;
    }
  }

  return {
    /** @param {"valid" | "invalid" | "empty"} linkState */
    pageViewed(linkState) {
      capture("$pageview", { link_state: linkState });
    },
    appOpenSelected() {
      capture("invite_app_open_selected", {});
    },
    /** @param {boolean} copied */
    codeCopySubmitted(copied) {
      capture("invite_web_code_copy_submitted", {
        result: copied ? "success" : "failure",
        manual_copy_offered: !copied,
      });
    },
    /** @param {"app_store" | "google_play"} store */
    storeSelected(store) {
      capture("invite_store_selected", { store });
    },
  };
}
