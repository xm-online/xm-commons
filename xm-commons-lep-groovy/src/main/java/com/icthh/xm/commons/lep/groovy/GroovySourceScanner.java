package com.icthh.xm.commons.lep.groovy;

import java.util.Set;

/**
 * EXPERIMENTAL. Enabled by {@code application.lep.experimental-metadata-prescan}, disabled by default.
 *
 * <p>Answers one question about a groovy source: <b>can it declare a type at all?</b> When the answer is no,
 * {@link GroovyFileParser} knows the whole metadata up front - no class, no static field, no static method,
 * and the file compiles to a bare script - so it can skip building the AST, which costs about a millisecond
 * per lep. This scanner costs about four microseconds.
 *
 * <p>The scan is deliberately biased. {@code false} is a guarantee: nothing is declared here. {@code true} is
 * only a suspicion, and the price of a wrong suspicion is one ordinary parse. Everything it does not
 * understand therefore counts as {@code true}.
 *
 * <p>Why hand written and not a lexer: the groovy lexer costs about 25% of a full parse, and only 11 to 30%
 * of real leps can be skipped, so it never pays off. This scanner costs about 0.4% of a parse.
 */
final class GroovySourceScanner {

    /** Keywords that start a type declaration. {@code @interface} is covered by {@code interface}. */
    private static final Set<String> TYPE_KEYWORDS = Set.of("class", "interface", "enum", "trait", "record");

    /** {@code new} matters because {@code new Foo() { ... }} declares an anonymous class. */
    private static final String NEW_KEYWORD = "new";

    private final String source;

    /** Index of the character the scanner looks at right now. */
    private int position;

    private GroovySourceScanner(String source) {
        this.source = source;
    }

    /**
     * @return {@code false} only when the source declares no type whatsoever - no named class, interface,
     *         enum, trait or record, and no anonymous class body either.
     */
    static boolean mayDeclareType(String source) {
        return new GroovySourceScanner(source).scan();
    }

    private boolean scan() {
        while (hasMore()) {
            if (atLineComment()) {
                // "// class Foo {}" declares nothing
                skipLineComment();
            } else if (atBlockComment()) {
                // "/* class Foo {} */" declares nothing either
                skipBlockComment();
            } else if (atQuote()) {
                // neither does "class Foo {}" written inside a string
                skipString();
            } else if (atIdentifierStart()) {
                // the only place a declaration can begin: a word of plain code
                if (declarationStartsHere()) {
                    return true;
                }
            } else {
                // punctuation, digits, whitespace - nothing of interest
                position++;
            }
        }
        return false;
    }

    /**
     * Reads the word under the cursor and decides whether a type declaration starts with it.
     * The cursor always ends up after the word, so the scan makes progress in every case.
     */
    private boolean declarationStartsHere() {
        String word = readIdentifier();

        if (TYPE_KEYWORDS.contains(word)) {
            // "class Foo" declares a type, "foo.class" and "record" used as a variable name do not
            return nameFollows();
        }
        if (NEW_KEYWORD.equals(word)) {
            // "new Runnable() { ... }" declares an anonymous class, "new HashMap()" does not
            return classBodyFollows();
        }
        return false;
    }

    // ---------------------------------------------------------------- lookahead

    /**
     * A type keyword only declares a type when a name follows it. The name may sit on the next line or
     * behind a comment - {@code class /* here * / Foo} is valid groovy - so both are skipped before looking.
     *
     * <p>The cursor is restored afterwards: the lookahead only answers a question, it does not consume input.
     */
    private boolean nameFollows() {
        int startPosition = position;
        try {
            skipWhitespaceAndComments();
            return hasMore() && atIdentifierStart();
        } finally {
            position = startPosition;
        }
    }

    private void skipWhitespaceAndComments() {
        while (hasMore()) {
            if (Character.isWhitespace(source.charAt(position))) {
                position++;
            } else if (atLineComment()) {
                skipLineComment();
            } else if (atBlockComment()) {
                skipBlockComment();
            } else {
                return;
            }
        }
    }

