/*
 * Copyright 2022 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.j2cl.benchmarks.jre.helper;

/** Benchmark xplat RegExp matching performance. */
public final class RegexConstants {
  private RegexConstants() {}

  public static final String ALL_ALPHANUMERIC_PATTERN = "^[a-zA-Z0-9]+$";

  public static final String LOWERCASE_ALPHANUMERIC_PATTERN = "^[a-z0-9]+$";

  // 20 chars
  public static final String SHORT = "qzxR51WVl7P1M1Uaiipw";
  // 100 chars
  public static final String MEDIUM =
      "ugbpYrrar1EhcJp8DEoWpfqCpBDwrVUIt2F95B3uFNNvBJr771dJMMY5NE0WmXnWg2M3tjarLizZyrFJDLE8lId1CP8q"
          + "SlHiJLUt";
  // 400 chars
  public static final String LONG =
      "koPmzkwKHd6HzEfZ5QHKNhKsydkzaKJjyxDAS74e8DjsW77shQigaJDJSRTG4psaIh1JSW7zBRMRAsk72ZlmjoiHW5pc"
          + "qwH7pdGjHydHAHhgouNoP0aMdW1uiIpx7u6XA49GSw2ByW5ndPRYghkYAQ338GrUKats4sFf1yIxO96N1OYIeG"
          + "s1fmce5l018gis6D9yujN6GuXEzM4VWxGY7POd3EKJjtZuSUWEVD7KMdjggAr0OqhVUl23U8WDL2T4221kAhiP"
          + "TqgVKFjjn4J7eNDA2JhRGzG5jh6kOFnkXqYDRhb6BlRRTOdLCmWD0uw4Ncfj5TxhlLdSlHQYg0fTwAv9rlpOzc"
          + "9baUbuJonGltWdvYt5o7xmn3Zzq5gkAnz5JQKbw5wF9vqzNuWt";
  // 999 chars
  public static final String EXTRA_LONG =
      "3Cuia5asBJjZL6XpFy9yngPVrOc76EsZwuR2MLxjRPkNhUjwD5K97XFQnR40w2li0I1fMQHv12HRVLoDTUhj40A6YnTG"
          + "nqNmL6M6YzXpr9UbPHQIpkeOM5r3Yi5h3Y8dwRsvWniZ3K3ZEAeNcuI9K3LpF8aMqDDPkmCKMF3cQmun3uc8tv"
          + "vE4mqe7bnNM6qO0HbtWhS4pjt1JDsuYcxjroWDvotlujewc6uhhB3lNvbOzZmPjZRn26egJD06ORlk45Uzyrfk"
          + "dSCdBdWmNWQzLPf5kKpY8H8kkLQvP4MeMtOtNtlftuPWn7GA9zRJxYXZEKsy8Mo2sPSKEbPraQ5w3eXqH7JXov"
          + "CALPUrLgaat8VwJkP86fyowlnAQdHFJa2kQaIn0yAJDuoj5fsJG4cvNzBcUaH2ox1vRBOyGUQeLncB222mvO80"
          + "KLUmPZEFl3KQQczSQisVCKI2cq63GjsHC5PbM7kk3Sbhz361fncIUgYnAg1W9qzIAKCH8FlkHgHWUmgggXvIml"
          + "dN8t0iiE5QubpJAN6ofwUWN5YnDrySKzenMIDUidXzsc19idIG81PlFm8k887C7aDjG8LpOyCAdIYLz2y5WEsL"
          + "aGZTxqFQV7vgUNIl9lBrGHjmy6UnUD7YWLS9XJHNCv87CmfSGwpKB0ijgGZ7chwzmyyCM0ElfXD2BQgBmRtPaQ"
          + "1NkfxztJi1ukj9s9aQ9PCkZGx5JnnsdWMHJyCF7ES9LDWWVsO5QSpYZfV59r86TccAZyc5oRaUIaI5ZttA6owN"
          + "4F7doGrSWsRqjxP8OQDzt4iTARyAc3kh8iGDfjG8vow26fFqmNv0DHy55X3uPJtLtEtFYWpKaEPXbInrjPWnKn"
          + "knUsi51LJ5eEAkH9UZzOfr9b4Xg5ucay0deqh75x6HTbdTU2vfOBN1q2NCihMXNASyRe96U3FZswMnLTwsjTFi"
          + "9gWoYW4mBJnkWYiERVjU9WdoOA1zD7wAkRs8oiWBf2FAFHI";

