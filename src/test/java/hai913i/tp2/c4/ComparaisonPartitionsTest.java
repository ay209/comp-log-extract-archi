package hai913i.tp2.c4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hai913i.tp2.regroupement.Groupe;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Bonus C4 : comparaison de partitions, valeurs calculées à la main. */
class ComparaisonPartitionsTest {

    private static final double EPS = 1e-9;

    @Test
    void comptageDesCouplesALaMain() {
        // A = {a,b,c}{d} ; B = {a,b}{c,d}, plus e qui n'existe que dans B
        Map<String, String> a = Map.of("p.a", "M1", "p.b", "M1", "p.c", "M1", "p.d", "M2");
        Map<String, String> b = Map.of("p.a", "X", "p.b", "X", "p.c", "Y", "p.d", "Y", "p.e", "Z");
        ComparaisonPartitions.Resultat r = ComparaisonPartitions.comparer(a, b);

        assertEquals(4, r.classes());
        assertEquals(List.of("p.e"), List.copyOf(r.seulementB()));
        assertEquals(1, r.ensembleDansLesDeux());   // (a,b)
        assertEquals(2, r.ensembleDansASeul());     // (a,c), (b,c)
        assertEquals(1, r.ensembleDansBSeul());     // (c,d)
        assertEquals(2, r.separesDansLesDeux());    // (a,d), (b,d)
        assertEquals(3.0 / 6, r.rand(), EPS);
        assertEquals(3.0 / 4, r.purete(), EPS);         // M1 : 2 sur 3 en X ; M2 : 1 sur 1
        assertEquals(3.0 / 4, r.pureteInverse(), EPS);  // X : 2 sur 2 ; Y : 1 sur 2
        assertFalse(r.identiques());
    }

    @Test
    void unePartitionEstIdentiqueAElleMeme() {
        Map<String, String> a = Map.of("p.a", "M1", "p.b", "M1", "p.c", "M2");
        ComparaisonPartitions.Resultat r = ComparaisonPartitions.comparer(a, a);
        assertTrue(r.identiques());
        assertEquals(1.0, r.rand(), EPS);
        assertEquals(1.0, r.purete(), EPS);
    }

    @Test
    void lesModulesDUneCoupeDeviennentUnePartition() {
        Groupe a = new Groupe(0, "p.A");
        Groupe b = new Groupe(1, "p.B");
        Groupe c = new Groupe(2, "q.C");
        Groupe ab = new Groupe(3, a, b, 0.5);
        Map<String, String> p = ComparaisonPartitions.partition(List.of(ab, c));
        assertEquals(Map.of("p.A", "M1", "p.B", "M1", "q.C", "M2"), p);

        List<String> detail = ComparaisonPartitions.detailParModule(List.of(ab, c),
                Map.of("p.A", "p", "p.B", "p", "q.C", "q"));
        assertEquals(1, detail.size());                 // le module d'une classe n'est pas détaillé
        assertTrue(detail.get(0).contains("dominant p (2/2)"));
    }
}
