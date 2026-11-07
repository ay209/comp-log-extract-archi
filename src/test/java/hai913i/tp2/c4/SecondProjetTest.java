package hai913i.tp2.c4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Regroupement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Bonus C4 : la chaîne complète sur le second projet (JUnit 3.8.2) et sur la validation,
 * à partir des faits reconstruits par l'extracteur JDT du TP 1 (dossier c4/faits).
 */
class SecondProjetTest {

    private static final double EPS = 1e-9;
    private static final Path FAITS = Path.of("c4", "faits");

    private static FactsSource junit() throws IOException {
        return new CsvFactsSource(FAITS.resolve("junit-3.8.2-jdt"), "facts-junit-3.8.2-jdt");
    }

    @Test
    void faitsEtCouplageDeJUnit() throws IOException {
        FactsSource f = junit();
        assertEquals(58, f.classNames().size());
        assertEquals(489, f.methodCount());
        assertEquals(515, f.arcs().size());

        Couplage c = new Couplage(f);
        assertEquals(192, c.nbArcsInter());          // 515 - 323 arcs internes
        assertEquals(85, c.nbCouplesNonNuls());
        assertEquals(1.0, c.somme(), EPS);
        assertEquals(12.0 / 192, c.valeur("junit.runner.BaseTestRunner", "junit.swingui.TestRunner"), EPS);
    }

    @Test
    void modulesDeJUnitEtContrainte() throws IOException {
        FactsSource f = junit();
        Couplage c = new Couplage(f);
        Regroupement r = new Regroupement(c, f.classNames());
        assertEquals(57, r.fusions().size());

        List<Groupe> a005 = Coupe.couper(r.racine(), c, 0.005);
        assertEquals(26, a005.size());                // ≤ 29 = M/2 : coupe retenue
        assertTrue(Coupe.partitionValide(a005, 58));
        assertTrue(Coupe.seuilRespecte(a005, c, 0.005));
        assertEquals(33, Coupe.couper(r.racine(), c, 0.006).size());   // > 29 : rejetée

        ComparaisonPartitions.Resultat p = ComparaisonPartitions.comparer(
                ComparaisonPartitions.partition(a005), Paquetages.reels(f));
        assertEquals(0.7290, p.rand(), 1e-4);
        assertEquals(0.7586, p.purete(), 1e-4);
        assertEquals(0.3103, p.pureteInverse(), 1e-4);
    }

    @Test
    void validationReconstruiteParJdt() throws IOException {
        FactsSource f = new CsvFactsSource(FAITS.resolve("validation-jdt"), "facts-validation-jdt");
        assertEquals(15, f.classNames().size());
        assertEquals(59, f.methodCount());
        assertEquals(44, f.arcs().size());
        Couplage c = new Couplage(f);
        assertEquals(37, c.nbArcsInter());
        Regroupement r = new Regroupement(c, f.classNames());
        assertEquals(7, Coupe.couper(r.racine(), c, 0.025).size());
        assertEquals(10, Coupe.couper(r.racine(), c, 0.045).size());
    }

    @Test
    void paquetageReelDUneClasseImbriquee() {
        // le jeu canonique donne la classe englobante comme « paquetage » d'une classe imbriquée
        Map<String, String> declares = Map.of("p.A", "p", "p.A.In", "p.A", "q.B", "q");
        FactsSource f = new FactsSource() {
            @Override public String name() { return "t"; }
            @Override public List<String> classNames() { return List.of("p.A", "p.A.In", "q.B"); }
            @Override public int methodCount() { return 0; }
            @Override public List<Arc> arcs() { return List.of(); }
            @Override public String packageOf(String c) { return declares.get(c); }
        };
        assertEquals(Map.of("p.A", "p", "p.A.In", "p", "q.B", "q"), Paquetages.reels(f));
    }

    @Test
    void nomsCourtsNonAmbigus() {
        try {
            Groupe.fixerNoms(List.of("junit.textui.TestRunner", "junit.swingui.TestRunner",
                    "junit.runner.BaseTestRunner"));
            assertEquals("textui.TestRunner", Groupe.nomCourt("junit.textui.TestRunner"));
            assertEquals("swingui.TestRunner", Groupe.nomCourt("junit.swingui.TestRunner"));
            assertEquals("BaseTestRunner", Groupe.nomCourt("junit.runner.BaseTestRunner"));
            // sans homonyme : le nom simple, comme avant
            Groupe.fixerNoms(List.of("library.service.LoanService", "library.service.LoanService.Loan"));
            assertEquals("Loan", Groupe.nomCourt("library.service.LoanService.Loan"));
        } finally {
            Groupe.fixerNoms(List.of());
        }
    }

    @Test
    void tableauDuBalayage() {
        assertEquals("14+4×2+3+1×2", Balayage.resumerTailles(List.of(14, 4, 4, 3, 1, 1)));
        assertFalse(Balayage.SEUILS.isEmpty());
    }
}
