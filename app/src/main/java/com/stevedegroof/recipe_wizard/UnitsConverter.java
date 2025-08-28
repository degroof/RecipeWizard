package com.stevedegroof.recipe_wizard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for calculating and converting quantities between different units and measurement systems.
 *
 * <p>This class provides functionality to:
 * <ul>
 *     <li>Parse ingredient strings to extract numerical values and units.</li>
 *     <li>Convert quantities between imperial and metric systems.</li>
 *     <li>Adjust quantities based on serving sizes.</li>
 *     <li>Round converted quantities to practical, human-readable values.</li>
 *     <li>Identify and convert temperature values within text.</li>
 *     <li>Handle common cooking units like teaspoons, tablespoons, cups, ounces, grams, etc.</li>
 *     <li>Distinguish between mass and volume for units like ounces based on ingredient context.</li>
 *     <li>Extract and manage scalable phrases from recipe directions.</li>
 * </ul>
 *
 * <p>It uses a set of predefined constants for units and conversion factors, along with regular expressions
 * for parsing and detecting quantities in text. The class also includes logic to make educated guesses
 * about the nature of ingredients (dry vs. wet) to aid in accurate conversions.
 */
public class UnitsConverter
{

    public static final int METRIC = 1;
    public static final int IMPERIAL = 2;

    public static final String VALUE_PARSE = "^([0-9¼½¾⅓⅔⅛\\./ -]+|a |another )";

    private static final int NONE = -1;
    private static final int SMIDGEN = 0;
    private static final int PINCH = 1;
    private static final int TSP = 2;
    private static final int TBSP = 3;
    private static final int FL_OZ = 4;
    private static final int CUP = 5;
    private static final int PINT = 6;
    private static final int ML = 7;
    private static final int OZ = 8;
    private static final int LB = 9;
    private static final int GRAM = 10;
    private static final int DASH = 11;
    private static final int KG = 12;
    private static final int MM = 13;
    private static final int CM = 14;
    private static final int INCH = 15;
    private static final int QT = 16;
    private static final int CAN = 17;
    private static final int PKG = 18;

    private static final double TSP_TO_ML = 4.93d;
    private static final double TBSP_TO_ML = 14.79d;
    private static final double TBSP_TO_TSP = 3d;
    private static final double CUPS_TO_ML = 236.59d;
    private static final double PINTS_TO_ML = 473.18d;
    private static final double OZ_TO_ML = 29.57d;
    private static final double OZ_TO_G = 28.35d;
    private static final double OZ_TO_KG = 28350d;
    private static final double LB_TO_OZ = 16d;
    private static final double TSP_TO_PINCH = 16d;
    private static final double TSP_TO_SMIDGEN = 32d;
    private static final double OZ_TO_TSP = 6d;
    private static final double OZ_TO_TBSP = 2d;
    private static final double OZ_TO_DASH = 48d;
    private static final double OZ_TO_PINCH = 96d;
    private static final double OZ_TO_SMIDGEN = 192d;
    private static final double OZ_TO_CUPS = 1d / 8d;
    private static final double OZ_TO_PINTS = 1d / 16d;
    private static final double OZ_TO_QTS = 1d / 32d;
    private static final double INCH_TO_MM = 25.4d;
    private static final String FRACTIONS = "[¼½¾⅓⅔⅛]";
    private static final String UNITS_DETECT = "([\\s-])?(oz|cups?|T\\b|t\\b|c\\b|c\\.|TBSP\\b|tbsp\\b|tsp\\b|tablespoons?\\b|Tablespoons?\\b|teaspoons?\\b|quart\\b|qt\\b|inch(es)?\\b|mls?\\b|cm\\b|mm\\b|liters?\\b|litres?\\b|g\\b|grams?\\b|kgs?\\b)";
    private static final String VALUE_DETECT1 = "\\s[0-9]+\\.[0-9]+";
    private static final String VALUE_DETECT2 = "\\s([0-9]+)?([\\s-])?(([0-9]+/[0-9]+)|[¼½¾⅓⅔⅛])";
    private static final String VALUE_DETECT3 = "\\s[0-9]+";
    private static final String[] INGREDIENT_DETECT = {VALUE_DETECT1 + UNITS_DETECT, VALUE_DETECT2 + UNITS_DETECT, VALUE_DETECT3 + UNITS_DETECT};

    private static final String[] DRY_INGREDIENTS = {"noodles", "ginger root", "chocolate chips",
            "asparagus", "thyme", "tomatoes", "almonds", "cheese", "prosciutto", "arugula", "macaroni",
            "meat", "potatoes", "barramundi", "greens", "beef", "nuts", "beans", "mushrooms", "sausage",
            "chicken"};
    private static final String[] WET_INGREDIENTS = {"sauce", "paste", "soup", "bouillon", "juice",
            "liqueur", "extract", "puree", "purée", "stock", "salsa", "mayo", "mayonnaise", "dressing",
            "milk", "broth"};
    private static final String[] PREP_WORDS = {"chopped", "diced", "quartered", "mashed", "shredded",
            "minced", "cubed", "cooked", "uncooked", "drained", "undrained", "chilled", "cold",
            "halved", "seeded", "peeled", "divided", "beaten", "rinsed", "blanched", "juiced",
            "dry", "flaked", "melted", "softened", "room temperature"};
    private final int detectedSystem = IMPERIAL;
    private double value = 0;
    private int units = NONE;
    private ArrayList<DirectionsPhrase> excludedPhrases;

    /**
     * Tries to guess if the ingredient is measured by mass or volume.
     * This is primarily used to differentiate between weight ounces (mass)
     * and fluid ounces (volume).
     *
     * <p>The method checks if the ingredient name contains keywords typically
     * associated with dry (mass-based) ingredients. If a dry keyword is found,
     * it then checks if the ingredient also contains keywords associated with
     * wet (volume-based) ingredients. If a wet keyword is found after a dry
     * keyword, it assumes the ingredient is volume-based (e.g., "tomato sauce").
     * Otherwise, if only a dry keyword is found, it's assumed to be mass-based.
     *
     * @param ingredient The name of the ingredient.
     * @return {@code true} if the ingredient is likely measured by mass,
     * {@code false} if it's likely measured by volume or if it cannot be determined.
     */
    private boolean isMass(String ingredient)
    {
        boolean result = false;
        for (int i = 0; i < DRY_INGREDIENTS.length && !result; i++)
        {
            if (ingredient.toLowerCase().contains(DRY_INGREDIENTS[i])) result = true;
        }
        for (int i = 0; i < WET_INGREDIENTS.length && result; i++)
        {
            if (ingredient.toLowerCase().contains(WET_INGREDIENTS[i])) result = false;
        }
        return result;
    }

