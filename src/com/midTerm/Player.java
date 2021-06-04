package com.midTerm;

public class Player {
    // character is one of the game characters which is indicated to this player
    // characterDescription is the description of the characters' abilities and role
    private GameCharacter character;
    private String characterDescription;

    /**
     * this is a constructor
     * @param character is the character and role which is given to the player
     */
    public Player(GameCharacter character, String characterDescription) {
        this.characterDescription = characterDescription;
        this.character = character;
    }

    /**
     * this method returns the character of the player
     * @return character of the player
     */
    public GameCharacter getCharacter() {
        return  character;
    }
}
