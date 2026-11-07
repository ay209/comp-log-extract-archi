package hai913i.tp2;

import hai913i.tp2.c4.Balayage;
import hai913i.tp2.c4.ComparaisonPartitions;
import hai913i.tp2.c4.Paquetages;
import hai913i.tp2.couplage.Couple;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.graphe.Arete;
import hai913i.tp2.graphe.GrapheCouplage;
import hai913i.tp2.gui.Explorateur;
import hai913i.tp2.gui.Modele;
import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.input.Heritage;
import hai913i.tp2.regroupement.ComparaisonLiaisons;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Dendrogramme;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Liaison;
import hai913i.tp2.regroupement.Regroupement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Point d'entrée du TP 2.
 *
 * Usage :
 *   java -jar target/hai913i-tp2.jar <dossier-des-faits> [--cp <valeur>] [--comparer-liaisons] [--heritage]
 *        [--gui] [--sorties <dossier>] [--balayage] [--paquetages] [--comparer-avec <dossier-des-faits>]
 *
 * A0 : charge les faits et affiche les 3 compteurs.
 * A1 : calcule le couplage entre classes.
 * A2 : graphe de couplage pondéré (exports CSV et DOT).
 * B1 : clustering hiérarchique ascendant (lien moyen).
 * B2 : dendrogramme (exports texte, CSV et DOT).
 * B3 : coupe du dendrogramme en modules (contrainte M/2, seuil CP).
 * C1 (bonus, --comparer-liaisons) : compare le lien moyen et le lien simple.
 * C2 (bonus, --heritage) : enrichit la relation avec l'héritage et compare les modules.
 * C3 (bonus, --gui) : explorateur graphique Swing.
 * C4 (bonus, second projet) : --sorties sépare les exports de chaque projet,
 *   --balayage coupe l'arbre pour une gamme de CP, --paquetages compare les modules aux
 *   paquetages, --comparer-avec compare les modules à ceux d'un autre jeu de faits du même
 *   projet (autre extracteur).
 */
public final class Main {

