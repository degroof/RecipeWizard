package com.stevedegroof.recipe_wizard;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility to calculate and convert quantities
 */
public class UnitsConverter
{
    //measurement systems
    public static final int METRIC = 1;
    public static final int IMPERIAL = 2;
    //units
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

    //conversion factors
    private static final double TSP_TO_ML = 4.93d;
    private static final double TBSP_TO_ML = 14.79d;
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

    //used to detect values
    public static final String VALUE_PARSE = "^([0-9¼½¾⅓⅔⅛\\./ -]+|a |another )";
    private static final String FRACTIONS = "[¼½¾⅓⅔⅛]";
    private static final String UNITS_DETECT = "([\\s-])?(oz|cups?|T\\b|t\\b|c\\b|c\\.|TBSP\\b|tbsp\\b|tsp\\b|tablespoons?\\b|Tablespoons?\\b|teaspoons?\\b|quart\\b|qt\\b|inch(es)?\\b|mls?\\b|cm\\b|mm\\b|liters?\\b|litres?\\b|g\\b|grams?\\b|kgs?\\b)";
    private static final String VALUE_DETECT1 = "\\s[0-9]+\\.[0-9]+";
    private static final String VALUE_DETECT2 = "\\s([0-9]+)?([\\s-])?(([0-9]+/[0-9]+)|[¼½¾⅓⅔⅛])";
    private static final String VALUE_DETECT3 = "\\s[0-9]+";
    private static final String[] INGREDIENT_DETECT = {VALUE_DETECT1 + UNITS_DETECT, VALUE_DETECT2 + UNITS_DETECT, VALUE_DETECT3 + UNITS_DETECT};
    //used to determine if ounces are mass or volume
    private static final String DRY_INGREDIENTS[] = {"noodles", "ginger root", "chocolate chips",
            "asparagus", "thyme", "tomatoes", "almonds", "cheese", "prosciutto", "arugula", "macaroni",
            "meat", "potatoes", "barramundi", "greens", "beef", "nuts", "beans", "mushrooms", "sausage",
            "chicken"};
    private static final String WET_INGREDIENTS[] = {"sauce", "paste", "soup", "bouillon", "juice",
            "liqueur", "extract", "puree", "purée", "stock", "salsa", "mayo", "mayonnaise", "dressing",
            "milk", "broth"};
    private static final String PREP_WORDS[] = {"chopped", "diced", "quartered", "mashed", "shredded",
            "minced", "cubed", "cooked", "uncooked", "drained", "undrained", "chilled", "cold",
            "halved", "seeded", "peeled", "divided", "beaten", "rinsed", "blanched", "juiced",
            "dry", "flaked", "melted"};

    private double value = 0; //numeric part of the quantity
    private int units = NONE; //units part of the quantity
    private ArrayList<DirectionsPhrase> excludedPhrases; //phrases in the directions that should not be scales
    private int detectedSystem = IMPERIAL;

    /**
     * Try to guess if the ingredient is mass vs volume (used for ounces)
     *
     * @param ingredient
     * @return
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
     * Round and format ml
     *
     * @param ml
     * @return
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
     * Round and format mm
     *
     * @param mm
     * @return
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
     * Round and format cm
     *
     * @param cm
     * @return
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
     * Round and format grams
     *
     * @param gram
     * @return
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
     * round and format ounces
     *
     * @param oz
     * @return
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
     * Round and format tsp
     *
     * @param tsp
     * @return
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
     * Round and format inches
     *
     * @param value
     * @return
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
     * Round and format units (generic)
     *
     * @param value
     * @return
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
     * Round and format pints
     *
     * @param pint
     * @return
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
     * Round and format pounds
     *
     * @param lb
     * @return
     */
    private String roundLb(double lb)
    {
        double rounded = 0;
        if (lb < 4)
        {
            int intPart = (int) lb;
            double frac = lb - (double) intPart;
            if (frac < .75)
            {
                return (intPart == 0 ? "" : new DecimalFormat("#").format(Math.floor(lb)) + (frac > 0.25 ? " 1/2" : ""));
            } else
            {
                return new DecimalFormat("#").format(Math.round(lb));
            }
        } else
        {
            return new DecimalFormat("#").format(Math.round(lb));
        }
    }


