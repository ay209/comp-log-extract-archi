package hai913i.tp2.gui;

import hai913i.tp2.couplage.Couple;
import hai913i.tp2.regroupement.Groupe;
import hai913i.tp2.regroupement.Liaison;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;

/**
 * Bonus C3 : explorateur graphique des résultats, par-dessus le même modèle que la ligne de commande.
 *
 * Interactions :
 *  - curseur (ou champ) CP : la coupe est recalculée en direct, la contrainte M/2 est signalée ;
 *  - clic sur un nœud du dendrogramme ou sur une classe du graphe : inspection à droite ;
 *  - clic sur un module dans la liste : le nœud correspondant est sélectionné ;
 *  - recherche d'une classe (surlignée dans les deux vues) ;
 *  - filtre sur le poids des arêtes du graphe ;
 *  - changement de critère de liaison (moyen / simple) et de relation (appels / + héritage) ;
 *  - export de la vue courante en PNG.
 */
@SuppressWarnings("serial")
public final class Explorateur extends JFrame {

    private static final Color[] PALETTE = {
            new Color(0x4E79A7), new Color(0xF28E2B), new Color(0x59A14F), new Color(0xE15759),
            new Color(0xB07AA1), new Color(0x76B7B2), new Color(0xEDC948), new Color(0x9C755F),
            new Color(0xFF9DA7), new Color(0x86BCB6), new Color(0xBAB0AC), new Color(0x8CD17D)
    };
    private static final Color ISOLE = new Color(0xBBBBBB);

    private final Modele modele;

    // état courant
    private double cp;
    private List<Groupe> modules = List.of();
    private final Map<String, Integer> moduleDe = new HashMap<>(); // classe -> n° de module (0..)
    private Groupe groupeSelectionne;
    private String classeSelectionnee;
    private String recherche = "";

    // composants
    private final JSlider curseurCp = new JSlider(0, 1000, 0);
    private final JTextField champCp = new JTextField(7);
    private final JLabel etat = new JLabel(" ");
    private final DefaultListModel<Groupe> listeModel = new DefaultListModel<>();
    private final JList<Groupe> listeModules = new JList<>(listeModel);
    private final JTextArea info = new JTextArea();
    private final JTabbedPane onglets = new JTabbedPane();
    private final VueDendrogramme vueDendro;
    private final VueGraphe vueGraphe;
    private boolean miseAJourInterne = false;

    public Explorateur(Modele modele, double cpInitial) {
        super("HAI913I TP 2 — Explorateur (bonus C3) — " + modele.nomJeu());
        this.modele = modele;
        this.cp = cpInitial;
        // les vues lisent le modèle : on les construit une fois celui-ci affecté
        this.vueDendro = new VueDendrogramme();
        this.vueGraphe = new VueGraphe();

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout(6, 6));
        add(construireBarreHaute(), BorderLayout.NORTH);

        onglets.addTab("Dendrogramme", vueDendro);
        onglets.addTab("Graphe de couplage", vueGraphe);

