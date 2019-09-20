package com.stevedegroof.recipe_wizard;

/**
 * Parses plain text, and attempts to convert to a recipe
 */
public class RecipeParser
{
    public static final String[] IMPERIAL_DETECTION_UNITS = {"tbsp", "tablespoons", "tablespoon", "tsp", "teaspoons", "teaspoon", "oz", "cup", "cups", "c", "lb", "pound", "lbs", "pounds", "can", "package", "pkg"};
    public static final String[] METRIC_DETECTION_UNITS = {"ml", "g", "kg", "gram", "grams", "l", "liter", "liters", "litre", "litres", "can", "package", "pkg"};
    private String[] lines = null;

    private int titleStart = -1;
    private int ingredientsStart = -1;
    private int directionsStart = -1;
    private String prepTimeString = "";
    private String cookTimeString = "";
    private String totalTimeString = "";
    private String servingsString = "";
    private boolean isMetric = false;
    private String directions = "";
    private String title = "";
    private String ingredients = "";
    private int servings = 4;
    private String rawText = "";
    private boolean isVerbatim = false;

    /**
     * convert text to title case
     *
     * @param text
     * @return
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
     * Locate the title
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
     * extract ingredients
     */
    private void parseIngredients()
    {
        String ingredients = "";
        for (int i = ingredientsStart + 1; i < directionsStart; i++)
        {
            if (!lines[i].trim().isEmpty() && !isSpecialLine(lines[i]))
            {
                if (!ingredients.isEmpty() && (isVerbatim || lines[i].matches(UnitsConverter.VALUE_PARSE+".*")))
                {
                    ingredients += "\n";
                }
                ingredients += parseDoubleLine(lines[i]);
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
     * try to distinguish metric vs imperial
     *
     */
    private void parseMetric()
    {
        int metricCount = 0;
        int imperialCount = 0;
        String lines[]=ingredients.split("\n");
        for(String line :lines)
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
                }
                else
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
            if(metricCount>imperialCount) isMetric=true;
        }
    }

    public boolean isMetric()
    {
        return this.isMetric;
    }

    /**
     * extract directions
     */
    private void parseDirections()
    {
        String directions = "";
        for (int i = directionsStart + 1; i < lines.length; i++)
        {
            if (!lines[i].trim().isEmpty() && !isSpecialLine(lines[i]))
            {
                String line = lines[i];
                if (line.matches("[0-9]+[).] .+"))
                {
                    line = line.substring(line.indexOf(" ")).trim();
                }
                directions += line;
                if (directions.trim().endsWith(".") || isVerbatim)
                {
                    directions += "\n";
                } else if (directions.endsWith("-"))
                {
                    directions = directions.substring(0, directions.length() - 1);
                } else
                {
                    directions += " ";
                }
            }
        }
        this.directions = directions;
    }

    /**
     * extract any special lines (servings, prep time, cook time, etc)
     *
     * @param line
     * @return
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
     * extract the number of servings
     */
    private void parseServings()
    {
        int servings = 4;
        if (servingsString.isEmpty())
        {
            this.servings = servings;
        } else
        {
            String[] elements = servingsString.split("\\s|\\xA0|\\.|\\t");
            for (int i = 0; i < elements.length; i++)
            {
                try
                {
                    servings = Integer.parseInt(elements[i]);
                } catch (NumberFormatException e)
                {
                }
            }
            this.servings = servings;
        }
    }

    /**
     * read a recipe's text and parse it into a recipe
     * @param text
     */
    public void setRawText(String text)
    {
        rawText = text;
        parse();
    }

    /**
     * read a recipe's text and parse it into a recipe
     * @param text
     * @param verbatim if true, assumes recipe is formatted correctly and skips some heuristics
     */
    public void setRawText(String text, boolean verbatim)
    {
        isVerbatim = verbatim;
        rawText = text;
        parse();
    }

    /**
     * main parsing routine
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
            //look for actual words for ingredients
            if (lines[i].toLowerCase().startsWith("ingredients") || lines[i].toLowerCase().equals("you will need"))
            {
                ingredientsStart = i;
            }
            //look for actual words for directions
            if (lines[i].toLowerCase().startsWith("directions") || lines[i].toLowerCase().startsWith("instructions") || lines[i].toLowerCase().startsWith("preparation"))
            {
                directionsStart = i;
            }
        }
        if (ingredientsStart < 0) //if didn't find ingredients
        {
            ingredientsStart = firstBreak; //try first blank line
            if (ingredientsStart < 0) //still not there?
            {
                scanForIngredients(); //scan for ingredient-like lines
            }
        }

        if (directionsStart < 0) //if didn't find directions
        {
            directionsStart = secondBreak; //try second blank line
        }
        parseTitle(); //get the title
        parseIngredients(); //get the ingredients
        parseDirections(); //get the directions
        parseServings(); //get number of servings
    }

    /**
     * scan for ingredient-like lines
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

}

