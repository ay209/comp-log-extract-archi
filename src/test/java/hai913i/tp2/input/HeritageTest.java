package hai913i.tp2.input;

import static hai913i.tp2.JeuxDeTest.EPS;
import static hai913i.tp2.JeuxDeTest.v;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.regroupement.ComparaisonLiaisons;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Regroupement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bonus C2 : relation enrichie par l'héritage et l'implémentation d'interface. */
class HeritageTest {

    private static List<Heritage.Lien> liens(FactsSource f) throws IOException {
        return Heritage.lire(JeuxDeTest.dossierValidation(), "facts-validation-jdt", f.classNames());
    }

    @Test
    void quatreLiensEntreClassesDuProjet() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        assertEquals(Set.of(
                new Heritage.Lien(v("Book"), v("Item")),
                new Heritage.Lien(v("Dvd"), v("Item")),
                new Heritage.Lien(v("Item"), v("Loanable")),
                new Heritage.Lien(v("ConsoleNotifier"), v("Notifier"))),
                new HashSet<>(liens(f)));      // java.lang.Exception, java.lang.Enum... écartés
    }

    @Test
    void couplageEnrichi() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f, liens(f));
        assertEquals(41, c.nbArcsInter());
        assertEquals(22, c.nbCouplesNonNuls());
        assertEquals(1.0, c.somme(), EPS);
        assertEquals(4.0 / 41, c.valeur(v("Item"), v("LoanService")), EPS);
        assertEquals(1.0 / 41, c.valeur(v("Dvd"), v("Item")), EPS);         // Dvd n'est plus isolée
        for (String autre : f.classNames()) {
            assertEquals(0.0, c.valeur(v("LoanException"), autre), EPS);  // elle, si
        }
    }

    @Test
    void modulesAuSeuilEquivalent() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage imposee = new Couplage(f);
        Couplage enrichie = new Couplage(f, liens(f));
        double cp = 0.025 * 37 / 41;
        List<Groupe> avant = Coupe.couper(new Regroupement(imposee, f.classNames()).racine(), imposee, 0.025);
        List<Groupe> apres = Coupe.couper(new Regroupement(enrichie, f.classNames()).racine(), enrichie, cp);
        assertEquals(7, apres.size());
        assertTrue(Coupe.partitionValide(apres, 15));

        int differents = 0;
        for (Groupe m : apres) {
            if (!ComparaisonLiaisons.moduleIdentiqueDans(m, avant)) {
                differents++;
            }
            if (m.classes.contains(v("Notifier"))) {
                assertEquals(1, m.classes.size());              // Notifier quitte le cœur
            }
            if (m.classes.contains(v("Item"))) {
                assertTrue(m.classes.contains(v("Loanable")));  // Loanable le rejoint
                assertFalse(m.classes.contains(v("Notifier")));
            }
        }
        assertEquals(2, differents);
    }

    @Test
    void sansFichierJsonRefuse() {
        assertThrows(IOException.class,
                () -> Heritage.lire(Path.of("src", "test", "resources"), "facts-mini", List.of("demo.A")));
    }
}
