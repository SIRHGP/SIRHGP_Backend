# SIRHGP Backend

API REST Spring Boot, Java 17+, MySQL 8.

## Démarrage local avec Docker

Depuis le dossier `SIRHGP_Backend` :

```powershell
Copy-Item .env.docker.example .env
# Modifie ensuite DB_PASSWORD et MYSQL_ROOT_PASSWORD dans .env.
docker compose up --build
```

L'API écoute sur `http://localhost:8080` et MySQL est publié sur le port `3307`. Le frontend lancé séparément avec `npm run dev` utilise le proxy Vite vers l'API. Les données MySQL et les fichiers téléversés sont conservés dans des volumes Docker. `docker compose down` conserve ces volumes ; `docker compose down -v` les supprime définitivement.

Cette configuration est destinée au développement local : elle utilise le profil `dev` et une base Docker neuve. Elle ne copie ni ne modifie la base MySQL locale existante. Les migrations manuelles de production restent à appliquer explicitement sur la base cible.

Pour vérifier l'état des services :

```powershell
docker compose ps
Invoke-RestMethod http://localhost:8080/actuator/health
```

Le conteneur de production utilise le profil `prod`, `ddl-auto=validate` et exige `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` et `CORS_ORIGINS`. Ne publie jamais les valeurs de `.env`.

## Démarrage local (PowerShell)

Dans `backend_sirhgp`, lance `./start-local.ps1` ou `./run-local.ps1`. Le script demande le nom d'utilisateur et le mot de passe MySQL dans la session, génère un secret JWT temporaire si `JWT_SECRET` n'est pas déjà défini, puis démarre le profil `dev`. Les secrets ne sont pas écrits dans le dépôt.

Pour un lancement direct depuis VS Code, copie `.env.local.example` en `.env.local`, puis renseigne le vrai `DB_PASSWORD` ; ce fichier est ignoré par Git et lu par `launch.json`. Pour IntelliJ, configure `DB_URL`, `DB_USERNAME` et `DB_PASSWORD` avec leurs valeurs réelles dans l'environnement de la configuration d'exécution. Ne mets pas `${DB_USERNAME}` ou `${DB_PASSWORD}` comme valeur : ces chaînes sont transmises littéralement. `JWT_SECRET` est facultatif en développement : s'il manque, le backend génère une clé temporaire aléatoire en mémoire. Les tokens actifs seront invalidés au prochain redémarrage. En production, `JWT_SECRET` reste obligatoire. Ce projet utilise Spring Boot 3.3.4 : règle le SDK et le JRE d'exécution IntelliJ sur Java 21 (ou Java 23 maximum), pas sur Java 26.

Pour lancer manuellement l'application et garder le contrôle de chaque variable :

```powershell
$env:DB_URL = 'jdbc:mysql://localhost:3306/sirhgp_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME = Read-Host 'Utilisateur MySQL'
$env:DB_PASSWORD = [System.Net.NetworkCredential]::new('', (Read-Host 'Mot de passe MySQL' -AsSecureString)).Password
$jwtBytes = New-Object byte[] 32
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
mvn -o spring-boot:run
```

Adapter `DB_USERNAME` et `DB_PASSWORD` à un compte MySQL qui a les droits requis sur `sirhgp_db`. Le secret JWT généré dans la session change à chaque session PowerShell ; les tokens existants seront donc invalides au prochain redémarrage. Pour conserver les sessions, définir un `JWT_SECRET` stable et aléatoire dans la configuration locale de l'IDE ou dans les variables d'environnement de l'utilisateur.

Le profil `dev` écoute sur le port `8080` et met le schéma à jour avec Hibernate. Le frontend Vite proxy `/api` sur ce port par défaut ; `SERVER_PORT` et `BACKEND_PORT` permettent de le modifier.

## Initialisation des données

`APP_INIT_DATA` vaut `true` en développement et `false` par défaut ailleurs. En développement, l'initialiseur configure les rôles, permissions et postes. Les comptes de démonstration ne sont créés que si leurs mots de passe sont fournis via `APP_DEFAULT_USERS_SUPERADMIN_PASSWORD`, `APP_DEFAULT_USERS_ADMIN_PASSWORD`, `APP_DEFAULT_USERS_MANAGER_PASSWORD`, `APP_DEFAULT_USERS_STAFF_PASSWORD` et `APP_DEFAULT_USERS_STAGIAIRE_PASSWORD`; sinon, ils sont ignorés sans mot de passe faible par défaut.

Le profil `prod` désactive l'initialisation, exige les paramètres de base (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`) et un `JWT_SECRET` (Base64 d'au moins 32 octets aléatoires), et valide le schéma sans le modifier. Les migrations SQL manuelles sont sous `backend_sirhgp/src/main/resources/db/manual/`.

Pour une base qui contient encore le rôle `SECRETAIRE_DIRECTION`, activer ponctuellement `APP_MIGRATE_LEGACY_SECRETARY_ROLE=true` au démarrage du profil `prod`. Cette opération transactionnelle transfère les associations vers `SECRETAIRE` puis normalise les comptes actifs (garde le plus ancien par succursale, rétrograde les doublons ou comptes sans succursale en `STAFF`). Après le démarrage réussi, remettre la variable à `false`.
