# HAI913I – TP 2 : Compréhension et architectures

Commandes pour rejouer les étapes A0 à C4.

## Projet

```bash
cd ~/Téléchargements/"starter(4)"
```

## A0

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.025
mvn clean test
```

Cas d'erreur :

```bash
java -jar target/hai913i-tp2.jar /chemin/inexistant
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp abc
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp -3
```

## A1

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources
```

## A2

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources
```

## B1

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources
```

## B2

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources
dot -Tpng sorties/dendrogramme.dot -o sorties/dendrogramme.png
```

## B3

```bash
mvn clean package
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.015
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.025
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.045
```

## Bonus C1 : lien simple contre lien moyen

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --comparer-liaisons
dot -Tpng sorties/dendrogramme-simple.dot -o sorties/dendrogramme-simple.png
```

## Bonus C2 : relation enrichie avec l'héritage

```bash
mvn clean package
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --heritage
dot -Tpng sorties/dendrogramme-heritage.dot -o sorties/dendrogramme-heritage.png
```

## Bonus C3 : explorateur graphique (Swing)

```bash
mvn clean package
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --gui
```

## Bonus C4 : un second projet, JUnit 3.8.2

Les faits de JUnit 3.8.2 (partie produit) ont été reconstruits par l'extracteur JDT
du TP 1, puis exportés au format du jeu canonique : ils sont dans `c4/faits/`
(provenance détaillée dans `c4/README.md`). L'outil les lit par le chemin 2, comme
le jeu fourni.

```bash
mvn clean package
# la chaîne complète sur JUnit, avec le balayage de CP et la comparaison aux paquetages
java -jar target/hai913i-tp2.jar c4/faits/junit-3.8.2-jdt --cp 0.005 \
     --sorties sorties-c4/junit-3.8.2 --balayage --paquetages

# l'effet de l'extracteur : faits canoniques contre faits reconstruits par JDT
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.025 \
     --sorties sorties-c4/validation --paquetages --comparer-avec c4/faits/validation-jdt
```

Options ajoutées pour C4 :

| Option | Rôle |
|---|---|
| `--sorties <dossier>` | dossier des exports (défaut : `sorties`), pour ne pas écraser ceux de la validation |
| `--balayage` | coupe le même arbre pour 16 valeurs de CP et vérifie M/2 à chaque fois |
| `--paquetages` | compare les modules de la coupe aux paquetages (indice de Rand, puretés) |
| `--comparer-avec <dossier>` | compare les modules à ceux d'un autre jeu de faits du même projet, au même CP |

Attendu : 58 classes, 489 méthodes, 515 arcs, |Einter| = 192 ; 26 modules à
CP = 0,005 et 33 (contrainte violée) à 0,006 ; avec `--comparer-avec`,
« partitions identiques ».

Les options se combinent, par exemple :

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.025 --comparer-liaisons --heritage
```

Les fichiers exportés (CSV, DOT, texte) sont écrits dans `sorties/`.

## Bonus : qualité de modularisation (MQ) et choix automatique de CP

```bash
java -jar target/hai913i-tp2.jar c4/faits/validation-jdt --cp 0.025 --mq
java -jar target/hai913i-tp2.jar c4/faits/junit-3.8.2-jdt --cp 0.005 --mq
```

`--mq` énumère toutes les coupes possibles de l'arbre (une par intervalle de CP : la coupe
ne change que quand CP franchit le couplage interne moyen d'un nœud), calcule pour chacune
le MQ de Mancoridis et al. (1998) sur le graphe orienté des dépendances entre classes, et
indique la coupe de meilleur MQ qui respecte M/2, avec son intervalle de CP.

Attendu : validation, meilleur MQ 0,0899 à 7 modules, CP dans [0,02327 ; 0,02703[ (le
choix du rapport) ; JUnit, meilleur MQ 0,1186 à 20 modules, CP dans [0,00312 ; 0,00347[,
contre 0,1028 pour la coupe à 0,005.

## Tests et intégration continue

```bash
mvn clean test                                   # 49 tests JUnit, de A0 à C4 et MQ
mvn clean package                                # le jar, puis :
bash scripts/verifier-chaine.sh                  # la chaîne de bout en bout
FAITS_CANONIQUES=<dossier-du-jeu-canonique> bash scripts/verifier-chaine.sh   # + canonique contre JDT
```

Les tests des étapes A1 à C3 utilisent le micro jeu (`src/test/resources`) et le projet
de validation reconstruit par JDT (`c4/faits/validation-jdt`) : le jeu canonique n'est pas
dans le dépôt, mais ce jeu-là donne exactement la même partition.

`scripts/verifier-chaine.sh` lance le jar sur la validation (avec C1, C2, `--paquetages`)
et sur JUnit, vérifie les points de contrôle du sujet dans la sortie, échoue si une
vérification intégrée affiche `false`, contrôle les codes de sortie des cas d'erreur
(1 : argument invalide ; 2 : fichier absent ou illisible, `--gui` sans écran), puis, si
Graphviz est installé, convertit chaque `.dot` en PNG. Les exports vont dans `sorties-ci/`
(ignoré par git).

Le workflow `.github/workflows/ci.yml` fait la même chose à chaque poussée : tests sur
JDK 17 et 21, puis la chaîne complète avec Graphviz. Les sorties, les images et le jar
sont joints au run (onglet Actions).