    /**
     * create string representing the quantity
     *
     * @param round
     * @return
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
                return round ? roundOz(value).trim() + " oz" : df.format(value) + " oz";
            case LB:
                return round ? roundGeneric(value).trim() + " lb" : df.format(value) + " lb";
            case FL_OZ:
                return round ? roundOz(value).trim() + " oz" : df.format(value) + " oz";
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
                return round ? new DecimalFormat("#").format(Math.round(value)) + " tbsp" : df.format(value) + " tbsp";
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
            case NONE:
                return "";
            case TBSP:
                return "T";
            case TSP:
                return "t";
            case SMIDGEN:
                return "smidgen";
            case PINCH:
                return "pinch";
            case FL_OZ:
                return "oz";
            case CUP:
                return "c";
            case PINT:
                return "pint";
            case ML:
                return "ml";
            case OZ:
                return "oz";
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
     * Set units based on ingredient string
     *
     * @param ingredient
     * @return text representing units
     */
    private String setUnits(String ingredient)
    {
        String unitWord = "";
        units = NONE;
        String words[] = ingredient.split("\\s|\\t|\\xA0");
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
            } else if ((firstWordLc.equals("fl") || firstWordLc.equals("fl.")) && (secondWord.toLowerCase().equals("oz") || secondWord.toLowerCase().equals("oz.")))
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
     * Parse numeric part of quantity
     *
     * @param value
     * @return a double representing the value
     */
    double parseNumber(String value)
    {
        if (value.toLowerCase().equals("a") || value.toLowerCase().equals("another")) return 1d;
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
     * Set quantity (value and units) from ingredient string
     *
     * @param ingredient
     * @return remaining ingredient (minus quantity text)
     */
    private String setQuantity(String ingredient)
    {
        units = NONE;
        value = 0d;
        Pattern pattern = Pattern.compile(VALUE_PARSE);
        String valueString = "";
        String remainingIngredient = ingredient.replaceAll("\\s|\\t|\\xA0", " ");
        Matcher matcher = pattern.matcher(remainingIngredient);
        if (matcher.find())
        {
            valueString = matcher.group(0);
            remainingIngredient = remainingIngredient.substring(valueString.length());
            String unitWord = setUnits(remainingIngredient);
            if (!unitWord.isEmpty())
            {
                remainingIngredient = remainingIngredient.replaceAll("\\s|\\t|\\xA0", " ");
                remainingIngredient = remainingIngredient.substring(unitWord.length());
            }
        } else
        {
            valueString = "";
        }
        if (!valueString.isEmpty())
        {
            String valueParts[] = valueString.trim().split("[ -]");
            double value = 0d;
            for (int i = 0; i < valueParts.length; i++)
            {
                value += parseNumber(valueParts[i]);
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
     * Set quantity (value and units) from grocery list string
     *
     * @param ingredient
     * @return remaining ingredient (minus quantity text)
     */
    private String setQuantityFromGroceryListItem(String ingredient)
    {
        try
        {
            String remainingIngredient = ingredient;
            if (!remainingIngredient.endsWith(")") || !remainingIngredient.contains("("))
                return remainingIngredient;
            int quantityStart = remainingIngredient.lastIndexOf("(");
            String quantity = remainingIngredient.substring(quantityStart + 1, remainingIngredient.length() - 1);
            remainingIngredient = remainingIngredient.substring(0, quantityStart);
            setQuantity(quantity + " x");
            return remainingIngredient;
        } catch (Exception e)
        {
            return ingredient;
        }
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
            return;
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
                    if (isExcluded(phrase)) //if this phrase matches one in the list of exclusions, don't scale
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
     * Get all potentially scalable measures in the directions
     *
     * @param directions
     * @return
     */
    public ArrayList<DirectionsPhrase> getPhrases(String directions)
    {
        ArrayList<DirectionsPhrase> phrases = new ArrayList<DirectionsPhrase>();
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
                            if (directions.charAt(i) == " ".charAt(0)) cStart = i;
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
                            if (directions.charAt(i) == " ".charAt(0))
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
     * Convert ingredients list into grocery list items
     *
     * @param ingredients
     * @return
     */
    ArrayList<String> convertIngredientsToGroceryList(ArrayList<String> ingredients)
    {
        ArrayList<String> groceryList = new ArrayList<String>();
        for (String ingredient : ingredients)
        {
            String name = stripPrep(setQuantity(ingredient));
            String quantity = toString(true);
            groceryList.add(quantity.trim().isEmpty() ? name.trim() : name.trim() + " (" + quantity + ")");
        }
        return groceryList;
    }

    /**
     * Match up similar ingredients and add their quantities
     *
     * @param ingredients
     * @return
     */
    ArrayList<String> consolidateGroceryList(ArrayList<String> ingredients)
    {
        ArrayList<String> result = new ArrayList<String>();

        for (String thisIngredient : ingredients)
        {
            int thisSystem = IMPERIAL;
            String thisName = setQuantityFromGroceryListItem(thisIngredient);
            thisName = stripPrep(thisName);
            int thisUnits = units;
            boolean thisMass = isMass(thisIngredient);
            double thisValue = value;
            if (units == CM || units == MM || units == ML || units == KG || units == GRAM)
            {
                thisSystem = METRIC;
            }
            boolean found = false;
            for (String otherIngredient : result)
            {
                int otherSystem = IMPERIAL;
                String otherName = setQuantityFromGroceryListItem(otherIngredient);
                otherName = stripPrep(otherName);
                if (units == CM || units == MM || units == ML || units == KG || units == GRAM)
                {
                    otherSystem = METRIC;
                }
                int otherUnits = units;
                boolean otherMass = isMass(otherIngredient);
                double otherValue = value;
                if (thisSystem == otherSystem && thisMass == otherMass && thisName.trim().equalsIgnoreCase(otherName.trim()))
                {
                    String newIngredient = addValues(thisValue, thisUnits, otherValue, otherUnits, thisName, thisSystem, thisMass);
                    result.remove(otherIngredient);
                    result.add(newIngredient);
                    found = true;
                    break;
                }
            }
            if (!found && !thisIngredient.trim().endsWith(":"))
            {
                thisIngredient = stripPrep(thisIngredient);
                result.add(thisIngredient);
            }
        }

        return result;
    }

    /**
     * remove preparation text (e.g. diced, cooked) from ingredient
     *
     * @param name
     * @return
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

    /**
     * Add the quantities of two similar ingredients together
     *
     * @param thisValue
     * @param thisUnits
     * @param otherValue
     * @param otherUnits
     * @param name
     * @param system
     * @param isMass
     * @return
     */
    private String addValues(double thisValue, int thisUnits, double otherValue, int otherUnits, String name, int system, boolean isMass)
    {
        DecimalFormat df = new DecimalFormat("0.0000");

        units = thisUnits;
        String thisIngredient = df.format(thisValue) + " " + getUnitsString() + " " + name;
        units = otherUnits;
        String otherIngredient = df.format(otherValue) + " " + getUnitsString() + " " + name;
        convert(thisIngredient, 1, 1, system, system, false, true);
        double newValue = value;
        convert(otherIngredient, 1, 1, system, system, false, true);
        value += newValue;
        String newIngredient = df.format(value) + " " + getUnitsString() + " " + name;
        ArrayList<String> ingredients = new ArrayList<String>();
        ingredients.add(convert(newIngredient, 1, 1, system, system, true));
        ingredients = convertIngredientsToGroceryList(ingredients);
        return ingredients.get(0);
    }



}
