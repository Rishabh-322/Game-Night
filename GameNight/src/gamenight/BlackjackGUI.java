package gamenight;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import javax.swing.*;

/**
 * BlackjackGUI - Full Blackjack implementation for Game Night.
 * Supports 1-3 human players vs CPU dealer.
 *
 * Game flow:
 *  1. BETTING phase  - each player enters a bet and clicks Deal
 *  2. PLAYING phase  - players take turns hitting/staying
 *  3. DEALER phase   - dealer auto-plays (hits until score >= 17)
 *  4. PAYOUT phase   - results shown, next round available
 */
public class BlackjackGUI extends JFrame {

    // ---- Phase constants ----
    private static final int PHASE_BETTING = 0;
    private static final int PHASE_PLAYING = 1;
    private static final int PHASE_DEALER  = 2;
    private static final int PHASE_PAYOUT  = 3;

    // ---- Game state ----
    private Deck deck;
    private final int numPlayers;
    private int gamePhase;
    private int currentPlayer;
    private int betsCollected;

    @SuppressWarnings("unchecked")
    private ArrayList<Card>[] playerHands = new ArrayList[0];
    private ArrayList<Card> dealerHand;
    private int[]     playerMoney;
    private int[]     playerBets;
    private boolean[] playerStood;
    private boolean[] playerBust;
    private boolean   dealerRevealed;

    // ---- UI - Dealer ----
    private JLabel dealerScoreLabel;
    private JPanel dealerCardsPanel;

    // ---- UI - Players ----
    private JPanel[] playerPanels;
    private JLabel[] playerMoneyLabels;
    private JLabel[] playerBetLabels;
    private JLabel[] playerScoreLabels;
    private JPanel[] playerCardsPanels;

    // ---- UI - Controls ----
    private JLabel     statusLabel;
    private JTextField betField;
    private JButton    dealButton;
    private JButton    hitButton;
    private JButton    stayButton;
    private JButton    nextRoundButton;

    // ---- Colors ----
    private static final Color TABLE_GREEN = new Color(0, 102, 0);
    private static final Color DARK_GREEN  = new Color(0, 70, 0);
    private static final Color GOLD        = new Color(212, 175, 55);
    private static final Color GOLD_BRIGHT = new Color(255, 215, 0);
    private static final Color DIM_BORDER  = new Color(80, 120, 80);

    // ================================================================
    //  Constructors
    // ================================================================

    public BlackjackGUI(int numPlayers) {
        this.numPlayers = Math.max(1, Math.min(3, numPlayers));
        initGameState();
        buildUI();
        startBettingPhase();
    }

    public BlackjackGUI() { this(1); }

    // ================================================================
    //  State init
    // ================================================================

    @SuppressWarnings("unchecked")
    private void initGameState() {
        playerHands  = new ArrayList[numPlayers];
        playerMoney  = new int[numPlayers];
        playerBets   = new int[numPlayers];
        playerStood  = new boolean[numPlayers];
        playerBust   = new boolean[numPlayers];
        for (int i = 0; i < numPlayers; i++) {
            playerHands[i] = new ArrayList<>();
            playerMoney[i] = 1000;
        }
        dealerHand = new ArrayList<>();
        deck = new Deck();
        deck.shuffle();
    }

    private void resetRound() {
        for (int i = 0; i < numPlayers; i++) {
            playerHands[i].clear();
            playerBets[i]  = 0;
            playerStood[i] = false;
            playerBust[i]  = false;
        }
        dealerHand.clear();
        dealerRevealed = false;
        betsCollected  = 0;
        if (deck.countCards() < 15) { deck = new Deck(); deck.shuffle(); }
    }

    // ================================================================
    //  UI Construction
    // ================================================================

