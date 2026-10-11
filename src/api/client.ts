/**
 * Lightweight fetch-based API client for the Smart Traffic backend.
 *
 * The base URL comes from the Vite environment variable `VITE_API_BASE_URL`
 * (declared in `.env` / `.env.local`, see `.env.example`). It falls back to
 * the Spring Boot dev server on localhost:8080, which allows the Vite dev
 * server (5173) and preview server (4173) via CORS.
 */
const BASE_URL =
  (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, '') ||
  'http://localhost:8080';

interface ProblemDetail {
  type?: string;
  title?: string;
  status: number;
  detail: string;
  instance?: string;
  [key: string]: unknown;
}

/** Error raised for any non-2xx response, carrying the backend's `detail` message. */
export class ApiError extends Error {
  readonly status: number;
  readonly detail: string;

  constructor(status: number, detail: string) {
    super(detail);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
  }
}

/** Raised when the request never reached the server (backend down, CORS, DNS...). */
export class NetworkError extends Error {
  readonly baseUrl: string;

  constructor(baseUrl: string, reason: string) {
    super(reason);
    this.name = 'NetworkError';
    this.baseUrl = baseUrl;
  }
}

function isProblemDetail(value: unknown): value is ProblemDetail {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as ProblemDetail).status === 'number' &&
    typeof (value as ProblemDetail).detail === 'string'
  );
}

/** Pull a human-readable message out of an error response body. */
async function readErrorDetail(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json();
    if (isProblemDetail(body)) return body.detail;
    if (typeof body === 'string' && body.trim()) return body;
    if (body && typeof body === 'object') {
      const record = body as Record<string, unknown>;
      // Spring's default binding-error shape: { errors: [...], message: "..." }
      if (typeof record.message === 'string' && record.message) return record.message;
      if (typeof record.error === 'string' && record.error) return record.error;
      return JSON.stringify(body);
    }
  } catch {
    // Body is not JSON — fall through to the status text.
  }
  return response.statusText || `Request failed with status ${response.status}.`;
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const url = new URL(path, BASE_URL);

  let response: Response;
  try {
    response = await fetch(url.toString(), {
      ...options,
      headers: {
        Accept: 'application/json',
        'Content-Type': 'application/json',
        ...options?.headers,
      },
    });
  } catch {
    throw new NetworkError(
      BASE_URL,
      `Could not reach the backend at ${BASE_URL}. Check that the Spring Boot server is running and that CORS allows this origin.`,
    );
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readErrorDetail(response));
  }

  // 204 No Content and empty bodies have nothing to parse.
  if (response.status === 204 || response.headers.get('content-length') === '0') {
    return undefined as unknown as T;
  }

  const text = await response.text();
  if (!text) return undefined as unknown as T;
  return JSON.parse(text) as T;
}

// ---- convenience helpers -------------------------------------------------

export const api = {
  /** GET, returns the parsed JSON body. */
  get: <T>(path: string) => request<T>(path),

  /** POST with a JSON body, returns the parsed JSON body. */
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),

  /** PUT with a JSON body, returns the parsed JSON body. */
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),

  /** PATCH with a JSON body, returns the parsed JSON body. */
  patch: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),

  /** DELETE, returns void. */
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
};

/** Convert any thrown value to a human-readable message suitable for banners. */
export function apiErrorMessage(err: unknown): string {
  if (err instanceof ApiError || err instanceof NetworkError) return err.message;
  if (err instanceof Error) return err.message;
  if (typeof err === 'string') return err;
  try {
    return JSON.stringify(err);
  } catch {
    return 'An unexpected error occurred.';
  }
}

export { BASE_URL };
