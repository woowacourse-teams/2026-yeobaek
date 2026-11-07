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