    private static final double CP_DEFAUT = 0.025;

    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits> [--cp <valeur>] [--comparer-liaisons] [--heritage] [--gui]");
            System.err.println("        [--sorties <dossier>] [--balayage] [--paquetages] [--comparer-avec <dossier-des-faits>]");
            System.exit(1);
            return;
        }

        // lecture des arguments
        Path dir = Path.of(args[0]);
        double cp = CP_DEFAUT;
        boolean comparerLiaisons = false;   // bonus C1
        boolean avecHeritage = false;       // bonus C2
        boolean gui = false;                // bonus C3
        Path sorties = Path.of("sorties");  // bonus C4 : dossier des exports
        boolean balayage = false;           // bonus C4
        boolean paquetages = false;         // bonus C4
        Path autresFaits = null;            // bonus C4 : second jeu de faits du même projet
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.println("--cp attend une valeur.");
                    System.exit(1);
                    return;
                }
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    System.err.println("Valeur de --cp invalide : " + args[i]);
                    System.exit(1);
                    return;
                }
                if (!Double.isFinite(cp) || cp < 0) {
                    System.err.println("--cp doit être un nombre positif ou nul : " + cp);
                    System.exit(1);
                    return;
                }
            } else if ("--comparer-liaisons".equals(args[i])) {
                comparerLiaisons = true;
            } else if ("--heritage".equals(args[i])) {
                avecHeritage = true;
            } else if ("--gui".equals(args[i])) {
                gui = true;
            } else if ("--sorties".equals(args[i]) || "--comparer-avec".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.println(args[i] + " attend un dossier.");
                    System.exit(1);
                    return;
                }
                if ("--sorties".equals(args[i])) {
                    sorties = Path.of(args[++i]);
                } else {
                    autresFaits = Path.of(args[++i]);
                }
            } else if ("--balayage".equals(args[i])) {
                balayage = true;
            } else if ("--paquetages".equals(args[i])) {
                paquetages = true;
            } else {
                System.err.println("Argument inconnu : " + args[i]);
                System.exit(1);
                return;
            }
        }

        // ---------- A0 : chargement des faits derrière FactsSource ----------
        FactsSource facts;
        String prefix;
        try {
            prefix = Facts.discoverPrefix(dir);
            facts = new CsvFactsSource(dir, prefix);
        } catch (IOException e) {
            System.err.println("Erreur de lecture : " + e.getMessage());
            System.exit(2);
            return;
        }

        // noms courts non ambigus pour tout l'affichage (JUnit a trois TestRunner)
        Groupe.fixerNoms(facts.classNames());

        System.out.println("Dossier des faits : " + dir.toAbsolutePath());
        System.out.println("Jeu de données    : " + facts.name());
        System.out.println("Classes           : " + facts.classNames().size());
        System.out.println("Méthodes          : " + facts.methodCount());
        System.out.println("Arcs du graphe    : " + facts.arcs().size());
        System.out.println("Seuil CP retenu   : " + cp);

        // ---------- A1 : couplage entre classes ----------
        System.out.println("===============A1=========================");
        Couplage couplage = new Couplage(facts);

        System.out.println();
        System.out.println("Arcs internes écartés : " + (facts.arcs().size() - couplage.nbArcsInter()));
        System.out.println("Arcs inter-classes    : " + couplage.nbArcsInter());
        System.out.println("Couples non nuls      : " + couplage.nbCouplesNonNuls());
        System.out.println(String.format(Locale.ROOT, "Somme des couplages   : %.4f", couplage.somme()));

        // vérification : symétrie et diagonale nulle
        boolean symetrique = true;
        boolean diagonaleNulle = true;
        for (String a : facts.classNames()) {
            if (couplage.valeur(a, a) != 0.0) {
                diagonaleNulle = false;
            }
            for (String b : facts.classNames()) {
                if (couplage.valeur(a, b) != couplage.valeur(b, a)) {
                    symetrique = false;
                }
            }
        }
        System.out.println("Symétrique : " + symetrique);
        System.out.println("Diagonale nulle : " + diagonaleNulle);
        System.out.println();

        // couples par couplage décroissant
        for (Couple c : couplage.couplesTries()) {
            double valeur = couplage.valeur(c.classeA, c.classeB);
            System.out.println(String.format(Locale.ROOT, "%s - %s  r=%d  C=%.4f",
                    c.classeA, c.classeB, c.nbArcs, valeur));
        }

        // ---------- A2 : graphe de couplage pondéré ----------
        System.out.println("===============A2=========================");
        GrapheCouplage graphe = new GrapheCouplage(couplage, facts.classNames());
        System.out.println("Nœuds  : " + graphe.nbNoeuds());
        System.out.println("Arêtes : " + graphe.nbAretes());

        System.out.println("Les 5 couples les plus couplés :");
        for (int i = 0; i < 5 && i < graphe.nbAretes(); i++) {
            Arete a = graphe.aretes().get(i);
            System.out.println(String.format(Locale.ROOT, "  %s - %s  C=%.4f", a.classeA, a.classeB, a.poids));
        }
        System.out.println("Classes isolées : " + graphe.classesIsolees());

        // export, puis rechargement pour vérifier qu'on retrouve les mêmes valeurs
        try {
            Path csv = sorties.resolve("graphe.csv");
            graphe.exporterCsv(csv);
            graphe.exporterDot(sorties.resolve("graphe.dot"));

            GrapheCouplage recharge = GrapheCouplage.charger(csv);
            boolean identique = recharge.nbNoeuds() == graphe.nbNoeuds()
                    && recharge.nbAretes() == graphe.nbAretes();
            for (Arete a : graphe.aretes()) {
                if (recharge.poids(a.classeA, a.classeB) != a.poids) {
                    identique = false;
                }
            }
            System.out.println("Exports écrits dans : " + sorties.toAbsolutePath());
            System.out.println("Rechargement identique : " + identique);
        } catch (IOException e) {
            System.err.println("Erreur d'export : " + e.getMessage());
            System.exit(2);
            return;
        }

        // ---------- B1 : clustering hiérarchique ascendant ----------------
        System.out.println("===============B1=========================");
        Regroupement regroupement = new Regroupement(couplage, facts.classNames());

        int etape = 1;
        boolean decroissant = true;          // vrai tant que les similarités ne remontent jamais
        double precedente = Double.MAX_VALUE;
        for (Groupe fusion : regroupement.fusions()) {
            System.out.println(String.format(Locale.ROOT, "Étape %2d : %s + %s  S=%.4f  -> groupe %d",
                    etape, fusion.gauche.libelle(), fusion.droit.libelle(), fusion.similarite, fusion.id));
            if (fusion.similarite > precedente + 1e-9) {
                decroissant = false;
            }
            precedente = fusion.similarite;
            etape++;
        }
        System.out.println("Nombre de fusions : " + regroupement.fusions().size());
        System.out.println("Similarités qui ne remontent jamais : " + decroissant);
        System.out.println("Classes dans la racine : " + regroupement.racine().classes.size());

        // ---------- B2 : dendrogramme ----------
        System.out.println("===============B2=========================");
        Groupe racine = regroupement.racine();
        System.out.println("Feuilles       : " + Dendrogramme.compterFeuilles(racine));
        System.out.println("Nœuds internes : " + Dendrogramme.compterInternes(racine));
        System.out.println("Arbre valide   : " + Dendrogramme.estValide(racine, facts.classNames().size()));
        try {
            Dendrogramme.exporterTexte(sorties.resolve("dendrogramme.txt"), racine);
            Dendrogramme.exporterFusions(sorties.resolve("fusions.csv"), regroupement.fusions());
            Dendrogramme.exporterDot(sorties.resolve("dendrogramme.dot"), racine);
            System.out.println("Exports : " + sorties.resolve("dendrogramme.txt") + ", fusions.csv, dendrogramme.dot");
        } catch (IOException e) {
            System.err.println("Erreur d'export : " + e.getMessage());
            System.exit(2);
            return;
        }

        // ---------- B3 : coupe du dendrogramme ----------
        System.out.println("===============B3=========================");
        int nbClasses = facts.classNames().size();
        int maxModules = nbClasses / 2;
        List<Groupe> modules = Coupe.couper(racine, couplage, cp);

        System.out.println("Seuil CP : " + cp);
        int numero = 1;
        for (Groupe module : modules) {
            String interne = "-"; // un module d'une seule classe n'a aucun couple
            if (module.classes.size() >= 2) {
                interne = String.format(Locale.ROOT, "%.4f", Coupe.couplageInterneMoyen(module, couplage));
            }
            System.out.println(String.format(Locale.ROOT, "Module %2d : %2d classe(s)  C interne = %s  %s",
                    numero, module.classes.size(), interne, module.libelle()));
            numero++;
        }

        System.out.println("Nombre de modules : " + modules.size() + " (maximum autorisé M/2 = " + maxModules + ")");
        if (modules.size() > maxModules) {
            System.out.println("CONTRAINTE M/2 VIOLÉE : cette coupe est rejetée.");
        } else {
            System.out.println("Contrainte M/2 respectée.");
        }
        System.out.println("Partition valide : " + Coupe.partitionValide(modules, nbClasses));
        System.out.println("Seuil respecté par tous les modules : " + Coupe.seuilRespecte(modules, couplage, cp));

        // ---------- C4 (bonus) : outils pour analyser un second projet ----------
        if (balayage || paquetages || autresFaits != null) {
            System.out.println("===============C4=========================");
        }
        if (balayage) {
            System.out.println("Effet de CP (même arbre, M/2 = " + maxModules + ") :");
            for (String ligne : Balayage.tableau(Balayage.balayer(racine, couplage, nbClasses))) {
                System.out.println(ligne);
            }
        }
        if (paquetages) {
            Map<String, String> pk = Paquetages.reels(facts);
            ComparaisonPartitions.Resultat r = ComparaisonPartitions.comparer(
                    ComparaisonPartitions.partition(modules), pk);
            System.out.println("Modules (CP = " + cp + ") comparés aux paquetages :");
            for (String ligne : ComparaisonPartitions.resume(r, "modules", "paquetages")) {
                System.out.println("  " + ligne);
            }
            for (String ligne : ComparaisonPartitions.detailParModule(modules, pk)) {
                System.out.println(ligne);
            }
        }
        if (autresFaits != null) {
            FactsSource autres;
            try {
                autres = new CsvFactsSource(autresFaits, Facts.discoverPrefix(autresFaits));
            } catch (IOException e) {
                System.err.println("Erreur de lecture (--comparer-avec) : " + e.getMessage());
                System.exit(2);
                return;
            }
            Couplage couplageAutre = new Couplage(autres);
            Regroupement regroupementAutre = new Regroupement(couplageAutre, autres.classNames());
            List<Groupe> modulesAutre = Coupe.couper(regroupementAutre.racine(), couplageAutre, cp);
            System.out.println("Second jeu de faits : " + autres.name() + " (" + autres.classNames().size()
                    + " classes, " + autres.methodCount() + " méthodes, " + autres.arcs().size() + " arcs)");
            System.out.println("Modules au même CP (" + cp + ") : " + modules.size() + " ici, "
                    + modulesAutre.size() + " avec " + autres.name());
            ComparaisonPartitions.Resultat r = ComparaisonPartitions.comparer(
                    ComparaisonPartitions.partition(modules), ComparaisonPartitions.partition(modulesAutre));
            for (String ligne : ComparaisonPartitions.resume(r, facts.name(), autres.name())) {
                System.out.println("  " + ligne);
            }
        }

        // ---------- C1 (bonus) : lien moyen contre lien simple ----------
        if (comparerLiaisons) {
            System.out.println("===============C1=========================");
            Regroupement simple = new Regroupement(couplage, facts.classNames(), Liaison.SIMPLE);
            List<Groupe> fusionsMoyen = regroupement.fusions();
            List<Groupe> fusionsSimple = simple.fusions();

            System.out.println("Étape | lien moyen                                   | lien simple");
            for (int i = 0; i < fusionsMoyen.size(); i++) {
                Groupe m = fusionsMoyen.get(i);
                Groupe sm = fusionsSimple.get(i);
                System.out.println(String.format(Locale.ROOT, "%5d | S=%.4f %s + %s | S=%.4f %s + %s",
                        i + 1, m.similarite, m.gauche.libelle(), m.droit.libelle(),
                        sm.similarite, sm.gauche.libelle(), sm.droit.libelle()));
            }
            int divergence = ComparaisonLiaisons.premiereDivergence(fusionsMoyen, fusionsSimple);
            if (divergence == -1) {
                System.out.println("Les deux arbres sont identiques.");
            } else {
                System.out.println("Première divergence à l'étape " + divergence);
            }

            // exports du lien simple, dans des fichiers à part (ceux du lien moyen ne sont pas écrasés)
            try {
                Dendrogramme.exporterTexte(sorties.resolve("dendrogramme-simple.txt"), simple.racine());
                Dendrogramme.exporterFusions(sorties.resolve("fusions-simple.csv"), fusionsSimple);
                Dendrogramme.exporterDot(sorties.resolve("dendrogramme-simple.dot"), simple.racine());
                System.out.println("Exports : " + sorties.resolve("dendrogramme-simple.txt") + ", fusions-simple.csv, dendrogramme-simple.dot");
            } catch (IOException e) {
                System.err.println("Erreur d'export : " + e.getMessage());
                System.exit(2);
                return;
            }

            // modules obtenus avec le lien simple, au même CP
            List<Groupe> modulesSimple = Coupe.couper(simple.racine(), couplage, cp);
            System.out.println("Lien simple, CP = " + cp + " : " + modulesSimple.size() + " module(s)");
            for (Groupe module : modulesSimple) {
                System.out.println("  " + module.libelle());
            }
        }

        // ---------- C2 (bonus) : relation enrichie avec l'héritage ----------
        if (avecHeritage) {
            System.out.println("===============C2=========================");
            System.out.println("Relation C2 : appels de méthode + héritage + implémentation d'interface");
            System.out.println("(chaque lien entre deux classes du projet compte comme un arc inter-classes)");
            try {
                List<Heritage.Lien> liens = Heritage.lire(dir, prefix, facts.classNames());
                for (Heritage.Lien lien : liens) {
                    System.out.println("  lien : " + Groupe.nomCourt(lien.enfant()) + " -> " + Groupe.nomCourt(lien.parent()));
                }
                Couplage couplageC2 = new Couplage(facts, liens);
                System.out.println("Liens ajoutés : " + liens.size());
                System.out.println("|Einter| : " + couplage.nbArcsInter() + " -> " + couplageC2.nbArcsInter());
                System.out.println("Couples non nuls : " + couplage.nbCouplesNonNuls() + " -> " + couplageC2.nbCouplesNonNuls());
                System.out.println(String.format(Locale.ROOT, "Somme des couplages : %.4f", couplageC2.somme()));

                Regroupement regroupementC2 = new Regroupement(couplageC2, facts.classNames());
                int etapeC2 = 1;
                for (Groupe fusion : regroupementC2.fusions()) {
                    System.out.println(String.format(Locale.ROOT, "Étape %2d : %s + %s  S=%.4f",
                            etapeC2, fusion.gauche.libelle(), fusion.droit.libelle(), fusion.similarite));
                    etapeC2++;
                }
                Dendrogramme.exporterTexte(sorties.resolve("dendrogramme-heritage.txt"), regroupementC2.racine());
                Dendrogramme.exporterFusions(sorties.resolve("fusions-heritage.csv"), regroupementC2.fusions());
                Dendrogramme.exporterDot(sorties.resolve("dendrogramme-heritage.dot"), regroupementC2.racine());
                System.out.println("Exports : " + sorties.resolve("dendrogramme-heritage.txt") + ", fusions-heritage.csv, dendrogramme-heritage.dot");

                // Les liens ajoutés agrandissent |Einter| : tous les couplages sont réduits d'autant.
                // Pour comparer à exigence équivalente, on ramène CP à la même échelle.
                double cpC2 = cp * couplage.nbArcsInter() / couplageC2.nbArcsInter();
                System.out.println(String.format(Locale.ROOT,
                        "CP équivalent : %.4f x %d / %d = %.4f", cp, couplage.nbArcsInter(),
                        couplageC2.nbArcsInter(), cpC2));
                List<Groupe> modulesC2 = Coupe.couper(regroupementC2.racine(), couplageC2, cpC2);
                System.out.println("Modules avec la relation enrichie, CP = " + String.format(Locale.ROOT, "%.4f", cpC2)
                        + " : " + modulesC2.size() + " (maximum autorisé M/2 = " + maxModules + ")");
                if (modulesC2.size() > maxModules) {
                    System.out.println("CONTRAINTE M/2 VIOLÉE : cette coupe est rejetée.");
                }
                for (Groupe module : modulesC2) {
                    String statut = ComparaisonLiaisons.moduleIdentiqueDans(module, modules)
                            ? "identique au modèle imposé (CP = " + cp + ")" : "DIFFÉRENT du modèle imposé";
                    System.out.println("  " + module.libelle() + "   [" + statut + "]");
                }
                System.out.println("Partition valide : " + Coupe.partitionValide(modulesC2, nbClasses));
                System.out.println("Seuil respecté par tous les modules : " + Coupe.seuilRespecte(modulesC2, couplageC2, cpC2));
            } catch (IOException e) {
                System.err.println("Erreur C2 : " + e.getMessage());
                System.exit(2);
                return;
            }
        }
        // ---------- C3 (bonus)
        if (gui) {
            if (java.awt.GraphicsEnvironment.isHeadless()) {
                System.err.println("--gui demande un environnement graphique (aucun écran détecté).");
                System.exit(2);
                return;
            }
            List<Heritage.Lien> liensGui = null;
            try {
                liensGui = Heritage.lire(dir, prefix, facts.classNames());
            } catch (IOException e) {
                System.out.println("Héritage indisponible (" + e.getMessage() + ") : option désactivée dans la GUI.");
            }
            final Modele modeleGui = new Modele(facts, liensGui);
            final double cpGui = cp;
            javax.swing.SwingUtilities.invokeLater(() -> new Explorateur(modeleGui, cpGui).setVisible(true));
            return;
        }
    }
}