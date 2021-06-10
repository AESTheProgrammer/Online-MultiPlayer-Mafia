package com.midTerm;

// this is an enum used for indicating a character
public enum GameCharacter {
    GODFATHER(1, false, 0, 0),
    DOCTORLECTOR(1, false, 1, 1),
    MAFIA(1, false, 2, 0),
    MAYOR(1, true, 3, 0),
    DOCTOR(1, true, 4, 1),
    DETECTOR(1, true, 5, 0),
    PROFESSIONAL(1, true, 6, 0),
    MENTALIST(1, true, 7, 0),
    DIEHARD(2, true, 8, 2),
    CITIZEN(1, true, 9, 0);

    // is the number of lives for the specific character
    // is the priority of this player in making a game
    // a boolean for being a citizen or not
    // is an arbitrarily constrain
    private int lives;
    private int priority;
    private boolean isCitizen;
    private int constraint;
    /**
     * this is a constructor
     * @param lives is the number of lives
     * @param isCitizen refers to being a citizen or mafia
     * @param priority is the priority of this player
     * @param constraint is an arbitrarily constraint
     */
    GameCharacter(int lives, boolean isCitizen, int priority, int constraint) {
        this.lives = lives;
        this.isCitizen = isCitizen;
        this.priority = priority;
        this.constraint = constraint;
    }

    /**
     * this method get a name and returns the character
     * @param name of that character
     * @return real instance of that character
     */
    public static GameCharacter getCharacterByName(String name) {
        for (var character : GameCharacter.values())
            if (name.equalsIgnoreCase(character.toString()))
                return character;
        return null;
    }

    /**
     * @return lives of the character
     */
    public int getLives() {
        return lives;
    }

    /**
     * @return priority of the character
     */
    public int getPriority() {
        return priority;
    }

    /**
     * @return if it is citizen or not
     */
    public boolean isCitizen() {
        return isCitizen;
    }

    /**
     * decrease number of lives of the character by one
     */
    public void decreaseLive() {
        lives -= 1;
    }

    /**
     * increase number of lives of the character by one
     */
    public void increaseLives() {
        lives += 1;
    }

    /**
     * decrease the constraint by 1
     */
    public void decreaseConstraint() {
        constraint--;
    }

    /**
     * this is a getter
     * @return the constrain of the character
     */
    public int getConstraint() {
        return constraint;
    }
}
