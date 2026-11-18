# 🛥️ SMART MARINA — Guide Complet du Projet (Semaines 1 à 6)

Bienvenue dans le guide complet et accessible de votre projet **Smart Marina** ! Ce document explique de façon claire, détaillée et sans jargon complexe tout ce qui a été développé de la semaine 1 à la semaine 6.

---

## 🧭 Sommaire
1. [La Vue d'Ensemble : C'est quoi Smart Marina ?](#1-la-vue-densemble--cest-quoi-smart-marina-)
2. [Les Outils Expliqués Simplement](#2-les-outils-expliqu%C3%A9s-simplement)
   - [Spring Boot](#spring-boot)
   - [Docker & Ce qui tourne dedans](#docker--ce-qui-tourne-dedans)
   - [Postman](#postman)
   - [Git & GitHub](#git--github)
   - [Apache Kafka](#apache-kafka)
3. [L'Architecture Microservices & Schéma](#3-larchitecture-microservices)
4. [Les 5 Couches d'un Microservice](#4-les-5-couches-dun-microservice)
5. [Déroulement Semaine par Semaine](#5-d%C3%A9roulement-semaine-par-semaine)
   - [Semaine 1 : Installation et Outils](#semaine-1--installation-et-outils)
   - [Semaine 2 : port-service (Gestion des Ports)](#semaine-2--port-service-port-8081)
   - [Semaine 3 : pedestal-service (Gestion des Bornes)](#semaine-3--pedestal-service--restclient-port-8082)
   - [Semaine 4 : API Gateway (Porte d'Entrée Unique)](#semaine-4--api-gateway-port-8080)
   - [Semaine 5 : consumption-service (Relevés d'Eau et d'Électricité)](#semaine-5--consumption-service-port-8083)
   - [Semaine 6 : Kafka & notification-service (Architecture Événementielle)](#semaine-6--kafka--notification-service-port-8084)
6. [Synchrone vs Asynchrone : La Règle d'Or](#6-synchrone-vs-asynchrone--la-r%C3%A8gle-dor)
7. [Fiche de Révision pour la Soutenance (Questions / Réponses)](#7-fiche-de-r%C3%A9vision-pour-la-soutenance)

---

## 1. La Vue d'Ensemble : C'est quoi Smart Marina ?

Dans un port de plaisance traditionnel, la gestion des quais, des bateaux, de l'eau et de l'électricité est souvent notée à la main sur du papier.
**Smart Marina** est un système informatique distribué qui automatise l'ensemble des activités portuaires :
- **Les Ports (`port-service`) :** Répertorie les différents ports maritimes (ex. : Port de Sidi Bou Said).
- **Les Bornes de ponton (`pedestal-service`) :** Les piliers physiques placés sur les quais où les bateaux se branchent pour avoir du courant et de l'eau.
- **Les Consommations (`consumption-service`) :** Enregistre la quantité d'électricité (en kilowatt-heure, kWh) et d'eau (en mètres cubes, $m^3$) consommée par chaque bateau.
- **Les Alertes (`notification-service`) :** Préviens la capitainerie en temps réel si un bateau dépasse un seuil anormal.
- **La Porte d'Entrée (`api-gateway`) :** Le guichet unique : les utilisateurs et applications externes n'ont qu'une seule adresse à contacter, et la passerelle se charge de rediriger vers le bon microservice.

Au lieu de regrouper tout le code dans un seul gros programme géant (appelé **Monolithe**), nous l'avons découpé en **Microservices** indépendants. Si un microservice tombe en panne, le reste du port continue de fonctionner normalement.

---

## 2. Les Outils Expliqués Simplement

### Spring Boot
Java de base demande des centaines de lignes pour démarrer un serveur web et se connecter à une base de données. **Spring Boot** fournit des composants prêts à l'emploi avec un serveur web intégré (**Tomcat** ou **Netty**). Avec de simples annotations (comme `@RestController`, `@Service`, `@Entity`), Spring Boot gère le réseau, les requêtes HTTP et le format JSON automatiquement.

### Docker & Ce qui tourne dedans
**Docker** permet d'exécuter des logiciels dans des environnements isolés appelés **Conteneurs** (comme de petits ordinateurs virtuels très légers). Cela évite d'installer 3 bases PostgreSQL et un serveur Kafka directement sur Windows.

Dans votre projet (`infra/docker-compose.yml`), vous avez **4 conteneurs actifs** :
1. **`postgres-port` (port 5433) :** La base de données privée de `port-service`.
2. **`postgres-pedestal` (port 5434) :** La base de données privée de `pedestal-service`.
3. **`postgres-consumption` (port 5435) :** La base de données privée de `consumption-service`.
4. **`kafka` (port 9092) :** Le courtier de messages Apache Kafka pour transmettre les événements en direct.

> **Règle fondamentale : "Une base par microservice"**  
> Chaque microservice possède sa propre base de données. Un service n'a **jamais le droit** d'accéder directement à la base d'un autre microservice ! Pour échanger des données, ils passent toujours par une API REST ou par un événement Kafka.

### Postman
Comme nos services n'ont pas encore d'écran graphique avec des boutons, ils échangent des messages au format JSON. **Postman** est le client qui nous sert à tester le système : il permet d'envoyer des requêtes (`GET`, `POST`, `PUT`, `DELETE`) et d'observer les codes de retour (`200 OK`, `201 Created`, `400 Bad Request`, `404 Not Found`, `503 Service Unavailable`).

### Git & GitHub
- **Git** est la machine à remonter le temps sur votre PC : chaque validation (`git commit`) crée un point de sauvegarde.
- **GitHub** est la sauvegarde en ligne dans le cloud (`git push`), permettant au professeur d'évaluer votre travail.

### Apache Kafka
Kafka est un tableau d'affichage ou une boîte aux lettres ultra-performante :
- Un service annonce : *"La borne 3 a enregistré un relevé de 75 kWh"* (Le **Producteur**).
- Il l'affiche sur un tableau appelé un **Topic** (`consumption-events`).
- Un autre service surveille le tableau et réagit aussitôt (Le **Consommateur**).
Le producteur dépose son message et repart immédiatement travailler, sans attendre que le consommateur ait fini de lire.

---

## 3. L'Architecture Microservices

```
Postman / Clients
       │
       ▼ (Toutes les requêtes passent par le port 8080)
┌────────────────────────────────────────────────────────┐
│                   API GATEWAY (8080)                   │
│   • Achemine /api/v1/ports/**        ──► Port 8081    │
│   • Achemine /api/v1/pedestals/**    ──► Port 8082    │
│   • Achemine /api/v1/consumptions/** ──► Port 8083    │
│   • Ajoute l'en-tête : X-Gateway: smart-marina-gateway │
└────────────────────────────────────────────────────────┘
       │                 │                 │
       ▼                 ▼                 ▼
 ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
 │ port-service │  │pedestal-serv.│  │consumption-serv. │
 │  Port: 8081  │  │  Port: 8082  │  │   Port: 8083     │
 └──────┬───────┘  └──────┬───────┘  └────────┬─────────┘
        │                 │                   │
        │                 │ (Appel REST)      │ (Appel REST)
        │                 ├───────────────────┤
        │                 │ "Le port existe?" │ "La borne existe?"
        ▼                 ▼                   ▼
  [(postgres-port)] [(postgres-ped.)]  [(postgres-cons.)]
    Port: 5433        Port: 5434          Port: 5435
                                              │
                                              ▼ (Événement Kafka)
                                   ╔═════════════════════════╗
                                   ║    APACHE KAFKA (9092)  ║
                                   ║ Topic: consumption-events║
                                   ╚═════════════════════════╝
                                              │
                                              ▼ (Asynchrone)
                                    ┌───────────────────┐
                                    │ notification-serv.│
                                    │    Port: 8084     │
                                    │ (Alertes en direct)
                                    └───────────────────┘
```

---

## 4. Les 5 Couches d'un Microservice

Chaque microservice du projet respecte les 5 couches logicielles propres à Spring Boot :
1. **Entité (`Entity`) :** La classe Java liée à la table PostgreSQL (ex. : `Port.java`).
2. **DTO (`Data Transfer Object`) :** L'objet de transfert qui valide les données entrantes et sortantes (ex. : `PortRequest.java`).
3. **Dépôt (`Repository`) :** L'interface qui communique avec PostgreSQL sans écrire de SQL à la main (Spring Data JPA).
4. **Service (`Service`) :** Le cerveau qui applique les règles métier et les vérifications.
5. **Contrôleur (`Controller`) :** La porte HTTP qui répond aux requêtes des utilisateurs en JSON.
6. **Client (`Client`) :** Utilise `RestClient` pour appeler un autre microservice via le réseau.

---

## 5. Déroulement Semaine par Semaine

| Semaine | Microservice | Port | Réalisations & Objectifs |
|---|---|---|---|
| **Semaine 1** | Configuration | — | Installation de l'environnement (Java 21, Maven, Docker, IntelliJ, Postman) et création du dépôt Git. |
| **Semaine 2** | `port-service` | `8081` | Premier microservice CRUD complet pour la gestion des ports. Base PostgreSQL `portdb` sur le port 5433. Validation avec DTOs. |
| **Semaine 3** | `pedestal-service` | `8082` | Deuxième microservice (gestion des bornes). Base dédiée `pedestaldb` (5434). Communication synchrone via `RestClient` pour vérifier l'existence du port. |
| **Semaine 4** | `api-gateway` | `8080` | Passerelle unique basée sur Spring Cloud Gateway (WebFlux). Routage des chemins et en-tête `X-Gateway`. Démonstration de la tolérance aux pannes. |
| **Semaine 5** | `consumption-service` | `8083` | Troisième microservice avec sa base `consumptiondb` (5435). Mesure des consommations (kWh / m³). Vérification de l'existence de la borne. |
| **Semaine 6** | `notification-service` + Kafka | `8084` | Architecture Événementielle (EDA). Producteur Kafka dans `consumption-service`, sujet `consumption-events`, et consommateur dans `notification-service` avec déclenchement d'alerte si électricité > 50 kWh. |

---

## 6. Synchrone vs Asynchrone : La Règle d'Or

| Critère | Synchrone (REST / HTTP) | Asynchrone (Kafka / Événements) |
|---|---|---|
| **Analogie** | Appel téléphonique : les deux interlocuteurs doivent être en ligne en même temps. | Boîte aux lettres : on dépose la lettre et le destinataire la lira à son rythme. |
| **Exemple dans le projet** | `pedestal-service` appelle `port-service` pour savoir si le port existe. | `consumption-service` annonce un relevé sur Kafka. |
| **Si le destinataire est en panne** | L'appel échoue immédiatement (Erreur `503`). | Le message reste en attente dans Kafka sans aucune perte de données. |
| **Couplage** | Couplage fort (dépendance temporelle et spatiale). | Couplage faible (le producteur ignore qui écoute). |
| **La Règle d'Or** | Utilisé quand on **a impérativement besoin de la réponse pour continuer**. | Utilisé quand on doit **seulement annoncer qu'un fait s'est produit**. |

---

## 7. Fiche de Révision pour la Soutenance

### Q1 : Pourquoi utiliser une architecture microservices plutôt qu'un monolithe ?
> **Réponse :** Pour l'isolation des pannes (si le service de notification plante, les ports et les bornes continuent de fonctionner normalement), pour la flexibilité de déploiement, et pour pouvoir faire évoluer ou monter en charge (*scaler*) chaque service indépendamment.

### Q2 : Pourquoi chaque microservice possède-t-il sa propre base de données ?
> **Réponse :** C'est le principe du **Database-per-Service**. Cela garantit le couplage lâche. Si tous les services partageaient la même base, une modification de table par une équipe risquerait de casser tous les autres services, et une panne de la base bloquerait l'ensemble du port.

### Q3 : Quel est le rôle de l'API Gateway et que ne doit-elle jamais faire ?
> **Réponse :** Elle sert de point d'accès unique pour les clients extérieurs, centralise le routage, la sécurité et les en-têtes. Elle ne doit **jamais contenir de logique métier** (les règles et calculs doivent rester à l'intérieur des microservices).

### Q4 : Pourquoi avoir choisi `pedestalId` comme clé de message Kafka ?
> **Réponse :** Dans Kafka, l'ordre des messages est garanti uniquement au sein d'une même partition. En choisissant l'identifiant de la borne comme clé, tous les relevés de cette borne atterrissent systématiquement dans la même partition et sont donc traités dans l'ordre chronologique exact.