    /**
     * Rounds and formats a given milliliter (ml) value into a human-readable string.
     * The rounding precision varies based on the magnitude of the input value:
     * <ul>
     *     <li>Less than 0.5 ml: rounded to the nearest 0.1 ml.</li>
     *     <li>0.5 ml to less than 1 ml: rounded to the nearest 0.25 ml.</li>
     *     <li>1 ml to less than 5 ml: rounded to the nearest 0.5 ml.</li>
     *     <li>5 ml to less than 50 ml: rounded to the nearest 5 ml.</li>
     *     <li>50 ml to less than 100 ml: rounded to the nearest 10 ml.</li>
     *     <li>100 ml to less than 500 ml: rounded to the nearest 25 ml.</li>
     *     <li>500 ml to less than 1000 ml: rounded to the nearest 100 ml.</li>
     *     <li>1000 ml or more: rounded to the nearest 250 ml.</li>
     * </ul>
     * The result is formatted to a maximum of two decimal places.
     *
     * @param ml The milliliter value to round and format.
     * @return A string representation of the rounded and formatted milliliter value.
     */
    private String roundMl(double ml)
    {
        double rounded = 0d;
        if (ml < 0.5d)
        {
            rounded = Math.round(ml * 10d) / 10d;
        } else if (ml < 1d)
        {
            rounded = Math.round(ml * 4d) / 4d;
        } else if (ml < 5d)
        {
            rounded = Math.round(ml * 2d) / 2d;
        } else if (ml < 50d)
        {
            rounded = Math.round(ml / 5d) * 5d;
        } else if (ml < 100d)
        {
            rounded = Math.round(ml / 10d) * 10d;
        } else if (ml < 500d)
        {
            rounded = Math.round(ml / 25d) * 25d;
        } else if (ml < 1000d)
        {
            rounded = Math.round(ml / 100d) * 100d;
        } else
        {
            rounded = Math.round(ml / 250d) * 250d;
        }
        return new DecimalFormat("#.##").format(rounded);
    }

    /**
     * Round and format millimeters.
     * If the value is greater than 15mm, it will be converted to centimeters and rounded using {@link #roundCm(double)}.
     * Otherwise, it will be rounded to the nearest whole number and formatted as "Xmm".
     *
     * @param mm The value in millimeters to round and format.
     * @return A string representing the rounded and formatted millimeters, or centimeters if applicable.
     */
    private String roundMm(double mm)
    {
        double rounded = 0;
        if (mm > 15)
        {
            return roundCm(mm / 10);
        } else
        {
            return new DecimalFormat("#").format(Math.round(mm)) + "mm";
        }
    }

    /**
     * Rounds and formats a value in centimeters (cm).
     *
     * <p>The rounding logic is as follows:
     * <ul>
     *   <li>If cm < 1.5, it's converted to millimeters (mm) and rounded using {@link #roundMm(double)}.
     *   <li>If 1.5 <= cm < 50, it's rounded to the nearest 5 cm.
     *   <li>If 50 <= cm < 100, it's rounded to the nearest 10 cm.
     *   <li>If 100 <= cm < 500, it's rounded to the nearest 25 cm.
     *   <li>If 500 <= cm < 1000, it's rounded to the nearest 100 cm.
     *   <li>If cm >= 1000, it's rounded to the nearest 250 cm.
     * </ul>
     * The rounded value is then formatted as a whole number string followed by "cm".
     *
     * @param cm The value in centimeters to round and format.
     * @return A string representing the rounded and formatted centimeter value.
     */
    private String roundCm(double cm)
    {
        double rounded = 0;
        if (cm < 1.5d)
        {
            return roundMm(cm * 10d);
        } else if (cm < 50d)
        {
            rounded = Math.round(cm / 5d) * 5d;
        } else if (cm < 100d)
        {
            rounded = Math.round(cm / 10d) * 10d;
        } else if (cm < 500d)
        {
            rounded = Math.round(cm / 25d) * 25d;
        } else if (cm < 1000d)
        {
            rounded = Math.round(cm / 100d) * 100d;
        } else
        {
            rounded = Math.round(cm / 250d) * 250d;
        }
        return new DecimalFormat("#").format(rounded) + "cm";
    }

    /**
     * Rounds and formats a given gram value.
     * The rounding precision depends on the magnitude of the input value:
     * <ul>
     *     <li>If gram < 0.5: rounds to the nearest 0.1.</li>
     *     <li>If 0.5 <= gram < 1: rounds to the nearest 0.25.</li>
     *     <li>If 1 <= gram < 10: rounds to the nearest 0.5.</li>
     *     <li>If 10 <= gram < 50: rounds to the nearest 5.</li>
     *     <li>If 50 <= gram < 100: rounds to the nearest 10.</li>
     *     <li>If 100 <= gram < 500: rounds to the nearest 25.</li>
     *     <li>If 500 <= gram < 1000: rounds to the nearest 100.</li>
     *     <li>If gram >= 1000: rounds to the nearest 250.</li>
     * </ul>
     * The result is then formatted to a string with up to two decimal places.
     *
     * @param gram the gram value to round and format.
     * @return a string representation of the rounded and formatted gram value.
     */
    private String roundGram(double gram)
    {
        double rounded = 0;
        if (gram < 0.5d)
        {
            rounded = Math.round(gram * 10d) / 10d;
        } else if (gram < 1d)
        {
            rounded = Math.round(gram * 4d) / 4d;
        } else if (gram < 10d)
        {
            rounded = Math.round(gram * 2d) / 2d;
        } else if (gram < 50d)
        {
            rounded = Math.round(gram / 5d) * 5d;
        } else if (gram < 100d)
        {
            rounded = Math.round(gram / 10d) * 10d;
        } else if (gram < 500d)
        {
            rounded = Math.round(gram / 25d) * 25d;
        } else if (gram < 1000d)
        {
            rounded = Math.round(gram / 100d) * 100d;
        } else
        {
            rounded = Math.round(gram / 250d) * 250d;
        }
        return new DecimalFormat("#.##").format(rounded);
    }

    /**
     * Rounds and formats ounce values.
     * The rounding precision depends on the magnitude of the input value:
     * <ul>
     *     <li>If oz &lt; 0.5: rounds to the nearest 0.1 oz.</li>
     *     <li>If 0.5 &le; oz &lt; 1: rounds to the nearest 0.25 oz.</li>
     *     <li>If 1 &le; oz &lt; 10: rounds to the nearest 0.5 oz.</li>
     *     <li>If 10 &le; oz &lt; 50: rounds to the nearest 5 oz.</li>
     *     <li>If 50 &le; oz &lt; 100: rounds to the nearest 10 oz.</li>
     *     <li>If 100 &le; oz &lt; 500: rounds to the nearest 25 oz.</li>
     *     <li>If 500 &le; oz &lt; 1000: rounds to the nearest 100 oz.</li>
     *     <li>If oz &ge; 1000: rounds to the nearest 250 oz.</li>
     * </ul>
     * The result is formatted to a string with up to two decimal places.
     *
     * @param oz The ounce value to round and format.
     * @return A string representation of the rounded and formatted ounce value.
     */
    private String roundOz(double oz)
    {
        double rounded = 0;
        if (oz < 0.5d)
        {
            rounded = Math.round(oz * 10d) / 10d;
        } else if (oz < 1d)
        {
            rounded = Math.round(oz * 4d) / 4d;
        } else if (oz < 10d)
        {
            rounded = Math.round(oz * 2d) / 2d;
        } else if (oz < 50d)
        {
            rounded = Math.round(oz / 5d) * 5d;
        } else if (oz < 100d)
        {
            rounded = Math.round(oz / 10d) * 10d;
        } else if (oz < 500d)
        {
            rounded = Math.round(oz / 25d) * 25d;
        } else if (oz < 1000d)
        {
            rounded = Math.round(oz / 100d) * 100d;
        } else
        {
            rounded = Math.round(oz / 250d) * 250d;
        }
        return new DecimalFormat("#.##").format(rounded);
    }

