package ru.terra.discosuspension.activity.components;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

public class IntEditTextPreference extends EditTextPreference {

    public IntEditTextPreference(final Context context) {
        super(context);
    }

    public IntEditTextPreference(final Context context, final AttributeSet attrs) {
        super(context, attrs);
    }

    public IntEditTextPreference(final Context context, final AttributeSet attrs, final int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected String getPersistedString(final String defaultReturnValue) {
        return String.valueOf(getPersistedInt(-1));
    }

    @Override
    protected boolean persistString(final String value) {
        return persistInt(Integer.valueOf(value));
    }
}