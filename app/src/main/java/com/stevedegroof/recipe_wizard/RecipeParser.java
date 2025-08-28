package com.stevedegroof.recipe_wizard;

/**
 * Parses plain text from various sources and attempts to convert it into a structured recipe format.
 * This class uses a set of heuristics to identify different sections of a recipe,
 * such as the title, ingredients, directions, and notes. It can also attempt to
 * determine if the recipe uses metric or imperial units.
 *
 * <p>The parser works by analyzing the input text line by line, looking for keywords
 * (e.g., "ingredients", "directions") and common formatting patterns (e.g., blank lines
 * separating sections, numbered lists for directions).
 *
 * <p>Key features include:
 * <ul>
 *     <li>Extraction of title, ingredients, directions, and notes.</li>
 *     <li>Detection of serving size, prep time, cook time, and total time if present.</li>
 *     <li>Attempting to differentiate between metric and imperial units based on common unit abbreviations.</li>
 *     <li>Handling of some common formatting variations, such as side-by-side ingredient lists.</li>
 *     <li>An option for "verbatim" parsing, which assumes a more structured input and skips some heuristics.</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>
 * {@code
 * RecipeParser parser = new RecipeParser();
 * String recipeText = "My Awesome Cake\n\nIngredients:\n1 cup flour\n1 egg\n\nDirections:\nMix ingredients.\nBake at 350.";
 * parser.setRawText(recipeText, false); // false for heuristic-based parsing
 *
 * String title = parser.getTitle();
 * String ingredients = parser.getIngredients();
 * String directions = parser.getDirections();
 * int servings = parser.getServings();
 * boolean isMetric = parser.isMetric();
 * // ... and so on for other extracted information.
 * }
 * </pre>
 *
 * <p>Note: The parser's accuracy depends on the formatting and clarity of the input text.
 * It may not correctly parse all possible recipe formats, especially those that are
 * highly unconventional or poorly structured.
 */
public class RecipeParser
{
    public static final String[] IMPERIAL_DETECTION_UNITS = {"tbsp", "tablespoons", "tablespoon", "tsp", "teaspoons", "teaspoon", "oz", "cup", "cups", "c", "lb", "pound", "lbs", "pounds", "can", "package", "pkg"};
    public static final String[] METRIC_DETECTION_UNITS = {"ml", "g", "kg", "gram", "grams", "l", "liter", "liters", "litre", "litres", "can", "package", "pkg"};
    private String[] lines = null;

    private int titleStart = -1;
    private int ingredientsStart = -1;
    private int directionsStart = -1;
    private int notesStart = -1;
    private String prepTimeString = "";
    private String cookTimeString = "";
    private String totalTimeString = "";
    private String servingsString = "";
    private boolean isMetric = false;
    private String directions = "";
    private String title = "";
    private String ingredients = "";
    private int servings = 4;
    private String notes = "";
    private String rawText = "";
    private boolean isVerbatim = false;

