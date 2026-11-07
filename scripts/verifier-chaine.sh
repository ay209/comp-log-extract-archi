#!/usr/bin/env bash
# Test de bout en bout : lance le jar sur les jeux de faits du dépôt et vérifie les
# points de contrôle du sujet (A0 à B3), les bonus C1 à C4 et les codes de sortie.
#
#   scripts/verifier-chaine.sh [chemin/du/jar]        (défaut : target/hai913i-tp2.jar)
#
# Variables facultatives :
#   SORTIES=dossier            où écrire les exports (défaut : sorties-ci)
#   FAITS_CANONIQUES=dossier   jeu canonique du sujet (facts-validation-*), s'il est disponible :
#                              ajoute la comparaison faits canoniques / faits JDT
#
# Code de sortie : 0 si tout passe, 1 sinon. Lancer depuis la racine du dépôt.
set -u

JAR=${1:-target/hai913i-tp2.jar}
OUT=${SORTIES:-sorties-ci}
VALID=c4/faits/validation-jdt
JUNIT=c4/faits/junit-3.8.2-jdt
MINI=src/test/resources

if [ ! -f "$JAR" ]; then
    echo "jar introuvable : $JAR (lancer d'abord mvn package)" >&2
    exit 1
fi
rm -rf "$OUT"
mkdir -p "$OUT"

echecs=0
ok() { echo "  ok     $1"; }
ko() { echo "  ÉCHEC  $1"; echecs=$((echecs + 1)); }

# lancer <fichier-console> <arguments...> : exécute le jar, sortie standard et erreurs dans le fichier
lancer() {
    local console=$1; shift
    java -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Djava.awt.headless=true \
         -jar "$JAR" "$@" > "$console" 2>&1
}

# attendu <fichier> <texte exact> : le texte doit apparaître dans la sortie
attendu() {
    if grep -qF -- "$2" "$1"; then ok "$2"; else ko "attendu : « $2 »  (voir $1)"; fi
}

# sans_false <fichier> : aucune vérification intégrée au programme ne doit afficher false
sans_false() {
    if grep -nE ": false$" "$1"; then ko "vérification intégrée à false dans $1"
    else ok "aucune vérification intégrée à false"; fi
}

# code <attendu> <description> <arguments...> : vérifie le code de sortie
code() {
    local attendu=$1 description=$2; shift 2
    lancer "$OUT/erreur.txt" "$@"
    local obtenu=$?
    if [ "$obtenu" -eq "$attendu" ]; then ok "code $attendu : $description"
    else ko "code $obtenu au lieu de $attendu : $description ($(head -1 "$OUT/erreur.txt"))"; fi
}

echo "== Validation, CP = 0.025, avec C1, C2 et les paquetages (C4)"
C=$OUT/validation.txt
lancer "$C" "$VALID" --cp 0.025 --sorties "$OUT/validation" \
       --comparer-liaisons --heritage --paquetages --comparer-avec "$VALID"
attendu "$C" "Classes           : 15"
attendu "$C" "Méthodes          : 59"
attendu "$C" "Arcs du graphe    : 44"
attendu "$C" "Arcs internes écartés : 7"
attendu "$C" "Arcs inter-classes    : 37"
attendu "$C" "Couples non nuls      : 19"
attendu "$C" "Somme des couplages   : 1.0000"
attendu "$C" "library.model.Item - library.service.LoanService  r=4  C=0.1081"
attendu "$C" "Arêtes : 19"
attendu "$C" "Classes isolées : [library.model.Dvd, library.service.LoanException]"
attendu "$C" "Étape  1 : {Item} + {LoanService}  S=0.1081"
attendu "$C" "Étape  2 : {LibraryApp} + {Catalog}  S=0.0811"
attendu "$C" "Étape  3 : {Item, LoanService} + {LibraryApp, Catalog}  S=0.0608"
attendu "$C" "Nombre de fusions : 14"
attendu "$C" "Nœuds internes : 14"
attendu "$C" "Nombre de modules : 7 (maximum autorisé M/2 = 7)"
attendu "$C" "Contrainte M/2 respectée."
attendu "$C" "Première divergence à l'étape 2"
attendu "$C" "Lien simple, CP = 0.025 : 7 module(s)"
attendu "$C" "Liens ajoutés : 4"
attendu "$C" "|Einter| : 37 -> 41"
attendu "$C" "Modules avec la relation enrichie, CP = 0.0226 : 7"
attendu "$C" "Indice de Rand                 : 0.6381"
attendu "$C" "(partitions identiques)"
sans_false "$C"

