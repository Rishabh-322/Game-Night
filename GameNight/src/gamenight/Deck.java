/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gamenight;


import java.util.ArrayList;
import java.util.Collections;

public class Deck {

    private ArrayList<Card> deck;

    // Constructor
    public Deck() {
        deck = new ArrayList<Card>();

        // Create 52 cards
        for (int suit = 0; suit < 4; suit++) {
            for (int value = 0; value < 13; value++) {
                deck.add(new Card(suit, value));
            }
        }
    }

    // Shuffle the deck using Collections.shuffle()
    public void shuffle() {
        Collections.shuffle(deck);
    }

    // Deal one card from the top of the deck
    public Card deal() {
        if (deck.size() > 0) {
            return deck.remove(0);
        }
        return null;
    }

    // Print all cards
    public void printAllCards() {
        for (Card card : deck) {
            System.out.println(card);
        }
    }

    // Count cards
    public int countCards() {
        return deck.size();
    }
}
