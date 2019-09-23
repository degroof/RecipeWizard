package com.stevedegroof.recipe_wizard;

/**
 * a single grocery list item
 * contains text and whether it's been checked off
 */
public class GroceryListItem
{
    private String text = "";
    private boolean checked = false;

    public GroceryListItem(String text, boolean checked)
    {
        setChecked(checked);
        setText(text);
    }

    public String getText()
    {
        return text;
    }

    public void setText(String text)
    {
        this.text = text;
    }

    public boolean isChecked()
    {
        return checked;
    }

    public void setChecked(boolean checked)
    {
        this.checked = checked;
    }
}
