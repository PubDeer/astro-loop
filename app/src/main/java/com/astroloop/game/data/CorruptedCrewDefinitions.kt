package com.astroloop.game.data

data class CorruptedPilot(
    val id: String,
    val mirrorsPilotId: String,
    val lines: List<String>
)

object CorruptedCrewDefinitions {
    val pilots = listOf(
        CorruptedPilot("corrupted_1", "pilot_medic", listOf(
            "...vitals... stable? No. Never.",
            "I used to heal. Now I...",
            "The wounds don't close anymore.",
            "Patient zero. That was me.",
            "Nano gel... it burns... always..."
        )),
        CorruptedPilot("corrupted_2", "pilot_rascal", listOf(
            "Shiny... so shiny in here...",
            "Took something. Can't remember what.",
            "The vents... go on forever...",
            "Five-second rule... time doesn't...",
            "Redistributing... what was I...?"
        )),
        CorruptedPilot("corrupted_3", "pilot_brutus", listOf(
            "...hit me. HIT ME. I can't feel it.",
            "Revenge... against what? I forgot.",
            "Strong. I was strong. Wasn't I?",
            "The scars... they're moving.",
            "EVERY HIT... I FEEL NOTHING..."
        )),
        CorruptedPilot("corrupted_4", "pilot_frost", listOf(
            "Temperature... nominal? No. Cold.",
            "Ice doesn't melt here. Nor do I.",
            "Waddle... waddle... can't stop...",
            "Calibration error. Permanent.",
            "Cold never bothered me. Nor this."
        )),
        CorruptedPilot("corrupted_5", "pilot_dash", listOf(
            "Fast... faster... can't stop...",
            "Outran something. It's inside me.",
            "Speed is life. Speed is life. Speed—",
            "My legs won't stop. Won't stop.",
            "Momentum. Forward. Never back."
        )),
        CorruptedPilot("corrupted_6", "pilot_ember", listOf(
            "Rise... from the ashes... which?",
            "The flames won't go out. Please...",
            "Phoenix... reborn too many times.",
            "Burning. Always. Never consumed.",
            "The fire remembers what I can't."
        )),
        CorruptedPilot("corrupted_7", "pilot_fang", listOf(
            "I hear them. In the dark. It's me.",
            "Echolocation shows... nothing. None.",
            "Blood... fuel... same thing now.",
            "Hanging... suspended... between?",
            "The night shift never ended."
        )),
        CorruptedPilot("corrupted_8", "pilot_whiskers", listOf(
            "Knocked it over. It didn't fall.",
            "Nap time was... when? When?",
            "Lucky once. The luck ran out.",
            "If I fits... nothing fits anymore.",
            "Annoying. Everything. Even silence."
        )),
        CorruptedPilot("corrupted_9", "pilot_kraken", listOf(
            "Eight arms. None of them are mine.",
            "The deep void... I AM the void.",
            "Tentacles reaching... for what?",
            "Extra arms. What were they for?",
            "Home. This is home. The void. Home."
        )),
        CorruptedPilot("corrupted_10", "pilot_havoc", listOf(
            "RISK! FUN! ...why isn't it fun?",
            "Full power. No shields. No... none.",
            "Reckless. Efficient. Same thing?",
            "Glass cannon... shattered glass...",
            "They said reckless. They were right."
        )),
        CorruptedPilot("corrupted_11", "pilot_unit7", listOf(
            "Diagnostics... all corrupted.",
            "Survival: ERROR. ERROR. ERROR.",
            "I understand humor now. Not funny.",
            "Duplicating... what am I copying?",
            "All systems... none of them nominal."
        ))
    )
}
