package hai913i.tp2.regroupement;

import static hai913i.tp2.JeuxDeTest.EPS;
import static hai913i.tp2.JeuxDeTest.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.FactsSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Étape B3 : la coupe sous contraintes (CP strict, M/2). */
class CoupeTest {

    private static Couplage couplage;
    private static Groupe racine;

    private static List<Groupe> couper(double cp) throws IOException {
        FactsSource f = JeuxDeTest.validation();
        couplage = new Couplage(f);
        racine = new Regroupement(couplage, f.classNames()).racine();
        return Coupe.couper(racine, couplage, cp);
    }

    private static List<Integer> tailles(List<Groupe> modules) {
        List<Integer> t = new ArrayList<>();
        for (Groupe m : modules) {
            t.add(m.classes.size());
        }
        t.sort((a, b) -> b - a);
        return t;
    }

    @Test
    void microJeu() throws IOException {
        FactsSource f = JeuxDeTest.mini();
        Couplage c = new Couplage(f);
        Groupe r = new Regroupement(c, f.classNames()).racine();
        assertEquals(1.0 / 3, Coupe.couplageInterneMoyen(r, c), EPS);   // (2/3 + 1/3 + 0) / 3
        assertEquals(1, Coupe.couper(r, c, 0.20).size());
        List<Groupe> a050 = Coupe.couper(r, c, 0.50);
        assertEquals(2, a050.size());
        assertTrue(a050.size() > f.classNames().size() / 2);            // M/2 = 1 : violée
    }

    @Test
    void troisSeuilsDuSujet() throws IOException {
        assertEquals(List.of(11, 2, 1, 1), tailles(couper(0.015)));
        assertEquals(List.of(7, 2, 2, 1, 1, 1, 1), tailles(couper(0.025)));
        List<Groupe> a045 = couper(0.045);
        assertEquals(10, a045.size());
        assertTrue(a045.size() > 15 / 2);                                // violée
    }

    @Test
    void modulesRetenus() throws IOException {
        List<Groupe> modules = couper(0.025);
        assertTrue(Coupe.partitionValide(modules, 15));
        assertTrue(Coupe.seuilRespecte(modules, couplage, 0.025));
        Set<Set<String>> attendus = Set.of(
                Set.of(v("Statistics"), v("TextUtils")),
                Set.of(v("Member"), v("ConsoleNotifier")),
                Set.of(v("Notifier"), v("Book"), v("Loan"), v("Item"), v("LoanService"),
                        v("LibraryApp"), v("Catalog")));
        int trouves = 0;
        for (Groupe m : modules) {
            if (m.classes.size() >= 2) {
                assertTrue(attendus.contains(new HashSet<>(m.classes)), m.classes.toString());
                trouves++;
            }
        }
        assertEquals(3, trouves);
    }

    @Test
    void couplagesInternesMoyens() throws IOException {
        couper(0.025);
        assertEquals(1.0 / 105, Coupe.couplageInterneMoyen(racine, couplage), EPS);  // somme 1 / 105 paires
        for (Groupe m : Coupe.couper(racine, couplage, 0.025)) {
            if (m.classes.contains(v("Member"))) {
                assertEquals(1.0 / 37, Coupe.couplageInterneMoyen(m, couplage), EPS);
            }
            if (m.classes.size() == 7) {
                assertEquals(0.0335, Coupe.couplageInterneMoyen(m, couplage), 1e-4);
            }
        }
    }

    @Test
    void seuilStrict() throws IOException {
        // M4 = {Member, ConsoleNotifier} a exactement 1/37 : il n'est pas retenu à CP = 1/37
        List<Groupe> modules = couper(1.0 / 37);
        for (Groupe m : modules) {
            assertTrue(!m.classes.contains(v("Member")) || m.classes.size() == 1);
        }
    }
}
