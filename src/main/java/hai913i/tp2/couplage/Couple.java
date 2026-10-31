package hai913i.tp2.couplage;

public class Couple {

    /** Un couple de classes et le nombre d'arcs qui les relient. */

        public String classeA;
        public String classeB;
        public int nbArcs;

        public Couple(String classeA, String classeB) {
            this.classeA = classeA;
            this.classeB = classeB;
            this.nbArcs = 0;
        }
}
