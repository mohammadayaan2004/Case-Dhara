package com.casedhara.util

object IpcBnsMapper {

    private val ipcToBns: Map<String, String> = mapOf(
        // Offences Against the Human Body
        "299" to "100", "302" to "101", "304" to "105", "304A" to "106",
        "304B" to "80",  "305" to "107", "306" to "108", "307" to "109",
        "308" to "110",  "309" to "226", "312" to "88",  "313" to "89",
        "314" to "90",   "315" to "91",  "316" to "92",  "317" to "93",
        "318" to "94",   "319" to "113", "320" to "114", "321" to "115",
        "322" to "116",  "323" to "115", "324" to "117", "325" to "116",
        "326" to "118",  "326A" to "119","326B" to "120","327" to "121",
        "328" to "122",  "329" to "123", "330" to "124", "331" to "125",
        "332" to "121",  "333" to "123", "334" to "126", "335" to "127",
        "336" to "125",  "337" to "125", "338" to "125", "339" to "128",
        "340" to "129",  "341" to "128", "342" to "129", "343" to "130",
        "344" to "131",  "345" to "132", "346" to "133", "347" to "134",
        "348" to "135",  "349" to "136", "350" to "137", "351" to "351",
        "352" to "138",  "353" to "132",
        // Sexual Offences
        "354" to "74",   "354A" to "75", "354B" to "76", "354C" to "77",
        "354D" to "78",  "355" to "139", "375" to "63",  "376" to "64",
        "376A" to "66",  "376AB" to "65","376B" to "67", "376C" to "68",
        "376D" to "70",  "376E" to "72",
        // Kidnapping / Trafficking
        "359" to "143",  "360" to "144", "361" to "145", "362" to "146",
        "363" to "137",  "363A" to "148","364" to "139", "364A" to "140",
        "365" to "141",  "366" to "142", "366A" to "143","366B" to "144",
        "370" to "143",  "370A" to "144","372" to "150", "373" to "151",
        "374" to "152",
        // Property
        "378" to "303",  "379" to "303", "380" to "305", "381" to "306",
        "382" to "307",  "383" to "308", "384" to "308", "390" to "314",
        "391" to "310",  "392" to "309", "393" to "315", "394" to "316",
        "395" to "310",  "396" to "311", "397" to "317", "398" to "318",
        "399" to "319",  "400" to "320", "401" to "321", "402" to "322",
        "403" to "323",  "404" to "324", "405" to "316", "406" to "316",
        "407" to "317",  "408" to "318", "409" to "319", "415" to "318",
        "416" to "319",  "417" to "316", "418" to "317", "419" to "318",
        "420" to "318",  "425" to "324", "426" to "324",
        // Forgery / Documents
        "463" to "336",  "464" to "337", "465" to "338", "466" to "339",
        "467" to "340",  "468" to "341", "469" to "342", "470" to "343",
        "471" to "344",  "477A" to "351",
        // Marriage
        "494" to "82",   "495" to "83",  "496" to "84",  "498" to "85",
        "498A" to "85",
        // Abetment / Conspiracy
        "107" to "45",   "108" to "46",  "109" to "48",  "110" to "49",
        "120A" to "61",  "120B" to "61",
        // State / Sedition
        "121" to "147",  "121A" to "148","124A" to "152",
        // False evidence
        "191" to "227",  "192" to "228", "193" to "229", "194" to "230",
        "201" to "237",  "211" to "247", "212" to "248",
        // Contempt of authority
        "172" to "209",  "177" to "214", "186" to "221", "188" to "223",
        // Attempt
        "511" to "62"
    )

    private val bnsToIpc: Map<String, String> =
        ipcToBns.entries.associate { (k, v) -> v to k }

    private val descriptions: Map<String, String> = mapOf(
        "302" to "Murder",            "101" to "Murder",
        "375" to "Rape",              "63"  to "Rape",
        "376" to "Punishment for rape","64"  to "Punishment for rape",
        "420" to "Cheating",          "318" to "Cheating",
        "498A" to "Cruelty by husband/relatives", "85" to "Cruelty by husband/relatives",
        "304B" to "Dowry death",      "80"  to "Dowry death",
        "307" to "Attempt to murder", "109" to "Attempt to murder",
        "354" to "Outraging modesty", "74"  to "Outraging modesty",
        "354A" to "Sexual harassment","75"  to "Sexual harassment",
        "354C" to "Voyeurism",        "77"  to "Voyeurism",
        "354D" to "Stalking",         "78"  to "Stalking",
        "326A" to "Acid attack",      "119" to "Acid attack",
        "370" to "Trafficking",       "143" to "Trafficking",
        "120B" to "Criminal conspiracy","61" to "Criminal conspiracy",
        "379" to "Theft",             "303" to "Theft",
        "406" to "Criminal breach of trust","316" to "Criminal breach of trust",
        "465" to "Forgery",           "338" to "Forgery",
        "468" to "Forgery for cheating","341" to "Forgery for cheating",
        "494" to "Bigamy",            "82"  to "Bigamy",
        "511" to "Attempt",           "62"  to "Attempt"
    )

    fun getEquivalent(section: String, fromIpc: Boolean): String? =
        if (fromIpc) ipcToBns[section] else bnsToIpc[section]

    fun getDescription(section: String): String? = descriptions[section]

    fun isIpcSection(section: String): Boolean = ipcToBns.containsKey(section)

    fun isBnsSection(section: String): Boolean = bnsToIpc.containsKey(section)

    fun formatMapping(section: String, fromIpc: Boolean): String {
        val equivalent = getEquivalent(section, fromIpc)
            ?: return if (fromIpc) "IPC § $section" else "BNS § $section"
        val desc = getDescription(section) ?: getDescription(equivalent) ?: ""
        val suffix = if (desc.isNotEmpty()) " ($desc)" else ""
        return if (fromIpc) "IPC § $section → BNS § $equivalent$suffix"
               else         "BNS § $section → IPC § $equivalent$suffix"
    }
}
