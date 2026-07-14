package com.voxia.utils

import com.voxia.brain.Language

object LanguageDetector {

    fun detect(text: String): Language {
        val lower = text.lowercase().trim()
        if (lower.isBlank()) return Language.UNKNOWN

        val frenchWords = setOf("bonjour","salut","bonsoir","merci","oui","non","heure","date",
            "batterie","volume","raconte","blague","drôle","décris","décrire","autour","objet",
            "tiens","quoi","vois","lis","document","lettre","écrit","appelle","appel","calcule",
            "ouvre","lance","arrête","arrêter","répète","répéter","encore","aide","qui","tu",
            "motivation","encourage","français","francais","histoire","fort","plus","moins","bas",
            "réveille","rappelle","silence","passe","donne","fais","dis","mets","que","comment")

        val englishWords = setOf("hello","hi","hey","thanks","yes","no","time","date","battery",
            "volume","tell","story","joke","funny","describe","surroundings","object","what",
            "holding","identify","read","document","text","call","phone","contact","calculate",
            "open","stop","repeat","again","help","who","are","you","motivational","encourage",
            "english","french","switch","up","down","wake","remind","analyze","scan","speak",
            "show","give","set","make")

        val words = lower.split("\\s+".toRegex())
        var fr = 0; var en = 0

        for (w in words) {
            if (w in frenchWords) fr++
            if (w in englishWords) en++
        }

        return when {
            fr > en -> Language.FRENCH
            en > fr -> Language.ENGLISH
            else -> Language.UNKNOWN
        }
    }

    fun isFrench(text: String): Boolean = detect(text) == Language.FRENCH
    fun isEnglish(text: String): Boolean = detect(text) == Language.ENGLISH
}