    /**
     * Rounds and formats a given teaspoon (tsp) value into a human-readable string.
     * The method handles various ranges of tsp values and represents them as fractions
     * (e.g., "1/2 tsp"), whole numbers (e.g., "2 tsp"), or descriptive terms
     * (e.g., "a pinch", "a smidgeon") for very small amounts.
     *
     * <p>The rounding logic is as follows:
     * <ul>
     *     <li>Values less than 0.047 tsp are "a smidgeon".</li>
     *     <li>Values less than 0.094 tsp are "a pinch".</li>
     *     <li>Values less than 0.19 tsp are "1/8 tsp".</li>
     *     <li>Values less than 0.38 tsp are "1/4 tsp".</li>
     *     <li>Values less than 0.75 tsp are "1/2 tsp".</li>
     *     <li>Values less than 1 tsp are "1 tsp".</li>
     *     <li>For values between 1 tsp and 4 tsp (exclusive of 4):
     *         <ul>
     *             <li>If the fractional part is less than 0.75:
     *                 <ul>
     *                     <li>The integer part is shown.</li>
     *                     <li>If the fractional part is greater than 0.25, " 1/2" is appended.</li>
     *                 </ul>
     *             </li>
     *             <li>Otherwise (fractional part is 0.75 or more), the value is rounded to the nearest whole number.</li>
     *         </ul>
     *         In both sub-cases, " tsp" is appended.
     *     </li>
     *     <li>For values 4 tsp or greater, the value is rounded to the nearest whole number and " tsp" is appended.</li>
     * </ul>
     *
     * @param tsp The teaspoon value as a double.
     * @return A string representing the rounded and formatted teaspoon measurement.
     */
    private String roundTsp(double tsp)
    {
        double rounded = 0;
        if (tsp < 0.047d)
        {
            return "a smidgeon";
        } else if (tsp < 0.094d)
        {
            return "a pinch";
        } else if (tsp < 0.19d)
        {
            return "1/8 tsp";
        } else if (tsp < 0.38d)
        {
            return "1/4 tsp";
        } else if (tsp < 0.75d)
        {
            return "1/2 tsp";
        } else if (tsp < 1d)
        {
            return "1 tsp";
        } else if (tsp < 4d)
        {
            int intPart = (int) tsp;
            double frac = tsp - (double) intPart;
            if (frac < .75d)
            {
                return new DecimalFormat("#").format(Math.floor(tsp)) + (frac > 0.25d ? " 1/2" : "") + " tsp";
            } else
            {
                return new DecimalFormat("#").format(Math.round(tsp)) + " tsp";
            }
        } else
        {
            return new DecimalFormat("#").format(Math.round(tsp)) + " tsp";
        }
    }

    /**
     * Rounds and formats a tablespoon measurement.
     * If the value is less than 2 tablespoons and the fractional part is less than 0.75,
     * it converts the value to teaspoons and rounds it using {@link #roundTsp(double)}.
     * Otherwise, it rounds to the nearest half or whole tablespoon.
     *
     * @param tbsp The tablespoon value to round and format.
     * @return A string representation of the rounded tablespoon measurement (e.g., "1 1/2 tbsp", "2 tbsp").
     */
    private String roundTbsp(double tbsp)
    {
        double rounded = 0;
        int intPart = (int) tbsp;
        double frac = tbsp - (double) intPart;
        if (frac < 0.75 && intPart < 2)
        {
            return roundTsp(tbsp * TBSP_TO_TSP);
        } else if (frac < 0.75d && frac > 0.25d)
        {
            return new DecimalFormat("#").format(Math.floor(tbsp)) + " 1/2 tbsp";
        } else
        {
            return new DecimalFormat("#").format(Math.round(tbsp)) + " tbsp";
        }
    }


    /**
     * Round and format inches.
     * <p>
     * This method takes a double value representing inches and rounds it to the nearest practical
     * fraction or whole number. The returned string is formatted for display.
     * <p>
     * For values less than 1.375 inches, the method rounds to the nearest 1/16th, 1/8th, 3/16th,
     * 1/4th, 5/16th, 3/8th, 1/2, 5/8th, 3/4th, or 7/8th of an inch.
     * For values between 1.375 and 3 inches, the method rounds to the nearest whole number,
     * or to a whole number plus a fractional part if the fractional part is less than 0.875.
     * For values between 3 and 6 inches, the method rounds to the nearest whole number,
     * or to a whole number plus 1/2 if the fractional part is between 0.25 and 0.75.
     * For values greater than or equal to 6 inches, the method rounds to the nearest whole number.
     *
     * @param value the value in inches to be rounded and formatted
     * @return a string representing the rounded and formatted inches
     */
    private String roundInches(double value)
    {
        if (value < 0.09375d)
        {
            return "1/16";
        } else if (value < 0.15625d)
        {
            return "1/8";
        } else if (value < 0.21875d)
        {
            return "3/16";
        } else if (value < 0.28125d)
        {
            return "1/4";
        } else if (value < 0.34375d)
        {
            return "5/16";
        } else if (value < 0.4375d)
        {
            return "3/8";
        } else if (value < 0.5625d)
        {
            return "1/2";
        } else if (value < 0.6875d)
        {
            return "5/8";
        } else if (value < 0.8125d)
        {
            return "3/4";
        } else if (value < 0.9375d)
        {
            return "7/8";
        } else if (value < 1.375)
        {
            return "1";
        } else if (value < 3)
        {
            int intPart = (int) value;
            double frac = value - (double) intPart;
            if (frac < .875)
            {
                return new DecimalFormat("#").format(Math.floor(value)) + " " + roundInches(frac);
            } else
            {
                return new DecimalFormat("#").format(Math.round(value));
            }
        } else if (value < 6)
        {
            int intPart = (int) value;
            double frac = value - (double) intPart;
            if (frac < .75)
            {
                return new DecimalFormat("#").format(Math.floor(value)) + (frac > 0.25 ? " 1/2" : "");
            } else
            {
                return new DecimalFormat("#").format(Math.round(value));
            }
        } else
        {
            return new DecimalFormat("#").format(Math.round(value));
        }
    }

