package com.midTerm;

// this is an enum used for indicating a character
public enum GameCharacter {
    GODFATHER(1, false, 0),
    DOCTORLECTOR(1, false, 1),
    MAFIA(1, false, 2),
    MAYOR(1, true, 3),
    DOCTOR(1, true, 4),
    DETECTOR(1, true, 5),
    PROFESSIONAL(1, true, 6),
    MENTALIST(1, true, 7),
    DIEHARD(2, true, 8),
    CITIZEN(1, true, 9);

    private int lives;
    private int priority;
    private boolean isCitizen;
    GameCharacter(int lives, boolean isCitizen, int priority) {
        this.lives = lives;
        this.isCitizen = isCitizen;
        this.priority = priority;
    }

    public void decreaseLive() {
        lives -= 1;
    }

    public static GameCharacter getCharacterByName(String name) {
        for (var character : GameCharacter.values())
            if (name.equalsIgnoreCase(character.toString()))
                return character;
        return null;
    }

    public int getLives() {
        return lives;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isCitizen() {
        return isCitizen;
    }
}
