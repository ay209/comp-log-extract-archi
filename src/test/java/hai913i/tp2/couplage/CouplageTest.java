package hai913i.tp2.couplage;

import static hai913i.tp2.JeuxDeTest.EPS;
import static hai913i.tp2.JeuxDeTest.v;
import static org.junit.jupiter.api.Assertions.assertEquals;

import hai913i.tp2.JeuxDeTest;
import hai913i.tp2.input.FactsSource;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Étape A1 : du graphe d'appel au couplage. */
class CouplageTest {

    @Test
    void microJeu() throws IOException {
        Couplage c = new Couplage(JeuxDeTest.mini());
        assertEquals(3, c.nbArcsInter());              // 4 arcs, dont 1 interne à A
        assertEquals(2, c.nbCouplesNonNuls());
        assertEquals(2.0 / 3, c.valeur("demo.A", "demo.B"), EPS);
        assertEquals(1.0 / 3, c.valeur("demo.B", "demo.C"), EPS);
        assertEquals(0.0, c.valeur("demo.A", "demo.C"), EPS);
        assertEquals(1.0, c.somme(), EPS);
    }

    @Test
    void validationPointsDeControle() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        assertEquals(7, f.arcs().size() - c.nbArcsInter());   // arcs internes écartés
        assertEquals(37, c.nbArcsInter());
        assertEquals(19, c.nbCouplesNonNuls());
        assertEquals(1.0, c.somme(), EPS);

        Couple max = c.couplesTries().get(0);
        assertEquals(v("Item"), max.classeA);
        assertEquals(v("LoanService"), max.classeB);
        assertEquals(4.0 / 37, c.valeur(v("Item"), v("LoanService")), EPS);
    }

    @Test
    void symetrieEtDiagonaleNulle() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        for (String a : f.classNames()) {
            assertEquals(0.0, c.valeur(a, a), EPS);
            for (String b : f.classNames()) {
                assertEquals(c.valeur(a, b), c.valeur(b, a), EPS);
            }
        }
    }

    @Test
    void classesSansCouple() throws IOException {
        FactsSource f = JeuxDeTest.validation();
        Couplage c = new Couplage(f);
        for (String isolee : List.of(v("Dvd"), v("LoanException"))) {
            for (String autre : f.classNames()) {
                assertEquals(0.0, c.valeur(isolee, autre), EPS);
            }
        }
    }
}
