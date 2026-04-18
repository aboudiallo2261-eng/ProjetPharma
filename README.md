# 🏥 VetPharma — Système de Gestion de Pharmacie Vétérinaire

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17.0.6-blue.svg)](https://openjfx.io/)
[![Hibernate](https://img.shields.io/badge/Hibernate-6.4-green.svg)](https://hibernate.org/)
[![Build](https://img.shields.io/badge/build-passing-brightgreen.svg)]()
[![Tests](https://img.shields.io/badge/tests-4%20passing-brightgreen.svg)]()

> Application desktop de gestion de pharmacie vétérinaire, conçue pour offrir un workflow professionnel complet : de l'approvisionnement fournisseur à la caisse, en passant par la traçabilité des stocks et la clôture journalière.

---

## 📋 Table des Matières

1. [Fonctionnalités](#fonctionnalités)
2. [Architecture](#architecture)
3. [Stack Technique](#stack-technique)
4. [Installation & Lancement](#installation--lancement)
5. [Tests Unitaires](#tests-unitaires)
6. [Décisions Architecturales Clés](#décisions-architecturales-clés)
7. [Structure du Projet](#structure-du-projet)
8. [Sécurité & Accès](#sécurité--accès)

---

## 🚀 Fonctionnalités

### Module Caisse (Ventes)
- ✅ Interface de vente ultra-rapide avec recherche produit par code-barres ou nom
- ✅ Déconditionnement unitaire des boîtes (vente à l'unité)
- ✅ Paiement mixte (Espèces + Mobile Money) en une seule transaction
- ✅ Impression de ticket thermique (via `PrinterService`)
- ✅ **Protection concurrentielle JAT** (Just-In-Time) : rejet automatique si deux agents vendent le dernier flacon simultanément
- ✅ Session de caisse avec fond de démarrage et clôture sécurisée

### Module Approvisionnement (Achats)
- ✅ Enregistrement de commandes fournisseurs en **ACID transactionnel** complet
- ✅ Gestion des lots (numéro, date d'expiration, quantité)
- ✅ Réapprovisionnement automatique d'un lot existant (fusion intelligente)
- ✅ Génération de bon de commande PDF
- ✅ **Alertes de rupture de stock** intelligentes lors de la saisie

### Module Rapports (Livre de Bord)
- ✅ Journal des ventes avec filtrage multi-critères (Date, Caissier, Catégorie, Espèce)
- ✅ **Filtrage Server-Side via HQL** : aucune donnée superflue en RAM
- ✅ Audit Trail complet des mouvements de stock
- ✅ Export PDF et Excel (CSV) des rapports
- ✅ Suivi des clôtures de caisse avec calcul des écarts

### Module Sécurité & Administration
- ✅ Authentification BCrypt (hachage robuste des mots de passe)
- ✅ Système de profils RBAC (Super-Admin, Caissier, Gestionnaire)
- ✅ Gestion des droits d'accès par vue FXML
- ✅ Sauvegarde automatique de la base de données

---

## 🏛️ Architecture

Le projet suit le patron **MVC strict** enrichi d'une couche **Service** pour isoler la logique métier :

```
┌─────────────────────────────────────────────────────┐
│                    UI Layer (FXML + CSS)             │
│  ventes.fxml │ achats.fxml │ rapports.fxml │ ...    │
└──────────────────────┬──────────────────────────────┘
                       │ Events FXML (@FXML)
┌──────────────────────▼──────────────────────────────┐
│              Controllers Layer (JavaFX)              │
│  VenteController │ AchatController │ ReportController│
│  (UI Logic + Orchestration uniquement)              │
└──────────┬──────────────────────┬───────────────────┘
           │ Appels métier        │ Lecture/Écriture DAO
┌──────────▼─────────┐    ┌───────▼───────────────────┐
│   Services Layer   │    │       DAO Layer            │
│  VenteService      │    │  GenericDAO<T>             │
│  AchatService      │    │  LigneVenteDAO             │
│  (Logique ACID)    │    │  LotDAO │ MouvementDAO     │
└────────────────────┘    └───────────────────────────┘
                                      │ Hibernate ORM
                          ┌───────────▼───────────────┐
                          │      MySQL Database        │
                          │  (18 tables gérées par    │
                          │   Hibernate auto-DDL)     │
                          └───────────────────────────┘
```

### Pattern DAO Générique

La couche de persistance est structurée autour d'un `GenericDAO<T>` paramétrique :

```java
// Toutes les entités héritent de ce comportement commun via GenericDAO<T>
GenericDAO<Produit> produitDAO = new GenericDAO<>(Produit.class);
produitDAO.save(produit);         // INSERT
produitDAO.update(produit);       // UPDATE (Hibernate merge)
produitDAO.findById(1L);          // SELECT par ID
produitDAO.findAll();             // SELECT *
produitDAO.delete(produit);       // DELETE sécurisé
```

Les DAOs spécialisés (ex: `LigneVenteDAO`) **étendent** ce genericDAO pour ajouter leurs propres requêtes HQL métier.

---

## 💻 Stack Technique

| Couche | Technologie | Version | Rôle |
|---|---|---|---|
| **UI** | JavaFX + FXML | 17.0.6 | Interface graphique déclarative |
| **ORM** | Hibernate | 6.4.4 | Mapping Objet-Relationnel + Gestion Session |
| **BDD** | MySQL | 8.0 | Base de données relationnelle de production |
| **Sécurité** | jBCrypt | 0.4 | Hachage des mots de passe (non-réversible) |
| **PDF** | Apache PDFBox | 3.0.1 | Génération de tickets et rapports PDF |
| **Logging** | SLF4J + Logback | 2.0.12 / 1.5.0 | Logging structuré en production |
| **Build** | Maven Wrapper | 3.x | Gestion des dépendances et du cycle de vie |
| **Tests** | JUnit 5 + Mockito | 5.10.2 / 5.11.0 | Tests unitaires avec isolation des DAOs |
| **Assertions** | AssertJ | 3.25.3 | Assertions fluides et lisibles |
| **Mapping** | Lombok | 1.18.30 | Réduction du boilerplate (getters/setters) |

---

## ⚙️ Installation & Lancement

### Prérequis
- **JDK 17+** (OpenJDK ou Oracle JDK)
- **MySQL 8.0+** en cours d'exécution
- Accès internet pour le premier téléchargement des dépendances Maven

### Configuration de la Base de Données

Créez la base et configurez le fichier Hibernate :

```sql
CREATE DATABASE pharmacie_vet CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

Modifiez `src/main/resources/hibernate.cfg.xml` :
```xml
<property name="hibernate.connection.url">
    jdbc:mysql://localhost:3306/pharmacie_vet?useSSL=false&serverTimezone=UTC
</property>
<property name="hibernate.connection.username">votre_user</property>
<property name="hibernate.connection.password">votre_mdp</property>
```

### Lancement

```bash
# Compiler et lancer l'application
.\mvnw clean compile javafx:run

# Uniquement les tests unitaires (sans lancer l'UI)
.\mvnw test
```

Au **premier lancement**, Hibernate crée automatiquement toutes les tables (`ddl-auto=update`) et `SecuritySeeder` initialise les profils et le super-administrateur par défaut.

**Identifiants par défaut :**
| Identifiant | Mot de passe |
|---|---|
| `admin` | `admin` |

> ⚠️ **Changez le mot de passe par défaut dès le premier lancement en production.**

---

## 🧪 Tests Unitaires

Le projet comprend une suite de tests unitaires couvrant les couches les plus critiques (logique métier et session). Les DAOs sont **isolés avec Mockito** : aucun accès base de données n'est requis pour exécuter les tests.

```bash
.\mvnw test
```

**Résultats attendus :**
```
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS
```

### Couverture des Tests

| Classe Testée | Scénarios Couverts |
|---|---|
| `VenteService` | Rejet d'un panier vide |
| `VenteService` | **Détection de concurrence** : rejet si stock modifié entre affichage et encaissement |
| `SessionManager` | Login (persistence de l'utilisateur connecté) |
| `SessionManager` | Logout (nettoyage de la session) |

### Cas de Test Clé — Condition de Course (Race Condition)

Le test `testValiderVenteRejetteConcurrence` valide un scénario critique en production : deux agents tentent de vendre le **même dernier flacon** au même moment. Le système doit détecter ce conflit et protéger l'intégrité du stock :

```java
// Cache de stock affiché à l'écran = 5 unités
cacheDispo.put(produitId, 5);

// Mais la vente en cours demande 10 unités → Race Condition détectée !
assertThatThrownBy(() -> venteService.validerVente(panier, ...))
    .hasMessageContaining("ALERTE MAJEURE DE CONCURRENCE");
```

---

## 🔑 Décisions Architecturales Clés

### 1. Filtrage Server-Side des Rapports (P2.C)
**Problème :** Le naïf `findAll().stream().filter()` charge toutes les lignes de vente (potentiellement des dizaines de milliers) en RAM avant de filtrer.

**Solution :** `LigneVenteDAO.rechercherLignesVente()` construit une requête **HQL dynamique** qui délègue le filtrage à MySQL. La RAM utilisée est proportionnelle au résultat, pas à la taille de la table.

```java
// Avant : O(N) en RAM — dangereux en production
List<LigneVente> all = ligneVenteDAO.findAllWithDetails();
all.stream().filter(lv -> ...).collect(toList()); // N = toute la table

// Après : O(résultat) — scalable
ligneVenteDAO.rechercherLignesVente(start, end, userId, catId, espId);
```

### 2. Logging Professionnel SLF4J/Logback (P2.B)
Tous les `System.out.println` et `e.printStackTrace()` ont été remplacés par **SLF4J**. Les logs sont horodatés, catégorisés par niveau (`INFO`, `WARN`, `ERROR`) et configurés via `logback.xml` pour une rotation de fichiers en production.

```java
// ❌ Avant (amateur)
System.out.println("Vente validée !");
e.printStackTrace();

// ✅ Après (industriel)
logger.info("Vente (ID: {}) validée — Montant: {} FCFA", vente.getId(), total);
logger.error("Erreur transaction ACID — Rollback effectué", e);
```

### 3. Transaction ACID pour les Achats (AchatService)
L'enregistrement d'une commande fournisseur implique 5 opérations en base (Achat, Lots, LignesAchat, MouvementStock, MàJ Prix). En cas d'échec à n'importe quelle étape, un **rollback complet** garantit qu'aucune donnée partielle n'est écrite.

### 4. Protection Concurrentielle à la Caisse (VenteService)
Un cache client-side (`Map<Long, Integer>`) est constitué au chargement de l'écran de vente. Juste avant l'encaissement, le service **revérifie** les quantités requises contre ce cache. Si le stock a évolué entre-temps (autre vente en cours), la transaction est rejetée avec un message explicite.

---

## 📁 Structure du Projet

```
src/
├── main/
│   ├── java/com/pharmacie/
│   │   ├── controllers/     # Couche UI (11 controllers JavaFX)
│   │   ├── dao/             # Couche Persistance (GenericDAO + DAOs spécialisés)
│   │   ├── models/          # Entités JPA/Hibernate (15 entités)
│   │   ├── services/        # Logique Métier ACID (VenteService, AchatService)
│   │   ├── utils/           # Services transversaux (PDF, Print, Logging, Session)
│   │   └── MainApp.java     # Point d'entrée JavaFX
│   └── resources/
│       ├── fxml/            # Vues FXML (12 fichiers)
│       ├── css/             # Feuilles de style
│       ├── logback.xml      # Configuration du logging
│       └── hibernate.cfg.xml
└── test/
    └── java/com/pharmacie/
        ├── services/VenteServiceTest.java   # Tests Mockito
        └── utils/SessionManagerTest.java   # Tests Session
```

---

## 🔐 Sécurité & Accès

| Mécanisme | Implémentation |
|---|---|
| **Hachage des mots de passe** | jBCrypt (irréversible, salté automatiquement) |
| **Contrôle d'accès** | RBAC par profil : `SUPER-ADMIN`, `CAISSIER`, `GESTIONNAIRE` |
| **Vérification des permissions** | `SessionManager.hasPermission(fxmlView)` avant chaque navigation |
| **Session de caisse** | Ouverture/Fermeture traçée avec fond, totaux et écarts calculés |
| **Audit Trail** | Chaque mouvement de stock (vente, achat, ajustement) est loggué en base |

---

*Projet développé avec Java 17, JavaFX et Hibernate 6 — Architecture MVC + Service Pattern.*
