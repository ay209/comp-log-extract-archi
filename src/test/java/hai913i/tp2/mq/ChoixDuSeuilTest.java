package hai913i.tp2.mq;

import static hai913i.tp2.JeuxDeTest.EPS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Regroupement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bonus MQ : énumération de toutes les coupes possibles et choix de CP. */
class ChoixDuSeuilTest {

    private static List<ChoixDuSeuil.Palier> paliers(FactsSource f) {
        Couplage c = new Couplage(f);
        Groupe racine = new Regroupement(c, f.classNames()).racine();
        return ChoixDuSeuil.paliers(racine, c, QualiteModularisation.graphe(f), f.classNames().size());
    }

    private static Set<Set<String>> partition(List<Groupe> modules) {
        Set<Set<String>> p = new HashSet<>();
        for (Groupe g : modules) {
            p.add(new HashSet<>(g.classes));
        }
        return p;
    }

    @Test
    void microJeu() throws IOException {
        List<ChoixDuSeuil.Palier> p = paliers(JeuxDeTest.mini());
        assertEquals(3, p.size());                       // 1, 2 puis 3 modules
        assertEquals(1.0 / 3, p.get(1).debut(), EPS);     // C̄ de la racine
        assertEquals(2.0 / 3, p.get(2).debut(), EPS);     // C̄ de {A, B}
        assertTrue(Double.isInfinite(p.get(2).fin()));
        assertSame(p.get(0), ChoixDuSeuil.meilleur(p));  // seule coupe sous M/2 = 1
        assertSame(p.get(1), ChoixDuSeuil.palierDe(p, 0.50));
    }

    @Test
    void validationLeMQRetrouveLeChoixDuRapport() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        List<ChoixDuSeuil.Palier> p = paliers(f);
        assertEquals(14, p.size());                      // de 1 à 15 modules (11 et 13 absents)
        ChoixDuSeuil.Palier retenu = ChoixDuSeuil.meilleur(p);
        assertEquals(7, retenu.modules().size());
        assertEquals(0.0233, retenu.debut(), 1e-4);      // C̄ du nœud de 9 classes
        assertEquals(1.0 / 37, retenu.fin(), EPS);        // C̄ de M4
        assertSame(retenu, ChoixDuSeuil.palierDe(p, 0.025));
        assertSame(retenu, ChoixDuSeuil.meilleur(p, false));   // même sans la contrainte M/2

        Couplage c = new Couplage(f);
        List<Groupe> a025 = Coupe.couper(new Regroupement(c, f.classNames()).racine(), c, 0.025);
        assertEquals(partition(a025), partition(retenu.modules()));
    }

    @Test
    void junitLeMQPrefereMoinsDeModules() throws IOException {
        FactsSource f = new CsvFactsSource(Path.of("c4", "faits", "junit-3.8.2-jdt"), "facts-junit-3.8.2-jdt");
        List<ChoixDuSeuil.Palier> p = paliers(f);
        ChoixDuSeuil.Palier retenu = ChoixDuSeuil.meilleur(p);
        assertEquals(20, retenu.modules().size());
        assertEquals(0.1186, retenu.mq().mq(), 1e-4);
        assertTrue(retenu.debut() <= 0.0033 && 0.0033 < retenu.fin());
        assertSame(retenu, ChoixDuSeuil.meilleur(p, false));
        ChoixDuSeuil.Palier a005 = ChoixDuSeuil.palierDe(p, 0.005);   // choix « le plus exigeant sous M/2 »
        assertEquals(26, a005.modules().size());
        assertEquals(0.1028, a005.mq().mq(), 1e-4);
    }
}
