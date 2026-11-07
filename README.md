# HAI913I – TP 2 : Compréhension et architectures

Commandes pour rejouer les étapes A0 à C3.

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

Les options se combinent, par exemple :

```bash
java -jar target/hai913i-tp2.jar /home/user/Téléchargements/resources --cp 0.025 --comparer-liaisons --heritage
```

Les fichiers exportés (CSV, DOT, texte) sont écrits dans `sorties/`.
