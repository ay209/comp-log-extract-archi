package hai913i.tp2.regroupement;

import hai913i.tp2.couplage.Couplage;
import java.util.ArrayList;
import java.util.List;

/** Clustering hiérarchique ascendant */
public class Regroupement {

    // deux nombres à moins de 1e-9 d'écart sont considérés égaux
    private static final double TOLERANCE = 1e-9;

    private Couplage couplage;
    private Liaison liaison;                          // MOYEN  ou SIMPLE
    private List<Groupe> fusions = new ArrayList<>(); // les fusions, dans l'ordre où elles ont lieu
    private Groupe racine;                            // le dernier groupe : il contient toutes les classes

    /** Constructeur du sujet : lien moyen. */
    public Regroupement(Couplage couplage, List<String> classes) {
        this(couplage, classes, Liaison.MOYEN);
    }

    public Regroupement(Couplage couplage, List<String> classes, Liaison liaison) {
        this.couplage = couplage;
        this.liaison = liaison;

        // départ : un groupe par classe
        List<Groupe> actifs = new ArrayList<>();
        for (int i = 0; i < classes.size(); i++) {
            actifs.add(new Groupe(i, classes.get(i)));
        }
        int prochainId = classes.size(); // le premier groupe créé par fusion

        // on fusionne jusqu'à ce qu'il ne reste qu'un seul groupe
        while (actifs.size() > 1) {
            Groupe meilleurPetit = null;   // groupe de plus petit id du meilleur couple
            Groupe meilleurGrand = null;   // groupe de plus grand id du meilleur couple
            double meilleureSimilarite = -1;

            // on essaie tous les couples de groupes actifs
            for (int i = 0; i < actifs.size(); i++) {
                for (int j = i + 1; j < actifs.size(); j++) {

                    // le couple s'écrit toujours (plus petit id, plus grand id)
                    Groupe petit = actifs.get(i);
                    Groupe grand = actifs.get(j);
                    if (petit.id > grand.id) {
                        Groupe temp = petit;
                        petit = grand;
                        grand = temp;
                    }

                    double s = similarite(petit, grand);

                    boolean aRetenir = false;
                    if (meilleurPetit == null || s > meilleureSimilarite + TOLERANCE) {
                        // première valeur, ou similarité strictement plus grande
                        aRetenir = true;
                    } else if (Math.abs(s - meilleureSimilarite) <= TOLERANCE) {
                        // égalité : règle de départage du sujet
                        // on garde le plus petit couple : d'abord le premier id, puis le second
                        if (petit.id < meilleurPetit.id
                                || (petit.id == meilleurPetit.id && grand.id < meilleurGrand.id)) {
                            aRetenir = true;
                        }
                    }

                    if (aRetenir) {
                        meilleurPetit = petit;
                        meilleurGrand = grand;
                        meilleureSimilarite = s;
                    }
                }
            }

            // on fusionne le meilleur couple en un nouveau groupe
            Groupe nouveau = new Groupe(prochainId, meilleurPetit, meilleurGrand, meilleureSimilarite);
            prochainId++;
            actifs.remove(meilleurPetit);
            actifs.remove(meilleurGrand);
            actifs.add(nouveau);
            fusions.add(nouveau); // on garde la fusion : groupes réunis + similarité
        }

        if (!actifs.isEmpty()) {
            racine = actifs.get(0);
        }
    }

    /**
     * Similarité entre deux groupes, selon le critère de liaison choisi.
     * On repart toujours des couplages entre classes, jamais des anciennes similarités.
     *  - MOYEN  : moyenne sur TOUS les couples inter-groupes (les couples à 0 comptent),
     *             dénominateur |G1| x |G2|.
     *  - SIMPLE : valeur du meilleur couple inter-groupes (le maximum).
     */
    private double similarite(Groupe g1, Groupe g2) {
        double somme = 0;
        double meilleur = 0;
        for (String a : g1.classes) {
            for (String b : g2.classes) {
                double c = couplage.valeur(a, b); // 0 si le couple est absent
                somme += c;
                if (c > meilleur) {
                    meilleur = c;
                }
            }
        }
        if (liaison == Liaison.SIMPLE) {
            return meilleur;
        }
        return somme / (g1.classes.size() * g2.classes.size());
    }

    /** Les fusions dans l'ordre  */
    public List<Groupe> fusions() {
        return fusions;
    }

    /** Le groupe final qui contient toutes les classes. */
    public Groupe racine() {
        return racine;
    }
}