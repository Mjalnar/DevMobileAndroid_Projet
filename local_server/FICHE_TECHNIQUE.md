# Fiche technique — Serveur de démonstration (`server.py`)

---

## 1. En une phrase

`server.py` est un **petit serveur HTTP écrit en Python**, sans aucune
dépendance à installer, qui **reçoit les lieux envoyés par l'application Android
et les enregistre dans une base de données**, afin de démontrer la fonction de
**synchronisation réseau** (sauvegarde des données locales sur un serveur distant).

---

## 2. À quoi il sert / pourquoi il existe

L'application enregistre les lieux **en local** sur le téléphone (base SQLite).
La fonctionnalité de **synchronisation** consiste à envoyer ces lieux vers un
**serveur distant** pour les sauvegarder / les centraliser.

Le projet contenait déjà une API en **PHP + MySQL** (dossier `api_php/`), mais
PHP et MySQL **ne sont pas installés** sur la machine de démonstration. Pour
montrer la synchro **sans rien installer**, j'ai recréé le strict nécessaire en
**Python**, qui est déjà présent sur la machine.

> Le serveur Python **remplit le même rôle** que `api_php/add.php` : recevoir un
> lieu et renvoyer l'identifiant créé. Il ne remplace pas le projet, c'est un
> outil de démonstration.

---

## 3. Technologies utilisées

| Élément | Rôle | Particularité |
|---|---|---|
| **Python 3** | Langage | Déjà installé sur la machine |
| **`http.server`** | Module standard pour créer un serveur HTTP | Aucune installation (inclus dans Python) |
| **`sqlite3`** | Module standard de base de données | Base = un simple fichier `carnet.db` |
| **`json`** | Lecture/écriture du format JSON | Format d'échange avec l'app Android |

**Point clé à dire** : aucune bibliothèque externe (pas de `pip install`). Tout
vient de la **bibliothèque standard** de Python.

---

## 4. Comment ça marche — la chaîne complète

```
   APPLICATION ANDROID                          SERVEUR (server.py)
   ───────────────────                          ───────────────────
   Lieu en base locale (SQLite)
        │
        │  SyncManager.sendToServer()
        │  HTTP POST + corps JSON
        ▼
   { "title": "...", "latitude": ..., ... }  ─────►  do_POST()
                                                        │ json.loads(...)
                                                        │ INSERT dans carnet.db
                                                        │ récupère lastrowid
        ◄──────────────  { "remote_id": 1 }  ──────────┘
        │
   updateRemoteId() : l'entrée a maintenant un remote_id
   → elle ne sera plus renvoyée à la prochaine synchro
```

**Le contrat (très important)** : l'app envoie un JSON, le serveur répond
`{"remote_id": N}`. C'est exactement ce qu'attend `SyncManager` à la ligne
`res.getLong("remote_id")`. Ce contrat est **identique** à celui du `add.php`
d'origine — c'est pour ça qu'on peut remplacer l'un par l'autre.

---

## 5. Lecture du code, section par section

### a) Configuration
```python
HOST = "0.0.0.0"   # écoute sur toutes les interfaces réseau
PORT = 8000
DB_FILE = "carnet.db"
```
- `0.0.0.0` = le serveur accepte les connexions venant de **n'importe quelle
  interface** (le PC lui-même ET le téléphone sur le WiFi). Si on mettait
  `127.0.0.1`, seul le PC pourrait se connecter.

### b) `get_db()`
Ouvre la base SQLite et **crée la table `entries` si elle n'existe pas**
(`CREATE TABLE IF NOT EXISTS`). La base est un simple fichier créé
automatiquement au premier lancement.

### c) `do_POST()` — le cœur de la synchro
1. Lit la taille du corps (`Content-Length`) puis le corps de la requête.
2. `json.loads(...)` transforme le texte JSON reçu en dictionnaire Python.
3. `INSERT` des champs dans la base, avec des **requêtes paramétrées** (`?`).
4. `cur.lastrowid` = l'identifiant auto-incrémenté du nouvel enregistrement.
5. Répond `{"remote_id": <id>}`.

