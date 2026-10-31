package hai913i.tp2;

import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Point d'entrée du TP 2.
 *
 * Usage :
 *   java -jar target/hai913i-tp2.jar <dossier-des-faits> [--cp <valeur>]
 *
 * Étape A0 : charge les faits et affiche les 3 compteurs.
 *
 */
public final class Main {

    private static final PrintStream OUT = new PrintStream(System.out, true, StandardCharsets.UTF_8);
    private static final PrintStream ERR = new PrintStream(System.err, true, StandardCharsets.UTF_8);
    private static final double CP_DEFAUT = 0.025;
    private Main() {}

    public static void main(String[] args) {
        if (args.length == 0) {
            ERR.println("Usage : java -jar hai913i-tp2.jar <dossier-des-faits> [--cp <valeur>]");
            System.exit(1);
            return;
        }

        // lecture des arguments
        Path dir = Path.of(args[0]);
        double cp = CP_DEFAUT;
        for (int i = 1; i < args.length; i++) {
            if ("--cp".equals(args[i])) {
                if (i + 1 >= args.length) {
                    ERR.println("--cp attend une valeur.");
                    System.exit(1);
                    return;
                }
                try {
                    cp = Double.parseDouble(args[++i]);
                } catch (NumberFormatException e) {
                    ERR.println("Valeur de --cp invalide : " + args[i]);
                    System.exit(1);
                    return;
                }
                if (!Double.isFinite(cp) || cp < 0) {
                    ERR.println("--cp doit être un nombre positif ou nul : " + cp);
                    System.exit(1);
                    return;
                }
            } else {
                ERR.println("Argument inconnu : " + args[i]);
                System.exit(1);
                return;
            }
        }

        // ---------- A0 : chargement des faits derrière FactsSource ----------
        FactsSource facts;
        try {
            String prefix = Facts.discoverPrefix(dir);
            facts = new CsvFactsSource(dir, prefix);
        } catch (IOException e) {
            ERR.println("Erreur de lecture : " + e.getMessage());
            System.exit(2);
            return;
        }

        OUT.println("Dossier des faits : " + dir.toAbsolutePath());
        OUT.println("Jeu de données    : " + facts.name());
        OUT.println("Classes           : " + facts.classNames().size());
        OUT.println("Méthodes          : " + facts.methodCount());
        OUT.println("Arcs du graphe    : " + facts.arcs().size());
        OUT.println("Seuil CP retenu   : " + cp);
    }
}
