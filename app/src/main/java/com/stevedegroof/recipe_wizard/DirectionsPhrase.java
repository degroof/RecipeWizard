package com.stevedegroof.recipe_wizard;

/**
 * Represents a phrase within recipe directions, often indicating a measurement or specific action.
 * This class encapsulates the text of the phrase, its starting and ending positions within the
 * overall directions, and the surrounding context of the phrase.
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
