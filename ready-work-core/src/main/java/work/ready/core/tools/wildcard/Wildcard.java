/**
 *
 * Original work Copyright Spring
 * Modified work Copyright (c) 2020 WeiHua Lyu [ready.work]
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
package work.ready.core.tools.wildcard;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Wildcard {

    private static final WildcardRule REGEX_QUESTION_MARK_RULE = new WildcardRule("?", ".");
    private static final WildcardRule REGEX_STAR_RULE = new WildcardRule("*", ".*");
    private static final WildcardRules DEFAULT_REGEX_RULES = new WildcardRules(new HashSet<>(Arrays.asList(REGEX_QUESTION_MARK_RULE, REGEX_STAR_RULE)));

    private static final WildcardRule SQL_QUESTION_MARK_RULE = new WildcardRule("?", "_");
    private static final WildcardRule SQL_STAR_RULE = new WildcardRule("*", "%");
    private static final WildcardRules DEFAULT_SQL_RULES = new WildcardRules(new HashSet<>(Arrays.asList(SQL_QUESTION_MARK_RULE, SQL_STAR_RULE)));

    private Wildcard() {
        throw new IllegalStateException("JWildcard is a utility class, and can't be instantiated");
    }

    public static String wildcardToRegex(final String wildcard) {
        return wildcardToRegex(wildcard, DEFAULT_REGEX_RULES, true);
    }

    public static String wildcardToRegex(final String wildcard, boolean strict) {
        return wildcardToRegex(wildcard, DEFAULT_REGEX_RULES, strict);
    }

    public static String wildcardToRegex(final String wildcard, final WildcardRules rules, boolean strict) {
        return JWildcardToRegex.wildcardToRegex(wildcard, rules, strict);
    }

    public static String wildcardToSqlPattern(final String wildcard) {
        return JWildcardToSql.wildcardToSqlPattern(wildcard, DEFAULT_SQL_RULES);
    }

    public static boolean matches(String wildcard, String text) {
        if(text == null) {
            throw new IllegalArgumentException("Text must not be null");
        }

        Pattern pattern = Pattern.compile(wildcardToRegex(wildcard));
        Matcher matcher = pattern.matcher(text);
        return matcher.matches();
    }

    private static class JWildcardToSql {
        private static String wildcardToSqlPattern(final String wildcard, final WildcardRules rules) {

            List<JWildcardRuleWithIndex> listOfOccurrences = getContainedWildcardPairsOrdered(wildcard, rules);
            return getSqlString(wildcard, listOfOccurrences);
        }

        private static String getSqlString(String wildcard, List<JWildcardRuleWithIndex> listOfOccurrences) {
            StringBuilder sql = new StringBuilder();
            int cursor = 0;
            for (JWildcardRuleWithIndex jWildcardRuleWithIndex : listOfOccurrences) {
                int index = jWildcardRuleWithIndex.getIndex();
                if (index != 0) {
                    sql.append(wildcard.substring(cursor, index));
                }
                sql.append(jWildcardRuleWithIndex.getRule().getTarget());
                cursor = index + jWildcardRuleWithIndex.getRule().getSource().length();
            }

            if (cursor <= wildcard.length() - 1) {
                sql.append(wildcard.substring(cursor, wildcard.length()));
            }

            return sql.toString();
        }
    }

    private static class JWildcardToRegex {

        private static String wildcardToRegex(final String wildcard, final WildcardRules rules, boolean strict) {
            if (wildcard == null) {
                throw new IllegalArgumentException("Wildcard must not be null");
            }

            if (rules == null) {
                throw new IllegalArgumentException("Rules must not be null");
            }

            List<JWildcardRuleWithIndex> listOfOccurrences = getContainedWildcardPairsOrdered(wildcard, rules);
            String regex = getRegexString(wildcard, listOfOccurrences);

            if (strict) {
                return "^" + regex + "$";
            } else {
                return regex;
            }
        }

        private static String getRegexString(String wildcard, List<JWildcardRuleWithIndex> listOfOccurrences) {
            StringBuilder regex = new StringBuilder();
            int cursor = 0;
            for (JWildcardRuleWithIndex jWildcardRuleWithIndex : listOfOccurrences) {
                int index = jWildcardRuleWithIndex.getIndex();
                if (index != 0) {
                    regex.append(Pattern.quote(wildcard.substring(cursor, index)));
                }
                regex.append(jWildcardRuleWithIndex.getRule().getTarget());
                cursor = index + jWildcardRuleWithIndex.getRule().getSource().length();
            }

            if (cursor <= wildcard.length() - 1) {
                regex.append(Pattern.quote(wildcard.substring(cursor, wildcard.length())));
            }
            return regex.toString();
        }
    }

    private static List<JWildcardRuleWithIndex> getContainedWildcardPairsOrdered(final String wildcard, final WildcardRules rules) {
        List<JWildcardRuleWithIndex> listOfOccurrences = new LinkedList<>();
        for (WildcardRule wildcardRuleWithIndex : rules.getRules()) {
            int index = -1;
            do {
                index = wildcard.indexOf(wildcardRuleWithIndex.getSource(), index + 1);
                if (index > -1) {
                    listOfOccurrences.add(new JWildcardRuleWithIndex(wildcardRuleWithIndex, index));
                }
            } while (index > -1);
        }

        listOfOccurrences.sort((o1, o2) -> {
            if (o1.getIndex() == o2.getIndex()) {
                return 0;
            }

            return o1.getIndex() > o2.getIndex() ? 1 : -1;
        });

        return listOfOccurrences;
    }

    private static class JWildcardRuleWithIndex {
        private final WildcardRule rule;
        private final int index;

        JWildcardRuleWithIndex(WildcardRule rule, int index) {
            this.rule = rule;
            this.index = index;
        }

        private WildcardRule getRule() {
            return rule;
        }

        private int getIndex() {
            return index;
        }
    }
}