    /**
     * Looks for the class body of an anonymous class between {@code new} and the end of its expression.
     * Any brace on the way counts, so {@code new Foo(a, { it })} - a closure argument, not a class body -
     * costs one ordinary parse. That is the safe direction.
     *
     * <p>The cursor is restored afterwards: the lookahead only answers a question, it does not consume input.
     */
    private boolean classBodyFollows() {
        int startPosition = position;
        try {
            return scanForClassBody();
        } finally {
            position = startPosition;
        }
    }

    private boolean scanForClassBody() {
        // how deep inside ( ) and [ ] the cursor currently is
        int depth = 0;

        while (hasMore()) {
            char current = source.charAt(position);

            if (atLineComment()) {
                skipLineComment();
            } else if (atBlockComment()) {
                skipBlockComment();
            } else if (atQuote()) {
                skipString();
            } else if (current == '{') {
                // a body opened before the expression ended - this may be an anonymous class
                return true;
            } else if (current == '(' || current == '[') {
                depth++;
                position++;
            } else if (current == ')' || current == ']') {
                depth--;
                position++;
                if (depth < 0) {
                    // the bracket that closed is the one around the new expression, so the expression is over
                    return false;
                }
            } else if (depth == 0 && isExpressionEnd(current)) {
                // "new HashMap()" followed by a line break, a semicolon or a closing block: no class body
                return false;
            } else {
                position++;
            }
        }
        return false;
    }

    private static boolean isExpressionEnd(char current) {
        return current == ';' || current == '}' || isLineBreak(current);
    }

    // ---------------------------------------------------------------- cursor primitives

    private boolean hasMore() {
        return position < source.length();
    }

    private boolean atLineComment() {
        return startsHere("//");
    }

    private boolean atBlockComment() {
        return startsHere("/*");
    }

    private boolean atQuote() {
        char current = source.charAt(position);
        return current == '\'' || current == '"';
    }

    private boolean atIdentifierStart() {
        return Character.isJavaIdentifierStart(source.charAt(position));
    }

    private boolean startsHere(String text) {
        return source.startsWith(text, position);
    }

    /** Moves the cursor over the word under it and returns that word. */
    private String readIdentifier() {
        int start = position;
        position++; // the first character is already known to be an identifier start
        while (hasMore() && Character.isJavaIdentifierPart(source.charAt(position))) {
            position++;
        }
        return source.substring(start, position);
    }

    /** Moves the cursor to the line break that ends the comment, leaving the break itself unread. */
    private void skipLineComment() {
        while (hasMore() && source.charAt(position) != '\n' && source.charAt(position) != '\r') {
            position++;
        }
    }

    /** Moves the cursor past the closing {@code * /}, or to the end of an unterminated comment. */
    private void skipBlockComment() {
        int end = source.indexOf("*/", position + "/*".length());
        position = end < 0 ? source.length() : end + "*/".length();
    }

    /**
     * Moves the cursor past the string under it. Triple quoted strings end at a triple quote, every other
     * string ends at the next unescaped quote of the same kind. An unterminated string ends the scan - such
     * a source does not compile anyway, and the parse will report it.
     *
     * <p>Slashy strings are not recognised on purpose: reading {@code /} as division can only turn string
     * content into suspicious looking code, which costs a parse and never hides a declaration.
     */
    private void skipString() {
        char quote = source.charAt(position);
        boolean spansLines = atTripleQuote();
        String terminator = spansLines ? String.valueOf(quote).repeat(3) : String.valueOf(quote);
        position += terminator.length();

        while (hasMore()) {
            char current = source.charAt(position);
            if (current == '\\') {
                position += 2; // an escape hides the next character, whatever it is
            } else if (startsHere(terminator)) {
                position += terminator.length();
                return;
            } else if (!spansLines && isLineBreak(current)) {
                // only a triple quoted string may cross a line, so this quote was never closed. The source
                // is broken, but the rest of the file must not be swallowed - a declaration may follow.
                return;
            } else {
                position++;
            }
        }
    }

    private static boolean isLineBreak(char current) {
        return current == '\n' || current == '\r';
    }

    private boolean atTripleQuote() {
        char quote = source.charAt(position);
        return position + 2 < source.length()
            && source.charAt(position + 1) == quote
            && source.charAt(position + 2) == quote;
    }
}
