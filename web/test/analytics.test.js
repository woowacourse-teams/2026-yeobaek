import assert from "node:assert/strict";
import test from "node:test";
import { createInviteAnalytics } from "../analytics.js";

test("a valid invitation page sends a visit with a query-free URL", () => {
  const beacons = [];
  const analytics = createInviteAnalytics({
    projectToken: "phc_test",
    environment: "production",
    distinctId: "test-visit",
    origin: "https://yeobaek-invite.vercel.app",
    sendBeacon: (url, body) => {
      beacons.push({ url, payload: JSON.parse(body) });
      return true;
    },
  });

  analytics.pageViewed("valid");

  assert.deepEqual(beacons, [{
    url: "https://us.i.posthog.com/i/v0/e/",
    payload: {
      api_key: "phc_test",
      event: "$pageview",
      distinct_id: "test-visit",
      properties: {
        link_state: "valid",
        $current_url: "https://yeobaek-invite.vercel.app/",
        $process_person_profile: false,
        environment: "production",
      },
    },
  }]);
});

test("an unconfigured site leaves invitation actions usable without sending a beacon", () => {
  const analytics = createInviteAnalytics({
    projectToken: "",
    environment: "development",
    distinctId: "test-visit",
    origin: "https://yeobaek-invite.vercel.app",
    sendBeacon: () => assert.fail("Unconfigured site sent an analytics request"),
  });

  analytics.appOpenSelected();
});

test("includes attribution IDs in the tracked pageview and app open events", () => {
  const shareId = "79e37893-cd98-4db9-83db-ff90c3b80595";
  const webVisitId = "24a97a25-77e2-4b65-8432-d73ded1d5b47";
  const payloads = [];
  const analytics = createInviteAnalytics({
    projectToken: "phc_test",
    environment: "production",
    distinctId: webVisitId,
    shareId,
    origin: "https://yeobaek-invite.vercel.app",
    sendBeacon: (_url, body) => {
      payloads.push(JSON.parse(body));
      return true;
    },
  });

  analytics.pageViewed("valid");
  analytics.appOpenSelected("tracked");

  assert.deepEqual(payloads.map(({ event, properties }) => [
    event, properties.invite_share_id, properties.invite_web_visit_id, properties.$current_url,
  ]), [
    ["$pageview", shareId, webVisitId, "https://yeobaek-invite.vercel.app/"],
    ["invite_app_open_selected", shareId, webVisitId, "https://yeobaek-invite.vercel.app/"],
  ]);
});
