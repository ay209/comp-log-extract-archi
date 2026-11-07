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

/** Étapes B1 (regroupement hiérarchique, lien moyen) et B2 (dendrogramme). */
class RegroupementTest {

    private static Regroupement validation() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        return new Regroupement(new Couplage(f), f.classNames());
    }

    private static Set<String> classes(Groupe g) {
        return new HashSet<>(g.classes);
    }

    @Test
    void microJeu() throws IOException {
        FactsSource f = JeuxDeTest.mini();
        Regroupement r = new Regroupement(new Couplage(f), f.classNames());
        assertEquals(2, r.fusions().size());
        assertEquals(2.0 / 3, r.fusions().get(0).similarite, EPS);          // {A} + {B}
        assertEquals(Set.of("demo.A", "demo.B"), classes(r.fusions().get(0)));
        assertEquals((0 + 1.0 / 3) / 2, r.fusions().get(1).similarite, EPS); // {A,B} + {C}
    }

    @Test
    void troisPremieresFusions() throws IOException {
        List<Groupe> fusions = validation().fusions();
        assertEquals(14, fusions.size());
        assertEquals(Set.of(v("Item"), v("LoanService")), classes(fusions.get(0)));
        assertEquals(4.0 / 37, fusions.get(0).similarite, EPS);
        assertEquals(Set.of(v("LibraryApp"), v("Catalog")), classes(fusions.get(1)));
        assertEquals(3.0 / 37, fusions.get(1).similarite, EPS);
        // {Item, LoanService} + {LibraryApp, Catalog} : (3 + 2 + 3 + 1) / 37 / 4
        assertEquals(9.0 / (37 * 4), fusions.get(2).similarite, EPS);
    }

    @Test
    void similaritesNonCroissantesEtDeuxDernieresNulles() throws IOException {
        List<Groupe> fusions = validation().fusions();
        for (int i = 1; i < fusions.size(); i++) {
            assertTrue(fusions.get(i).similarite <= fusions.get(i - 1).similarite + EPS,
                    "la fusion " + (i + 1) + " remonte");
        }
        assertEquals(0.0, fusions.get(12).similarite, EPS);
        assertEquals(0.0, fusions.get(13).similarite, EPS);
        assertEquals(Set.of(v("Dvd"), v("LoanException")), classes(fusions.get(12)));
    }

    @Test
    void deuxExecutionsIdentiques() throws IOException {
        List<Groupe> a = validation().fusions();
        List<Groupe> b = validation().fusions();
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).id, b.get(i).id);
            assertEquals(a.get(i).gauche.id, b.get(i).gauche.id);
            assertEquals(a.get(i).droit.id, b.get(i).droit.id);
            assertEquals(a.get(i).similarite, b.get(i).similarite, 0.0);
        }
    }

    @Test
    void dendrogrammeValide() throws IOException {
        Groupe racine = validation().racine();
        assertEquals(15, racine.classes.size());
        assertEquals(15, Dendrogramme.compterFeuilles(racine));
        assertEquals(14, Dendrogramme.compterInternes(racine));
        assertTrue(Dendrogramme.estValide(racine, 15));
    }

    @Test
    void dendrogrammeInvalideDetecte() {
        // deux fils qui partagent la classe B : l'union n'est pas disjointe
        Groupe ab = new Groupe(3, new Groupe(0, "A"), new Groupe(1, "B"), 0.5);
        Groupe bc = new Groupe(4, new Groupe(1, "B"), new Groupe(2, "C"), 0.5);
        Groupe racine = new Groupe(5, ab, bc, 0.1);
        assertFalse(Dendrogramme.estValide(racine, 4));
        assertFalse(Dendrogramme.estValide(ab, 3));      // racine incomplète
    }
}