    /**
     * Rounds and formats a generic numeric value into a human-readable string.
     * This method is used for units where specific rounding logic (like for tsp or oz)
     * is not applicable or required. It rounds the input value to common fractions
     * or whole numbers.
     *
     * <p>The rounding logic is as follows:
     * <ul>
     *     <li>Values less than 0.06 are returned as an empty string.</li>
     *     <li>Values less than 0.19 are rounded to "1/8".</li>
     *     <li>Values less than 0.29 are rounded to "1/4".</li>
     *     <li>Values less than 0.42 are rounded to "1/3".</li>
     *     <li>Values less than 0.58 are rounded to "1/2".</li>
     *     <li>Values less than 0.71 are rounded to "2/3".</li>
     *     <li>Values less than 0.875 are rounded to "3/4".</li>
     *     <li>Values less than 1.29 are rounded to "1".</li>
     *     <li>For values between 1.29 and 3 (exclusive of 3):
     *         <ul>
     *             <li>If the fractional part is less than 0.875, the integer part is shown,
     *                 followed by a space and the recursively rounded fractional part.</li>
     *             <li>Otherwise, the value is rounded to the nearest whole number.</li>
     *         </ul>
     *     </li>
     *     <li>For values between 3 and 6 (exclusive of 6):
     *         <ul>
     *             <li>If the fractional part is less than 0.75:
     *                 <ul>
     *                     <li>The integer part is shown.</li>
     *                     <li>If the fractional part is greater than 0.25, " 1/2" is appended.</li>
     *                 </ul>
     *             </li>
     *             <li>Otherwise, the value is rounded to the nearest whole number.</li>
     *         </ul>
     *     </li>
     *     <li>For values 6 or greater, the value is rounded to the nearest whole number.</li>
     * </ul>
     */
    private String roundGeneric(double value)
    {
        if (value < 0.06d)
        {
            return "";
        } else if (value < 0.19d)
        {
            return "1/8";
        } else if (value < 0.29d)
        {
            return "1/4";
        } else if (value < 0.42d)
        {
            return "1/3";
        } else if (value < 0.58d)
        {
            return "1/2";
        } else if (value < 0.71d)
        {
            return "2/3";
        } else if (value < 0.875d)
        {
            return "3/4";
        } else if (value < 1.29d)
        {
            return "1";
        } else if (value < 3d)
        {
            int intPart = (int) value;
            double frac = value - (double) intPart;
            if (frac < .875)
            {
                return new DecimalFormat("#").format(Math.floor(value)) + " " + roundGeneric(frac);
            } else
            {
                return new DecimalFormat("#").format(Math.round(value));
            }
        } else if (value < 6)
        {
            int intPart = (int) value;
            double frac = value - (double) intPart;
            if (frac < .75)
            {
                return new DecimalFormat("#").format(Math.floor(value)) + (frac > 0.25 ? " 1/2" : "");
            } else
            {
                return new DecimalFormat("#").format(Math.round(value));
            }
        } else
        {
            return new DecimalFormat("#").format(Math.round(value));
        }
    }

    /**
     * Rounds and formats a pint measurement.
     * <p>
     * If the value is less than 4 pints:
     * <ul>
     *     <li>If the fractional part is less than 0.75, it's rounded down to the nearest whole or half pint.
     *         For example, 2.3 pints becomes "2 pints", 2.6 pints becomes "2 1/2 pints".
     *         If the integer part is 0, it's omitted (e.g., 0.3 pints becomes "1/2 pint").
     *         The word "pint" or "pints" is appended appropriately.</li>
     *     <li>Otherwise (fractional part is 0.75 or more), it's rounded to the nearest whole pint.
     *         For example, 2.8 pints becomes "3 pints".
     *         The word "pint" or "pints" is appended appropriately.</li>
     * </ul>
     * If the value is 4 pints or more, it's rounded to the nearest whole pint and "pints" is appended.
     *
     * @param pint The pint value to round and format.
     * @return A string representation of the rounded pint measurement (e.g., "1/2 pint", "2 1/2 pints", "4 pints").
     */
    private String roundPint(double pint)
    {
        double rounded = 0;
        if (pint < 4)
        {
            int intPart = (int) pint;
            double frac = pint - (double) intPart;
            if (frac < .75)
            {
                return (intPart == 0 ? "" : new DecimalFormat("#").format(Math.floor(pint)) + (frac > 0.25 ? " 1/2" : "")) + " pint" + (intPart == 0 ? "" : "s");
            } else
            {
                return new DecimalFormat("#").format(Math.round(pint)) + " pint" + (intPart == 1 ? "" : "s");
            }
        } else
        {
            return new DecimalFormat("#").format(Math.round(pint)) + " pints";
        }
    }

    /**
     * Rounds and formats a given pound (lb) value into a human-readable string.
     * <p>
     * The rounding logic is as follows:
     * <ul>
     *   <li>If the pound value is less than 15/16 lb (i.e., less than 15 oz),
     *       it is converted to ounces, rounded to the nearest whole ounce,
     *       and formatted as "X oz".</li>
     *   <li>If the pound value is between 15/16 lb (inclusive) and 4 lb (exclusive):
     *     <ul>
     *       <li>The integer part of the pound value is extracted.</li>
     *       <li>The fractional part is calculated.</li>
     *       <li>If the fractional part is less than 0.75:
     *         <ul>
     *           <li>The output starts with the floor of the pound value (if not zero).</li>
     *           <li>If the fractional part is greater than 0.25, " 1/2" is appended.</li>
     *           <li>" lb" is appended.</li>
     *         </ul>
     *       </li>
     *       <li>Otherwise (fractional part is 0.75 or more), the pound value is rounded
     *           to the nearest whole number and formatted as "X lb".</li>
     *     </ul>
     *   </li>
     *   <li>If the pound value is 4 lb or greater, it is rounded to the nearest
     *       whole number and formatted as "X lb".</li>
     * </ul>
     *
     * @param lb The pound value to round and format.
     * @return A string representation of the rounded and formatted pound value
     * (e.g., "10 oz", "1 1/2 lb", "3 lb").
     */
    private String roundLb(double lb)
    {
        double rounded = 0;
        if (lb < (15d / 16d))
        {
            double oz = lb * LB_TO_OZ;
            return new DecimalFormat("#").format(Math.round(oz)) + " oz";
        } else if (lb < 4)
        {
            int intPart = (int) lb;
            double frac = lb - (double) intPart;
            if (frac < .75)
            {
                return (intPart == 0 ? "" : new DecimalFormat("#").format(Math.floor(lb)) + (frac > 0.25 ? " 1/2" : "")) + " lb";
            } else
            {
                return new DecimalFormat("#").format(Math.round(lb)) + " lb";
            }
        } else
        {
            return new DecimalFormat("#").format(Math.round(lb)) + " lb";
        }
    }


