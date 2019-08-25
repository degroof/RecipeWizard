package com.stevedegroof.recipe_wizard;

/**
 * A phrase within the directions; usually a measurement
 */
public class DirectionsPhrase
{
    private String phraseText;
    private String phraseContext;
    private int phraseStart;
    private int phraseEnd;

    public String getPhraseText()
    {
        return phraseText;
    }

    public void setPhraseText(String phraseText)
    {
        this.phraseText = phraseText;
    }

    public int getPhraseStart()
    {
        return phraseStart;
    }

    public void setPhraseStart(int phraseStart)
    {
        this.phraseStart = phraseStart;
    }

    public int getPhraseEnd()
    {
        return phraseEnd;
    }

    public void setPhraseEnd(int phraseEnd)
    {
        this.phraseEnd = phraseEnd;
    }

    public String getPhraseContext()
    {
        return phraseContext;
    }

    public void setPhraseContext(String phraseContext)
    {
        this.phraseContext = phraseContext;
    }
}
