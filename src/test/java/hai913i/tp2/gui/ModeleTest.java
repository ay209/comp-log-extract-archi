package hai913i.tp2.gui;

import static hai913i.tp2.JeuxDeTest.EPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.input.Heritage;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Liaison;
import hai913i.tp2.regroupement.Regroupement;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bonus C3 : le modèle de l'explorateur, testé sans écran. */
class ModeleTest {

    private static Set<Set<String>> partition(List<Groupe> modules) {
        Set<Set<String>> p = new HashSet<>();
        for (Groupe m : modules) {
            p.add(new HashSet<>(m.classes));
        }
        return p;
    }

    @Test
    void memesModulesQueLaLigneDeCommande() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        List<Groupe> cli = Coupe.couper(new Regroupement(c, f.classNames()).racine(), c, 0.025);

        Modele m = new Modele(f, null);
        assertFalse(m.heritageDisponible());
        assertEquals(7, m.maxModules());
        assertEquals(partition(cli), partition(m.modules(0.025)));
        assertTrue(m.contrainteRespectee(m.modules(0.025)));
        assertFalse(m.contrainteRespectee(m.modules(0.045)));
        assertEquals(4.0 / 37, m.couplageMax(), EPS);
        assertEquals(4, m.nbArcs(JeuxDeTest.v("Item"), JeuxDeTest.v("LoanService")));
    }

    @Test
    void changerDeConfiguration() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        List<Heritage.Lien> liens =
                Heritage.lire(JeuxDeTest.dossierValidation(), "facts-validation-jdt", f.classNames());
        Modele m = new Modele(f, liens);
        assertTrue(m.heritageDisponible());

        m.configurer(Liaison.SIMPLE, false);                  // capture 12 : module de 8 classes
        int max = m.modules(0.025).stream().mapToInt(g -> g.classes.size()).max().getAsInt();
        assertEquals(8, max);

        m.configurer(Liaison.MOYEN, true);                    // relation C2
        assertEquals(41, m.couplage().nbArcsInter());
        assertEquals(7, m.modules(0.025 * 37 / 41).size());

        m.configurer(Liaison.MOYEN, false);                   // retour au modèle imposé
        assertEquals(37, m.couplage().nbArcsInter());
    }
}