    /**
     * Creates a string representation of the quantity.
     * The output format depends on the current unit and whether rounding is requested.
     * If rounding is requested, the value is rounded to a practical, human-readable format
     * specific to the unit (e.g., fractions for teaspoons, whole numbers for larger units).
     * If rounding is not requested, the value is formatted to four decimal places.
     *
     * @param round {@code true} to round the value to a practical representation,
     *              {@code false} to format the value to four decimal places.
     * @return A string representing the quantity, including the unit abbreviation
     * (e.g., "1 1/2 tsp", "10.0000 g").
     */
    public String toString(boolean round)
    {
        DecimalFormat df = new DecimalFormat("0.0000");
        switch (units)
        {
            case GRAM:
                return round ? roundMl(value).trim() + " g" : df.format(value) + " g";
            case ML:
                return round ? roundMl(value).trim() + " ml" : df.format(value) + " ml";
            case OZ:
            case FL_OZ:
                return round ? roundOz(value).trim() + " oz" : df.format(value) + " oz";
            case LB:
                return roundLb(value);
            case TSP:
                return round ? roundTsp(value) : df.format(value) + " tsp";
            case PINT:
                return round ? roundPint(value) : df.format(value) + " pt";
            case INCH:
                return round ? roundInches(value) + (value > 1d ? " inches" : " inch") : df.format(value) + (value > 1d ? " inches" : " inch");
            case MM:
                return round ? roundMm(value) : df.format(value) + " mm";
            case CM:
                return round ? roundCm(value) : df.format(value) + " cm";
            case QT:
                return round ? roundGeneric(value).trim() + "qt" : df.format(value) + " qt";
            case CUP:
                return round ? roundGeneric(value).trim() + " c" : df.format(value) + " c";
            case KG:
                return round ? roundGeneric(value).trim() + " kg" : df.format(value) + " kg";
            case TBSP:
                return round ? roundTbsp(value) : df.format(value) + " tbsp";
            case CAN:
                return (round ? roundGeneric(value).trim() + " can" : df.format(value) + " can") + (value > 1d ? "s" : "");
            case PKG:
                return (round ? roundGeneric(value).trim() + " pkg" : df.format(value) + " pkg") + (value > 1d ? "s" : "");
            default:
                return round ? roundGeneric(value).trim() : df.format(value);
        }
    }

    public int getUnits()
    {
        return units;
    }

    public void setUnits(int units)
    {
        this.units = units;
    }

    public double getValue()
    {
        return value;
    }

    public void setValue(double value)
    {
        this.value = value;
    }

    /**
     * Get text representing units
     *
     * @return
     */
    String getUnitsString()
    {
        switch (units)
        {
            case TBSP:
                return "T";
            case TSP:
                return "t";
            case SMIDGEN:
                return "smidgen";
            case PINCH:
                return "pinch";
            case FL_OZ:
            case OZ:
                return "oz";
            case CUP:
                return "c";
            case PINT:
                return "pint";
            case ML:
                return "ml";
            case LB:
                return "lb";
            case GRAM:
                return "g";
            case CAN:
                return "can";
            case PKG:
                return "pkg";
            default:
                return "";
        }
    }


    /**
     * Parses an ingredient string to determine the units of measurement.
     * It splits the ingredient string into words and attempts to identify
     * common cooking units from the first or first two words.
     * If a recognized unit is found, the internal {@code units} field is set
     * to the corresponding constant (e.g., {@code TBSP}, {@code TSP}, {@code CUP}).
     *
     * <p>The method handles various abbreviations and plural forms of units.
     * For example, "T.", "T", "tbsp.", "tbsp", "tablespoon", and "tablespoons"
     * will all be recognized as tablespoons.
     *
     * <p>If "fl" or "fl." is followed by "oz" or "oz.", it's treated as fluid ounces (FL_OZ).
     *
     * <p>If no recognizable unit is found, the {@code units} field is set to {@code NONE},
     * and an empty string is returned.
     *
     * @param ingredient The ingredient string to parse for units.
     *                   This string is typically the portion of an ingredient line
     *                   that comes after the numerical quantity.
     * @return The word(s) from the ingredient string that were identified as the unit(s),
     * or an empty string if no unit was identified. If the unit consists of
     * two words (e.g., "fl oz"), both words are returned, separated by a space.
     */
    private String setUnits(String ingredient)
    {
        String unitWord = "";
        units = NONE;
        String[] words = ingredient.split("\\s|\\t|\\xA0");
        String firstWord = "";
        String secondWord = "";
        String thirdWord = "";
        if (words.length > 0) firstWord = words[0];
        if (words.length > 1) secondWord = words[1];
        if (firstWord.isEmpty())
        {
            firstWord = secondWord;
            secondWord = thirdWord;
            unitWord = " ";
        }
        if (!firstWord.isEmpty())
        {
            unitWord = firstWord;
            String firstWordLc = firstWord.toLowerCase();
            if (firstWord.equals("T.") || firstWord.equals("T") || firstWordLc.equals("tbsp.") || firstWordLc.equals("tbsp") || firstWordLc.equals("tablespoon") || firstWordLc.equals("tablespoons"))
            {
                units = TBSP;
            } else if (firstWord.equals("t.") || firstWord.equals("t") || firstWordLc.equals("tsp") || firstWordLc.equals("tsp.") || firstWordLc.equals("teaspoon") || firstWordLc.equals("teaspoons"))
            {
                units = TSP;
            } else if (firstWordLc.equals("c.") || firstWordLc.equals("c") || firstWordLc.equals("cup") || firstWordLc.equals("cups"))
            {
                units = CUP;
            } else if (firstWordLc.equals("smidgen"))
            {
                units = SMIDGEN;
            } else if (firstWordLc.equals("dash"))
            {
                units = DASH;
            } else if (firstWordLc.equals("pinch"))
            {
                units = PINCH;
            } else if (firstWordLc.equals("g") || firstWordLc.equals("gram") || firstWordLc.equals("grams"))
            {
                units = GRAM;
            } else if (firstWordLc.equals("oz") || firstWordLc.equals("oz.") || firstWordLc.equals("ounces") || firstWordLc.equals("ounce"))
            {
                units = OZ;
            } else if ((firstWordLc.equals("fl") || firstWordLc.equals("fl.")) && (secondWord.equalsIgnoreCase("oz") || secondWord.equalsIgnoreCase("oz.")))
            {
                units = FL_OZ;
                unitWord += " " + secondWord;
            } else if (firstWordLc.equals("quart") || firstWordLc.equals("quarts") || firstWordLc.equals("qt") || firstWordLc.equals("qts"))
            {
                units = QT;
            } else if (firstWordLc.equals("pint") || firstWordLc.equals("pints"))
            {
                units = PINT;
            } else if (firstWordLc.equals("pound") || firstWordLc.equals("pounds") || firstWordLc.equals("lb.") || firstWordLc.equals("lb") || firstWordLc.equals("lbs") || firstWordLc.equals("lbs."))
            {
                units = LB;
            } else if (firstWordLc.equals("inch") || firstWordLc.equals("inches"))
            {
                units = INCH;
            } else if (firstWordLc.equals("ml") || firstWordLc.equals("ml.") || firstWordLc.equals("milliliter") || firstWordLc.equals("milliliters"))
            {
                units = ML;
            } else if (firstWordLc.equals("cm") || firstWordLc.equals("cm."))
            {
                units = CM;
            } else if (firstWordLc.equals("mm") || firstWordLc.equals("mm."))
            {
                units = MM;
                value = value / 10d;
            } else if (firstWordLc.equals("can") || firstWordLc.equals("cans"))
            {
                units = CAN;
            } else if (firstWordLc.equals("pkg.") || firstWordLc.equals("pkg") || firstWordLc.equals("package"))
            {
                units = PKG;
            } else
            {
                units = NONE;
                unitWord = "";
            }
        }
        return unitWord;
    }

