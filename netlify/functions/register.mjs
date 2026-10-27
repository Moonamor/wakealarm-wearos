import { authorized, json, tokens } from "./_lib.mjs";

/**
 * POST /register   header x-key: <WAKE_KEY>   body: {"token":"<fcm token>"}
 * Called automatically by the watch app. Stores the device token in Netlify Blobs.
 */
export default async (req) => {
  if (req.method === "OPTIONS") return json(204, {});
  if (!authorized(req)) return json(403, { ok: false, error: "forbidden" });

  let token = new URL(req.url).searchParams.get("token");
  if (!token && req.method === "POST") {
    const body = await req.json().catch(() => ({}));
    token = body.token;
  }
  if (!token || token.length < 20) return json(400, { ok: false, error: "token missing" });

  await tokens().set("device", token);
  return json(200, { ok: true });
};

export const config = { path: "/register" };
