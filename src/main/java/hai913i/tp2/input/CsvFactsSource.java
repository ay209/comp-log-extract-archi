package hai913i.tp2.input;

import hai913i.tp2.Facts;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Chemin 2 du sujet : lit le jeu de données canonique (CSV)
 * et expose les faits derrière l'interface FactsSource.
 * C'est la seule classe qui connaît Facts.java.
 * Le reste du programme ne parle qu'à FactsSource.
 */
public final class CsvFactsSource implements FactsSource {

    private final String name;
    private final List<String> classNames;
    private final int methodCount;
    private final List<Arc> arcs;

    /**
     * Charge un jeu de données CSV depuis un dossier.
     * @param dir    dossier contenant les trois fichiers CSV
     * @param prefix préfixe des fichiers, ex : "facts-validation"
     */
    public CsvFactsSource(Path dir, String prefix) throws IOException {
        Facts facts = Facts.load(dir, prefix);
        this.name = prefix;

        // classes : noms qualifiés triés (déjà trié par Facts.classNames())
        this.classNames = facts.classNames();

        // méthodes : on compte les entrées de methodOwners()
        this.methodCount = facts.methodOwners().size();

        // arcs : on traduit chaque CallArc en Arc (FactsSource.Arc)
        List<Arc> arcList = new ArrayList<>();
        for (Facts.CallArc a : facts.calls()) {
            arcList.add(new Arc(a.caller(), a.callerClass(), a.callee(), a.calleeClass()));
        }
        this.arcs = List.copyOf(arcList);
    }
    @Override
    public String name() {
        return name;
    }
    @Override
    public List<String> classNames() {
        return classNames;
    }
    @Override
    public int methodCount() {
        return methodCount;
    }
    @Override
    public List<Arc> arcs() {
        return arcs;
    }
}