    /**
     * Parses a string representation of a number into a double value.
     * This method can handle:
     * <ul>
     *     <li>Whole numbers (e.g., "1", "12")</li>
     *     <li>Decimal numbers (e.g., "1.5", "0.25")</li>
     *     <li>Common fraction characters (e.g., "¼", "½", "¾", "⅓", "⅔", "⅛")</li>
     *     <li>Fractions expressed with a slash (e.g., "1/2", "3/4")</li>
     *     <li>Mixed numbers where the whole number and fraction are concatenated with a fraction character (e.g., "1¼")</li>
     *     <li>The words "a" or "another" (case-insensitive), which are interpreted as 1.</li>
     * </ul>
     * If the input string cannot be parsed into a number, it returns -1.0.
     *
     * @param value The string to parse.
     * @return A double representing the numeric value of the input string, or -1.0 if parsing fails.
     */
    double parseNumber(String value)
    {
        if (value.equalsIgnoreCase("a") || value.equalsIgnoreCase("another")) return 1d;
        try
        {
            if (value.length() > 1 && value.substring(value.length() - 1).matches(FRACTIONS))
            {
                return parseNumber(value.substring(0, value.length() - 1)) + parseNumber(value.substring(value.length() - 1));
            } else
            {
                if (value.equals("¼")) return 1d / 4d;
                else if (value.equals("½")) return 1d / 2d;
                else if (value.equals("¾")) return 3d / 4d;
                else if (value.equals("⅓")) return 1d / 3d;
                else if (value.equals("⅔")) return 2d / 3d;
                else if (value.equals("⅛")) return 1d / 8d;
                else if (value.contains("/"))
                    return parseNumber(value.split("/")[0]) / parseNumber(value.split("/")[1]);
                else return Double.parseDouble(value);
            }
        } catch (NumberFormatException e)
        {
            return -1d;
        }
    }

    /**
     * Parses an ingredient string to extract the quantity (numerical value and units).
     *
     * <p>This method attempts to identify a numerical value at the beginning of the
     * ingredient string, followed by an optional unit of measurement. It uses regular
     * expressions to find the value and then calls {@link #setUnits(String)} to
     * determine the units from the subsequent word(s).
     *
     * <p>The method performs the following steps:
     * <ol>
     *     <li>Initializes {@code units} to {@code NONE} and {@code value} to {@code 0.0}.</li>
     *     <li>Normalizes whitespace in the input {@code ingredient} string.</li>
     *     <li>Uses a regular expression (defined by {@code VALUE_PARSE}) to find a potential
     *         numerical value at the beginning of the string.</li>
     *     <li>If a value is found:
     *         <ul>
     *             <li>The matched value string is extracted.</li>
     *             <li>The remaining part of the ingredient string is updated.</li>
     *             <li>{@link #setUnits(String)} is called on the remaining string to identify units.</li>
     *             <li>If units are identified, the unit word(s) are removed from the remaining ingredient string.</li>
     *         </ul>
     *     </li>
     *     <li>If a value string was extracted, it is split into parts (to handle mixed numbers like "1 1/2")
     *         and each part is parsed using {@link #parseNumber(String)}. The sum of these parts
     *         sets the internal {@code value} field.</li>
     *     <li>The remaining ingredient string is trimmed.</li>
     *     <li>If {@link #parseNumber(String)} returned -1 (indicating a parsing error for the value),
     *         the original ingredient string is returned unmodified.</li>
     *     <li>Otherwise, the remaining part of the ingredient string (after the quantity has been removed)
     *         is returned.</li>
     * </ol>
     *
     * @param ingredient The full ingredient string (e.g., "1 1/2 cups flour", "2 large eggs").
     * @return The remaining part of the ingredient string after the quantity (value and units)
     */
    private String setQuantity(String ingredient)
    {
        units = NONE;
        value = 0d;
        Pattern pattern = Pattern.compile(VALUE_PARSE);
        String valueString = "";
        String remainingIngredient = ingredient.replaceAll("\\s|\\t|\\xA0", " ").trim();
        Matcher matcher = pattern.matcher(remainingIngredient);
        if (matcher.find())
        {
            valueString = matcher.group(0);
            remainingIngredient = remainingIngredient.substring(valueString.length());
            remainingIngredient = remainingIngredient.replaceAll("\\s|\\t|\\xA0", " ");
            String unitWord = setUnits(remainingIngredient);
            if (!unitWord.isEmpty())
            {
                if (unitWord.length() < remainingIngredient.length())
                {
                    remainingIngredient = remainingIngredient.substring(unitWord.length());
                } else
                {
                    remainingIngredient = "";
                }
            }
        } else
        {
            valueString = "";
        }
        if (!valueString.isEmpty())
        {
            String[] valueParts = valueString.trim().split("[ -]");
            double value = 0d;
            for (String valuePart : valueParts)
            {
                value += parseNumber(valuePart);
            }
            this.value = value;
            remainingIngredient = remainingIngredient.trim();
        }
        if (value == -1d)
        {
            return ingredient;
        }
        return remainingIngredient;
    }


    /**
     * Convert ingredient based on servings and measurement system
     *
     * @param ingredient   ingredient string
     * @param fromServings servings listed in the original recipe
     * @param toServings   servings requested by user
     * @param fromSystem   measurement system listed in recipe
     * @param toSystem     measurement system requested by user
     * @param round        round to nearest practical measure
     * @return converted ingredient string
     */
    String convert(String ingredient, int fromServings, int toServings, int fromSystem, int toSystem, boolean round)
    {
        return convert(ingredient, fromServings, toServings, fromSystem, toSystem, round, false);
    }

