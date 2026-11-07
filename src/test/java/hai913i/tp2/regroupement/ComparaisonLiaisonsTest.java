package hai913i.tp2.regroupement;

import static hai913i.tp2.JeuxDeTest.EPS;
import static hai913i.tp2.JeuxDeTest.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.FactsSource;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bonus C1 : lien simple contre lien moyen. */
class ComparaisonLiaisonsTest {

    @Test
    void premiereDivergenceALEtape2() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        List<Groupe> moyen = new Regroupement(c, f.classNames(), Liaison.MOYEN).fusions();
        List<Groupe> simple = new Regroupement(c, f.classNames(), Liaison.SIMPLE).fusions();

        assertEquals(2, ComparaisonLiaisons.premiereDivergence(moyen, simple));
        assertEquals(-1, ComparaisonLiaisons.premiereDivergence(moyen, moyen));
        // lien simple, étape 2 : Loan rejoint {Item, LoanService} au meilleur couple, 4/37
        assertEquals(Set.of(v("Loan"), v("Item"), v("LoanService")), new HashSet<>(simple.get(1).classes));
        assertEquals(4.0 / 37, simple.get(1).similarite, EPS);
    }

    @Test
    void modulesDuLienSimple() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        Groupe racine = new Regroupement(c, f.classNames(), Liaison.SIMPLE).racine();
        List<Groupe> modules = Coupe.couper(racine, c, 0.025);
        assertEquals(7, modules.size());
        Groupe principal = modules.stream().max((a, b) -> a.classes.size() - b.classes.size()).get();
        assertEquals(8, principal.classes.size());
        assertTrue(principal.classes.contains(v("Member")));
        assertFalse(principal.classes.contains(v("ConsoleNotifier")));
        assertEquals(0.0290, Coupe.couplageInterneMoyen(principal, c), 1e-4);
    }
}
