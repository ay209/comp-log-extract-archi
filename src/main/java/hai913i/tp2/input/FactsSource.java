package hai913i.tp2.input;

import java.util.List;

/**
 * La seule porte d'entrée des faits dans le TP 2 (étape A0).
 *
 * Le reste du programme ne connaît que cette interface.
 * Deux implémentations sont possibles (§ 4 du sujet) :
 *   - CsvFactsSource : lit le jeu de données canonique (chemin 2)
 *   - Tp1FactsSource : adapte le modèle de faits du TP 1 (chemin 1)
 */
public interface FactsSource {

    /**
     * Un arc du graphe d'appel (méthode appelante → méthode appelée),
     * compté une seule fois même si l'appel apparaît plusieurs fois dans le code.
     */
    record Arc(String callerMethod, String callerClass, String calleeMethod, String calleeClass) {

        /** Vrai si les deux extrémités sont dans la même classe (cohésion, pas couplage). */
        public boolean isIntraClass() {
            return callerClass.equals(calleeClass);
        }
    }

    /** Nom du jeu de données (ex : "facts-validation"). */
    String name();

    /**
     * Toutes les classes du projet, noms qualifiés triés alphabétiquement,
     * y compris celles qui n'apparaissent dans aucun appel.
     * Sa taille est M (le nombre de classes du sujet).
     */
    List<String> classNames();

    /** Nombre total de méthodes du projet. */
    int methodCount();

    /** Tous les arcs du graphe d'appel, arcs intra-classe compris. */
    List<Arc> arcs();

    /**
     * Paquetage d'une classe, tel que le donne le fichier des classes (bonus C4 :
     * comparaison des modules avec les paquetages). Le jeu canonique donne à une classe
     * imbriquée sa classe englobante (LoanService.Loan → library.service.LoanService) ;
     * c4.Paquetages remonte alors au vrai paquetage.
     */
    String packageOf(String className);
}
