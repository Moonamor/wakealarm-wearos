import { authorized, json, sendPush, tokens } from "./_lib.mjs";

/**
 * Trigger the watch from anywhere:
 *   GET  /alert?key=<WAKE_KEY>&msg=<text shown on the watch>
 *   POST /alert   header x-key: <WAKE_KEY>   body: {"msg":"..."}
 */
export default async (req) => {
  if (req.method === "OPTIONS") return json(204, {});
  if (!authorized(req)) return json(403, { ok: false, error: "forbidden" });

  const url = new URL(req.url);
  let msg = url.searchParams.get("msg") || url.searchParams.get("message");
  if (!msg && req.method === "POST") {
    const body = await req.json().catch(() => ({}));
    msg = body.msg || body.message;
  }
  msg = (msg || "استيقظ!").toString().trim().slice(0, 200);

  const deviceToken = await tokens().get("device");
  if (!deviceToken) {
    return json(409, { ok: false, error: "no watch registered yet — open the app on the watch once" });
  }

  const result = await sendPush(deviceToken, msg);
  if (!result.ok) {
    return json(502, { ok: false, error: "fcm failed", status: result.status, fcm: result.body });
  }
  return json(200, { ok: true, sent: msg, fcm: result.body.name });
};

export const config = { path: "/alert" };