  // 20 chars, non-matching @ char 11
  public static final String SHORT_NON_MATCHING = "qzxR51WVl7_P1M1Uaiip";
  // 100 chars, non-matching @ char 11
  public static final String MEDIUM_NON_MATCHING =
      "ugbpYrrar1_EhcJp8DEoWpfqCpBDwrVUIt2F95B3uFNNvBJr771dJMMY5NE0WmXnWg2M3tjarLizZyrFJDLE8lId1CP8"
          + "qSlHiJLU";
  // 400 chars, non-matching @ char 11
  public static final String LONG_NON_MATCHING =
      "koPmzkwKHd_6HzEfZ5QHKNhKsydkzaKJjyxDAS74e8DjsW77shQigaJDJSRTG4psaIh1JSW7zBRMRAsk72ZlmjoiHW5p"
          + "cqwH7pdGjHydHAHhgouNoP0aMdW1uiIpx7u6XA49GSw2ByW5ndPRYghkYAQ338GrUKats4sFf1yIxO96N1OYIe"
          + "Gs1fmce5l018gis6D9yujN6GuXEzM4VWxGY7POd3EKJjtZuSUWEVD7KMdjggAr0OqhVUl23U8WDL2T4221kAhi"
          + "PTqgVKFjjn4J7eNDA2JhRGzG5jh6kOFnkXqYDRhb6BlRRTOdLCmWD0uw4Ncfj5TxhlLdSlHQYg0fTwAv9rlpOz"
          + "c9baUbuJonGltWdvYt5o7xmn3Zzq5gkAnz5JQKbw5wF9vqzNuW";
  // 999 chars, non-matching @ char 11
  public static final String EXTRA_LONG_NON_MATCHING =
      "3Cuia5asBJ_jZL6XpFy9yngPVrOc76EsZwuR2MLxjRPkNhUjwD5K97XFQnR40w2li0I1fMQHv12HRVLoDTUhj40A6YnT"
          + "GnqNmL6M6YzXpr9UbPHQIpkeOM5r3Yi5h3Y8dwRsvWniZ3K3ZEAeNcuI9K3LpF8aMqDDPkmCKMF3cQmun3uc8t"
          + "vvE4mqe7bnNM6qO0HbtWhS4pjt1JDsuYcxjroWDvotlujewc6uhhB3lNvbOzZmPjZRn26egJD06ORlk45Uzyrf"
          + "kdSCdBdWmNWQzLPf5kKpY8H8kkLQvP4MeMtOtNtlftuPWn7GA9zRJxYXZEKsy8Mo2sPSKEbPraQ5w3eXqH7JXo"
          + "vCALPUrLgaat8VwJkP86fyowlnAQdHFJa2kQaIn0yAJDuoj5fsJG4cvNzBcUaH2ox1vRBOyGUQeLncB222mvO8"
          + "0KLUmPZEFl3KQQczSQisVCKI2cq63GjsHC5PbM7kk3Sbhz361fncIUgYnAg1W9qzIAKCH8FlkHgHWUmgggXvIm"
          + "ldN8t0iiE5QubpJAN6ofwUWN5YnDrySKzenMIDUidXzsc19idIG81PlFm8k887C7aDjG8LpOyCAdIYLz2y5WEs"
          + "LaGZTxqFQV7vgUNIl9lBrGHjmy6UnUD7YWLS9XJHNCv87CmfSGwpKB0ijgGZ7chwzmyyCM0ElfXD2BQgBmRtPa"
          + "Q1NkfxztJi1ukj9s9aQ9PCkZGx5JnnsdWMHJyCF7ES9LDWWVsO5QSpYZfV59r86TccAZyc5oRaUIaI5ZttA6ow"
          + "N4F7doGrSWsRqjxP8OQDzt4iTARyAc3kh8iGDfjG8vow26fFqmNv0DHy55X3uPJtLtEtFYWpKaEPXbInrjPWnK"
          + "nknUsi51LJ5eEAkH9UZzOfr9b4Xg5ucay0deqh75x6HTbdTU2vfOBN1q2NCihMXNASyRe96U3FZswMnLTwsjTF"
          + "i9gWoYW4mBJnkWYiERVjU9WdoOA1zD7wAkRs8oiWBf2FAFH";
}
