package com.midTerm;

// this is an enum used for indicating a character
public enum GameCharacter {
    GODFATHER(1),
    MAFIA(1),
    DOCTORLECTOR(1),
    CITIZEN(1),
    MAYOR(1),
    DOCTOR(1),
    DIEHARD(2),
    MENTALIST(1),
    PROFESSIONAL(1),
    DETECTOR(1);

    private int lives;
    GameCharacter(int lives) {

    }

    public int getLives() {
        return lives;
    }
}
