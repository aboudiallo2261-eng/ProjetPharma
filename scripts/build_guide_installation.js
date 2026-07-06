// Génère docs/GUIDE_INSTALLATION_CLIENT.docx — guide imprimable pour installer VetPharma chez un client.
const fs = require("fs");
const path = require("path");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell, ImageRun,
  Header, Footer, AlignmentType, LevelFormat, HeadingLevel, BorderStyle,
  WidthType, ShadingType, PageNumber, PageBreak, TableOfContents, TabStopType, TabStopPosition,
} = require("docx");

const ROOT = path.resolve(__dirname, "..");
const VERT = "059669";
const SLATE = "334155";
const GRIS_CLAIR = "F1F5F9";

const border = { style: BorderStyle.SINGLE, size: 1, color: "CBD5E1" };
const borders = { top: border, bottom: border, left: border, right: border };
const cellMargins = { top: 80, bottom: 80, left: 120, right: 120 };

const h1 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_1, children: [new TextRun(t)] });
const h2 = (t) => new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun(t)] });
const p = (t, opts = {}) => new Paragraph({ spacing: { after: 120 }, children: [new TextRun({ text: t, ...opts })] });
const bullet = (t, opts = {}) => new Paragraph({ numbering: { reference: "puces", level: 0 }, spacing: { after: 60 }, children: [new TextRun({ text: t, ...opts })] });
const etape = (t) => new Paragraph({ numbering: { reference: "etapes", level: 0 }, spacing: { after: 80 }, children: [new TextRun(t)] });
const code = (t) => new Paragraph({
  spacing: { after: 60 }, shading: { fill: "1E293B", type: ShadingType.CLEAR },
  children: [new TextRun({ text: t, font: "Consolas", size: 18, color: "E2E8F0" })],
});
const attention = (t) => new Paragraph({
  spacing: { after: 120 }, shading: { fill: "FEF3C7", type: ShadingType.CLEAR },
  children: [new TextRun({ text: "⚠ " + t, bold: true, color: "92400E" })],
});
const astuce = (t) => new Paragraph({
  spacing: { after: 120 }, shading: { fill: "D1FAE5", type: ShadingType.CLEAR },
  children: [new TextRun({ text: "✔ " + t, color: "065F46" })],
});

