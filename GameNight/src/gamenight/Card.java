/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gamenight;


public class Card {

    private int suit, value;

    private String[] cardSuit = {"Spades", "Diamonds", "Hearts", "Clubs"};
    private String[] cardValue = {"Ace", "2", "3", "4", "5",
                                  "6", "7", "8", "9", "10",
                                  "Jack", "Queen", "King"};

    // Constructor
    public Card(int cSuit, int cValue) {
        suit = cSuit;
        value = cValue;
    }

    // Getter for suit
    public String getSuit() {
        return cardSuit[suit];
    }

    // Getter for value
    public String getValue() {
        return cardValue[value];
    }

    // Returns the path and name of the card image
    public String getImage() {
        return "/images/" + cardValue[value] + cardSuit[suit] + ".png";
    }

    // toString method
    public String toString() {
        return cardValue[value] + " of " + cardSuit[suit];
    }
}
