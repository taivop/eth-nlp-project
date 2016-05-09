package annotatorstub.utils;

import java.util.Collection;

public class StringUtils {


    private static int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    /**
     * Calculate Levenshtein edit distance between strings a and b.
     * @see https://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
     */
    private static int ED(CharSequence a, CharSequence b) {
        int[][] distance = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            distance[i][0] = i;
        for (int j = 1; j <= b.length(); j++)
            distance[0][j] = j;

        for (int i = 1; i <= a.length(); i++)
            for (int j = 1; j <= b.length(); j++)
                distance[i][j] = minimum(
                        distance[i - 1][j] + 1,
                        distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1));

        return distance[a.length()][b.length()];
    }

    /**
     * Calculate the MinED -- a measure of distance -- as described in the paper.
     */
    public static Double minED(String a, String b) {

        String[] termsInA = a.split(" ");
        String[] termsInB = b.split(" ");

        Double minDistancesSum = 0.0;

        for(String termInA : termsInA) {
            String closestInB = "";
            Double currentMinED = Double.POSITIVE_INFINITY;

            for(String termInB : termsInB) {
                Double editDistance = (double) ED(termInA, termInB);
                if(editDistance < currentMinED) {
                    closestInB = termInB;
                    currentMinED = editDistance;
                }
            }

            minDistancesSum += currentMinED;
        }

        return minDistancesSum / termsInA.length;
    }

    /**
     * Remove a trailing parenthetical string, e.g. 'Swiss (nationality)' -> 'Swiss '.
     * @param s string to truncate
     * @return string without trailing stuff in parentheses
     */
    public static String removeFinalParentheticalString(String s) {
        int lastOpeningParenIndex = s.lastIndexOf('(');
        int lastClosingParenIndex = s.lastIndexOf(')');

        if(lastClosingParenIndex > lastOpeningParenIndex) {     // Make sure the opening parenthesis is closed.
            return s.substring(0, lastOpeningParenIndex).trim();
        } else {
            return s;
        }
    }

    /**
     * Check if string is capitalised (positive examples: Taivo, Escher-Wyss Platz; negative: OMG, banana).
     * TODO Not sure if all-caps strings ("OMG") should return true or not -- the article doesn't specify.
     */
    public static boolean isCapitalised(String s) {
        String[] parts = s.split(" |-");
        for(String part : parts) {
            boolean firstLetterUpperCase = Character.isUpperCase(part.charAt(0));
            boolean restLowerCase = part.substring(1) == part.substring(1).toLowerCase();

            if(!firstLetterUpperCase || !restLowerCase) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the Wikipedia page title, given a Bing snippet title that links to a Wikipedia page.
     * TODO I (Taivo) think it doesn't work for long names because Bing clips the title if it's too long.
     */
    public static String extractPageTitleFromBingSnippetTitle(String bingSnippetTitle) {
        Integer clipStartIndex = bingSnippetTitle.indexOf(" - Wikip");
        if(clipStartIndex >= 0) {
            return bingSnippetTitle.substring(0, clipStartIndex);
        } else {
            return bingSnippetTitle;
        }
    }
}