echo "== Validation, les deux autres seuils du sujet"
C=$OUT/validation-0.015.txt
lancer "$C" "$VALID" --cp 0.015 --sorties "$OUT/cp-0.015"
attendu "$C" "Nombre de modules : 4 (maximum autorisé M/2 = 7)"
sans_false "$C"
C=$OUT/validation-0.045.txt
lancer "$C" "$VALID" --cp 0.045 --sorties "$OUT/cp-0.045"
attendu "$C" "Nombre de modules : 10 (maximum autorisé M/2 = 7)"
attendu "$C" "CONTRAINTE M/2 VIOLÉE"

echo "== Second projet (C4) : JUnit 3.8.2"
C=$OUT/junit.txt
lancer "$C" "$JUNIT" --cp 0.005 --sorties "$OUT/junit-3.8.2" --balayage --paquetages
attendu "$C" "Classes           : 58"
attendu "$C" "Méthodes          : 489"
attendu "$C" "Arcs du graphe    : 515"
attendu "$C" "Arcs inter-classes    : 192"
attendu "$C" "Nombre de fusions : 57"
attendu "$C" "Nombre de modules : 26 (maximum autorisé M/2 = 29)"
attendu "$C" "  0.006      33    29  VIOLÉE"
attendu "$C" "Indice de Rand                 : 0.7290"
sans_false "$C"

if [ -n "${FAITS_CANONIQUES:-}" ]; then
    echo "== Faits canoniques contre faits JDT"
    C=$OUT/canonique-vs-jdt.txt
    lancer "$C" "$FAITS_CANONIQUES" --cp 0.025 --sorties "$OUT/canonique" --comparer-avec "$VALID"
    attendu "$C" "Modules au même CP (0.025) : 7 ici, 7 avec facts-validation-jdt"
    attendu "$C" "(partitions identiques)"
    sans_false "$C"
fi

echo "== Codes de sortie"
code 1 "aucun argument"
code 1 "--cp sans valeur"            "$VALID" --cp
code 1 "--cp non numérique"          "$VALID" --cp abc
code 1 "--cp négatif"                "$VALID" --cp -3
code 1 "--sorties sans dossier"      "$VALID" --sorties
code 1 "argument inconnu"            "$VALID" --inconnu
code 2 "dossier de faits absent"     /chemin/inexistant
code 2 "--comparer-avec absent"      "$VALID" --sorties "$OUT/err" --comparer-avec /chemin/inexistant
code 2 "--heritage sans JSON"        "$MINI" --sorties "$OUT/err" --heritage
code 2 "--gui sans écran"            "$MINI" --sorties "$OUT/err" --gui
rm -rf "$OUT/err" "$OUT/erreur.txt"

echo "== Fichiers DOT (Graphviz)"
if command -v dot > /dev/null; then
    for f in "$OUT"/validation/*.dot "$OUT"/junit-3.8.2/*.dot; do
        if dot -Tpng "$f" -o "${f%.dot}.png" 2> "$OUT/dot-erreur.txt"; then ok "$f"
        else ko "$f refusé par Graphviz : $(head -1 "$OUT/dot-erreur.txt")"; fi
    done
    rm -f "$OUT/dot-erreur.txt"
else
    echo "  (Graphviz absent : vérification des .dot sautée)"
fi

echo
if [ "$echecs" -eq 0 ]; then
    echo "Tout passe."
    exit 0
fi
echo "$echecs vérification(s) en échec."
exit 1