function tableau(lignes, largeurs) {
  return new Table({
    width: { size: 9026, type: WidthType.DXA },
    columnWidths: largeurs,
    rows: lignes.map((cellules, i) => new TableRow({
      children: cellules.map((texte, j) => new TableCell({
        borders, width: { size: largeurs[j], type: WidthType.DXA },
        shading: i === 0 ? { fill: "D1FAE5", type: ShadingType.CLEAR } : undefined,
        margins: cellMargins,
        children: [new Paragraph({ children: [new TextRun({ text: texte, bold: i === 0, size: 20 })] })],
      })),
    })),
  });
}

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: "Arial", color: VERT },
        paragraph: { spacing: { before: 320, after: 200 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: "Arial", color: SLATE },
        paragraph: { spacing: { before: 240, after: 140 }, outlineLevel: 1 } },
    ],
  },
  numbering: {
    config: [
      { reference: "puces", levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "etapes", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "etapes2", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "etapes3", levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "checklist", levels: [{ level: 0, format: LevelFormat.BULLET, text: "☐", alignment: AlignmentType.LEFT, style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ],
  },
  sections: [
    // ═══ COUVERTURE ═══
    {
      properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
      children: [
        new Paragraph({ spacing: { before: 2400 }, alignment: AlignmentType.CENTER, children: [
          new ImageRun({ type: "png", data: fs.readFileSync(path.join(ROOT, "src/main/resources/images/logo_256.png")),
            transformation: { width: 180, height: 180 },
            altText: { title: "Logo", description: "Logo VetPharma Kaoural", name: "logo" } }),
        ]}),
        new Paragraph({ spacing: { before: 400 }, alignment: AlignmentType.CENTER, children: [
          new TextRun({ text: "VetPharma", bold: true, size: 72, color: VERT })] }),
        new Paragraph({ alignment: AlignmentType.CENTER, children: [
          new TextRun({ text: "Gestion de Pharmacie Vétérinaire", size: 32, color: SLATE })] }),
        new Paragraph({ spacing: { before: 800 }, alignment: AlignmentType.CENTER, children: [
          new TextRun({ text: "GUIDE D’INSTALLATION", bold: true, size: 44 })] }),
        new Paragraph({ alignment: AlignmentType.CENTER, children: [
          new TextRun({ text: "Installation complète sur le poste du client", size: 26, color: SLATE })] }),
        new Paragraph({ spacing: { before: 1600 }, alignment: AlignmentType.CENTER, children: [
          new TextRun({ text: "Version 1.0 — Juillet 2026", size: 22, color: "64748B" })] }),
      ],
    },
    // ═══ CONTENU ═══
    {
      properties: { page: { size: { width: 11906, height: 16838 }, margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 } } },
      headers: { default: new Header({ children: [new Paragraph({
        tabStops: [{ type: TabStopType.RIGHT, position: TabStopPosition.MAX }],
        border: { bottom: { style: BorderStyle.SINGLE, size: 4, color: VERT, space: 4 } },
        children: [new TextRun({ text: "VetPharma — Guide d’installation", size: 18, color: "64748B" }),
                   new TextRun({ text: "\tKAOURAL Clinique Pharmacie", size: 18, color: "64748B" })] })] }) },
      footers: { default: new Footer({ children: [new Paragraph({ alignment: AlignmentType.CENTER,
        children: [new TextRun({ text: "Page ", size: 18, color: "64748B" }),
                   new TextRun({ children: [PageNumber.CURRENT], size: 18, color: "64748B" })] })] }) },
      children: [
        new TableOfContents("Sommaire", { hyperlink: true, headingStyleRange: "1-2" }),
        new Paragraph({ children: [new PageBreak()] }),

        // 1. AVANT DE COMMENCER
        h1("1. Avant de commencer"),
        h2("1.1 Ce qu’il faut apporter"),
        bullet("La clé USB contenant le dossier « VetPharma » (application complète, ~200 Mo)"),
        bullet("Le fichier modèle config.properties.example"),
        bullet("Ce guide imprimé"),
        h2("1.2 Prérequis de la machine du client"),
        tableau([
          ["Élément", "Minimum requis"],
          ["Système", "Windows 10 ou 11 (64 bits)"],
          ["Mémoire", "4 Go de RAM"],
          ["Disque", "2 Go d’espace libre"],
          ["Écran", "1366 × 768 minimum"],
          ["Java", "AUCUN — la machine virtuelle Java est incluse dans VetPharma"],
          ["Internet", "Optionnel (uniquement pour le dashboard mobile et Google Drive)"],
        ], [3000, 6026]),
        astuce("VetPharma fonctionne 100 % hors ligne. Internet ne sert qu’à la synchronisation du dashboard mobile."),

        // 2. MYSQL
        h1("2. Installer MySQL (le seul prérequis)"),
        p("VetPharma stocke ses données dans MySQL. Le plus simple est d’installer WAMP Server (gratuit) :"),
        etape("Télécharger WAMP Server 64 bits depuis wampserver.aviatechno.net"),
        etape("Lancer l’installateur et accepter les choix par défaut (dossier C:\\wamp64)"),
        etape("Démarrer WAMP : l’icône dans la barre des tâches doit devenir VERTE"),
        etape("Configurer le démarrage automatique : touche Windows + R, taper « services.msc », trouver « wampmysqld64 », clic droit → Propriétés → Type de démarrage : Automatique"),
        attention("L’icône WAMP doit être VERTE avant de continuer. Orange ou rouge = MySQL n’est pas démarré."),

        // 3. BASE ET UTILISATEUR
        h1("3. Créer la base et l’utilisateur dédié"),
        p("Ouvrir une invite de commandes (Windows + R → « cmd ») puis lancer la console MySQL :"),
        code("C:\\wamp64\\bin\\mysql\\mysql9.1.0\\bin\\mysql.exe -u root"),
        p("Coller ces trois commandes (adapter « UnMotDePasseFort » — le noter au dos de ce guide) :"),
        code("CREATE DATABASE pharmacie_vet_db;"),
        code("CREATE USER 'vetpharma_app'@'localhost' IDENTIFIED BY 'UnMotDePasseFort';"),
        code("GRANT ALL PRIVILEGES ON pharmacie_vet_db.* TO 'vetpharma_app'@'localhost';"),
        attention("Ne JAMAIS configurer l’application avec le compte root. L’utilisateur dédié vetpharma_app limite les dégâts en cas de problème."),

        // 4. INSTALLER VETPHARMA
        h1("4. Installer VetPharma"),
        new Paragraph({ numbering: { reference: "etapes2", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Copier le dossier « VetPharma » de la clé USB vers C:\\VetPharma")] }),
        new Paragraph({ numbering: { reference: "etapes2", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Copier config.properties.example dans C:\\VetPharma et le renommer en config.properties")] }),
        new Paragraph({ numbering: { reference: "etapes2", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Ouvrir config.properties avec le Bloc-notes et renseigner :")] }),
        code("db.url=jdbc\\:mysql\\://localhost\\:3306/pharmacie_vet_db?serverTimezone\\=UTC&useSSL\\=false&allowPublicKeyRetrieval\\=true&createDatabaseIfNotExist\\=true"),
        code("db.username=vetpharma_app"),
        code("db.password=UnMotDePasseFort   (celui de l’étape 3)"),
        new Paragraph({ numbering: { reference: "etapes2", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Créer le raccourci : clic droit sur C:\\VetPharma\\VetPharma.exe → Envoyer vers → Bureau")] }),
        attention("NE PAS installer dans C:\\Program Files : l’application écrit ses sauvegardes et journaux dans son propre dossier, et Program Files est protégé en écriture par Windows."),

        // 5. PREMIER LANCEMENT
        h1("5. Premier lancement"),
        p("Double-cliquer sur VetPharma.exe. Au premier démarrage (30 à 60 secondes) :"),
        bullet("Les tables de la base sont créées automatiquement"),
        bullet("Les migrations Flyway s’appliquent"),
        bullet("Le compte administrateur par défaut est créé : identifiant « admin », mot de passe « admin »"),
        p("Se connecter avec admin / admin. Un dialogue impose immédiatement de choisir un nouveau mot de passe (4 caractères minimum)."),
        attention("Noter le nouveau mot de passe administrateur en lieu sûr : il n’existe pas de fonction « mot de passe oublié »."),

        // 6. CONFIGURATION INITIALE
        h1("6. Configuration initiale avec le client"),
        new Paragraph({ numbering: { reference: "etapes3", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Paramètres → renseigner le nom, l’adresse et le téléphone de la pharmacie (imprimés sur les tickets)")] }),
        new Paragraph({ numbering: { reference: "etapes3", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Paramètres → configurer le chemin de la clé USB de sauvegarde")] }),
        new Paragraph({ numbering: { reference: "etapes3", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Sécurité & Accès → créer un compte par agent de caisse avec le profil adapté (jamais le compte admin pour vendre)")] }),
        new Paragraph({ numbering: { reference: "etapes3", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Produits & Stock → saisir le catalogue initial (ou restaurer une base préchargée)")] }),
        new Paragraph({ numbering: { reference: "etapes3", level: 0 }, spacing: { after: 80 }, children: [new TextRun("Approvisionnement → enregistrer le stock initial en tant qu’achats (crée les lots et la traçabilité)")] }),

        // 7. FORMATION EXPRESS
        h1("7. Formation express de l’agent (30 minutes)"),
        p("Faire exécuter à l’agent, en conditions réelles, la checklist suivante :"),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Ouvrir sa caisse avec un fond de caisse")] }),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Faire une vente en espèces avec monnaie rendue + imprimer le ticket")] }),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Faire une vente Mobile Money, puis une vente mixte (espèces + mobile)")] }),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Vendre un produit déconditionnable à l’unité")] }),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Mettre un ticket en attente puis le reprendre")] }),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Clôturer la caisse (Z) : compter le tiroir, saisir le montant, comprendre l’écart")] }),
        new Paragraph({ numbering: { reference: "checklist", level: 0 }, spacing: { after: 60 }, children: [new TextRun("Vérifier que la sauvegarde de clôture s’est bien créée (dossier backups + clé USB)")] }),
        astuce("Règle d’or à transmettre : on ne quitte jamais son poste sans avoir clôturé sa caisse — l’application le bloque de toute façon."),

        // 8. DASHBOARD MOBILE
        h1("8. Dashboard mobile (optionnel)"),
        p("Si le client souhaite suivre sa pharmacie depuis son téléphone :"),
        bullet("Renseigner les lignes supabase.* dans config.properties (fournies par votre installateur)"),
        bullet("Sur le téléphone : ouvrir l’adresse de la PWA dans Chrome → menu → « Ajouter à l’écran d’accueil »"),
        bullet("Se connecter avec le compte fourni ; les données se mettent à jour toutes les 5 minutes quand le PC a Internet"),

        // 9. SAUVEGARDES
        h1("9. Sauvegardes : les 3 règles"),
        new Paragraph({ numbering: { reference: "puces", level: 0 }, spacing: { after: 60 }, children: [new TextRun({ text: "1 sauvegarde par jour ", bold: true }), new TextRun("— automatique à chaque clôture de caisse")] }),
        new Paragraph({ numbering: { reference: "puces", level: 0 }, spacing: { after: 60 }, children: [new TextRun({ text: "2 supports ", bold: true }), new TextRun("— le disque du PC + la clé USB (et Google Drive si Internet)")] }),
        new Paragraph({ numbering: { reference: "puces", level: 0 }, spacing: { after: 60 }, children: [new TextRun({ text: "1 test par trimestre ", bold: true }), new TextRun("— vérifier qu’une sauvegarde se restaure (voir docs/PROCEDURE_RESTAURATION.md)")] }),

        // 10. DÉPANNAGE
        h1("10. Dépannage — les 5 pannes les plus fréquentes"),
        tableau([
          ["Symptôme", "Cause probable", "Solution"],
          ["L’application ne démarre pas / erreur au lancement", "MySQL éteint (icône WAMP non verte)", "Démarrer WAMP, attendre l’icône verte, relancer"],
          ["« Identifiants BDD manquants »", "config.properties absent ou incomplet", "Vérifier le fichier à côté de VetPharma.exe (section 4)"],
          ["« Access denied for user »", "Mot de passe db.password différent de celui de MySQL", "Refaire l’étape 3 et recopier le même mot de passe"],
          ["Le ticket ne s’imprime pas", "Imprimante hors ligne ou non par défaut", "Panneau de configuration → Imprimantes → définir par défaut"],
          ["Dashboard mobile vide ou ancien", "PC sans Internet ou lignes supabase.* absentes", "Vérifier Internet sur le PC ; la synchro repart seule toutes les 5 min"],
        ], [2800, 3100, 3126]),
        new Paragraph({ spacing: { before: 300 }, children: [new TextRun({ text: "Support : ", bold: true }), new TextRun("________________________________  (nom et téléphone de votre installateur)")] }),
      ],
    },
  ],
});

Packer.toBuffer(doc).then((buffer) => {
  const out = path.join(ROOT, "docs", "GUIDE_INSTALLATION_CLIENT.docx");
  fs.writeFileSync(out, buffer);
  console.log("Guide généré : " + out);
});