    /**
     * Convert ingredient based on servings and measurement system
     *
     * @param ingredient   ingredient string
     * @param fromServings servings listed in the original recipe
     * @param toServings   servings requested by user
     * @param fromSystem   measurement system listed in recipe
     * @param toSystem     measurement system requested by user
     * @param round        round to nearest practical measure
     * @param setBaseUnits result will be in base units (oz, lb, ml, g)
     * @return converted ingredient string
     */
    String convert(String ingredient, int fromServings, int toServings, int fromSystem, int toSystem, boolean round, boolean setBaseUnits)
    {
        value = 0d;
        units = NONE;
        String ingredientRemainder = "";
        ingredientRemainder = setQuantity(ingredient);
        if (value == -1d) return ingredient;
        if (units == NONE)
        {
            return (roundGeneric(value * toServings / fromServings) + " " + ingredientRemainder.trim()).trim();
        }
        if (units == INCH && toSystem == METRIC)
        {
            value = value * INCH_TO_MM;
            units = MM;
            return toString(round) + " " + ingredientRemainder.trim();
        }
        if (units == MM && toSystem == IMPERIAL)
        {
            value = value / INCH_TO_MM;
            units = INCH;
            return toString(round) + " " + ingredientRemainder.trim();
        }
        if (units == CM && toSystem == IMPERIAL)
        {
            value = value / INCH_TO_MM * 10d;
            units = INCH;
            return toString(round) + " " + ingredientRemainder.trim();
        }
        if (setBaseUnits && units == CM)
        {
            units = MM;
            value = value * 10d;
        }
        if (units == INCH || units == CM || units == MM)
        {
            return toString(round) + " " + ingredientRemainder.trim();
        }
        value = value * toServings / fromServings;
        if (units == OZ && isMass(ingredientRemainder))
        {
            units = LB;
            value = value / LB_TO_OZ;
        }
        if (units == LB)
        {
            units = OZ;
            value = value * LB_TO_OZ;
            if (toSystem == IMPERIAL)
            {
                if (value >= LB_TO_OZ)
                {
                    value = value / LB_TO_OZ;
                    units = LB;
                }
            } else
            {
                units = GRAM;
                value = value * OZ_TO_G;
                if (value > 490 && value < 510 || value > 800)
                {
                    value = value / 1000;
                    units = KG;
                }
            }
            return toString(round) + " " + ingredientRemainder.trim();
        }
        if (units == DASH)
        {
            units = OZ;
            value = value / OZ_TO_DASH;
        } else if (units == SMIDGEN)
        {
            units = OZ;
            value = value / OZ_TO_SMIDGEN;
        } else if (units == PINCH)
        {
            units = OZ;
            value = value / OZ_TO_PINCH;
        } else if (units == TSP)
        {
            units = OZ;
            value = value / OZ_TO_TSP;
        } else if (units == TBSP)
        {
            units = OZ;
            value = value / OZ_TO_TBSP;
        } else if (units == CUP)
        {
            units = OZ;
            value = value / OZ_TO_CUPS;
        } else if (units == PINT)
        {
            units = OZ;
            value = value / OZ_TO_PINTS;
        } else if (units == QT)
        {
            units = OZ;
            value = value / OZ_TO_QTS;
        }
        if (units == OZ && toSystem == METRIC)
        {
            units = ML;
            value = value * OZ_TO_ML;
        }
        if (toSystem == IMPERIAL)
        {
            if (units == ML)
            {
                units = OZ;
                value = value / OZ_TO_ML;
            }
            if (units == OZ && !setBaseUnits)
            {
                doBestGuessImperial();
            }
            if (units == GRAM)
            {
                units = LB;
                value = value / OZ_TO_G / LB_TO_OZ;
            }
        }

        return toString(round) + " " + ingredientRemainder.trim();
    }


    /**
     * Attempt to convert imperial measurement to the closest practical units
     */
    private void doBestGuessImperial()
    {
        if (value > 10 / OZ_TO_CUPS)
        {
        } else if (value > 7d / 32d / OZ_TO_CUPS)
        {
            units = CUP;
            value = value * OZ_TO_CUPS;
        } else if (value >= 0.83d / OZ_TO_TBSP)
        {
            units = TBSP;
            value = value * OZ_TO_TBSP;
        } else
        {
            units = TSP;
            value = value * OZ_TO_TSP;
        }
    }

    /**
     * Convert a block of text from imperial to metric
     *
     * @param directions
     * @param fromServings
     * @param toServings
     * @param fromSystem
     * @param toSystem
     * @return
     */
    private String convertImpToMet(String directions, int fromServings, int toServings, int fromSystem, int toSystem)
    {
        String newDirections = directions;
        String temperature = "";
        String phrase = "";
        boolean found = true;
        Pattern pattern = Pattern.compile("([0-9]{3})(°?F| degrees? ?F?)");
        Matcher matcher = pattern.matcher(newDirections);
        while (found)
        {
            if (matcher.find())
            {
                temperature = matcher.group(1);
                phrase = matcher.group(0);
                temperature = fToC(temperature);
                newDirections = newDirections.replace(phrase.trim(), temperature + "C");
            } else
            {
                found = false;
            }
        }
        found = true;
        pattern = Pattern.compile("([0-9]{3})°");
        matcher = pattern.matcher(newDirections);
        while (found)
        {
            if (matcher.find())
            {
                temperature = matcher.group(1);
                phrase = matcher.group(0);
                temperature = fToC(temperature);
                newDirections = newDirections.replace(phrase.trim(), temperature + "C");
            } else
            {
                found = false;
            }
        }
        newDirections = convertSystemAndValue(newDirections, fromServings, toServings, fromSystem, toSystem);
        return newDirections;
    }

    /**
     * convert block of text from metric to imperial
     *
     * @param directions
     * @param fromServings
     * @param toServings
     * @param fromSystem
     * @param toSystem
     * @return
     */
    private String convertMetToImp(String directions, int fromServings, int toServings, int fromSystem, int toSystem)
    {
        String newDirections = directions;
        String temperature = "";
        String phrase = "";
        boolean found = true;
        Pattern pattern = Pattern.compile("([0-9]{3})(°?C| degrees? ?C?)");
        Matcher matcher = pattern.matcher(newDirections);
        while (found)
        {
            if (matcher.find())
            {
                temperature = matcher.group(1);
                phrase = matcher.group(0);
                temperature = cToF(temperature);
                newDirections = newDirections.replace(phrase.trim(), temperature + "F");
            } else
            {
                found = false;
            }
        }
        newDirections = convertSystemAndValue(newDirections, fromServings, toServings, fromSystem, toSystem);
        return newDirections;
    }

    /**
     * fahrenheit to celsius
     *
     * @param degrees
     * @return
     */
    private String fToC(String degrees)
    {
        String result = degrees;
        try
        {
            double f = Double.parseDouble(degrees);
            double c = (f - 32d) * 5d / 9d;
            c = Math.round(c / 10d) * 10d;
            result = new DecimalFormat("#").format(c);
        } catch (Exception e)
        {
        }
        return result;
    }

    /**
     * celsius to fahrenheit
     *
     * @param degrees
     * @return
     */
    private String cToF(String degrees)
    {
        String result = degrees;
        try
        {
            double c = Double.parseDouble(degrees);
            double f = c * 9d / 5d + 32d;
            f = Math.round(f / 25d) * 25d;
            result = new DecimalFormat("#").format(f);
        } catch (Exception e)
        {
        }
        return result;
    }

    /**
     * attempt to convert units within directions
     *
     * @param directions
     * @param fromServings
     * @param toServings
     * @param fromSystem
     * @param toSystem
     * @param excludedPhrases
     * @return
     */
    public String convertDirections(String directions, int fromServings, int toServings, int fromSystem, int toSystem, ArrayList<DirectionsPhrase> excludedPhrases)
    {
        this.excludedPhrases = excludedPhrases;
        String newDirections = directions;
        if (toSystem == METRIC && fromSystem == IMPERIAL)
        {
            newDirections = convertImpToMet(newDirections, fromServings, toServings, fromSystem, toSystem);
        } else if (toSystem == IMPERIAL && fromSystem == METRIC)
        {
            newDirections = convertMetToImp(newDirections, fromServings, toServings, fromSystem, toSystem);
        } else
        {
            newDirections = convertSystemAndValue(newDirections, fromServings, toServings, fromSystem, toSystem);
        }
        this.excludedPhrases = null;
        return newDirections;
    }


