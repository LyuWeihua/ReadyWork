/**
 *
 * Original work Copyright jetty
 * Modified Copyright (c) 2020 WeiHua Lyu [ready.work]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package work.ready.core.tools;

import java.io.IOException;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

public class QuotedStringTokenizer extends StringTokenizer
{
    private static final String __delim = "\t\n\r";
    private String _string;
    private String _delim = __delim;
    private boolean _returnQuotes = false;
    private boolean _returnDelimiters = false;
    private StringBuffer _token;
    private boolean _hasToken = false;
    private int _i = 0;
    private int _lastStart = 0;
    private boolean _double = true;
    private boolean _single = true;

    public QuotedStringTokenizer(String str,
                                 String delim,
                                 boolean returnDelimiters,
                                 boolean returnQuotes)
    {
        super("");
        _string = str;
        if (delim != null)
            _delim = delim;
        _returnDelimiters = returnDelimiters;
        _returnQuotes = returnQuotes;

        if (_delim.indexOf('\'') >= 0 ||
                _delim.indexOf('"') >= 0)
            throw new Error("Can't use quotes as delimiters: " + _delim);

        _token = new StringBuffer(_string.length() > 1024 ? 512 : _string.length() / 2);
    }

    public QuotedStringTokenizer(String str,
                                 String delim,
                                 boolean returnDelimiters)
    {
        this(str, delim, returnDelimiters, false);
    }

    public QuotedStringTokenizer(String str,
                                 String delim)
    {
        this(str, delim, false, false);
    }

    public QuotedStringTokenizer(String str)
    {
        this(str, null, false, false);
    }

    @Override
    public boolean hasMoreTokens()
    {
        
        if (_hasToken)
            return true;

        _lastStart = _i;

        int state = 0;
        boolean escape = false;
        while (_i < _string.length())
        {
            char c = _string.charAt(_i++);

            switch (state)
            {
                case 0: 
                    if (_delim.indexOf(c) >= 0)
                    {
                        if (_returnDelimiters)
                        {
                            _token.append(c);
                            return _hasToken = true;
                        }
                    }
                    else if (c == '\'' && _single)
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        state = 2;
                    }
                    else if (c == '\"' && _double)
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        state = 3;
                    }
                    else
                    {
                        _token.append(c);
                        _hasToken = true;
                        state = 1;
                    }
                    break;

                case 1: 
                    _hasToken = true;
                    if (_delim.indexOf(c) >= 0)
                    {
                        if (_returnDelimiters)
                            _i--;
                        return _hasToken;
                    }
                    else if (c == '\'' && _single)
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        state = 2;
                    }
                    else if (c == '\"' && _double)
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        state = 3;
                    }
                    else
                    {
                        _token.append(c);
                    }
                    break;

                case 2: 
                    _hasToken = true;
                    if (escape)
                    {
                        escape = false;
                        _token.append(c);
                    }
                    else if (c == '\'')
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        state = 1;
                    }
                    else if (c == '\\')
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        escape = true;
                    }
                    else
                    {
                        _token.append(c);
                    }
                    break;

                case 3: 
                    _hasToken = true;
                    if (escape)
                    {
                        escape = false;
                        _token.append(c);
                    }
                    else if (c == '\"')
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        state = 1;
                    }
                    else if (c == '\\')
                    {
                        if (_returnQuotes)
                            _token.append(c);
                        escape = true;
                    }
                    else
                    {
                        _token.append(c);
                    }
                    break;

                default:
                    throw new IllegalStateException();
            }
        }

        return _hasToken;
    }

    @Override
    public String nextToken()
            throws NoSuchElementException
    {
        if (!hasMoreTokens() || _token == null)
            throw new NoSuchElementException();
        String t = _token.toString();
        _token.setLength(0);
        _hasToken = false;
        return t;
    }

    @Override
    public String nextToken(String delim)
            throws NoSuchElementException
    {
        _delim = delim;
        _i = _lastStart;
        _token.setLength(0);
        _hasToken = false;
        return nextToken();
    }

    @Override
    public boolean hasMoreElements()
    {
        return hasMoreTokens();
    }

    @Override
    public Object nextElement()
            throws NoSuchElementException
    {
        return nextToken();
    }

    @Override
    public int countTokens()
    {
        return -1;
    }

    public static String quoteIfNeeded(String s, String delim)
    {
        if (s == null)
            return null;
        if (s.length() == 0)
            return "\"\"";

        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            if (c == '\\' || c == '"' || c == '\'' || Character.isWhitespace(c) || delim.indexOf(c) >= 0)
            {
                StringBuffer b = new StringBuffer(s.length() + 8);
                quote(b, s);
                return b.toString();
            }
        }

        return s;
    }

    public static void quoteIfNeeded(StringBuilder buf, String str, String delim)
    {
        if (str == null)
            return;
        
        int len = str.length();
        if (len == 0)
            return;

        int ch;
        for (int i = 0; i < len; i++)
        {
            ch = str.codePointAt(i);
            if (delim.indexOf(ch) >= 0)
            {
                
                quote(buf, str);
                return;
            }
        }

        buf.append(str);
    }

    public static String quote(String s)
    {
        if (s == null)
            return null;
        if (s.length() == 0)
            return "\"\"";

        StringBuffer b = new StringBuffer(s.length() + 8);
        quote(b, s);
        return b.toString();
    }

    private static final char[] escapes = new char[32];

    static
    {
        Arrays.fill(escapes, (char)0xFFFF);
        escapes['\b'] = 'b';
        escapes['\t'] = 't';
        escapes['\n'] = 'n';
        escapes['\f'] = 'f';
        escapes['\r'] = 'r';
    }

    public static void quote(Appendable buffer, String input)
    {
        if (input == null)
            return;

        try
        {
            buffer.append('"');
            for (int i = 0; i < input.length(); ++i)
            {
                char c = input.charAt(i);
                if (c >= 32)
                {
                    if (c == '"' || c == '\\')
                        buffer.append('\\');
                    buffer.append(c);
                }
                else
                {
                    char escape = escapes[c];
                    if (escape == 0xFFFF)
                    {
                        
                        buffer.append('\\').append('u').append('0').append('0');
                        if (c < 0x10)
                            buffer.append('0');
                        buffer.append(Integer.toString(c, 16));
                    }
                    else
                    {
                        buffer.append('\\').append(escape);
                    }
                }
            }
            buffer.append('"');
        }
        catch (IOException x)
        {
            throw new RuntimeException(x);
        }
    }

    public static void quoteOnly(Appendable buffer, String input)
    {
        if (input == null)
            return;

        try
        {
            buffer.append('"');
            for (int i = 0; i < input.length(); ++i)
            {
                char c = input.charAt(i);
                if (c == '"' || c == '\\')
                    buffer.append('\\');
                buffer.append(c);
            }
            buffer.append('"');
        }
        catch (IOException x)
        {
            throw new RuntimeException(x);
        }
    }

    public static String unquoteOnly(String s)
    {
        return unquoteOnly(s, false);
    }

    public static String unquoteOnly(String s, boolean lenient)
    {
        if (s == null)
            return null;
        if (s.length() < 2)
            return s;

        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (first != last || (first != '"' && first != '\''))
            return s;

        StringBuilder b = new StringBuilder(s.length() - 2);
        boolean escape = false;
        for (int i = 1; i < s.length() - 1; i++)
        {
            char c = s.charAt(i);

            if (escape)
            {
                escape = false;
                if (lenient && !isValidEscaping(c))
                {
                    b.append('\\');
                }
                b.append(c);
            }
            else if (c == '\\')
            {
                escape = true;
            }
            else
            {
                b.append(c);
            }
        }

        return b.toString();
    }

    public static String unquote(String s)
    {
        return unquote(s, false);
    }

    public static String unquote(String s, boolean lenient)
    {
        if (s == null)
            return null;
        if (s.length() < 2)
            return s;

        char first = s.charAt(0);
        char last = s.charAt(s.length() - 1);
        if (first != last || (first != '"' && first != '\''))
            return s;

        StringBuilder b = new StringBuilder(s.length() - 2);
        boolean escape = false;
        for (int i = 1; i < s.length() - 1; i++)
        {
            char c = s.charAt(i);

            if (escape)
            {
                escape = false;
                switch (c)
                {
                    case 'n':
                        b.append('\n');
                        break;
                    case 'r':
                        b.append('\r');
                        break;
                    case 't':
                        b.append('\t');
                        break;
                    case 'f':
                        b.append('\f');
                        break;
                    case 'b':
                        b.append('\b');
                        break;
                    case '\\':
                        b.append('\\');
                        break;
                    case '/':
                        b.append('/');
                        break;
                    case '"':
                        b.append('"');
                        break;
                    case 'u':
                        b.append((char)(
                                        (HashUtil.convertHexDigit((byte)s.charAt(i++)) << 24) +
                                                (HashUtil.convertHexDigit((byte)s.charAt(i++)) << 16) +
                                                (HashUtil.convertHexDigit((byte)s.charAt(i++)) << 8) +
                                                (HashUtil.convertHexDigit((byte)s.charAt(i++)))
                                )
                        );
                        break;
                    default:
                        if (lenient && !isValidEscaping(c))
                        {
                            b.append('\\');
                        }
                        b.append(c);
                }
            }
            else if (c == '\\')
            {
                escape = true;
            }
            else
            {
                b.append(c);
            }
        }

        return b.toString();
    }

    private static boolean isValidEscaping(char c)
    {
        return ((c == 'n') || (c == 'r') || (c == 't') ||
                (c == 'f') || (c == 'b') || (c == '\\') ||
                (c == '/') || (c == '"') || (c == 'u'));
    }

    public static boolean isQuoted(String s)
    {
        return s != null && s.length() > 0 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"';
    }

    public boolean getDouble()
    {
        return _double;
    }

    public void setDouble(boolean d)
    {
        _double = d;
    }

    public boolean getSingle()
    {
        return _single;
    }

    public void setSingle(boolean single)
    {
        _single = single;
    }
}
