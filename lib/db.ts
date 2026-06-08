import mysql from 'mysql2/promise';

// Single shared pool. Connects to the system MySQL (3306) where the `veggiebox`
// database lives — same pattern as the sibling apps (lawSuits, bakavua). The
// backend is a single centralized instance, so its local MySQL is the shared
// store for every worker that hits this server. Creds overridable via env so the
// same code runs on the NUC (prod) and the desktop (dev).
const globalForDb = globalThis as typeof globalThis & { _veggieboxPool?: mysql.Pool };

export const pool =
  globalForDb._veggieboxPool ??
  mysql.createPool({
    host: process.env.DB_HOST ?? 'localhost',
    port: Number(process.env.DB_PORT ?? 3306),
    user: process.env.DB_USER ?? 'veggiebox',
    password: process.env.DB_PASSWORD ?? 'veggiebox123',
    database: process.env.DB_NAME ?? 'veggiebox',
    waitForConnections: true,
    connectionLimit: 5,
    charset: 'utf8mb4',
    dateStrings: true,
  });

if (process.env.NODE_ENV !== 'production') globalForDb._veggieboxPool = pool;

// Thin query helper.
export async function q<T = Record<string, unknown>>(
  sql: string,
  params?: unknown[],
): Promise<T[]> {
  const [rows] = await pool.query(sql, params);
  return rows as T[];
}

export async function exec(sql: string, params?: unknown[]) {
  const [res] = await pool.query(sql, params);
  return res as mysql.ResultSetHeader;
}
