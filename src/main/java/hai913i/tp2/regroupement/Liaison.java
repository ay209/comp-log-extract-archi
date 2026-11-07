package hai913i.tp2.regroupement;

/** Critère de liaison : comment mesurer la similarité entre deux groupes. */
public enum Liaison {
    /** Lien moyen : moyenne du couplage sur tous les couples inter-groupes. */
    MOYEN,
    /** Lien simple (bonus C1) : couplage du MEILLEUR couple inter-groupes. */
    SIMPLE
}