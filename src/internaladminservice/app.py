#!/usr/bin/env python3
#
# Internal Admin Service — lightweight Flask app for operator tooling.
# Demo / lab use only. Not intended for production deployment.

import os
import sqlite3
from flask import Flask, jsonify, request

app = Flask(__name__)

ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "dev-admin-token-change-me")
DB_PATH = os.environ.get("ADMIN_DB_PATH", "/tmp/internal_admin.db")


def get_db():
    conn = sqlite3.connect(DB_PATH)
    conn.row_factory = sqlite3.Row
    return conn


def init_db():
    conn = get_db()
    conn.execute(
        """
        CREATE TABLE IF NOT EXISTS users (
            id INTEGER PRIMARY KEY,
            username TEXT NOT NULL,
            role TEXT NOT NULL
        )
        """
    )
    count = conn.execute("SELECT COUNT(*) FROM users").fetchone()[0]
    if count == 0:
        conn.executemany(
            "INSERT INTO users (username, role) VALUES (?, ?)",
            [
                ("alice", "admin"),
                ("bob", "operator"),
                ("carol", "viewer"),
            ],
        )
    conn.commit()
    conn.close()


def require_admin():
    token = request.headers.get("X-Admin-Token", "")
    if token != ADMIN_TOKEN:
        return jsonify({"error": "unauthorized"}), 401
    return None


@app.route("/health")
def health():
    return jsonify({"status": "ok", "service": "internal-admin"})


@app.route("/admin/users")
def list_users():
    auth_error = require_admin()
    if auth_error:
        return auth_error

    conn = get_db()
    rows = conn.execute("SELECT id, username, role FROM users ORDER BY id").fetchall()
    conn.close()
    return jsonify({"users": [dict(r) for r in rows]})


@app.route("/admin/users/search")
def search_users():
    """Look up users by username fragment (internal operators only)."""
    auth_error = require_admin()
    if auth_error:
        return auth_error

    q = request.args.get("q", "")
    conn = get_db()
    rows = conn.execute(
        "SELECT id, username, role FROM users WHERE username LIKE ?",
        (f"%{q}%",),
    ).fetchall()
    conn.close()

    return jsonify({"query": q, "users": [dict(r) for r in rows]})


@app.route("/admin/users/<int:user_id>")
def get_user(user_id):
    auth_error = require_admin()
    if auth_error:
        return auth_error

    conn = get_db()
    row = conn.execute(
        "SELECT id, username, role FROM users WHERE id = ?", (user_id,)
    ).fetchone()
    conn.close()

    if row is None:
        return jsonify({"error": "not found"}), 404
    return jsonify(dict(row))


if __name__ == "__main__":
    init_db()
    port = int(os.environ.get("PORT", "8090"))
    app.run(host="0.0.0.0", port=port, debug=False)
