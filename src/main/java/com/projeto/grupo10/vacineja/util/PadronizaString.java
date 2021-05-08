package com.projeto.grupo10.vacineja.util;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class PadronizaString {

    public static String padronizaString(String str) {
        str.toLowerCase();
        String StringNormalizadaNFD = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(StringNormalizadaNFD).replaceAll("");
    }

    public static Set<String> padronizaSetsDeString (Set<String> strings) {
        Set<String> setDeStringPadronizadas = new HashSet<String>();
        for (String string : strings) { setDeStringPadronizadas.add(padronizaString(string));}
        return setDeStringPadronizadas;
    }
}