### d) `do_GET()` — page de contrôle
Affiche dans le navigateur (<http://localhost:8000>) la liste des lieux reçus.
Sert uniquement à **vérifier visuellement** que la synchro a fonctionné.

### e) `ThreadingHTTPServer`
Démarre le serveur. La version « Threading » permet de gérer **plusieurs
requêtes en parallèle** (utile si plusieurs lieux partent presque en même temps).

---

## 6. Les 2 réglages côté Android (à savoir expliquer)

1. **`API_URL = "http://10.0.2.2:8000"`** dans `SyncManager.java`
   - `10.0.2.2` est une **adresse spéciale de l'émulateur Android** qui pointe
     vers le `localhost` du PC. (L'émulateur est une machine virtuelle : son
     propre `127.0.0.1` serait l'émulateur lui-même, pas le PC.)
   - Sur un **vrai téléphone**, on met l'**IP du PC sur le WiFi** (ex.
     `http://192.168.1.20:8000`).

2. **`android:usesCleartextTraffic="true"`** dans `AndroidManifest.xml`
   - Depuis **Android 9 (API 28)**, le trafic **HTTP non chiffré** est bloqué
     par défaut (sécurité). Comme la démo utilise `http://` (pas `https://`),
     il faut explicitement l'autoriser.

---

## 7. Questions probables du jury + réponses

**« Pourquoi Python et pas PHP comme prévu ? »**
> PHP et MySQL n'étaient pas installés sur la machine. Python l'était, et il
> permet de faire un serveur équivalent sans rien installer. L'API PHP
> d'origine reste dans le projet ; ce serveur Python remplit le même contrat.

**« C'est quoi `10.0.2.2` ? »**
> Une adresse de redirection propre à l'émulateur Android : elle pointe vers le
> localhost de la machine hôte. Sur un vrai téléphone, on utilise l'IP du PC.

**« Pourquoi avoir autorisé le HTTP en clair ? »**
> Pour une démo locale en HTTP. Android 9+ le bloque par défaut. En production,
> on passerait en HTTPS et on retirerait ce réglage.

**« Où sont stockées les données reçues ? »**
> Dans un fichier SQLite `carnet.db` créé à côté du script. On peut le supprimer
> pour repartir de zéro.

**« Comment l'app sait qu'un lieu est déjà synchronisé ? »**
> Chaque entrée a un champ `remoteId` initialisé à -1. Après l'envoi, on stocke
> le `remote_id` renvoyé par le serveur. À la synchro suivante, on n'envoie que
> les entrées encore à -1. Ça évite les doublons.

**« Que se passe-t-il s'il n'y a pas de réseau ? »**
> `SyncManager.isNetworkAvailable()` vérifie la connexion avant de lancer la
> synchro ; sinon l'app affiche un message « pas de réseau ».

**« Pourquoi un thread / `ExecutorService` côté Android ? »**
> Les appels réseau sont interdits sur le thread principal (UI) d'Android. La
> synchro tourne donc sur un thread séparé pour ne pas geler l'interface.

**« Ce serveur est-il sécurisé / prêt pour la production ? »**
> Non, c'est un outil de démonstration : pas d'authentification, pas de HTTPS,
> pas de validation poussée. En production : HTTPS, authentification, et une
> vraie base (MySQL/PostgreSQL) comme l'API PHP.

**« Pourquoi des requêtes paramétrées (`?`) ? »**
> Pour se protéger des **injections SQL** : les valeurs ne sont jamais
> concaténées directement dans la requête.

---

## 8. Limites / pistes d'amélioration (si on demande « et après ? »)

- Passer en **HTTPS** + **authentification** (token).
- Gérer aussi la **mise à jour** et la **suppression** distantes (comme
  `list.php` / `delete.php`).
- Synchro **bidirectionnelle** (récupérer aussi les lieux ajoutés ailleurs).
- Héberger sur un **vrai serveur** au lieu du PC local.
