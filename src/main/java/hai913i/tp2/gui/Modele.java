package hai913i.tp2.gui;

import hai913i.tp2.couplage.Couple;
import hai913i.tp2.couplage.Couplage;
import hai913i.tp2.input.FactsSource;
import hai913i.tp2.input.Heritage;
import hai913i.tp2.regroupement.Coupe;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Liaison;
import hai913i.tp2.regroupement.Regroupement;
import java.util.List;

/**
 * Bonus C3
 *
 * Cette classe ne connaît pas Swing : elle enchaîne les mêmes étapes que la ligne de commande
 * (couplage, regroupement, coupe) pour une configuration donnée (critère de liaison, héritage
 * ou non). Elle peut donc être testée sans écran.
 */
public final class Modele {

    private final FactsSource facts;
    private final List<Heritage.Lien> liens;

    private Liaison liaison = Liaison.MOYEN;
    private boolean avecHeritage = false;
    private Couplage couplage;
    private Regroupement regroupement;

    /** @param liens liens d'héritage (bonus C2) ; null ou vide si indisponibles */
    public Modele(FactsSource facts, List<Heritage.Lien> liens) {
        this.facts = facts;
        this.liens = liens == null ? List.of() : List.copyOf(liens);
        recalculer();
    }

    public boolean heritageDisponible() {
        return !liens.isEmpty();
    }

    /** Change la configuration et recalcule couplage et dendrogramme. */
    public void configurer(Liaison nouvelleLiaison, boolean heritage) {
        this.liaison = nouvelleLiaison;
        this.avecHeritage = heritage && heritageDisponible();
        recalculer();
    }

    private void recalculer() {
        couplage = avecHeritage ? new Couplage(facts, liens) : new Couplage(facts);
        regroupement = new Regroupement(couplage, facts.classNames(), liaison);
    }

    public String nomJeu() {
        return facts.name();
    }

    public List<String> classes() {
        return facts.classNames();
    }

    public Couplage couplage() {
        return couplage;
    }

    public Groupe racine() {
        return regroupement.racine();
    }

    public List<Groupe> fusions() {
        return regroupement.fusions();
    }

    public Liaison liaison() {
        return liaison;
    }

    public boolean avecHeritage() {
        return avecHeritage;
    }

    /** Contrainte M/2 (division entière). */
    public int maxModules() {
        return classes().size() / 2;
    }

    /** Plus grand couplage entre deux classes : sert d'échelle au curseur de CP. */
    public double couplageMax() {
        List<Couple> tries = couplage.couplesTries();
        return tries.isEmpty() ? 0.0 : couplage.valeur(tries.get(0).classeA, tries.get(0).classeB);
    }

    /** Plus grande similarité de fusion : sert d'échelle à l'axe du dendrogramme. */
    public double similariteMax() {
        double max = 0;
        for (Groupe g : fusions()) {
            max = Math.max(max, g.similarite);
        }
        return max;
    }

    /** Nombre d'arcs r(A, B) retrouvé à partir de C(A, B) et de |Einter|. */
    public int nbArcs(String a, String b) {
        return (int) Math.round(couplage.valeur(a, b) * couplage.nbArcsInter());
    }

    /** Les modules obtenus pour un seuil CP (règle de coupe du sujet). */
    public List<Groupe> modules(double cp) {
        return racine() == null ? List.of() : Coupe.couper(racine(), couplage, cp);
    }

    public boolean contrainteRespectee(List<Groupe> modules) {
        return modules.size() <= maxModules();
    }

    public double couplageInterne(Groupe g) {
        return Coupe.couplageInterneMoyen(g, couplage);
    }

    /** Paquetage d'une classe : « library.model » pour « library.model.Book ». */
    public static String paquetage(String classe) {
        int i = classe.lastIndexOf('.');
        return i < 0 ? "(défaut)" : classe.substring(0, i);
    }
}