import Anthropic from "@anthropic-ai/sdk";
import { buildSystemPrompt, buildUserMessage, DEFAULT_MEMES } from "./prompt";
import { validateRoastRequest } from "./validation";

export interface Env {
  ROAST_KV: KVNamespace;
  ANTHROPIC_API_KEY: string;
  APP_TOKEN: string;
  MODEL: string;
  RATE_LIMIT_PER_HOUR: string;
}

const MAX_TEXT_LENGTH = 300;

const json = (body: unknown, status = 200): Response =>
  new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (url.pathname !== "/roast") return json({ error: "not found" }, 404);
    if (request.method !== "POST") return json({ error: "method not allowed" }, 405);

    const token = request.headers.get("X-App-Token") ?? "";
    if (!env.APP_TOKEN || !(await safeEqual(token, env.APP_TOKEN))) {
      return json({ error: "unauthorized" }, 401);
    }

    let body: unknown;
    try {
      body = await request.json();
    } catch {
      return json({ error: "invalid JSON" }, 400);
    }
    const roastRequest = validateRoastRequest(body);
    if (typeof roastRequest === "string") return json({ error: roastRequest }, 400);

    if (!(await consumeRateLimit(env, token))) return json({ error: "rate limit exceeded" }, 429);

    try {
      const [memes, quotes] = await Promise.all([loadList(env, "memes"), loadList(env, "quotes")]);
      const client = new Anthropic({ apiKey: env.ANTHROPIC_API_KEY, maxRetries: 1, timeout: 8_000 });
      const message = await client.messages.create({
        model: env.MODEL || "claude-haiku-4-5",
        max_tokens: 150,
        system: buildSystemPrompt(memes.length > 0 ? memes : DEFAULT_MEMES, quotes),
        messages: [{ role: "user", content: buildUserMessage(roastRequest) }],
      });

      const text = message.content
        .flatMap((block) => (block.type === "text" ? [block.text] : []))
        .join(" ")
        .replace(/\s+/g, " ")
        .replace(/^["«]+|["»]+$/g, "")
        .trim()
        .slice(0, MAX_TEXT_LENGTH);

      if (text.length === 0) return json({ error: "empty completion" }, 502);
      return json({ text });
    } catch (error) {
      // Any upstream problem → 502 so the app falls back to local templates.
      if (error instanceof Anthropic.RateLimitError) {
        console.warn("Anthropic rate limit", error.message);
      } else if (error instanceof Anthropic.APIError) {
        console.error(`Anthropic API error ${error.status}`, error.message);
      } else {
        console.error("Roast generation failed", error);
      }
      return json({ error: "upstream error" }, 502);
    }
  },
} satisfies ExportedHandler<Env>;

/** Constant-time comparison via SHA-256 digests (equal length regardless of input). */
async function safeEqual(a: string, b: string): Promise<boolean> {
  const encoder = new TextEncoder();
  const [da, db] = await Promise.all([
    crypto.subtle.digest("SHA-256", encoder.encode(a)),
    crypto.subtle.digest("SHA-256", encoder.encode(b)),
  ]);
  return crypto.subtle.timingSafeEqual(da, db);
}

async function tokenId(token: string): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(token));
  return [...new Uint8Array(digest).slice(0, 8)].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/**
 * Fixed-window limiter: N requests per clock hour per token. KV is eventually consistent, so the
 * limit is approximate — good enough to cap API spend.
 */
async function consumeRateLimit(env: Env, token: string): Promise<boolean> {
  const limit = Number.parseInt(env.RATE_LIMIT_PER_HOUR, 10) || 30;
  const hour = Math.floor(Date.now() / 3_600_000);
  const key = `rl:${await tokenId(token)}:${hour}`;
  const count = Number.parseInt((await env.ROAST_KV.get(key)) ?? "0", 10) || 0;
  if (count >= limit) return false;
  await env.ROAST_KV.put(key, String(count + 1), { expirationTtl: 2 * 3600 });
  return true;
}

/** Reads a JSON array of strings from KV ("memes" or "quotes"); empty on any problem. */
async function loadList(env: Env, key: "memes" | "quotes"): Promise<string[]> {
  try {
    const value = await env.ROAST_KV.get<unknown>(key, { type: "json", cacheTtl: 300 });
    if (Array.isArray(value)) {
      return value
        .filter((m): m is string => typeof m === "string" && m.trim().length > 0)
        .map((m) => m.trim().slice(0, 200))
        .slice(0, 50);
    }
  } catch (error) {
    console.warn(`Failed to read ${key} from KV`, error);
  }
  return [];
}
