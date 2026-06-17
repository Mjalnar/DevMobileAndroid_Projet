# Serveur de démonstration (sync via réseau)

Petit serveur Python autonome pour démontrer la **synchronisation** de l'app
(envoi des lieux locaux vers un serveur distant). Aucune installation : utilise
uniquement la bibliothèque standard de Python (déjà présent sur la machine).

## 1. Lancer le serveur

Dans ce dossier (`local_server`) :

```bash
python server.py
```

Laisser la fenêtre ouverte. Elle affiche `[SYNC] Recu : ...` à chaque lieu reçu.

## 2. Lancer l'app

- **Émulateur Android** : rien à changer, l'URL est déjà `http://10.0.2.2:8000`
  (`10.0.2.2` = le PC vu depuis l'émulateur).
- **Vrai téléphone** (sur le **même WiFi** que le PC) :
  1. Trouver l'IP du PC : `ipconfig` (chercher « Adresse IPv4 », ex. `192.168.1.20`).
  2. Dans `SyncManager.java`, mettre `API_URL = "http://192.168.1.20:8000"`.

## 3. Démontrer

1. Ajouter un ou plusieurs lieux dans l'app.
2. Menu → **Synchroniser** : un toast confirme le nombre d'entrées envoyées.
3. Vérifier côté serveur :
   - dans la console (lignes `[SYNC]`),
   - ou dans un navigateur : <http://localhost:8000> (liste des lieux reçus).

## Comment ça marche (chaîne complète)

```
App Android (SQLite locale)
  └─ SyncManager.sendToServer()  --- HTTP POST {JSON} --->  server.py
                                                              └─ INSERT dans carnet.db
                                 <--- {"remote_id": N} -------┘
  └─ enregistre remote_id  (l'entrée ne sera plus renvoyée)
```

La base reçue est le fichier `carnet.db` créé automatiquement à côté du script.
Le supprimer remet la démo à zéro.