    /**
     * Convert measurements in directions
     *
     * @param directions
     * @param fromServings
     * @param toServings
     * @param fromSystem
     * @param toSystem
     * @return
     */
    private String convertSystemAndValue(String directions, int fromServings, int toServings, int fromSystem, int toSystem)
    {
        String newDirections = directions;
        String phrase;
        boolean found;
        Pattern pattern;
        Matcher matcher;
        for (String ingredientDetect : INGREDIENT_DETECT)
        {
            found = true;
            pattern = Pattern.compile(ingredientDetect);
            matcher = pattern.matcher(newDirections);
            String newPhrase;
            while (found)
            {
                if (matcher.find())
                {
                    phrase = matcher.group(0);
                    if (isExcluded(phrase))
                    {
                        newPhrase = convert(phrase, fromServings, fromServings, fromSystem, toSystem, false);
                    } else
                    {
                        newPhrase = convert(phrase, fromServings, toServings, fromSystem, toSystem, false);
                    }
                    newDirections = newDirections.replace(phrase, (phrase.matches("\\s.+") ? " " : "") + newPhrase);
                } else
                {
                    found = false;
                }
            }
        }
        found = true;
        pattern = Pattern.compile(INGREDIENT_DETECT[0]);
        matcher = pattern.matcher(newDirections);
        String newPhrase;
        while (found)
        {
            if (matcher.find())
            {
                phrase = matcher.group(0);
                newPhrase = convert(phrase, toServings, toServings, toSystem, toSystem, true);
                newDirections = newDirections.replace(phrase, (phrase.matches("\\s.+") ? " " : "") + newPhrase.trim());
            } else
            {
                found = false;
            }
        }
        return newDirections;
    }

    /**
     * Is this phrase in the list of exclusions?
     *
     * @param phrase
     * @return
     */
    private boolean isExcluded(String phrase)
    {
        boolean excluded = false;
        if (excludedPhrases != null && !excludedPhrases.isEmpty())
        {
            for (int i = 0; i < excludedPhrases.size() && !excluded; i++)
            {
                if (phrase.equals(excludedPhrases.get(i).getPhraseText()))
                {
                    excluded = true;
                }
            }
        }
        return excluded;
    }

    /**
     * Extracts all phrases from the recipe directions text that potentially represent
     * scalable measurements (e.g., "1 cup flour", "2 tsp salt").
     *
     * <p>This method iterates through predefined regular expression patterns (defined in
     * {@code INGREDIENT_DETECT}) to find matches in the provided directions string.
     * For each match found:
     * <ul>
     *     <li>A {@link DirectionsPhrase} object is created.</li>
     *     <li>The matched text itself is stored as the phrase text.</li>
     *     <li>The start and end indices of the match within the original directions string are stored.</li>
     *     <li>A context string is generated by taking a snippet of the directions text
     *         around the matched phrase (approximately 20 characters before and after).
     *         This context is trimmed and prefixed/suffixed with "..." to indicate it's a snippet.
     *         The context extraction tries to align with natural sentence boundaries like
     *         commas, periods, semicolons, or newlines.</li>
     *     <li>The created {@code DirectionsPhrase} object is added to a list.</li>
     * </ul>
     * If any exception occurs during the processing of a match, that match is skipped,
     * and the search continues.
     *
     * @param directions The full text of the recipe directions.
     * @return An {@link ArrayList} of {@link DirectionsPhrase} objects, each representing
     * a potentially scalable measurement found in the directions. If no such phrases
     * are found, an empty list is returned.
     */
    public ArrayList<DirectionsPhrase> getPhrases(String directions)
    {
        ArrayList<DirectionsPhrase> phrases = new ArrayList<>();
        String phrase;
        boolean found;
        Pattern pattern;
        Matcher matcher;
        for (String ingredientDetect : INGREDIENT_DETECT)
        {
            directions += " ";
            found = true;
            pattern = Pattern.compile(ingredientDetect);
            matcher = pattern.matcher(directions);
            while (found)
            {
                if (matcher.find())
                {
                    try
                    {
                        phrase = matcher.group(0);
                        int start = matcher.start(0);
                        int end = matcher.end(0);
                        DirectionsPhrase directionsPhrase = new DirectionsPhrase();
                        directionsPhrase.setPhraseText(phrase);
                        directionsPhrase.setPhraseStart(start);
                        directionsPhrase.setPhraseEnd(end);
                        int cStart = start - 20;
                        if (cStart < 0) cStart = 0;
                        int cEnd = end + 20;
                        if (cEnd > directions.length()) cEnd = directions.length();
                        for (int i = start; i > cStart; i--)
                        {
                            if ("\n,.;".contains(Character.toString(directions.charAt(i))))
                            {
                                cStart = i;
                            }
                        }
                        boolean done = false;
                        for (int i = cStart; i < start && !done; i++)
                        {
                            if (directions.charAt(i) == ' ') cStart = i;
                        }
                        done = false;
                        int tempEnd = cEnd;
                        for (int i = end; i < tempEnd && !done; i++)
                        {
                            if ("\n,.;".contains(Character.toString(directions.charAt(i))))
                            {
                                cEnd = i;
                                done = true;
                            }
                            if (directions.charAt(i) == ' ')
                            {
                                cEnd = i;
                            }
                        }
                        if (" ,.;".contains(Character.toString(directions.charAt(cStart))))
                            cStart++;
                        String context = directions.substring(cStart, cEnd);
                        context = "..." + context.trim() + "...";

                        directionsPhrase.setPhraseContext(context);

                        phrases.add(directionsPhrase);
                    } catch (Exception e)
                    {
                        found = false;
                    }
                } else
                {
                    found = false;
                }
            }
        }
        return phrases;
    }


    /**
     * Removes preparation-related words (e.g., "diced", "cooked", "chopped") from an ingredient name string.
     * This method iterates through a predefined list of preparation words ({@code PREP_WORDS})
     * and removes them if they appear in common patterns within the ingredient name:
     * <ul>
     *     <li>Preceded by a comma and a space (e.g., ", diced")</li>
     *     <li>At the end of the string, preceded by " and " (e.g., " and cooked")</li>
     *     <li>At the end of the string, preceded by " and " and followed by a period (e.g., " and cooked.")</li>
     * </ul>
     * The comparison and replacement are done in a case-insensitive manner.
     *
     * @param name The ingredient name string from which to remove preparation words.
     * @return The ingredient name string with preparation words removed, converted to lowercase.
     */
    private String stripPrep(String name)
    {
        String result = name;
        for (String prep : PREP_WORDS)
        {
            if (result.toLowerCase().contains(", " + prep))
            {
                result = result.toLowerCase().replace(", " + prep, "");
            }
            if (result.toLowerCase().endsWith(" and " + prep))
            {
                result = result.toLowerCase().replace(" and " + prep, "");
            }
            if (result.toLowerCase().endsWith(" and " + prep + "."))
            {
                result = result.toLowerCase().replace(" and " + prep + ".", "");
            }
        }
        return result;
    }


}
