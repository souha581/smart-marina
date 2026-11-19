# 🛥️ Smart Marina XYZ — Corrigé Complet des 27 Questions de Préparation

**Module :** Atelier SOA & Microservices  
**Architecture :** REST, API Gateway, Docker, PostgreSQL, Apache Kafka  

---

## 📑 Sommaire
- [Partie A. Comprendre : synchrone, asynchrone, couplage (Q1 à Q5)](#partie-a-comprendre--synchrone-asynchrone-couplage)
- [Partie B. Rôles, protocoles et outils (Q6 à Q11)](#partie-b-rôles-protocoles-et-outils)
- [Partie C. Fiabilité, cohérence et pièges (Q12 à Q18)](#partie-c-fiabilité-cohérence-et-pièges)
- [Partie D. Implémentation avec Spring Boot (Q19 à Q25)](#partie-d-implémentation-avec-spring-boot)
- [Partie E. Synthèse et validation du projet (Q26 à Q27)](#partie-e-synthèse-et-validation-du-projet)

---

## Partie A. Comprendre : synchrone, asynchrone, couplage

### Question 1 (3 pts) : Différence communication synchrone vs asynchrone ? Exemple tiré de Smart Marina

**1. Communication Synchrone (bloquante / temps réel) :**
- L'émetteur envoie une requête et **bloque son exécution en attendant la réponse immédiate** du récepteur. Si le récepteur est indisponible ou lent, l'émetteur échoue ou subit un timeout.
- *Analogie :* Un appel téléphonique ou un échange radio VHF.
- *Exemple Smart Marina :* **`pedestal-service` appelle `port-service`** en HTTP REST pour vérifier qu'un port existe avant de créer une borne. Sans réponse immédiate, impossible de valider la création.

**2. Communication Asynchrone (non-bloquante / événementielle) :**
- L'émetteur publie un message/événement dans un intermédiaire (broker comme Kafka) et **reprend son travail immédiatement sans attendre** que le ou les consommateurs aient traité le message.
- *Analogie :* Déposer un courrier dans une boîte aux lettres.
- *Exemple Smart Marina :* **`consumption-service` publie l'événement `ConsommationRelevee`** dans Kafka. Il répond immédiatement `201 Created` au client pendant que `notification-service` et `facturation-service` consomment l'événement à leur propre rythme.

---

### Question 2 (3 pts) : Pourquoi ne pas tout faire en synchrone ? Quels risques prend-on ?

1. **Effet domino (pannes en cascade) :** Si le service Notification ou Facturation est en panne, l'enregistrement d'une simple consommation de borne échoue, alors que la mesure physique a bel et bien eu lieu.
2. **Dégradation exponentielle des performances :** Le temps de réponse total devient la somme des temps de réponse de tous les services appelés en chaîne ($T_{total} = T_1 + T_2 + T_3 + \dots$).
3. **Couplage temporel et spatial fort :** Tous les services doivent être en ligne et joignables à la même milliseconde.
4. **Perte de résilience face aux pics de charge :** Une vague d'appels simultanés sature tous les threads des serveurs web (Tomcat) et provoque un effondrement global du système.

---

### Question 3 (3 pts) : Pourquoi ne pas tout faire en asynchrone ?

1. **Nécessité d'une réponse immédiate pour l'utilisateur :** Lorsqu'un plaisancier ouvre l'application pour voir les emplacements libres ou son profil, il attend les données à l'écran tout de suite.
2. **Complexité opérationnelle élevée :** L'asynchrone impose la gestion de l'idempotence, de la cohérence éventuelle, des messages en double, des transactions distribuées (Saga), et du monitoring de broker.
3. **Absence de transaction ACID distribuée simple :** En asynchrone, il est impossible de faire un `ROLLBACK` immédiat si une étape ultérieure échoue ; il faut écrire des transactions de compensation.

---

### Question 4 (4 pts) : Comment décide-t-on entre synchrone et asynchrone ? Application aux 5 interactions

**Règle d'or de décision :**
- Si l'émetteur a **impérativement besoin de la réponse pour continuer** son traitement ou répondre à l'utilisateur ➔ **Synchrone (REST/HTTP)**.
- Si l'action consiste à **annoncer qu'un fait s'est produit** (notification, journalisation, traitement différé) ➔ **Asynchrone (Kafka/RabbitMQ)**.

| Interaction | Mode | Justification |
|---|---|---|
| **1. Consulter les emplacements libres** | **Synchrone (REST)** | L'utilisateur attend la liste affichée sur son smartphone en temps réel. |
| **2. Calculer le prix d'une nuitée (Réservation ➔ Tarification)** | **Synchrone (REST)** | Réservation ne peut pas finaliser le devis/panier sans connaître le montant exact. |
| **3. Relevé d'une borne vers Consommation** | **Asynchrone (MQTT / Kafka)** | Flux continu de capteurs IoT sur réseau instable ; la borne n'attend aucune réponse métier. |
| **4. Dépassement de seuil vers Notification** | **Asynchrone (Kafka)** | Événement métier. Si l'envoi de SMS/Push est lent ou en panne, la mesure reste valide. |
| **5. Fin de séjour vers Facturation** | **Asynchrone (Kafka)** | Traitement lourd (calculs, génération PDF, envoi bancaire). Le plaisancier peut quitter le port sans attendre. |

---

### Question 5 (4 pts) : Qu'est-ce que le couplage ? Réduction par bus, et disparition complète ?

**Qu'est-ce que le couplage ?** C'est le degré d'interdépendance entre deux composants logiciels :
- *Couplage spatial :* Devoir connaître l'adresse IP, le port ou l'URL de l'autre.
- *Couplage temporel :* Devoir être tous les deux actifs et disponibles au même instant.
- *Couplage de contrat / syntaxique :* Connaître le format des données et les interfaces.

**En quoi le bus d'événements le réduit-il ?**
- **Supprime le couplage spatial :** Le producteur publie sur un *Topic* abstrait, sans savoir où habitent les consommateurs.
- **Supprime le couplage temporel :** Si Facturation est arrêtée pour maintenance, les messages patientent dans Kafka.
- **Supprime le couplage d'identité :** Le producteur ne sait même pas *qui* écoute (1, 5 ou 0 service).

**Le fait-il disparaître complètement ? NON !** Il reste le **couplage de schéma / contrat** : producteur et consommateur doivent toujours s'accorder sur la structure JSON de l'événement.

---

## Partie B. Rôles, protocoles et outils

### Question 6 (3 pts) : Différence API Gateway vs Bus d'événements ? Un frontend peut-il publier directement ?

| Critère | API Gateway (ex. Spring Cloud Gateway) | Bus d'événements (ex. Apache Kafka) |
|---|---|---|
| **Rôle principal** | Point d'entrée unique Nord-Sud (extérieur vers intérieur). Achemine les requêtes HTTP. | Canal de transport Est-Ouest (entre microservices). Diffuse des flux d'événements. |
| **Style** | Synchrone (requête/réponse HTTP/REST, WebSocket). | Asynchrone (publication/abonnement, streaming). |
| **Fonctions** | Routage, authentification JWT, CORS, rate limiting. | Rétention de messages, rejeu, partitionnement. |

**Un frontend peut-il publier directement sur le bus ?**  
**En règle générale, NON (fortement déconseillé en production).** Les navigateurs ou téléphones ne doivent pas être connectés directement aux ports de Kafka (9092) pour des raisons de sécurité (exposition du cluster, pas de validation métier fine, gestion des secrets impossible sur un terminal client). Le frontend appelle l'API Gateway ou un microservice via REST/WebSocket, qui valide la requête et publie ensuite l'événement dans Kafka.

---

### Question 7 (3 pts) : Distinguer message, commande et événement. Lequel privilégier ?

- **Message :** Terme générique pour tout paquet de données transporté par un bus (enveloppe contenant des métadonnées et un payload).
- **Commande :** Intention / Ordre adressé à *un destinataire précis* pour effectuer une action (verbe à l'impératif : `CouperBorne`, `GenererFacture`). Peut être rejetée.
- **Événement :** Constat d'un *fait déjà survenu et immuable* dans le passé (verbe au participe passé : `ConsommationRelevee`, `SejourCloture`). Ne peut pas être annulé ou refusé.

**Lequel privilégier dans Smart Marina ? L'ÉVÉNEMENT (Event-Driven Architecture).**  
Car il assure le couplage le plus lâche : un microservice émetteur annonce ce qui s'est produit, sans ordonner quoi faire. De nouveaux services (ex. Reporting, Télégestion) peuvent s'abonner au flux sans modifier une seule ligne du service producteur.

---

### Question 8 (4 pts) : File (point-à-point) vs Topic (pub/sub). Combinaison dans Smart Marina

- **File (Point-à-Point) :** Un message n'est traité que par **un seul consommateur** parmi ceux qui écoutent la file. Utile pour distribuer la charge de travail (Worker Queue).
- **Topic (Publication / Abonnement) :** Le message est diffusé à **tous les abonnés** enregistrés sur le sujet.

**Choix et combinaison dans Smart Marina :**  
On choisit un **Topic Kafka** (ex. `consumption-events`), combiné avec le mécanisme des **Consumer Groups** :
1. Le message est diffusé à la fois au groupe `notification-group` ET au groupe `facturation-group` (**modèle Topic / Pub-Sub**).
2. À l'intérieur de `notification-group`, si on lance 3 instances du microservice, chaque relevé n'est traité que par une seule instance (**modèle File / Point-à-point**).

---

### Question 9 (4 pts) : Protocoles dans Smart Marina (au moins 5) et chaîne complète depuis la borne

1. **MQTT / CoAP :** Protocole IoT ultra-léger et adapté aux réseaux instables pour faire remonter les mesures de la borne.
2. **HTTP / REST (JSON) :** Pour les échanges synchrones entre frontends, API Gateway et services métier.
3. **Kafka Protocol (TCP binaire) :** Pour l'échange asynchrone haut débit et persistant entre microservices.
4. **WebSocket / SSE :** Pour notifier en direct le navigateur ou l'application mobile du plaisancier.
5. **JDBC / PostgreSQL Protocol :** Pour les requêtes SQL internes entre chaque microservice et sa base PostgreSQL.

**Chaîne complète depuis la borne :**  
`Borne physique` ➔ *(MQTT)* ➔ `Passerelle IoT / Broker MQTT` ➔ *(REST ou Kafka)* ➔ `consumption-service` ➔ *(JDBC)* ➔ `PostgreSQL (5435)` ➔ *(Kafka TCP)* ➔ `Broker Kafka (Topic: consumption-events)` ➔ `notification-service` ➔ *(WebSocket/Firebase)* ➔ `Smartphone du Plaisancier`.

---

### Question 10 (4 pts) : Comparaison Kafka vs RabbitMQ (3 critères) + Choix pour Smart Marina

| Critère | Apache Kafka | RabbitMQ |
|---|---|---|
| **Modèle & Persistance** | **Journal d'événements distribué (Log-centric)** : messages conservés sur disque pendant X jours, même après lecture. Permet le rejeu. | **File de messages (Queue-centric)** : messages supprimés dès qu'ils sont acquittés (ACK) par le consommateur. |
| **Capacité & Débit** | Extrêmement élevé (millions de messages/seconde), passage à l'échelle par partitionnement horizontal. | Très rapide mais limité par la mémoire vive lors des pics massifs. |
| **Routage** | Routage simple par nom de topic et clé de partition. | Routage très riche et flexible (Direct, Topic, Fanout, Headers Exchanges). |

**Justification du choix pour Smart Marina :**  
**Apache Kafka** est le choix idéal car les relevés de consommation constituent un flux chronologique continu (streaming de données IoT). La persistance de Kafka permet le **rejeu historique** (indispensable si le service de facturation a un bug et doit recalculer le mois passé, ou pour alimenter un nouveau service d'analyse de données).

---

### Question 11 (4 pts) : Topic, Partition, Offset, Clé, Consumer Group + Choix de clé des bornes

- **Topic :** La catégorie logique dans laquelle les messages sont publiés (ex. : `consumption-events`).
- **Partition :** Sous-division ordonnée et immuable d'un topic permettant le parallélisme sur plusieurs disques/serveurs.
- **Offset :** Identifiant séquentiel unique attribué à chaque message au sein d'une partition, servant de curseur de lecture.
- **Clé (Message Key) :** Donnée optionnelle utilisée pour déterminer dans quelle partition le message doit être stocké.
- **Consumer Group :** Ensemble d'instances de consommateurs coopérant pour lire un topic. Chaque partition n'est attribuée qu'à un seul membre du groupe.

**Choix de clé pour les relevés de bornes : `pedestalId`.**  
**Conséquence fondamentale :** Kafka applique un hachage sur la clé (`hash(pedestalId) % nb_partitions`). Tous les relevés d'une même borne sont garantis d'être enregistrés dans la **même partition**, garantissant un **ordre chronologique strict** de traitement pour chaque borne.

---

## Partie C. Fiabilité, cohérence et pièges

### Question 12 (4 pts) : Garanties de livraison + Choix pour Facturation et précaution

1. **Au plus une fois (At-most-once) :** Le message est envoyé sans garantie d'arrivée. Zéro doublon mais risque de perte (relevés météo non critiques).
2. **Au moins une fois (At-least-once) :** Le message est renvoyé tant qu'aucun accusé de réception (ACK) n'est confirmé. Aucune perte, mais risque de doublons.
3. **Exactement une fois (Exactly-once) :** Garantie stricte sans perte ni doublon (très coûteuse et limitée souvent à l'écosystème Kafka Streams).

**Choix pour Facturation :** **Au moins une fois (At-least-once)**.  
**Précaution obligatoire :** Le consommateur de Facturation doit être **strictement idempotent** (ex. mémoriser les identifiants d'événements déjà facturés) pour ne jamais prélever deux fois le compte d'un plaisancier si un message est délivré deux fois.

---

### Question 13 (4 pts) : Qu'est-ce que l'idempotence, pourquoi est-elle vitale ? Comment l'implémenter ?

**Définition :** Une opération est idempotente si l'exécuter plusieurs fois de suite avec les mêmes paramètres produit exactement le même effet que de l'exécuter une seule fois.

**Pourquoi est-elle vitale en asynchrone ?**  
En réseau distribué, les pannes de timeout forcent les renvois (retries). Si un consommateur traite un message mais que le réseau coupe avant l'ACK, Kafka lui renverra le message. Sans idempotence, le client sera facturé deux fois ou recevra deux SMS.

**Comment l'implémenter ?**
1. **Clé d'idempotence (Idempotency Key / Event ID) :** Chaque événement possède un identifiant unique universel (UUID).
2. **Table de déduplication :** Avant de faire le traitement, le service vérifie dans une table SQL (ex. `processed_events(event_id)`) si l'UUID existe déjà.
3. Si oui ➔ Il ignore le traitement et valide l'ACK immédiatement.
4. Si non ➔ Il effectue le traitement et insère l'UUID dans la même transaction locale.

---

### Question 14 (5 pts) : Problème de la « double écriture » + Pattern Transactional Outbox

**Le problème :** Un service doit écrire dans sa base PostgreSQL ET publier un événement dans Kafka :
```java
repository.save(consumption); // Étape 1 : OK (en base)
// CRASH MACHINE ICI !
kafkaTemplate.send("topic", event); // Étape 2 : JAMAIS EXÉCUTÉE !
```
PostgreSQL et Kafka sont deux systèmes distincts ne partageant pas de transaction atomique commune.

**Solution : Le Pattern Transactional Outbox**
1. On crée une table `outbox_events` dans la base PostgreSQL du microservice.
2. Dans **une seule transaction SQL locale**, le service insère la consommation ET insère l'événement dans la table `outbox_events`. Si l'un échoue, tout est annulé.
3. Un processus tiers (ex. Debezium CDC ou batch) lit `outbox_events` et publie les messages dans Kafka, puis marque la ligne comme publiée.

**Limites de l'Outbox :** Nécessite un composant supplémentaire, génère des écritures supplémentaires en base de données, et délivre les messages en *At-least-once* (impose l'idempotence côté consommateur).

---

### Question 15 (3 pts) : Garantir l'ordre des événements + Ce qu'on ne peut pas supposer

**Comment garantir l'ordre :**
- L'ordre est garanti **uniquement au sein d'une même partition**.
- Il faut obligatoirement affecter une **clé métier consistante** (ex. `pedestalId`) pour que tous les événements d'une même borne aillent sur la même partition.
- Côté producteur, activer `enable.idempotence=true` et `max.in.flight.requests.per.connection=1` (ou 5 avec idempotence).

**Ce qu'on ne peut JAMAIS supposer :**
- Deux messages envoyés sur **deux partitions distinctes** n'arriveront pas forcément dans l'ordre de leur émission.
- Les horloges système (timestamp) ne sont jamais parfaitement synchronisées entre serveurs distincts.

---

### Question 16 (4 pts) : Gestion des échecs d'un consommateur + Stratégie complète

**Distinction des types d'erreurs :**
- **Erreurs transitoires (récupérables) :** Coupure réseau temporaire, base momentanément occupée ➔ Rejouer le message.
- **Erreurs fatales (non récupérables / "Poison Pills") :** JSON corrompu, division par zéro, violation de contrainte métier ➔ Rejouer en boucle bloquera la partition indéfiniment.

**Stratégie complète de résilience :**
1. **Retry avec Backoff exponentiel :** Réessayer après 1s, 5s, 30s pour laisser le temps au réseau de se rétablir.
2. **Dead Letter Queue (DLQ / Topic des morts) :** Après le nombre maximal d'essais (ex. 3), le message est routé vers `consumption-events-dlq`.
3. **Alerte et intervention humaine :** La partition principale continue son travail sans être bloquée, et les équipes d'exploitation peuvent analyser les messages dans la DLQ.

---

### Question 17 (5 pts) : Cohérence éventuelle ? Pattern Saga ? Scénario Smart Marina avec compensation

**Cohérence éventuelle (Eventual Consistency) :** Modèle où les différentes bases de données ne sont pas synchronisées à la même milliseconde, mais convergent vers un état cohérent après un certain délai.

**Pattern Saga :** Séquence de transactions locales au sein de chaque microservice. Chaque transaction met à jour la base locale et publie un événement déclenchant l'étape suivante. Si une étape échoue, la Saga exécute des **transactions de compensation** (annulation logique).

**Scénario Smart Marina « Réservation avec paiement » :**
1. *Étape 1 (Service Réservation) :* Crée la réservation à l'état `EN_ATTENTE_PAIEMENT` et émet `ReservationCreee`.
2. *Étape 2 (Service Emplacements) :* Bloque l'emplacement de quai et émet `EmplacementBloque`.
3. *Étape 3 (Service Paiement) :* Tente de débiter la carte bancaire ➔ **ÉCHEC (solde insuffisant)** ➔ émet `PaiementEchoue`.
4. **Transactions de compensation :**
   - Le Service Emplacements reçoit `PaiementEchoue` ➔ Débloque le quai (état `DISPONIBLE`).
   - Le Service Réservation reçoit `PaiementEchoue` ➔ Passe la réservation à `ANNULEE` et notifie le plaisancier.

---

### Question 18 (3 pts) : Versionnement d'événement ? Enveloppe standard

**Pourquoi ?** Permet au système d'évoluer (ajout/suppression de champs) sans casser les anciens consommateurs déjà déployés (compatibilité ascendante et descendante).

**Enveloppe standard (format CloudEvents) :**
```json
{
  "specversion": "1.0",
  "id": "e84b2a30-8a12-4f8a-9817-21a4f910b831",
  "type": "com.xyz.smartmarina.consumption.recorded",
  "source": "/pedestals/3",
  "time": "2026-11-19T08:30:00Z",
  "datacontenttype": "application/json",
  "schemaversion": "2.0",
  "data": {
    "pedestalId": 3,
    "type": "ELECTRICITY",
    "value": 75.5,
    "unit": "kWh"
  }
}
```

---

## Partie D. Implémentation avec Spring Boot

### Question 19 (4 pts) : Structure JSON de l'événement ConsommationReleve & Justification

```json
{
  "eventId": "b9f2c694-5cb4-42b3-90d2-97217594bf32",
  "eventType": "ConsommationRelevee",
  "timestamp": "2026-11-19T08:45:00Z",
  "pedestalId": 3,
  "portId": 1,
  "type": "ELECTRICITY",
  "value": 75.50,
  "unit": "kWh"
}
```

**Justification des choix :**
- `eventId` (UUID) : Vital pour permettre la déduplication et assurer l'**idempotence** côté consommateur.
- `timestamp` (ISO 8601 UTC) : Horodatage précis de la mesure pour la facturation et le reporting.
- `pedestalId` : Identifiant de la borne, **choisi comme clé de message Kafka** pour garantir l'ordre séquentiel.
- `portId` : Évite aux consommateurs de devoir rappeler le service borne pour savoir à quel port elle appartient.
- `value` & `unit` : Décimal précis et unité explicite pour éviter toute confusion.

---

### Question 20 (4 pts) : Publication Spring Kafka de ConsommationReleve & Paramètres de fiabilité

```java
@Service
public class ConsumptionEventProducer {

    private final KafkaTemplate<String, ConsumptionEvent> kafkaTemplate;

    public ConsumptionEventProducer(KafkaTemplate<String, ConsumptionEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(ConsumptionEvent event) {
        String key = String.valueOf(event.pedestalId());
        // Envoi asynchrone non-bloquant retournant un CompletableFuture
        CompletableFuture<SendResult<String, ConsumptionEvent>> future = 
                kafkaTemplate.send("consumption-events", key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                // Succès : log partition et offset
            } else {
                // Échec : alerter ou journaliser l'erreur
            }
        });
    }
}
```

**Paramètres de fiabilité du producteur (`application.properties`) :**
- `acks=all` : Le broker n'acquitte que lorsque tous les réplicas synchronisés ont persisté le message (zéro perte).
- `retries=Integer.MAX_VALUE` : Réessayer automatiquement en cas d'erreur réseau transitoire.
- `enable.idempotence=true` : Garantit que les retries ne produiront aucun doublon dans la partition Kafka.

---

### Question 21 (5 pts) : Consommateur idempotent pour le service Facturation

```java
@Component
public class BillingEventListener {

    private final ProcessedEventRepository eventRepo;
    private final BillingService billingService;

    public BillingEventListener(ProcessedEventRepository eventRepo, BillingService billingService) {
        this.eventRepo = eventRepo;
        this.billingService = billingService;
    }

    @KafkaListener(topics = "consumption-events", groupId = "facturation-group")
    @Transactional
    public void onConsumptionReceived(ConsumptionEvent event) {
        // 1. Vérification d'idempotence
        if (eventRepo.existsById(event.eventId())) {
            return; // Déjà traité : on ignore
        }

        // 2. Traitement métier
        billingService.applyConsumption(event);

        // 3. Enregistrement de l'événement traité dans la même transaction
        eventRepo.save(new ProcessedEvent(event.eventId(), LocalDateTime.now()));
    }
}
```

- `groupId = "facturation-group"` : Assure que le service de facturation maintient son propre curseur (offset) indépendant de celui des notifications.
- `@Transactional` : Lie la mise à jour de la facture et l'insertion de l'id d'événement dans une transaction atomique.

---

### Question 22 (3 pts) : Tester un flux asynchrone (Outils & 4 cas impératifs)

**Outils :** `@EmbeddedKafka` (courtier Kafka en mémoire pour les tests unitaires Spring), **Testcontainers** (véritable conteneur Docker Kafka pour les tests d'intégration), et **Awaitility** (assertions avec temporisation asynchrone).

**4 cas à tester impérativement :**
1. **Cas nominal :** L'événement est publié, le consommateur le reçoit et met à jour sa base avec succès.
2. **Cas du doublon (Idempotence) :** Envoyer deux fois le même événement avec le même `eventId` et vérifier que le montant n'est facturé qu'une seule fois.
3. **Cas de l'erreur transitoire :** Simuler une indisponibilité temporaire et vérifier le mécanisme de retry avec succès.
4. **Cas du message empoisonné (DLQ) :** Envoyer un message corrompu et vérifier qu'il atterrit dans la Dead Letter Queue sans bloquer la partition.

---

### Question 23 (3 pts) : Traçabilité distribuée & Métriques

**Comment suivre un flux à travers plusieurs services ?**  
Grâce à la **Traçabilité Distribuée (Distributed Tracing)** via **Micrometer Tracing** (avec OpenTelemetry ou Zipkin) :
- `TraceId` : Identifiant unique généré à l'entrée (API Gateway) et propagé dans les en-têtes HTTP (`traceparent`) et les en-têtes de messages Kafka (Kafka Headers). Il permet de corréler tous les logs de tous les services pour une même requête.
- `SpanId` : Identifiant de chaque étape de traitement individuel.

**Métriques essentielles à surveiller :**
1. **Consumer Lag :** Nombre de messages en attente dans Kafka non encore traités par le consommateur (indicateur n°1 de saturation).
2. **Débit (Throughput) :** Nombre de messages/seconde produits et consommés.
3. **Taux d'erreur / Messages DLQ :** Nombre d'échecs de désérialisation ou de rejets métier.

---

### Question 24 (3 pts) : Comment sécuriser le bus et les échanges entre services ?

1. **Chiffrement en transit (TLS / mTLS) :** Toutes les communications avec Kafka sont chiffrées en TLS (port 9093 avec certificats SSL).
2. **Authentification des clients Kafka :** Utilisation de certificats clients mutuels (mTLS) ou identifiants sécurisés via **SASL/SCRAM-SHA-512** ou **OAuth2 / OIDC**.
3. **Autorisations strictes (ACLs Kafka) :** Seul `consumption-service` peut écrire sur `consumption-events`, et `notification-service` n'a qu'un droit de lecture (READ).
4. **Sécurité des échanges REST internes :** Utilisation de tokens **JWT** propagés depuis l'API Gateway ou mTLS entre microservices.

---

### Question 25 (3 pts) : Panne du service Notification lors d'une réservation (Synchrone vs Asynchrone)

**Dans une architecture 100% synchrone :**
- Le service Réservation appelle Notification via REST. Comme Notification est en panne, la requête tombe en timeout (erreur 500 / 504).
- **Conséquence :** La réservation échoue pour le client, alors que le quai était libre et le paiement validé !

**Dans une architecture asynchrone :**
- Réservation valide le quai, encaisse et publie `ReservationConfirmee` dans Kafka.
- La réservation **réussit immédiatement** (201 Created). Le message attend dans Kafka et sera dépilé dès que Notification redémarre.

**Protections indispensables côté synchrone :**
1. **Circuit Breaker (Resilience4j) :** Coupe les appels vers Notification dès qu'il détecte des pannes répétées.
2. **Timeouts courts :** Définir un timeout strict (ex. 1.5s) pour ne pas bloquer les threads du serveur.
3. **Fallback gracieux :** En cas d'échec de notification, terminer la réservation quand même au lieu de bloquer l'utilisateur.

---

## Partie E. Synthèse et validation du projet

### Question 26 (5 pts) : Scénario complet « De la borne à la facture »

| Étape | Acteurs & Interaction | Mode | Protocole | Événement / Payload | Garantie de fiabilité |
|---|---|---|---|---|---|
| **1. Mesure** | Borne physique ➔ Passerelle IoT | Asynchrone | MQTT | Payload télémétrie : `{borne:3, kWh:75.5}` | QoS 1 (At least once) |
| **2. Réception** | Passerelle ➔ `consumption-service` | Synchrone | HTTP REST | `POST /api/v1/consumptions` | Code HTTP 201 Created |
| **3. Validation** | `consumption-service` ➔ `pedestal-service` | Synchrone | HTTP REST | `GET /api/v1/pedestals/3` | Vérification présence physique |
| **4. Persistance** | `consumption-service` ➔ PostgreSQL | Synchrone | JDBC / SQL | `INSERT INTO consumptions...` | Transaction ACID locale |
| **5. Publication** | `consumption-service` ➔ Broker Kafka | Asynchrone | Kafka TCP | Événement : `ConsommationRelevee` (clé: "3") | `acks=all`, idempotent producer |
| **6. Alerte** | Broker Kafka ➔ `notification-service` | Asynchrone | Kafka TCP | Lecture du topic `consumption-events` | Consumer Group `notification-group` |
| **7. Facturation** | Broker Kafka ➔ `facturation-service` | Asynchrone | Kafka TCP | Calcul montant et débit du compte | Table de déduplication (Idempotence) |

---

### Question 27 (3 pts) : Cinq erreurs fréquentes de conception asynchrone et leurs conséquences

1. **Oublier l'idempotence côté consommateur :**  
   *Conséquence :* En cas de retry réseau ou rebalance Kafka, les messages délivrés deux fois provoqueront des facturations doubles.
2. **Mettre de la logique métier synchrone dans l'API Gateway :**  
   *Conséquence :* L'API Gateway devient un goulot d'étranglement monolithique lourd et fragile, violant le principe de responsabilité unique.
3. **Ne pas définir de clé de partitionnement sur un flux séquentiel :**  
   *Conséquence :* Les événements d'une même borne sont répartis au hasard sur plusieurs partitions. Le consommateur reçoit le relevé de fin de journée avant celui du matin.
4. **Absence de Dead Letter Queue (DLQ) pour les messages empoisonnés :**  
   *Conséquence :* Dès qu'un message corrompu arrive, le consommateur plante en boucle, bloquant la partition pour tous les messages suivants.
5. **Utiliser Kafka pour des requêtes où l'utilisateur attend une réponse immédiate :**  
   *Conséquence :* Complexité extrême de corrélation asynchrone (Request-Reply) et temps de réponse inacceptable pour l'utilisateur.
