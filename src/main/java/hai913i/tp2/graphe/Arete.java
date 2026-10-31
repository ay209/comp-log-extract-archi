package hai913i.tp2.graphe;

/** Une arête non orientée du graphe (deux classes et un poids) */
public class Arete {
    public String classeA;
    public String classeB;
    public double poids;

    public Arete(String classeA, String classeB, double poids) {
        this.classeA = classeA;
        this.classeB = classeB;
        this.poids = poids;
    }
}