        listeModules.setCellRenderer(new RenduModule());
        listeModules.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !miseAJourInterne && listeModules.getSelectedValue() != null) {
                selectionnerGroupe(listeModules.getSelectedValue(), false);
            }
        });
        info.setEditable(false);
        info.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        info.setLineWrap(true);
        info.setWrapStyleWord(true);
        JScrollPane haut = new JScrollPane(listeModules);
        haut.setBorder(BorderFactory.createTitledBorder("Modules (selon CP)"));
        JScrollPane bas = new JScrollPane(info);
        bas.setBorder(BorderFactory.createTitledBorder("Inspection"));
        JSplitPane droite = new JSplitPane(JSplitPane.VERTICAL_SPLIT, haut, bas);
        droite.setResizeWeight(0.45);
        droite.setPreferredSize(new Dimension(380, 600));

        JSplitPane centre = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, onglets, droite);
        centre.setResizeWeight(1.0);
        add(centre, BorderLayout.CENTER);

        etat.setBorder(BorderFactory.createEmptyBorder(2, 8, 4, 8));
        add(etat, BorderLayout.SOUTH);

        setSize(1280, 760);
        setLocationRelativeTo(null);
        appliquerCp(cp, true);
    }

    // ------------------------------------------------------------------ barre de commandes

    private JComponent construireBarreHaute() {
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));

        barre.add(new JLabel("CP :"));
        curseurCp.setPreferredSize(new Dimension(260, 24));
        curseurCp.addChangeListener(e -> {
            if (!miseAJourInterne) {
                appliquerCp(positionVersCp(curseurCp.getValue()), false);
            }
        });
        barre.add(curseurCp);
        champCp.setToolTipText("Valeur exacte de CP, puis Entrée");
        champCp.addActionListener(e -> {
            try {
                double v = Double.parseDouble(champCp.getText().trim().replace(',', '.'));
                if (!Double.isFinite(v) || v < 0) {
                    throw new NumberFormatException();
                }
                appliquerCp(v, true);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "CP doit être un nombre positif ou nul.",
                        "Valeur invalide", JOptionPane.WARNING_MESSAGE);
                champCp.setText(formater(cp));
            }
        });
        barre.add(champCp);

        JComboBox<String> liaison = new JComboBox<>(new String[] {"Lien moyen", "Lien simple"});
        JCheckBox heritage = new JCheckBox("+ héritage (C2)");
        heritage.setEnabled(modele.heritageDisponible());
        if (!modele.heritageDisponible()) {
            heritage.setToolTipText("Aucun lien d'héritage disponible dans ce jeu de données");
        }
        Runnable reconfigurer = () -> {
            modele.configurer(liaison.getSelectedIndex() == 0 ? Liaison.MOYEN : Liaison.SIMPLE,
                    heritage.isSelected());
            groupeSelectionne = null;
            classeSelectionnee = null;
            appliquerCp(cp, true);
        };
        liaison.addActionListener(e -> reconfigurer.run());
        heritage.addActionListener(e -> reconfigurer.run());
        barre.add(liaison);
        barre.add(heritage);

        barre.add(new JLabel("Classe :"));
        JTextField champRecherche = new JTextField(12);
        champRecherche.setToolTipText("Filtre sur le nom ; Entrée sélectionne la première correspondance");
        champRecherche.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { maj(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { maj(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { maj(); }
            private void maj() {
                recherche = champRecherche.getText().trim().toLowerCase(Locale.ROOT);
                repaint();
            }
        });
        champRecherche.addActionListener(e -> {
            for (String c : modele.classes()) {
                if (!recherche.isEmpty() && c.toLowerCase(Locale.ROOT).contains(recherche)) {
                    selectionnerClasse(c);
                    return;
                }
            }
        });
        barre.add(champRecherche);

        JButton export = new JButton("Exporter PNG");
        export.addActionListener(e -> exporterVueCourante());
        barre.add(export);
        return barre;
    }

    // ------------------------------------------------------------------ état et calculs

    /** Le curseur couvre [0 ; 1,2 x le plus fort couplage] : la même échelle que les données. */
    private double echelleCp() {
        double max = modele.couplageMax();
        return max > 0 ? max * 1.2 : 0.1;
    }

    private double positionVersCp(int pos) {
        return pos / 1000.0 * echelleCp();
    }

    private int cpVersPosition(double v) {
        return (int) Math.max(0, Math.min(1000, Math.round(v / echelleCp() * 1000)));
    }

    private static String formater(double v) {
        return String.format(Locale.ROOT, "%.4f", v);
    }

    private void appliquerCp(double nouveauCp, boolean synchroniserCurseur) {
        cp = nouveauCp;
        miseAJourInterne = true;
        if (synchroniserCurseur) {
            curseurCp.setValue(cpVersPosition(cp));
        }
        champCp.setText(formater(cp));
        modules = modele.modules(cp);

        moduleDe.clear();
        listeModel.clear();
        for (int i = 0; i < modules.size(); i++) {
            listeModel.addElement(modules.get(i));
            for (String c : modules.get(i).classes) {
                moduleDe.put(c, i);
            }
        }
        miseAJourInterne = false;

        boolean ok = modele.contrainteRespectee(modules);
        etat.setText(String.format(Locale.ROOT, "CP = %.4f  →  %d module(s), maximum autorisé M/2 = %d  —  %s   |   %s%s",
                cp, modules.size(), modele.maxModules(),
                ok ? "contrainte M/2 respectée" : "CONTRAINTE M/2 VIOLÉE : coupe à rejeter",
                modele.liaison() == Liaison.MOYEN ? "lien moyen" : "lien simple",
                modele.avecHeritage() ? ", appels + héritage" : ", appels seuls"));
        etat.setForeground(ok ? new Color(0x2E7D32) : new Color(0xC62828));
        rafraichirInfo();
        repaint();
    }

    private void selectionnerGroupe(Groupe g, boolean synchroniserListe) {
        groupeSelectionne = g;
        classeSelectionnee = null;
        if (synchroniserListe) {
            miseAJourInterne = true;
            int idx = modules.indexOf(g);
            if (idx >= 0) {
                listeModules.setSelectedIndex(idx);
            } else {
                listeModules.clearSelection();
            }
            miseAJourInterne = false;
        }
        rafraichirInfo();
        repaint();
    }

    private void selectionnerClasse(String c) {
        classeSelectionnee = c;
        groupeSelectionne = null;
        miseAJourInterne = true;
        Integer m = moduleDe.get(c);
        if (m != null) {
            listeModules.setSelectedIndex(m);
        }
        miseAJourInterne = false;
        rafraichirInfo();
        repaint();
    }

    private Color couleurClasse(String c) {
        Integer m = moduleDe.get(c);
        if (m == null || modules.get(m).classes.size() < 2) {
            return ISOLE;
        }
        return PALETTE[m % PALETTE.length];
    }

    private boolean correspond(String classe) {
        return !recherche.isEmpty() && classe.toLowerCase(Locale.ROOT).contains(recherche);
    }

    // ------------------------------------------------------------------ panneau d'inspection

    private void rafraichirInfo() {
        StringBuilder t = new StringBuilder();
        if (groupeSelectionne != null) {
            Groupe g = groupeSelectionne;
            t.append(g.estFeuille() ? "FEUILLE " : "GROUPE ").append(g.id).append('\n');
            t.append(g.libelle()).append("\n\n");
            t.append("Classes : ").append(g.classes.size()).append('\n');
            if (!g.estFeuille()) {
                t.append(String.format(Locale.ROOT, "Similarité de la fusion : %.4f%n", g.similarite));
                t.append("  (entre ses deux fils : ").append(g.gauche.id).append(" et ").append(g.droit.id).append(")\n");
                double c = modele.couplageInterne(g);
                t.append(String.format(Locale.ROOT, "Couplage interne moyen C(G) : %.4f%n", c));
                t.append(String.format(Locale.ROOT, "  C(G) %s CP (%.4f)%n", c > cp + 1e-9 ? ">" : "<=", cp));
                t.append("Fils gauche : ").append(g.gauche.libelle()).append('\n');
                t.append("Fils droit  : ").append(g.droit.libelle()).append('\n');
            }
            t.append('\n').append(situation(g)).append('\n');
            t.append("\nPaquetages : ").append(paquetages(g)).append('\n');
        } else if (classeSelectionnee != null) {
            String c = classeSelectionnee;
            t.append("CLASSE ").append(c).append("\n\n");
            t.append("Paquetage : ").append(Modele.paquetage(c)).append('\n');
            Integer m = moduleDe.get(c);
            if (m != null) {
                t.append("Module ").append(m + 1).append(" : ").append(modules.get(m).libelle()).append('\n');
            }
            t.append("\nVoisins (couplage décroissant) :\n");
            List<String[]> voisins = new ArrayList<>();
            for (String autre : modele.classes()) {
                double v = modele.couplage().valeur(c, autre);
                if (v > 0) {
                    voisins.add(new String[] {autre, String.valueOf(v)});
                }
            }
            voisins.sort((p, q) -> Double.compare(Double.parseDouble(q[1]), Double.parseDouble(p[1])));
            if (voisins.isEmpty()) {
                t.append("  aucun : classe isolée (aucun appel avec une autre classe).\n");
            }
            for (String[] v : voisins) {
                t.append(String.format(Locale.ROOT, "  %-18s r=%d  C=%.4f%n",
                        Groupe.nomCourt(v[0]), modele.nbArcs(c, v[0]), Double.parseDouble(v[1])));
            }
        } else {
            t.append("Cliquez sur un nœud du dendrogramme, une classe du graphe\nou un module de la liste.\n\n");
            t.append("Classes : ").append(modele.classes().size()).append('\n');
            t.append("|Einter| : ").append(modele.couplage().nbArcsInter()).append('\n');
            t.append("Couples non nuls : ").append(modele.couplage().nbCouplesNonNuls()).append('\n');
            t.append(String.format(Locale.ROOT, "Plus fort couplage : %.4f%n", modele.couplageMax()));
        }
        info.setText(t.toString());
        info.setCaretPosition(0);
    }

    private String situation(Groupe g) {
        int idx = modules.indexOf(g);
        if (idx >= 0) {
            return "→ C'est le module " + (idx + 1) + " de la coupe courante.";
        }
        for (int i = 0; i < modules.size(); i++) {
            if (modules.get(i).classes.containsAll(g.classes)) {
                return "→ Contenu dans le module " + (i + 1) + " (sous la coupe).";
            }
        }
        return "→ Au-dessus de la coupe : C(G) ne dépasse pas CP, on descend dans ses fils.";
    }

    private String paquetages(Groupe g) {
        SortedMap<String, Integer> p = new TreeMap<>();
        for (String c : g.classes) {
            p.merge(Modele.paquetage(c), 1, Integer::sum);
        }
        return p.toString();
    }

    // ------------------------------------------------------------------ export

    private void exporterVueCourante() {
        Component vue = onglets.getSelectedComponent();
        JFileChooser fc = new JFileChooser(new File("sorties"));
        fc.setSelectedFile(new File(onglets.getSelectedIndex() == 0 ? "vue-dendrogramme.png" : "vue-graphe.png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        BufferedImage img = new BufferedImage(vue.getWidth(), vue.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        vue.paint(g);
        g.dispose();
        try {
            ImageIO.write(img, "png", fc.getSelectedFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Export impossible : " + e.getMessage(), "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------ rendu de la liste des modules

    private final class RenduModule extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
            Groupe g = (Groupe) v;
            String interne = g.classes.size() < 2 ? "-" : formater(modele.couplageInterne(g));
            String texte = String.format(Locale.ROOT, "%2d  [%d]  C=%s  %s", i + 1, g.classes.size(), interne, g.libelle());
            JLabel e = (JLabel) super.getListCellRendererComponent(l, texte, i, sel, foc);
            e.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
            Color c = g.classes.size() < 2 ? ISOLE : PALETTE[i % PALETTE.length];
            e.setIcon(new Icon() {
                public void paintIcon(Component cc, Graphics gg, int x, int y) { gg.setColor(c); gg.fillRect(x, y + 2, 10, 10); }
                public int getIconWidth() { return 12; }
                public int getIconHeight() { return 14; }
            });
            return e;
        }
    }

    // ------------------------------------------------------------------ vue : dendrogramme

    private final class VueDendrogramme extends JPanel {
        private static final int MG = 30, MD = 190, MH = 20, MB = 66;
        private final Map<Groupe, Point2D.Double> pos = new IdentityHashMap<>();
        private double xFeuille;
        private double pasY;

        VueDendrogramme() {
            setBackground(Color.WHITE);
            ToolTipManager.sharedInstance().registerComponent(this);
            addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    Groupe g = trouver(e.getPoint());
                    if (g != null) {
                        selectionnerGroupe(g, true);
                    }
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    Groupe g = trouver(e.getPoint());
                    setToolTipText(g == null ? null : bulle(g));
                    setCursor(Cursor.getPredefinedCursor(g == null ? Cursor.DEFAULT_CURSOR : Cursor.HAND_CURSOR));
                }
            });
        }

        private String bulle(Groupe g) {
            if (g.estFeuille()) {
                return g.classes.get(0);
            }
            return String.format(Locale.ROOT, "<html>Groupe %d — %d classes<br>similarité de fusion %.4f<br>C(G) = %.4f</html>",
                    g.id, g.classes.size(), g.similarite, modele.couplageInterne(g));
        }

        private void calculer() {
            pos.clear();
            Groupe racine = modele.racine();
            if (racine == null) {
                return;
            }
            List<Groupe> feuilles = new ArrayList<>();
            ordonner(racine, feuilles);
            int n = feuilles.size();
            double hauteur = Math.max(1, getHeight() - MH - MB);
            pasY = n > 1 ? hauteur / (n - 1) : 0;
            double span = Math.max(50, getWidth() - MG - MD - 20);
            xFeuille = MG + span + 20;
            Map<Groupe, Double> yFeuille = new IdentityHashMap<>();
            for (int i = 0; i < n; i++) {
                yFeuille.put(feuilles.get(i), MH + i * pasY);
            }
            placer(racine, yFeuille, span);
        }

        private void ordonner(Groupe g, List<Groupe> out) {
            if (g.estFeuille()) {
                out.add(g);
            } else {
                ordonner(g.gauche, out);
                ordonner(g.droit, out);
            }
        }

        private double xSimilarite(double s, double span) {
            double max = Math.max(modele.similariteMax(), 1e-12);
            return MG + (s / max) * span; // racine (S = 0) à gauche, fusions fortes près des feuilles
        }

        private Point2D.Double placer(Groupe g, Map<Groupe, Double> yFeuille, double span) {
            Point2D.Double p;
            if (g.estFeuille()) {
                p = new Point2D.Double(xFeuille, yFeuille.get(g));
            } else {
                Point2D.Double a = placer(g.gauche, yFeuille, span);
                Point2D.Double b = placer(g.droit, yFeuille, span);
                p = new Point2D.Double(xSimilarite(g.similarite, span), (a.y + b.y) / 2);
            }
            pos.put(g, p);
            return p;
        }

        private Groupe trouver(Point m) {
            if (pos.isEmpty()) {
                calculer();
            }
            Groupe meilleur = null;
            double dmin = 10;
            for (Map.Entry<Groupe, Point2D.Double> e : pos.entrySet()) {
                Point2D.Double p = e.getValue();
                double d;
                if (e.getKey().estFeuille()) {
                    // la feuille inclut son étiquette
                    d = (m.x >= p.x - 8 && m.x <= p.x + MD - 10) ? Math.abs(m.y - p.y) : Double.MAX_VALUE;
                } else {
                    d = p.distance(m);
                }
                if (d < dmin) {
                    dmin = d;
                    meilleur = e.getKey();
                }
            }
            return meilleur;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            calculer();
            Groupe racine = modele.racine();
            if (racine == null) {
                g.drawString("Aucune classe à regrouper.", 20, 30);
                return;
            }
            dessinerAxe(g);
            Set<Groupe> sousArbre = Collections.newSetFromMap(new IdentityHashMap<>());
            if (groupeSelectionne != null) {
                collecter(groupeSelectionne, sousArbre);
            }
            dessiner(g, racine, sousArbre);
            dessinerNoeuds(g, racine);
            g.setFont(getFont().deriveFont(11f));
            g.setColor(Color.DARK_GRAY);
            g.drawString("Un module = une branche dont C(G) > CP (couleur) ; en gris : nœuds au-dessus de la coupe, isolées.",
                    MG, getHeight() - 8);
        }

        private void collecter(Groupe g, Set<Groupe> out) {
            out.add(g);
            if (!g.estFeuille()) {
                collecter(g.gauche, out);
                collecter(g.droit, out);
            }
        }

        private Color couleurNoeud(Groupe g) {
            for (int i = 0; i < modules.size(); i++) {
                if (modules.get(i).classes.containsAll(g.classes) && (g.classes.size() > 1 || modules.get(i) == g)) {
                    return modules.get(i).classes.size() < 2 ? ISOLE : PALETTE[i % PALETTE.length];
                }
            }
            return new Color(0x777777);
        }

        private void dessiner(Graphics2D g, Groupe n, Set<Groupe> sel) {
            if (n.estFeuille()) {
                return;
            }
            Point2D.Double p = pos.get(n);
            for (Groupe f : new Groupe[] {n.gauche, n.droit}) {
                Point2D.Double q = pos.get(f);
                g.setColor(couleurNoeud(f));
                g.setStroke(new BasicStroke(sel.contains(f) ? 3.2f : 1.6f));
                g.draw(new java.awt.geom.Line2D.Double(p.x, p.y, p.x, q.y));
                g.draw(new java.awt.geom.Line2D.Double(p.x, q.y, q.x, q.y));
                dessiner(g, f, sel);
            }
        }

        private void dessinerNoeuds(Graphics2D g, Groupe n) {
            Point2D.Double p = pos.get(n);
            boolean module = modules.contains(n);
            if (n.estFeuille()) {
                String nom = Groupe.nomCourt(n.classes.get(0));
                g.setFont(getFont().deriveFont(module || correspond(n.classes.get(0)) ? Font.BOLD : Font.PLAIN, 12f));
                FontMetrics fm = g.getFontMetrics();
                if (correspond(n.classes.get(0))) {
                    g.setColor(new Color(0xFFF176));
                    g.fillRect((int) p.x + 7, (int) p.y - fm.getAscent() + 2, fm.stringWidth(nom) + 6, fm.getHeight());
                }
                g.setColor(Color.BLACK);
                g.drawString(nom, (int) p.x + 10, (int) p.y + 4);
                g.setColor(couleurNoeud(n));
                g.fill(new java.awt.geom.Ellipse2D.Double(p.x - 3, p.y - 3, 6, 6));
            } else {
                dessinerNoeuds(g, n.gauche);
                dessinerNoeuds(g, n.droit);
                g.setColor(couleurNoeud(n));
                double r = module ? 6 : 3.5;
                g.fill(new java.awt.geom.Ellipse2D.Double(p.x - r, p.y - r, 2 * r, 2 * r));
                if (module) {
                    g.setColor(Color.BLACK);
                    g.setStroke(new BasicStroke(1f));
                    g.draw(new java.awt.geom.Ellipse2D.Double(p.x - r, p.y - r, 2 * r, 2 * r));
                }
            }
            if (n == groupeSelectionne) {
                g.setColor(new Color(0xD32F2F));
                g.setStroke(new BasicStroke(2f));
                g.draw(new java.awt.geom.Ellipse2D.Double(p.x - 9, p.y - 9, 18, 18));
            }
        }

        private void dessinerAxe(Graphics2D g) {
            double span = xFeuille - 20 - MG;
            double y = getHeight() - MB + 12;
            g.setColor(Color.GRAY);
            g.setStroke(new BasicStroke(1f));
            g.draw(new java.awt.geom.Line2D.Double(MG, y, MG + span, y));
            g.setFont(getFont().deriveFont(10.5f));
            double max = modele.similariteMax();
            for (int i = 0; i <= 4; i++) {
                double s = max * i / 4;
                double x = xSimilarite(s, span);
                g.draw(new java.awt.geom.Line2D.Double(x, y, x, y + 4));
                String txt = String.format(Locale.ROOT, "%.3f", s);
                g.drawString(txt, (int) x - g.getFontMetrics().stringWidth(txt) / 2, (int) y + 16);
            }
            g.drawString("similarité de fusion S (≠ C(G))", MG, (int) y + 28);
        }
    }

    // ------------------------------------------------------------------ vue : graphe de couplage

    private final class VueGraphe extends JPanel {
        private final JSlider seuil = new JSlider(0, 100, 0);
        private final JLabel etiquetteSeuil = new JLabel();
        private final Toile toile = new Toile();
        private final Map<String, Point2D.Double> pos = new HashMap<>();

        VueGraphe() {
            super(new BorderLayout());
            JPanel haut = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
            haut.add(new JLabel("Filtre : poids minimum des arêtes"));
            seuil.setPreferredSize(new Dimension(220, 22));
            seuil.addChangeListener(e -> { majEtiquette(); toile.repaint(); });
            haut.add(seuil);
            haut.add(etiquetteSeuil);
            add(haut, BorderLayout.NORTH);
            add(toile, BorderLayout.CENTER);
            majEtiquette();
        }

        private double poidsMin() {
            return seuil.getValue() / 100.0 * modele.couplageMax();
        }

        private void majEtiquette() {
            int rMin = (int) Math.ceil(poidsMin() * modele.couplage().nbArcsInter() - 1e-9);
            etiquetteSeuil.setText(String.format(Locale.ROOT, "C ≥ %.4f  (r ≥ %d)", poidsMin(), Math.max(rMin, 1)));
        }

        private final class Toile extends JPanel {
            Toile() {
                setBackground(Color.WHITE);
                addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        for (Map.Entry<String, Point2D.Double> en : pos.entrySet()) {
                            if (en.getValue().distance(e.getPoint()) <= 17) {
                                selectionnerClasse(en.getKey());
                                return;
                            }
                        }
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g0) {
                super.paintComponent(g0);
                Graphics2D g = (Graphics2D) g0;
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                majEtiquette();

                // disposition circulaire : les classes d'un même module sont voisines
                List<String> ordre = new ArrayList<>(modele.classes());
                ordre.sort(Comparator.<String>comparingInt(c -> moduleDe.getOrDefault(c, 999)).thenComparing(c -> c));
                double cx = getWidth() / 2.0, cy = getHeight() / 2.0;
                double rayon = Math.max(40, Math.min(getWidth(), getHeight()) / 2.0 - 75);
                pos.clear();
                for (int i = 0; i < ordre.size(); i++) {
                    double a = 2 * Math.PI * i / ordre.size() - Math.PI / 2;
                    pos.put(ordre.get(i), new Point2D.Double(cx + rayon * Math.cos(a), cy + rayon * Math.sin(a)));
                }

                double max = Math.max(modele.couplageMax(), 1e-12);
                double min = poidsMin() - 1e-12;
                g.setFont(getFont().deriveFont(10.5f));
                List<Couple> couples = modele.couplage().couplesTries();
                for (int pass = 0; pass < 2; pass++) { // arêtes de fond d'abord, celles de la sélection par-dessus
                    for (Couple c : couples) {
                        double w = modele.couplage().valeur(c.classeA, c.classeB);
                        if (w < min) {
                            continue;
                        }
                        boolean liee = c.classeA.equals(classeSelectionnee) || c.classeB.equals(classeSelectionnee);
                        if ((pass == 1) != liee) {
                            continue;
                        }
                        Point2D.Double a = pos.get(c.classeA), b = pos.get(c.classeB);
                        Integer ma = moduleDe.get(c.classeA), mb = moduleDe.get(c.classeB);
                        boolean memeModule = ma != null && ma.equals(mb) && modules.get(ma).classes.size() > 1;
                        g.setColor(liee ? new Color(0xD32F2F) : memeModule
                                ? withAlpha(PALETTE[ma % PALETTE.length], 190) : new Color(0x99, 0x99, 0x99, 150));
                        g.setStroke(new BasicStroke((float) (1 + 6 * w / max)));
                        g.draw(new java.awt.geom.Line2D.Double(a.x, a.y, b.x, b.y));
                        if (couples.size() <= 30 || liee) {
                            g.setColor(Color.DARK_GRAY);
                            g.drawString(String.valueOf(c.nbArcs), (float) ((a.x + b.x) / 2 + 3), (float) ((a.y + b.y) / 2 - 3));
                        }
                    }
                }
                for (String c : ordre) {
                    Point2D.Double p = pos.get(c);
                    boolean isole = true;
                    for (String autre : ordre) {
                        if (modele.couplage().valeur(c, autre) > 0) { isole = false; break; }
                    }
                    g.setColor(couleurClasse(c));
                    g.fill(new java.awt.geom.Ellipse2D.Double(p.x - 12, p.y - 12, 24, 24));
                    g.setColor(Color.BLACK);
                    g.setStroke(isole ? new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[] {3f}, 0f)
                            : new BasicStroke(1.2f));
                    g.draw(new java.awt.geom.Ellipse2D.Double(p.x - 12, p.y - 12, 24, 24));
                    if (c.equals(classeSelectionnee)) {
                        g.setColor(new Color(0xD32F2F));
                        g.setStroke(new BasicStroke(2.5f));
                        g.draw(new java.awt.geom.Ellipse2D.Double(p.x - 16, p.y - 16, 32, 32));
                    }
                    String nom = Groupe.nomCourt(c);
                    g.setFont(getFont().deriveFont(correspond(c) ? Font.BOLD : Font.PLAIN, 12f));
                    FontMetrics fm = g.getFontMetrics();
                    double dx = p.x - cx, dy = p.y - cy;
                    double norme = Math.max(1, Math.hypot(dx, dy));
                    int lx = (int) (p.x + dx / norme * 22 - (dx < -5 ? fm.stringWidth(nom) : dx > 5 ? 0 : fm.stringWidth(nom) / 2.0));
                    int ly = (int) (p.y + dy / norme * 22 + (dy > 0 ? fm.getAscent() * 0.8 : 0));
                    if (correspond(c)) {
                        g.setColor(new Color(0xFFF176));
                        g.fillRect(lx - 2, ly - fm.getAscent() + 2, fm.stringWidth(nom) + 4, fm.getHeight());
                    }
                    g.setColor(Color.BLACK);
                    g.drawString(nom, lx, ly);
                }
                g.setFont(getFont().deriveFont(11f));
                g.setColor(Color.DARK_GRAY);
                g.drawString("Épaisseur ∝ C(A,B) ; nombre = r(A,B) ; couleur = module ; pointillés = classe isolée.", 12, getHeight() - 8);
            }
        }

        private Color withAlpha(Color c, int a) {
            return new Color(c.getRed(), c.getGreen(), c.getBlue(), a);
        }
    }
}