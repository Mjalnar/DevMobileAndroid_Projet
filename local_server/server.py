import json
import sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

HOST = "0.0.0.0"
PORT = 8000
DB_FILE = "carnet.db"


def get_db():
    conn = sqlite3.connect(DB_FILE)
    conn.execute("""
        CREATE TABLE IF NOT EXISTS entries (
            id        INTEGER PRIMARY KEY AUTOINCREMENT,
            title     TEXT,
            note      TEXT,
            latitude  REAL,
            longitude REAL,
            address   TEXT,
            timestamp INTEGER
        )
    """)
    conn.commit()
    return conn


class Handler(BaseHTTPRequestHandler):

    def _send_json(self, code, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        length = int(self.headers.get("Content-Length", 0))
        raw = self.rfile.read(length)
        try:
            data = json.loads(raw.decode("utf-8"))
        except Exception:
            self._send_json(400, {"error": "JSON invalide"})
            return

        conn = get_db()
        cur = conn.execute(
            "INSERT INTO entries (title, note, latitude, longitude, address, timestamp) "
            "VALUES (?, ?, ?, ?, ?, ?)",
            (
                data.get("title"),
                data.get("note"),
                data.get("latitude"),
                data.get("longitude"),
                data.get("address"),
                data.get("timestamp"),
            ),
        )
        conn.commit()
        remote_id = cur.lastrowid
        conn.close()

        print(f"  [SYNC] Recu : '{data.get('title')}'  ->  remote_id = {remote_id}")

        self._send_json(200, {"remote_id": remote_id})

    def do_GET(self):
        if self.path.startswith("/list"):
            self._send_list_json()
            return

        conn = get_db()
        rows = conn.execute(
            "SELECT id, title, address, timestamp FROM entries ORDER BY id DESC"
        ).fetchall()
        conn.close()

        lignes = "".join(
            f"<li>#{r[0]} — <b>{r[1]}</b> ({r[2]}) — {r[3]}</li>" for r in rows
        )
        html = (
            "<html><head><meta charset='utf-8'><title>Carnet - serveur</title></head>"
            f"<body><h2>Lieux recus : {len(rows)}</h2><ul>{lignes}</ul></body></html>"
        )
        body = html.encode("utf-8")
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def _send_list_json(self):
        conn = get_db()
        rows = conn.execute(
            "SELECT id, title, note, latitude, longitude, address, timestamp "
            "FROM entries ORDER BY id DESC"
        ).fetchall()
        conn.close()

        entries = [
            {
                "remote_id": r[0],
                "title": r[1],
                "note": r[2],
                "latitude": r[3],
                "longitude": r[4],
                "address": r[5],
                "timestamp": r[6],
            }
            for r in rows
        ]
        self._send_json(200, entries)

    def log_message(self, *args):
        pass


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print("=" * 55)
    print(f"  Serveur Carnet de voyage demarre sur le port {PORT}")
    print(f"  - Emulateur Android  : http://10.0.2.2:{PORT}")
    print(f"  - Telephone (WiFi)   : http://<IP-DE-TON-PC>:{PORT}")
    print(f"  - Navigateur du PC   : http://localhost:{PORT}")
    print("  (Ctrl+C pour arreter)")
    print("=" * 55)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nServeur arrete.")
