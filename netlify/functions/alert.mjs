import { authorized, json, sendPush, tokens } from "./_lib.mjs";

/**
 * Trigger the watch from anywhere.
 *
 *   GET  /alert?key=<WAKE_KEY>&msg=<text>[&level=soft|normal|urgent][&siren=1][&link=<url>][&phone=<number>]
 *   POST /alert   header x-key: <WAKE_KEY>   body: {"msg":"...","level":"urgent","siren":true,"link":"...","phone":"..."}
 *
 *   level  : vibration strength + screen colour (default normal)
 *   siren  : loud siren sound on the watch speaker (default: on for urgent, off otherwise)
 *   link   : shows "open on phone" button; also auto-detected from msg
 *   phone  : shows "call" button; also auto-detected from msg
 */
const LEVELS = new Set(["soft", "normal", "urgent"]);

export default async (req) => {
  if (req.method === "OPTIONS") return json(204, {});
  if (!authorized(req)) return json(403, { ok: false, error: "forbidden" });

  const url = new URL(req.url);
  const q = (k) => url.searchParams.get(k);
  let body = {};
  if (req.method === "POST") body = await req.json().catch(() => ({}));

  const pick = (...keys) => {
    for (const k of keys) {
      const v = body[k] ?? q(k);
      if (v !== undefined && v !== null && String(v).trim() !== "") return String(v).trim();
    }
    return undefined;
  };

  const msg = (pick("msg", "message") || "استيقظ!").slice(0, 200);
  const level = (pick("level") || "normal").toLowerCase();
  if (!LEVELS.has(level)) return json(400, { ok: false, error: "level must be soft|normal|urgent" });

  const sirenRaw = pick("siren");
  const siren = sirenRaw === undefined ? level === "urgent" : /^(1|true|yes|on)$/i.test(sirenRaw);

  const data = { msg, level, siren: siren ? "1" : "0" };
  const link = pick("link", "url");
  const phone = pick("phone", "tel");
  if (link) data.link = link.slice(0, 500);
  if (phone) data.phone = phone.replace(/[\s-]/g, "").slice(0, 20);

  const deviceToken = await tokens().get("device");
  if (!deviceToken) {
    return json(409, { ok: false, error: "no watch registered yet — open the app on the watch once" });
  }

  const result = await sendPush(deviceToken, data);
  if (!result.ok) {
    return json(502, { ok: false, error: "fcm failed", status: result.status, fcm: result.body });
  }
  return json(200, { ok: true, sent: data, fcm: result.body.name });
};

export const config = { path: "/alert" };
