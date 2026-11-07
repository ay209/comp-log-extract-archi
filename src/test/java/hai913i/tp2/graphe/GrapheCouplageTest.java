package hai913i.tp2.graphe;

import static hai913i.tp2.JeuxDeTest.EPS;
import static hai913i.tp2.JeuxDeTest.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.FactsSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Étape A2 : le graphe de couplage pondéré et ses exports. */
class GrapheCouplageTest {

    private static GrapheCouplage graphe() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        return new GrapheCouplage(new Couplage(f), f.classNames());
    }

    @Test
    void noeudsAretesEtIsolees() throws IOException {
        GrapheCouplage g = graphe();
        assertEquals(15, g.nbNoeuds());
        assertEquals(19, g.nbAretes());
        assertEquals(List.of(v("Dvd"), v("LoanException")), g.classesIsolees());
    }

    @Test
    void lesCinqAretesLesPlusLourdes() throws IOException {
        List<Arete> a = graphe().aretes();
        double[] attendus = {4.0 / 37, 4.0 / 37, 3.0 / 37, 3.0 / 37, 3.0 / 37};
        for (int i = 0; i < attendus.length; i++) {
            assertEquals(attendus[i], a.get(i).poids, EPS);
        }
        assertEquals(4.0 / 37, graphe().poids(v("LoanService"), v("Item")), EPS);  // dans les deux sens
    }

    @Test
    void rechargementFidele(@TempDir Path dossier) throws IOException {
        GrapheCouplage g = graphe();
        Path csv = dossier.resolve("graphe.csv");
        g.exporterCsv(csv);
        GrapheCouplage relu = GrapheCouplage.charger(csv);
        assertEquals(g.nbNoeuds(), relu.nbNoeuds());
        assertEquals(g.nbAretes(), relu.nbAretes());
        for (Arete a : g.aretes()) {
            assertEquals(a.poids, relu.poids(a.classeA, a.classeB), 0.0);   // à l'identique
        }
    }

    @Test
    void exportDotBienForme(@TempDir Path dossier) throws IOException {
        Path dot = dossier.resolve("graphe.dot");
        graphe().exporterDot(dot);
        List<String> lignes = Files.readAllLines(dot);
        assertEquals("graph couplage {", lignes.get(0));
        assertEquals("}", lignes.get(lignes.size() - 1));
        assertEquals(1 + 15 + 19 + 1, lignes.size());   // en-tête, nœuds, arêtes, fin
        assertTrue(lignes.stream().filter(l -> l.contains(" -- ")).count() == 19);
    }
}
