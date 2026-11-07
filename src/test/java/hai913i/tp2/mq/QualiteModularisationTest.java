package hai913i.tp2.mq;

import static hai913i.tp2.JeuxDeTest.EPS;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.input.CsvFactsSource;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Regroupement;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Bonus MQ : la qualité de modularisation de Mancoridis et al. (1998), recalculée à la main. */
class QualiteModularisationTest {

    @Test
    void grapheDeDependances() throws IOException {
        // micro jeu : A→B (deux arcs de méthodes, un seul arc de classes), B→C ; A→A écarté
        assertEquals(Set.of(new QualiteModularisation.Dependance("demo.A", "demo.B"),
                        new QualiteModularisation.Dependance("demo.B", "demo.C")),
                QualiteModularisation.graphe(JeuxDeTest.mini()));
        assertEquals(19, QualiteModularisation.graphe(JeuxDeTest.validation()).size());
        FactsSource junit = new CsvFactsSource(Path.of("c4", "faits", "junit-3.8.2-jdt"), "facts-junit-3.8.2-jdt");
        assertEquals(88, QualiteModularisation.graphe(junit).size());
    }

    @Test
    void microJeuALaMain() throws IOException {
        Set<QualiteModularisation.Dependance> mdg = QualiteModularisation.graphe(JeuxDeTest.mini());

        // un module {A, B, C} : A = 2 / 3² ; pas de terme externe
        QualiteModularisation.Resultat un =
                QualiteModularisation.calculerClasses(List.of(List.of("demo.A", "demo.B", "demo.C")), mdg);
        assertEquals(2.0 / 9, un.mq(), EPS);

        // {A, B} et {C} : interne (1/4 + 0) / 2 ; externe 1 / (2·2·1), une seule paire
        QualiteModularisation.Resultat deux = QualiteModularisation.calculerClasses(
                List.of(List.of("demo.A", "demo.B"), List.of("demo.C")), mdg);
        assertEquals(1.0 / 8, deux.interne(), EPS);
        assertEquals(1.0 / 4, deux.externe(), EPS);
        assertEquals(-1.0 / 8, deux.mq(), EPS);

        // trois classes seules : interne 0 ; externe (1/2 + 1/2 + 0) / 3 paires
        QualiteModularisation.Resultat trois = QualiteModularisation.calculerClasses(
                List.of(List.of("demo.A"), List.of("demo.B"), List.of("demo.C")), mdg);
        assertEquals(-1.0 / 3, trois.mq(), EPS);
    }

    @Test
    void coupeRetenueSurLaValidation() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        List<Groupe> modules = Coupe.couper(new Regroupement(c, f.classNames()).racine(), c, 0.025);
        QualiteModularisation.Resultat r = QualiteModularisation.calculer(modules, QualiteModularisation.graphe(f));
        // 19 arcs orientés : 11 dans M5 (7 classes), 1 dans M1, 1 dans M4 (2 classes chacun), 6 entre modules.
        // Entre modules, quatre paires de modules, chacune à 1/14 : Loanable–M5, Category–M5,
        // M4–M5 (2 arcs / (2·2·7)), M1–M5 (2 arcs / (2·2·7)) ; 21 paires de modules en tout.
        assertEquals(7, r.modules());
        assertEquals((1.0 / 4 + 1.0 / 4 + 11.0 / 49) / 7, r.interne(), EPS);
        assertEquals((4.0 / 14) / 21, r.externe(), EPS);
        assertEquals(0.0899, r.mq(), 1e-4);
    }
}
