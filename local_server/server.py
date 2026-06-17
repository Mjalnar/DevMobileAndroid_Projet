"""
SERVEUR DE DEMONSTRATION (Python, sans rien a installer)
=========================================================
Reproduit le role de api_php/add.php, mais en Python pur :
  - il recoit les lieux envoyes par l'application Android (HTTP POST + JSON),
  - il les enregistre dans une base SQLite locale (fichier carnet.db),
  - il renvoie l'identifiant genere (remote_id), comme le faisait MySQL.

Aucune dependance externe : http.server et sqlite3 font partie de Python.

LANCEMENT :
    python server.py
Puis laisser la fenetre ouverte pendant la demo.
"""

import json
import sqlite3
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

# --- Configuration ---
HOST = "0.0.0.0"   # 0.0.0.0 = ecoute sur toutes les interfaces (emulateur ET telephone WiFi)
PORT = 8000
DB_FILE = "carnet.db"


def get_db():
    """Ouvre la base SQLite et cree la table si elle n'existe pas encore."""
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
        """Petit utilitaire pour repondre du JSON."""
        body = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def do_POST(self):
        """Reception d'un nouveau lieu envoye par l'application (= sync)."""
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

        # Trace visible dans la console -> pratique pour montrer la sync en direct
        print(f"  [SYNC] Recu : '{data.get('title')}'  ->  remote_id = {remote_id}")

        # Meme reponse que add.php : {"remote_id": N}
        self._send_json(200, {"remote_id": remote_id})

    def do_GET(self):
        """
        Page de controle : ouvrir http://localhost:8000 dans un navigateur
        pour verifier en direct ce que le serveur a recu.
        """
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

    def log_message(self, *args):
        """On desactive les logs HTTP bruts (on garde juste nos prints [SYNC])."""
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