    private void buildUI() {
        setTitle("Blackjack – Game Night");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem mainItem = new JMenuItem("Main Menu");
        mainItem.addActionListener(e -> { dispose(); new StartGUI().setVisible(true); });
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(mainItem);
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(TABLE_GREEN);
        root.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        root.add(buildDealerSection(),   BorderLayout.NORTH);
        root.add(buildPlayersSection(),  BorderLayout.CENTER);
        root.add(buildControlsSection(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    private JPanel buildDealerSection() {
        JPanel outer = new JPanel(new BorderLayout(5, 5));
        outer.setBackground(DARK_GREEN);
        outer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(GOLD, 2),
            BorderFactory.createEmptyBorder(6, 8, 6, 8)
        ));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        header.setBackground(DARK_GREEN);

        JLabel title = new JLabel("DEALER  (CPU)");
        title.setForeground(GOLD_BRIGHT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));

        dealerScoreLabel = new JLabel("Score: ??");
        dealerScoreLabel.setForeground(Color.WHITE);
        dealerScoreLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        header.add(title);
        header.add(dealerScoreLabel);
        outer.add(header, BorderLayout.NORTH);

        dealerCardsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
        dealerCardsPanel.setBackground(DARK_GREEN);
        dealerCardsPanel.setPreferredSize(new Dimension(700, 107));

        JScrollPane scroll = new JScrollPane(dealerCardsPanel,
            JScrollPane.VERTICAL_SCROLLBAR_NEVER,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(DARK_GREEN);
        outer.add(scroll, BorderLayout.CENTER);

        return outer;
    }

    private JPanel buildPlayersSection() {
        JPanel grid = new JPanel(new GridLayout(1, numPlayers, 8, 0));
        grid.setBackground(TABLE_GREEN);
        grid.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));

        playerPanels      = new JPanel[numPlayers];
        playerMoneyLabels = new JLabel[numPlayers];
        playerBetLabels   = new JLabel[numPlayers];
        playerScoreLabels = new JLabel[numPlayers];
        playerCardsPanels = new JPanel[numPlayers];

