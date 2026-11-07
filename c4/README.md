# Bonus C4 : provenance des faits

Ces dossiers sont des jeux de faits au format du jeu canonique (trois CSV et un
JSON avec le bloc `extra.inheritance`). L'outil du TP 2 les lit par le chemin 2.

| Dossier | Projet analysé | Extracteur |
|---|---|---|
| `faits/junit-3.8.2-jdt/` | JUnit 3.8.2, partie produit | JDT (extracteur du TP 1 d'Ay Guncavdi) |
| `faits/validation-jdt/` | projet de validation du sujet | JDT (même extracteur) |

## Le second projet

JUnit 3.8.2, étiquette `r3.8.2` du dépôt `junit-team/junit4`. Seuls les six
paquetages du produit sont analysés : `framework`, `extensions`, `runner`,
`textui`, `awtui` et `swingui` (49 fichiers). `tests` et `samples` sont écartés.

## Comment les faits ont été produits

Dans le projet du TP 1 (option `--export-facts`, ajoutée pour ce bonus) :

```bash
java -cp target/hai913i-tp1-analyzer.jar hai913i.tp2.Main c4/junit-3.8.2 \
     --export-facts sorties/c4/junit-3.8.2-jdt/faits
```

Le script `c4/run-c4.sh` du projet TP 1 refait tout, avec JDT puis avec Spoon.

## Ajouter les faits Spoon

Le même script produit aussi `sorties/c4/junit-3.8.2-spoon/faits` et
`sorties/c4/validation-spoon/faits`. Pour les comparer ici :

```bash
cp -r <projet-TP1>/sorties/c4/junit-3.8.2-spoon/faits c4/faits/junit-3.8.2-spoon
java -jar target/hai913i-tp2.jar c4/faits/junit-3.8.2-jdt --cp 0.005 \
     --sorties sorties-c4/jdt-vs-spoon --comparer-avec c4/faits/junit-3.8.2-spoon
```
