import { getStore } from "@netlify/blobs";
import { JWT } from "google-auth-library";

export const json = (status, body) =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", "access-control-allow-origin": "*" },
  });

/** Shared secret check: header `x-key`, or `?key=` in the URL. */
export function authorized(req) {
  const expected = process.env.WAKE_KEY;
  if (!expected) return false;
  const url = new URL(req.url);
  const given = req.headers.get("x-key") || url.searchParams.get("key") || "";
  return given === expected;
}

export const tokens = () => getStore("wakealarm");

let cachedClient;
function serviceAccount() {
  const raw = process.env.FIREBASE_SERVICE_ACCOUNT;
  if (!raw) throw new Error("FIREBASE_SERVICE_ACCOUNT env var is missing");
  return JSON.parse(raw);
}

async function accessToken() {
  const sa = serviceAccount();
  cachedClient ??= new JWT({
    email: sa.client_email,
    key: sa.private_key,
    scopes: ["https://www.googleapis.com/auth/firebase.messaging"],
  });
  const { token } = await cachedClient.getAccessToken();
  return { token, projectId: sa.project_id };
}

/** Send a high-priority data message to one device token. */
export async function sendPush(deviceToken, msg) {
  const { token, projectId } = await accessToken();
  const res = await fetch(`https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`, {
    method: "POST",
    headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
    body: JSON.stringify({
      message: {
        token: deviceToken,
        data: { msg, ts: String(Date.now()) },
        android: { priority: "HIGH", ttl: "120s" },
      },
    }),
  });
  const body = await res.json().catch(() => ({}));
  return { ok: res.ok, status: res.status, body };
}