        for (int i = 0; i < numPlayers; i++) {
            JPanel panel = new JPanel(new BorderLayout(4, 4));
            panel.setBackground(DARK_GREEN);
            panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIM_BORDER, 2),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)
            ));
            playerPanels[i] = panel;

            JPanel info = new JPanel(new GridLayout(4, 1, 2, 2));
            info.setBackground(DARK_GREEN);

            JLabel nameLbl = new JLabel("Player " + (i + 1), SwingConstants.CENTER);
            nameLbl.setForeground(GOLD_BRIGHT);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));

            playerMoneyLabels[i] = new JLabel("Money: $1000", SwingConstants.CENTER);
            playerMoneyLabels[i].setForeground(Color.WHITE);
            playerMoneyLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));

            playerBetLabels[i] = new JLabel("Bet: –", SwingConstants.CENTER);
            playerBetLabels[i].setForeground(new Color(255, 200, 50));
            playerBetLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));

            playerScoreLabels[i] = new JLabel("Score: –", SwingConstants.CENTER);
            playerScoreLabels[i].setForeground(Color.WHITE);
            playerScoreLabels[i].setFont(new Font("Segoe UI", Font.PLAIN, 12));

            info.add(nameLbl);
            info.add(playerMoneyLabels[i]);
            info.add(playerBetLabels[i]);
            info.add(playerScoreLabels[i]);
            panel.add(info, BorderLayout.NORTH);

            JPanel cards = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 3));
            cards.setBackground(DARK_GREEN);
            cards.setPreferredSize(new Dimension(220, 107));
            playerCardsPanels[i] = cards;

            JScrollPane scroll = new JScrollPane(cards,
                JScrollPane.VERTICAL_SCROLLBAR_NEVER,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scroll.setBorder(null);
            scroll.getViewport().setBackground(DARK_GREEN);
            panel.add(scroll, BorderLayout.CENTER);

            grid.add(panel);
        }
        return grid;
    }

    private JPanel buildControlsSection() {
        JPanel outer = new JPanel(new BorderLayout(4, 4));
        outer.setBackground(TABLE_GREEN);

        statusLabel = new JLabel("Welcome to Blackjack!", SwingConstants.CENTER);
        statusLabel.setForeground(GOLD_BRIGHT);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        outer.add(statusLabel, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 6));
        row.setBackground(TABLE_GREEN);

        JLabel betLbl = new JLabel("Bet: $");
        betLbl.setForeground(Color.WHITE);
        betLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));

        betField = new JTextField("100", 5);
        betField.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        dealButton      = makeBtn("Deal");
        hitButton       = makeBtn("Hit");
        stayButton      = makeBtn("Stay");
        nextRoundButton = makeBtn("Next Round");

        dealButton.addActionListener(e -> handleDeal());
        hitButton.addActionListener(e  -> handleHit());
        stayButton.addActionListener(e -> handleStay());
        nextRoundButton.addActionListener(e -> {
            resetRound();
            clearAllUI();
            startBettingPhase();
        });

        hitButton.setEnabled(false);
        stayButton.setEnabled(false);
        nextRoundButton.setEnabled(false);

        row.add(betLbl);
        row.add(betField);
        row.add(dealButton);
        row.add(Box.createHorizontalStrut(15));
        row.add(hitButton);
        row.add(stayButton);
        row.add(Box.createHorizontalStrut(15));
        row.add(nextRoundButton);

        outer.add(row, BorderLayout.CENTER);
        return outer;
    }

    private JButton makeBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.BOLD, 13));
        return b;
    }

    // ================================================================
    //  Phase transitions
    // ================================================================

    private void startBettingPhase() {
        gamePhase     = PHASE_BETTING;
        currentPlayer = 0;
        betsCollected = 0;
        setButtons(true, false, false, false);
        highlightPlayer(0);
        setStatus("Player 1 — enter your bet and click Deal");
    }

    private void dealCards() {
        gamePhase = PHASE_PLAYING;

        for (int i = 0; i < numPlayers; i++) {
            playerHands[i].add(deck.deal());
            playerHands[i].add(deck.deal());
        }
        dealerHand.add(deck.deal());
        dealerHand.add(deck.deal());

        refreshDealerUI();
        for (int i = 0; i < numPlayers; i++) refreshPlayerUI(i);

        // Auto-stand any player dealt 21
        for (int i = 0; i < numPlayers; i++) {
            if (getScore(playerHands[i]) == 21) playerStood[i] = true;
        }

        advanceToNext(-1);
    }

    private void advanceToNext(int from) {
        for (int i = from + 1; i < numPlayers; i++) {
            if (!playerStood[i] && !playerBust[i]) {
                currentPlayer = i;
                highlightPlayer(i);
                setButtons(false, true, true, false);
                setStatus("Player " + (i + 1) + " — Hit or Stay  (Score: "
                          + getScore(playerHands[i]) + ")");
                return;
            }
        }
        runDealerTurn();
    }

    private void runDealerTurn() {
        gamePhase      = PHASE_DEALER;
        dealerRevealed = true;
        setButtons(false, false, false, false);
        refreshDealerUI();
        setStatus("Dealer reveals hand — Score: " + getScore(dealerHand));

        Timer t = new Timer(700, null);
        t.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int score = getScore(dealerHand);
                if (score < 17) {
                    dealerHand.add(deck.deal());
                    refreshDealerUI();
                    setStatus("Dealer hits... (Score: " + getScore(dealerHand) + ")");
                } else {
                    t.stop();
                    evaluateRound();
                }
            }
        });

        if (getScore(dealerHand) < 17) t.start();
        else evaluateRound();
    }

    private void evaluateRound() {
        gamePhase = PHASE_PAYOUT;
        int dealerScore = getScore(dealerHand);
        boolean dealerBust = dealerScore > 21;

        StringBuilder sb = new StringBuilder(
            dealerBust ? "Dealer BUSTED (" + dealerScore + ")! — "
                       : "Dealer stands at " + dealerScore + " — "
        );

        for (int i = 0; i < numPlayers; i++) {
            int   pScore  = getScore(playerHands[i]);
            int   bet     = playerBets[i];
            Color color   = Color.WHITE;
            String label;

            if (playerBust[i]) {
                playerMoney[i] -= bet;
                label = "P" + (i + 1) + ": BUST –$" + bet;
                color = Color.RED;
            } else if (dealerBust || pScore > dealerScore) {
                playerMoney[i] += bet;
                label = "P" + (i + 1) + ": WIN +$" + bet;
                color = Color.GREEN;
            } else if (pScore == dealerScore) {
                label = "P" + (i + 1) + ": PUSH";
                color = Color.YELLOW;
            } else {
                playerMoney[i] -= bet;
                label = "P" + (i + 1) + ": LOSE –$" + bet;
                color = Color.RED;
            }

            playerScoreLabels[i].setText("Score: " + pScore + (playerBust[i] ? " BUST" :
                                          playerStood[i] ? " Stood" : ""));
            playerScoreLabels[i].setForeground(color);
            playerMoneyLabels[i].setText("Money: $" + Math.max(0, playerMoney[i]));
            if (playerMoney[i] <= 0) {
                playerMoney[i] = 0;
                playerMoneyLabels[i].setForeground(Color.RED);
            }

            sb.append(label);
            if (i < numPlayers - 1) sb.append("  |  ");
        }

        unhighlightAll();
        setStatus(sb.toString());

        boolean allBroke = true;
        for (int m : playerMoney) if (m > 0) { allBroke = false; break; }

        if (allBroke) {
            setStatus("Game over — all players are out of money!");
            setButtons(false, false, false, false);
        } else {
            setButtons(false, false, false, true);
        }
    }

    // ================================================================
    //  Button handlers
    // ================================================================

    private void handleDeal() {
        if (gamePhase != PHASE_BETTING) return;

        int bet;
        try { bet = Integer.parseInt(betField.getText().trim()); }
        catch (NumberFormatException ex) { setStatus("Enter a valid bet amount."); return; }

        if (bet <= 0) { setStatus("Bet must be greater than $0."); return; }
        if (bet > playerMoney[currentPlayer]) {
            setStatus("Player " + (currentPlayer + 1) + " only has $"
                      + playerMoney[currentPlayer] + "!");
            return;
        }

        playerBets[currentPlayer] = bet;
        playerBetLabels[currentPlayer].setText("Bet: $" + bet);
        betsCollected++;

        if (betsCollected < numPlayers) {
            // Find next player who still has money
            boolean found = false;
            for (int i = betsCollected; i < numPlayers; i++) {
                if (playerMoney[i] > 0) {
                    currentPlayer = i;
                    highlightPlayer(i);
                    betField.setText("100");
                    setStatus("Player " + (i + 1) + " — enter your bet and click Deal");
                    found = true;
                    break;
                }
                // Broke player skips betting (bet stays 0)
                playerBetLabels[i].setText("Bet: $0 (out)");
                betsCollected++;
            }
            if (!found) {
                // All remaining players are broke, just deal
                betField.setEnabled(false);
                dealButton.setEnabled(false);
                dealCards();
            }
        } else {
            betField.setEnabled(false);
            dealButton.setEnabled(false);
            dealCards();
        }
    }

    private void handleHit() {
        if (gamePhase != PHASE_PLAYING) return;

        playerHands[currentPlayer].add(deck.deal());
        refreshPlayerUI(currentPlayer);
        int score = getScore(playerHands[currentPlayer]);

        if (score > 21) {
            playerBust[currentPlayer] = true;
            playerScoreLabels[currentPlayer].setText("Score: " + score + " — BUST!");
            playerScoreLabels[currentPlayer].setForeground(Color.RED);
            setStatus("Player " + (currentPlayer + 1) + " busts with " + score + "!");
            setButtons(false, false, false, false);
            int cp = currentPlayer;
            Timer t = new Timer(900, e -> advanceToNext(cp));
            t.setRepeats(false); t.start();
        } else if (score == 21) {
            playerStood[currentPlayer] = true;
            playerScoreLabels[currentPlayer].setText("Score: 21 — Stood");
            setStatus("Player " + (currentPlayer + 1) + " hits 21! Auto-standing.");
            setButtons(false, false, false, false);
            int cp = currentPlayer;
            Timer t = new Timer(900, e -> advanceToNext(cp));
            t.setRepeats(false); t.start();
        } else {
            setStatus("Player " + (currentPlayer + 1) + " hits — Score: "
                      + score + ". Hit or Stay?");
        }
    }

    private void handleStay() {
        if (gamePhase != PHASE_PLAYING) return;
        playerStood[currentPlayer] = true;
        int score = getScore(playerHands[currentPlayer]);
        playerScoreLabels[currentPlayer].setText("Score: " + score + " — Stood");
        setStatus("Player " + (currentPlayer + 1) + " stays at " + score + ".");
        setButtons(false, false, false, false);
        int cp = currentPlayer;
        Timer t = new Timer(600, e -> advanceToNext(cp));
        t.setRepeats(false); t.start();
    }

    // ================================================================
    //  Scoring
    // ================================================================

    private int getScore(ArrayList<Card> hand) {
        int score = 0, aces = 0;
        for (Card c : hand) {
            String v = c.getValue();
            switch (v) {
                case "Ace":                     score += 11; aces++; break;
                case "Jack": case "Queen": case "King": score += 10; break;
                default:                        score += Integer.parseInt(v);
            }
        }
        while (score > 21 && aces > 0) { score -= 10; aces--; }
        return score;
    }

    // ================================================================
    //  UI helpers
    // ================================================================

    private void refreshDealerUI() {
        dealerCardsPanel.removeAll();
        for (int i = 0; i < dealerHand.size(); i++) {
            String path = (i == 1 && !dealerRevealed)
                ? "/images/b1fv.png" : dealerHand.get(i).getImage();
            dealerCardsPanel.add(cardLabel(path));
        }
        if (dealerRevealed && !dealerHand.isEmpty()) {
            int s = getScore(dealerHand);
            dealerScoreLabel.setText("Score: " + s + (s > 21 ? " — BUST!" : ""));
            dealerScoreLabel.setForeground(s > 21 ? Color.RED : Color.WHITE);
        } else {
            dealerScoreLabel.setText(dealerHand.isEmpty() ? "Score: ??" : "Score: ?? (card hidden)");
            dealerScoreLabel.setForeground(Color.WHITE);
        }
        dealerCardsPanel.revalidate();
        dealerCardsPanel.repaint();
    }

    private void refreshPlayerUI(int i) {
        playerCardsPanels[i].removeAll();
        for (Card c : playerHands[i]) playerCardsPanels[i].add(cardLabel(c.getImage()));
        if (!playerHands[i].isEmpty()) {
            int s = getScore(playerHands[i]);
            playerScoreLabels[i].setText("Score: " + s);
            playerScoreLabels[i].setForeground(Color.WHITE);
        }
        playerCardsPanels[i].revalidate();
        playerCardsPanels[i].repaint();
    }

    private void clearAllUI() {
        dealerScoreLabel.setText("Score: ??");
        dealerScoreLabel.setForeground(Color.WHITE);
        dealerCardsPanel.removeAll();
        dealerCardsPanel.revalidate();
        dealerCardsPanel.repaint();
        for (int i = 0; i < numPlayers; i++) {
            playerBetLabels[i].setText("Bet: –");
            playerScoreLabels[i].setText("Score: –");
            playerScoreLabels[i].setForeground(Color.WHITE);
            playerMoneyLabels[i].setForeground(Color.WHITE);
            playerMoneyLabels[i].setText("Money: $" + playerMoney[i]);
            playerCardsPanels[i].removeAll();
            playerCardsPanels[i].revalidate();
            playerCardsPanels[i].repaint();
        }
        unhighlightAll();
    }

    private JLabel cardLabel(String path) {
        JLabel lbl = new JLabel();
        try {
            java.net.URL url = getClass().getResource(path);
            if (url != null) lbl.setIcon(new ImageIcon(url));
            else { lbl.setText("?"); lbl.setForeground(Color.WHITE);
                   lbl.setPreferredSize(new Dimension(71, 97));
                   lbl.setHorizontalAlignment(SwingConstants.CENTER); }
        } catch (Exception ignored) {}
        return lbl;
    }

    private void highlightPlayer(int idx) {
        unhighlightAll();
        if (idx >= 0 && idx < numPlayers)
            playerPanels[idx].setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GOLD_BRIGHT, 3),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
    }

    private void unhighlightAll() {
        if (playerPanels == null) return;
        for (JPanel p : playerPanels)
            p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DIM_BORDER, 2),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
    }

    private void setStatus(String msg) {
        statusLabel.setText("<html><center>" + msg + "</center></html>");
    }

    private void setButtons(boolean deal, boolean hit, boolean stay, boolean next) {
        dealButton.setEnabled(deal);
        betField.setEnabled(deal);
        hitButton.setEnabled(hit);
        stayButton.setEnabled(stay);
        nextRoundButton.setEnabled(next);
    }

    // ================================================================
    //  Standalone entry point
    // ================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new BlackjackGUI(2).setVisible(true));
    }
}
