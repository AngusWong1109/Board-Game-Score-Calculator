package ca.cmpt276.iteration1.model;

import java.util.ArrayList;

/**
 * Represents a type of game. Stores the name (type) of the game, what a good score (per player)
 * would look like and what a bad score (per player) would look like
 */
public class GameType {
    private int goodScore;
    private int badScore;
    private String type;

    // Names for different achievement levels, sorted from worst to best
    private static final String[][] achievementLevels = {
            { // Animals
                "Cowardly Cows", // Below a bad score
                "Lowly Lamas",
                "Dead Dodos",
                "Average Alligators",
                "Fragrant Fish",
                "Excellent Eggs",
                "Beautiful Bears",
                "Godly Goats" // Above a good score
            },
            {
                "Devious Dragons",
                "Beautiful Basilisks",
                "Crafty Chimeras",
                "Subversive Sirens",
                "Keen Krakens",
                "Venomous Vampires",
                "Menacing Minotaurs",
                "Wonderful Werewolves"
            },
            { // Spongebob
                "Placid Patricks",
                "Sluggish Squidwards",
                "Standard Sandies",
                "Lethargic Larrys",
                "Pitiful Plankton",
                "Menacing Mr. Krabs",
                "Marvelous Mermaid Man",
                "Super Spongebob"
            }
    };

    public GameType(String type, int goodScore, int badScore) {
        this.type = type;
        this.goodScore = goodScore;
        this.badScore = badScore;

        if (goodScore < badScore) {
            throw new IllegalArgumentException("Bad score should be less than the good score.");
        }

        if (goodScore == badScore) {
            throw new IllegalArgumentException("Bad and good scores should not be equal!");
        }
    }

    /**
     * Gets the name of a specific achievement.
     *
     * Static so it can be called anywhere.
     * @param achievementIndex Index of the achievement earned (0 - max # of achievements)
     * @param achievementTheme Theme of the achievement to get
     * @return Name of an achievement
     */
    public static String getAchievementName(int achievementIndex, int achievementTheme) {
        return achievementLevels[achievementTheme][achievementIndex];
    }

    public String getGameType() {
        return type;
    }

    public int getGoodScore() {
        return goodScore;
    }

    public int getBadScore() {
        return badScore;
    }

    public void editGameType(String type, int goodScore, int badScore) {
        this.type = type;
        this.goodScore = goodScore;
        this.badScore = badScore;
    }

    /**
     * Maps a value from one range to another
     *
     * Ex: x = 10, [0,100] -> [0,10] = 1
     */
    private int map(int val, int oldMinimum, int oldMaximum, int newMinimum, int newMaximum) {
        // Math from https://math.stackexchange.com/questions/914823/shift-numbers-into-a-different-range
        float valueScale = (newMaximum - newMinimum)/(float)(oldMaximum - oldMinimum);
        float endpointShift = val - oldMinimum;
        int newValue = (int) Math.floor(newMinimum + (valueScale * endpointShift));

        return newValue;
    }

    /**
     * Gets the index of the achievement that was earned
     * @param score Score of the game
     * @param playerNumber Number of players in a game
     * @return 0 - max number of achievements, whichever one was earned by the player
     */
    public int getAchievementIndex(int score, int playerNumber) {
        int achievementTheme = GameManager.getInstance().getAchievementTheme();

        // Number of achievements
        int achievementCount = achievementLevels[achievementTheme].length;

        score /= playerNumber;

        // If worse than a bad score, return the worst achievement levels
        if (score < badScore) {
            return 0;
        }

        if (score > goodScore) {
            return achievementCount - 1;
        }

        // Scale the score to range from 1 to the number of achievements - 1
        int achievementIndex = map(score,badScore, goodScore, 1, achievementCount - 2);
        return achievementIndex;
    }


    public String getAchievementLevel(int score, int playerNumber) {
        int achievementTheme = GameManager.getInstance().getAchievementTheme();

        // Index of the achievement in a list of achievements
        int achievementIndex = getAchievementIndex(score, playerNumber);

        return getAchievementName(achievementIndex,achievementTheme);
    }

    public ArrayList<String> getAchievementLevelScoreRequirements(int playerNumber){
        int achievementTheme = GameManager.getInstance().getAchievementTheme();

        ArrayList<String> res = new ArrayList<>();
        // gets the good score - bad score and divides by 5 to get the intervals between achievements
        float difference = (float)(goodScore-badScore)/5;

        int max = achievementLevels[achievementTheme].length;

        res.add(achievementLevels[achievementTheme][0] + " <" + badScore*playerNumber);
        res.add(achievementLevels[achievementTheme][1] + " " + badScore*playerNumber);
        for(int i = 2; i<=max-3; i++){
            // below algo uses the formula from https://math.stackexchange.com/questions/914823/shift-numbers-into-a-different-range
            // Isolated t which is the score requirement for i which is the index of the achievement levels
            // if the game score divided by player number is >= to t then that is the achievement the game gets.
            int minScoreRequirement = (int) Math.ceil((((i-1)*difference) + badScore));
            minScoreRequirement = (minScoreRequirement*playerNumber);
            res.add(achievementLevels[achievementTheme][i] + " " + (minScoreRequirement));
        }
        res.add(achievementLevels[achievementTheme][max-2] + " " + goodScore*playerNumber);
        res.add(achievementLevels[achievementTheme][max-1] + " >" + goodScore*playerNumber);
        return res;
    }
}
