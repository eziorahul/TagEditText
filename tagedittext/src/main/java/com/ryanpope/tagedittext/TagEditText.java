package com.ryanpope.tagedittext;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.InputType;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.AttributeSet;
import android.view.View;
import android.widget.AutoCompleteTextView;

import com.ryanpope.tagedittext.tag.Tag;
import com.ryanpope.tagedittext.tag.TagViewComposer;
import com.ryanpope.tagedittext.tag.views.TagSpan;

import java.util.ArrayList;
import java.util.List;

import static android.text.TextUtils.isEmpty;

public class TagEditText extends AutoCompleteTextView {
    private TagViewComposer mTagViewComposer;

    private static final String SEPARATOR = "\\s+";

    public TagEditText(final Context context) {
        this(context, null);
    }

    public TagEditText(final Context context, final AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TagEditText(final Context context, final AttributeSet attrs, final int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(context, attrs, defStyleAttr);
    }

    private void init(final Context context, final AttributeSet attributeSet, final int defStyleAttr) {
        mTagViewComposer = new TagViewComposer(context, attributeSet, defStyleAttr);

        configureAttributes();
    }

    private void configureAttributes() {
        setFocusableInTouchMode(true);
        setMovementMethod(LinkMovementMethod.getInstance());
        setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

    }

    private void applySpansToText() {
        final CharSequence text = getText();

        final String[] tagsSeparatedBySeparator = getTagsSeparatedBySeparator(text);
        final List<Tag> tags = buildTagSpansFromSeparatedWords(tagsSeparatedBySeparator);

        addTagViewsToEditText(tags);
    }

    private String[] getTagsSeparatedBySeparator(final CharSequence text) {
        if (isEmpty(text)) {
            return new String[0];
        }

        final String currentText = String.valueOf(text);

        return currentText.split(SEPARATOR);
    }

    private List<Tag> buildTagSpansFromSeparatedWords(final String[] wordList) {
        final String text = getText().toString();
        final List<Tag> tagList = new ArrayList<>();
        int indexToStartFrom = 0;

        for (final String word : wordList) {
            final int startIndex = text.indexOf(word, indexToStartFrom);
            final int endIndex = startIndex + word.length();

            indexToStartFrom = endIndex;

            final Tag newTag = new Tag.Builder()
                    .withWord(word)
                    .withStartIndex(startIndex)
                    .withEndIndex(endIndex)
                    .build();

            tagList.add(newTag);
        }

        return tagList;
    }

    private void addTagViewsToEditText(final List<Tag> tags) {
        final Spannable textSpannable = getText();

        for (final Tag tag : tags) {
            setSpans(textSpannable, tag);
        }

        ensureTailWhitespace();
    }

    private void setSpans(final Spannable textSpannable, final Tag tag) {
        final Drawable drawable = mTagViewComposer.createTagSpanForTag(tag);
        final TagSpan tagSpan = new TagSpan(drawable, tag.getWord());
        final ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(final View widget) {
                removeSpan(tag);
            }
        };

        textSpannable.setSpan(
                tagSpan,
                tag.getStartIndex(),
                tag.getEndIndex(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        textSpannable.setSpan(
                clickableSpan,
                tag.getStartIndex(),
                tag.getEndIndex(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void removeSpan(final Tag span) {
        getText().delete(span.getStartIndex(), span.getEndIndex());
    }

    @Override
    public void onTextChanged(final CharSequence text, final int start, final int lengthBefore, final int lengthAfter) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter);

        if (!hasWhitespace(text.toString())
                && !isBeingCreated()
                && !isDeleting(lengthBefore, lengthAfter)) {
            return;
        }

        applySpansToText();
    }

    private boolean isBeingCreated() {
        return getText().getSpans(0, length(), TagSpan.class).length == 0;
    }

    private void ensureTailWhitespace() {
        if (hasWhitespace(getText().toString())) {
            return;
        }

        append(" ");
    }

    private boolean isDeleting(final int lengthBefore, final int lengthAfter) {
        return lengthBefore > lengthAfter;
    }

    private boolean hasWhitespace(final String text) {
        return !text.isEmpty() && Character.isWhitespace(text.charAt(text.length() - 1));
    }

    @Override
    protected void onSelectionChanged(final int selStart, final int selEnd) {
        super.onSelectionChanged(selStart, selEnd);

        setSelection(length());
    }
}