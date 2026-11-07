package hai913i.tp2.input;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bonus C2
 *
 * Pour chaque type du projet, on récupère ses superclasses et ses interfaces.
 * On ne garde que les liens dont les DEUX extrémités sont des classes du projet
 * (java.lang.Enum, java.lang.Exception... sont ignorées : elles ne sont pas dans M).
 *
 * Lecteur volontairement minimal (pas de bibliothèque JSON) : il suppose le format
 * décrit dans resources/README.txt.
 */
public final class Heritage {

    private Heritage() {}

    /** Un lien d'héritage ou d'implémentation : enfant -> parent. */
    public record Lien(String enfant, String parent) {}

    /**
     * @param dossier       dossier du jeu de données
     * @param prefixe       préfixe des fichiers, ex : "facts-validation"
     * @param classesProjet les classes du projet (pour écarter java.lang.*)
     */
    public static List<Lien> lire(Path dossier, String prefixe, List<String> classesProjet) throws IOException {
        Path json = dossier.resolve(prefixe + ".json");
        if (!Files.isRegularFile(json)) {
            throw new IOException("fichier JSON introuvable (il contient l'héritage) : " + json);
        }
        String texte = Files.readString(json, StandardCharsets.UTF_8);

        int debut = texte.indexOf("\"inheritance\"");
        if (debut < 0) {
            throw new IOException("pas de bloc \"inheritance\" dans " + json);
        }
        String bloc = texte.substring(debut);

        List<Lien> liens = new ArrayList<>();
        // chaque entrée est un objet { ... } sans objet imbriqué
        Matcher objet = Pattern.compile("\\{[^{}]*\\}").matcher(bloc);
        while (objet.find()) {
            String entree = objet.group();
            Matcher type = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]*)\"").matcher(entree);
            if (!type.find()) {
                continue;
            }
            String enfant = type.group(1);
            ajouter(liens, enfant, tableau(entree, "superclasses"), classesProjet);
            ajouter(liens, enfant, tableau(entree, "interfaces"), classesProjet);
        }
        return liens;
    }

    /** Les chaînes d'un champ tableau : "interfaces": [ "a", "b" ] */
    private static List<String> tableau(String entree, String champ) {
        List<String> valeurs = new ArrayList<>();
        Matcher t = Pattern.compile("\"" + champ + "\"\\s*:\\s*\\[([^\\]]*)\\]").matcher(entree);
        if (t.find()) {
            Matcher chaine = Pattern.compile("\"([^\"]*)\"").matcher(t.group(1));
            while (chaine.find()) {
                valeurs.add(chaine.group(1));
            }
        }
        return valeurs;
    }

    private static void ajouter(List<Lien> liens, String enfant, List<String> parents, List<String> classesProjet) {
        if (!classesProjet.contains(enfant)) {
            return;
        }
        for (String parent : parents) {
            if (classesProjet.contains(parent) && !parent.equals(enfant)) {
                liens.add(new Lien(enfant, parent));
            }
        }
    }
}