    /**
     * Converts a string to title case.
     * <p>
     * This method capitalizes the first letter of each word in the input string
     * and converts all other letters to lowercase. Words are delimited by spaces.
     * If the input string is null or empty, it is returned as is.
     *
     * @param text The string to be converted to title case.
     * @return The title-cased string, or the original string if it was null or empty.
     */
    public static String toTitleCase(String text)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }

        StringBuilder converted = new StringBuilder();

        boolean convertNext = true;
        for (char ch : text.toCharArray())
        {
            if (Character.isSpaceChar(ch))
            {
                convertNext = true;
            } else if (convertNext)
            {
                ch = Character.toTitleCase(ch);
                convertNext = false;
            } else
            {
                ch = Character.toLowerCase(ch);
            }
            converted.append(ch);
        }

        return converted.toString();
    }

    /**
     * Locates the title of the recipe.
     * The title is assumed to be the first non-empty, non-special line in the recipe text.
     * Special lines include those indicating servings, prep time, cook time, etc.
     * The located title is then converted to title case.
     */
    private void parseTitle()
    {
        String title = "unknown";
        boolean found = false;
        for (int i = 0; i < lines.length && !found; i++)
        {
            if (!lines[i].isEmpty() && !isSpecialLine(lines[i]))
            {
                title = lines[i];
                found = true;
                titleStart = i;
            }
        }
        this.title = toTitleCase(title);
    }

    /**
     * Extracts ingredient lines from the raw text.
     * It iterates through lines identified as belonging to the ingredients section,
     * skipping empty lines and special lines (like servings or prep time).
     * It also handles lines that might contain two ingredients separated by multiple spaces.
     * Finally, it calls {@link #parseMetric()} to determine if the ingredients are primarily metric or imperial.
     */
    private void parseIngredients()
    {
        StringBuilder ingredients = new StringBuilder();
        for (int i = ingredientsStart + 1; i < ((directionsStart > ingredientsStart) ? directionsStart : ((notesStart > directionsStart) ? notesStart : lines.length)); i++)
        {
            if (!lines[i].trim().isEmpty() && !isSpecialLine(lines[i]))
            {
                if (ingredients.length() > 0)
                {
                    ingredients.append("\n");
                }
                ingredients.append(parseDoubleLine(lines[i]));
            }
        }
        this.ingredients = ingredients + "\n";
        parseMetric();
    }

    /**
     * if a line has a large number of spaces (e.g. side-by-side ingredients), split it in two
     *
     * @param line
     * @return
     */
    private String parseDoubleLine(String line)
    {
        return line.replaceAll("\\s\\s\\s+", "\n");
    }

    /**
     * Tries to determine if the recipe uses metric or imperial units.
     * It iterates through the ingredient lines, strips out numbers and common fractions,
     * and then checks the first word (assumed to be the unit) against lists of
     * known imperial and metric units.
     * Sets the `isMetric` flag to true if more metric units are found than imperial units.
     */
    private void parseMetric()
    {
        int metricCount = 0;
        int imperialCount = 0;
        String[] lines = ingredients.split("\n");
        for (String line : lines)
        {
            try
            {
                String strippedLine = line.toLowerCase().replaceAll("[0-9¼½¾⅐⅑⅒⅓⅔⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞/\\.-]", "").trim();
                String units = strippedLine.split("\\s|\\t|\\xA0")[0];
                boolean found = false;
                for (int i = 0; i < IMPERIAL_DETECTION_UNITS.length && !found; i++)
                {
                    found = IMPERIAL_DETECTION_UNITS[i].equals(units);
                }
                if (found)
                {
                    imperialCount++;
                } else
                {
                    for (int i = 0; i < METRIC_DETECTION_UNITS.length && !found; i++)
                    {
                        found = METRIC_DETECTION_UNITS[i].equals(units);
                    }
                    if (found) metricCount++;
                }
            } catch (Exception e)
            {
            }
            if (metricCount > imperialCount) isMetric = true;
        }
    }

    public boolean isMetric()
    {
        return this.isMetric;
    }

    /**
     * Extracts and formats the directions from the parsed recipe text.
     * It iterates through the lines identified as directions, cleans them up,
     * and concatenates them into a single string.
     *
     * <p>Specifically, this method performs the following:
     * <ul>
     *     <li>Identifies the block of lines that constitute the directions, starting after {@code directionsStart}
     *         and ending before {@code notesStart} (if notes exist) or the end of the input.</li>
     *     <li>Skips empty lines and lines marked as "special" (e.g., servings, prep time).</li>
     *     <li>Removes leading numbers and periods/parentheses from lines that appear to be numbered steps
     *         (e.g., "1. Mix ingredients" becomes "Mix ingredients").</li>
     *     <li>Appends the processed line to a {@link StringBuilder}.</li>
     *     <li>Adds a newline character if the accumulated directions end with a period or colon, or if
     *         {@code isVerbatim} is true (indicating pre-formatted input).</li>
     *     <li>Removes a trailing hyphen if present.</li>
     *     <li>Otherwise, adds a space to separate sentences or phrases.</li>
     * </ul>
     * The final formatted directions string is stored in the {@code this.directions} field.
     */
    private void parseDirections()
    {
        StringBuilder directions = new StringBuilder();
        for (int i = directionsStart + 1; i < ((notesStart > directionsStart) ? notesStart : lines.length); i++)
        {
            if (!lines[i].trim().isEmpty() && !isSpecialLine(lines[i]))
            {
                String line = lines[i];
                if (line.matches("[0-9]+[).] .+"))
                {
                    line = line.substring(line.indexOf(" ")).trim();
                }
                directions.append(line);
                if (directions.toString().trim().endsWith(".") || directions.toString().trim().endsWith(":") || isVerbatim)
                {
                    directions = new StringBuilder(directions.toString().trim() + "\n");
                } else if (directions.toString().endsWith("-"))
                {
                    directions = new StringBuilder(directions.substring(0, directions.length() - 1));
                } else
                {
                    directions.append(" ");
                }
            }
        }
        this.directions = directions.toString();
    }

    /**
     * Extracts any notes found in the recipe.
     * Notes are typically found after a line starting with "notes".
     * This method iterates through the lines of the recipe starting from
     * the line after the "notes" keyword, appending non-empty lines to the
     * notes string.
     */
    private void parseNotes()
    {
        StringBuilder notes = new StringBuilder();
        for (int i = notesStart + 1; i < lines.length; i++)
        {
            if (!lines[i].trim().isEmpty())
            {
                notes.append(lines[i]);
                notes.append("\n");
            }
        }
        this.notes = notes.toString();
    }


    /**
     * Checks if a line contains special recipe information like servings, prep time, or cook time.
     * If a special line is found, the corresponding instance variable (e.g., {@code servingsString},
     * {@code prepTimeString}) is updated with the line's content.
     *
     * @param line The line of text to check.
     * @return {@code true} if the line is identified as a special line, {@code false} otherwise.
     */
    private boolean isSpecialLine(String line)
    {
        if (line.toLowerCase().startsWith("serves") || line.toLowerCase().startsWith("servings") || (!isVerbatim && (line.toLowerCase().endsWith("servings") || line.toLowerCase().endsWith("servings.") || line.trim().toLowerCase().matches("^.+[0-9]+ servings\\.$"))))
        {
            servingsString = line;
            return true;
        }
        if (line.toLowerCase().startsWith("prep time") || line.toLowerCase().startsWith("preparation time"))
        {
            prepTimeString = line;
            return true;
        }
        if (line.toLowerCase().startsWith("cook time") || line.toLowerCase().startsWith("bake time") || line.toLowerCase().endsWith("baking time") || line.toLowerCase().endsWith("cooking time"))
        {
            cookTimeString = line;
            return true;
        }
        if (line.toLowerCase().startsWith("total time"))
        {
            totalTimeString = line;
            return true;
        }
        return false;
    }

    /**
     * Extracts the number of servings from the servingsString.
     * It splits the servingsString by whitespace, non-breaking space, period, or tab
     * and attempts to parse each element as an integer.
     * The first successfully parsed integer will be set as the number of servings.
     * If no integer is found or servingsString is empty, the servings will default to 4.
     */
    private void parseServings()
    {
        int servings = 4;
        if (!servingsString.isEmpty())
        {
            String[] elements = servingsString.split("\\s|\\xA0|\\.|\\t");
            for (String element : elements)
            {
                try
                {
                    servings = Integer.parseInt(element);
                } catch (NumberFormatException e)
                {
                }
            }
        }
        this.servings = servings;
    }


    /**
     * Sets the raw text of the recipe and parses it.
     * This method will attempt to identify the title, ingredients, directions,
     * and other relevant information from the provided text.
     *
     * @param text     The raw text of the recipe.
     * @param verbatim If true, the parser assumes the recipe is formatted perfectly
     *                 and will skip certain heuristic checks.
     */
    public void setRawText(String text, boolean verbatim)
    {
        isVerbatim = verbatim;
        rawText = text;
        parse();
    }

    /**
     * Main parsing routine. This method orchestrates the parsing of the raw recipe text.
     * It first splits the text into lines and then identifies the start of key sections
     * like ingredients, directions, and notes based on common keywords or blank lines.
     * If keywords are not found, it uses blank lines as potential separators.
     * After identifying the sections, it calls specific parsing methods for each part
     * (title, ingredients, directions, notes, servings).
     */
    private void parse()
    {
        boolean foundFirstNonBlankLine = false;
        int firstBreak = -1;
        int secondBreak = -1;
        lines = rawText.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++)
        {
            lines[i] = lines[i].trim();
            if (!foundFirstNonBlankLine && !lines[i].isEmpty())
            {
                foundFirstNonBlankLine = true;
            }
            if (foundFirstNonBlankLine && firstBreak > -1 && secondBreak < 0 && lines[i].isEmpty())
            {
                secondBreak = i;
            }
            if (foundFirstNonBlankLine && firstBreak < 0 && lines[i].isEmpty())
            {
                firstBreak = i;
            }

            if (lines[i].toLowerCase().startsWith("ingredients") || lines[i].equalsIgnoreCase("you will need"))
            {
                ingredientsStart = i;
            }

            if (lines[i].toLowerCase().startsWith("directions") || lines[i].toLowerCase().startsWith("instructions") || lines[i].toLowerCase().startsWith("preparation"))
            {
                directionsStart = i;
            }
            if (lines[i].toLowerCase().startsWith("notes"))
            {
                notesStart = i;
            }
        }
        if (ingredientsStart < 0)
        {
            ingredientsStart = firstBreak;
            if (ingredientsStart < 0)
            {
                scanForIngredients();
            }
        }

        if (directionsStart < 0)
        {
            directionsStart = secondBreak;
        }
        parseTitle();
        parseIngredients();
        parseDirections();
        if (notesStart > 0) parseNotes();
        parseServings();
    }

    /**
     * Scans the lines of the recipe to identify the start of the ingredients list
     * and the start of the directions. This method is typically used when the
     * standard markers for these sections (e.g., "ingredients", "directions") are
     * not found.
     * <p>
     * It iterates through each line (starting from the second line, assuming the
     * first line might be the title) and attempts to detect ingredient units
     * (both imperial and metric).
     * <p>
     * If an ingredient unit is found and the `ingredientsStart` index hasn't
     * been set yet, it sets `ingredientsStart` to the previous line index.
     * <p>
     * Regardless of whether `ingredientsStart` was set by this line, if an
     * ingredient unit is found, it updates `directionsStart` to the current
     * line index. This implies that the line after the last detected ingredient
     * is considered the start of the directions.
     * <p>
     * The method uses predefined lists of imperial and metric units for detection.
     * It preprocesses each line by converting it to lowercase and removing numbers,
     * fractions, and common punctuation to isolate potential unit keywords.
     * <p>
     * Any exceptions encountered during the processing of a line are caught and
     * ignored, allowing the scan to continue with the next line.
     */
    private void scanForIngredients()
    {
        for (int i = 1; i < lines.length; i++)
        {
            String line = lines[i];
            try
            {
                String strippedLine = line.toLowerCase().replaceAll("[0-9¼½¾⅐⅑⅒⅓⅔⅕⅖⅗⅘⅙⅚⅛⅜⅝⅞/\\.-]", "").trim();
                String units = strippedLine.split("\\s|\\t|\\xA0")[0];
                boolean found = false;
                for (int j = 0; j < IMPERIAL_DETECTION_UNITS.length && !found; j++)
                {
                    found = IMPERIAL_DETECTION_UNITS[j].equals(units);
                }
                if (found && ingredientsStart < 0)
                {
                    ingredientsStart = i - 1;
                } else
                {
                    for (int k = 0; k < METRIC_DETECTION_UNITS.length && !found; k++)
                    {
                        found = METRIC_DETECTION_UNITS[k].equals(units);
                    }
                }
                if (found && ingredientsStart < 0)
                {
                    ingredientsStart = i - 1;
                }
                if (found)
                {
                    directionsStart = i;
                }
            } catch (Exception e)
            {
            }
        }
    }

    public String getDirections()
    {
        return directions;
    }

    public String getIngredients()
    {
        return ingredients;
    }

    public int getServings()
    {
        return servings;
    }

    public String getTitle()
    {
        return title;
    }

    public String getNotes()
    {
        return notes;
    }
}

