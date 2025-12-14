package trademind.tm_server.utils;

public final class StringUtils {

    // Private constructor to prevent instantiation
    private StringUtils() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    /**
     * Capitalizes the first letter of each word in a string.
     * Words are separated by spaces. All other characters are converted to lowercase.
     *
     * @param text the input string
     * @return string with each word capitalized, or null if input is null
     * @example "hello world" becomes "Hello World"
     * @example "HELLO WORLD" becomes "Hello World"
     */
    public static String capitalizeFirst(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String[] words = text.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            if (!words[i].isEmpty()) {
                result.append(words[i].substring(0, 1).toUpperCase())
                        .append(words[i].substring(1).toLowerCase());

                if (i < words.length - 1) {
                    result.append(" ");
                }
            }
        }

        return result.toString();
    }

    /**
     * Checks if a string is null or empty
     *
     * @param text the input string
     * @return true if null or empty
     */
    public static boolean isNullOrEmpty(String text) {
        return text == null || text.isEmpty();
    }
}