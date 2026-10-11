export const LEVELS = ["SOFT", "MEDIUM", "HARD", "PRAISE"] as const;
export type Level = (typeof LEVELS)[number];

export interface RoastRequest {
  level: Level;
  tasks: string[];
  idleHours: number;
  doneCount: number;
  totalCount: number;
  /** Profanity (мат) allowed. Optional, defaults to false. */
  explicit: boolean;
  /** Name-calling allowed; implies explicit. Optional, defaults to false. */
  personal: boolean;
}

const MAX_TASKS = 10;
const MAX_TASK_LENGTH = 80;

const isCount = (value: unknown): value is number =>
  typeof value === "number" && Number.isInteger(value) && value >= 0 && value <= 1000;

/** Returns a normalized request, or an error message describing the first problem. */
export function validateRoastRequest(body: unknown): RoastRequest | string {
  if (typeof body !== "object" || body === null || Array.isArray(body)) return "body must be a JSON object";
  const raw = body as Record<string, unknown>;

  if (typeof raw.level !== "string" || !LEVELS.includes(raw.level as Level)) {
    return `level must be one of ${LEVELS.join(", ")}`;
  }
  if (!Array.isArray(raw.tasks) || !raw.tasks.every((t) => typeof t === "string")) {
    return "tasks must be an array of strings";
  }
  if (!isCount(raw.idleHours) || raw.idleHours > 48) return "idleHours must be an integer 0..48";
  if (!isCount(raw.doneCount)) return "doneCount must be a non-negative integer";
  if (!isCount(raw.totalCount)) return "totalCount must be a non-negative integer";
  if (raw.doneCount > raw.totalCount) return "doneCount must not exceed totalCount";
  if (raw.explicit !== undefined && typeof raw.explicit !== "boolean") return "explicit must be a boolean";
  if (raw.personal !== undefined && typeof raw.personal !== "boolean") return "personal must be a boolean";

  const tasks = (raw.tasks as string[])
    .map((t) => t.replace(/\s+/g, " ").trim().slice(0, MAX_TASK_LENGTH))
    .filter((t) => t.length > 0)
    .slice(0, MAX_TASKS);

  return {
    level: raw.level as Level,
    tasks,
    idleHours: raw.idleHours,
    doneCount: raw.doneCount,
    totalCount: raw.totalCount,
    explicit: raw.explicit === true || raw.personal === true,
    personal: raw.personal === true,
  };
}
